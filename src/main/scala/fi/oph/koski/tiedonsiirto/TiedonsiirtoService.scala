package fi.oph.koski.tiedonsiirto

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.Date

import fi.oph.koski.date.DateOrdering
import fi.oph.koski.db.KoskiDatabase._
import fi.oph.koski.db.PostgresDriverWithJsonSupport.api._
import fi.oph.koski.db.Tables._
import fi.oph.koski.db.{KoskiDatabaseMethods, Tables, TiedonsiirtoRow}
import fi.oph.koski.elasticsearch.ElasticSearch
import fi.oph.koski.henkilo.HenkilöRepository
import fi.oph.koski.http.Http._
import fi.oph.koski.http.{Http, HttpStatus, HttpStatusException, KoskiErrorCategory}
import fi.oph.koski.json.Json._
import fi.oph.koski.json.{Json, Json4sHttp4s}
import fi.oph.koski.koodisto.KoodistoViitePalvelu
import fi.oph.koski.koskiuser.{KoskiSession, KoskiUserInfo, KoskiUserRepository}
import fi.oph.koski.log.KoskiMessageField._
import fi.oph.koski.log.KoskiOperation._
import fi.oph.koski.log.{AuditLog, AuditLogMessage, Logging}
import fi.oph.koski.organisaatio.OrganisaatioRepository
import fi.oph.koski.perustiedot.OpiskeluoikeudenPerustiedot
import fi.oph.koski.schema._
import fi.oph.koski.servlet.InvalidRequestException
import fi.oph.koski.util._
import io.prometheus.client.Counter
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.{JValue, _}


class TiedonsiirtoService(val db: DB, elasticSearch: ElasticSearch, mailer: TiedonsiirtoFailureMailer, organisaatioRepository: OrganisaatioRepository, henkilöRepository: HenkilöRepository, koodistoviitePalvelu: KoodistoViitePalvelu, userRepository: KoskiUserRepository) extends Logging with Timing with KoskiDatabaseMethods {
  private val tiedonSiirtoVirheet = Counter.build().name("fi_oph_koski_tiedonsiirto_TiedonsiirtoService_virheet").help("Koski tiedonsiirto virheet").register()

  def haeTiedonsiirrot(query: TiedonsiirtoQuery)(implicit koskiSession: KoskiSession): Either[HttpStatus, PaginatedResponse[Tiedonsiirrot]] = {
    def findFromElastic(oppilaitos: Option[String], pageInfo: Option[PaginationSettings])(implicit koskiSession: KoskiSession): Seq[TiedonsiirtoDocument] = timed("findByOrganisaatio") {
      val doc = Json.toJValue(Map(
        "query" -> Map()
      )) // TODO: filter by oppilaitos, do pagination

      try {
        val response = Http.runTask(elasticSearch.http.post(uri"/koski/tiedonsiirto/_search", doc)(Json4sHttp4s.json4sEncoderOf[JValue])(Http.parseJson[JValue]))
        (response \ "hits" \ "hits").extract[List[JValue]].map(j => (j \ "_source").extract[TiedonsiirtoDocument])
      } catch {
        case e: HttpStatusException if e.status == 400 =>
          logger.warn(e.getMessage)
          Nil
      }
    }

    AuditLog.log(AuditLogMessage(TIEDONSIIRTO_KATSOMINEN, koskiSession, Map(juuriOrganisaatio -> koskiSession.juuriOrganisaatio.map(_.oid).getOrElse("ei juuriorganisaatiota"))))

    val rows: Seq[TiedonsiirtoDocument] = findFromElastic(query.oppilaitos, query.paginationSettings)


    val oppilaitosResult = query.oppilaitos match {
      case Some(oppilaitosOid) =>
        val oppilaitos: Option[Oppilaitos] = organisaatioRepository.getOrganisaatioHierarkia(oppilaitosOid).flatMap(_.toOppilaitos)
        oppilaitos match {
          case Some(oppilaitos) => Right(Some(oppilaitos))
          case None => Left(KoskiErrorCategory.notFound.oppilaitostaEiLöydy(s"Oppilaitosta $oppilaitosOid ei löydy"))
        }
      case None =>
        Right(None)
    }

    oppilaitosResult.right.map { oppilaitos =>
      val converted: Tiedonsiirrot = Tiedonsiirrot(toHenkilönTiedonsiirrot(rows), oppilaitos = oppilaitos.map(_.toOidOrganisaatio))
      PaginatedResponse(query.paginationSettings, converted, rows.length)
    }
  }

  def virheelliset(query: TiedonsiirtoQuery)(implicit koskiSession: KoskiSession): Either[HttpStatus, PaginatedResponse[Tiedonsiirrot]] = {
    haeTiedonsiirrot(query.copy(paginationSettings = None)).right.map { tiedonsiirrot =>
      val result = tiedonsiirrot.result.copy(henkilöt = ListPagination.applyPagination(
        query.paginationSettings,
        tiedonsiirrot.result.henkilöt
          .filter { siirrot => siirrot.rivit.groupBy(_.oppilaitos).exists { case (_, rivit) => rivit.headOption.exists(_.virhe.isDefined) } }
          .map(v => v.copy(rivit = v.rivit.filter(_.virhe.isDefined)))
      ).toList)
      PaginatedResponse(query.paginationSettings, result, result.henkilöt.length)
    }
  }

  def storeTiedonsiirtoResult(implicit koskiSession: KoskiSession, oppijaOid: Option[OidHenkilö], validatedOppija: Option[Oppija], data: Option[JValue], error: Option[TiedonsiirtoError]) {
    if (!koskiSession.isPalvelukäyttäjä && !koskiSession.isRoot) {
      return
    }

    val henkilö = data.flatMap(extractHenkilö(_, oppijaOid))
    val lahdejarjestelma: Option[String] = data.flatMap(extractLahdejarjestelma)
    val oppilaitokset: Option[List[OidOrganisaatio]] = data.map(_ \ "opiskeluoikeudet" \ "oppilaitos" \ "oid").map(jsonStringList).map(_.flatMap(organisaatioRepository.getOrganisaatio).map(_.toOidOrganisaatio))
    val koulutustoimija: Option[OidOrganisaatio] = validatedOppija.flatMap(_.opiskeluoikeudet.headOption.flatMap(_.koulutustoimija.map(_.toOidOrganisaatio)))

    val juuriOrganisaatio = if (koskiSession.isRoot) koulutustoimija else koskiSession.juuriOrganisaatio

    juuriOrganisaatio.foreach((org: OrganisaatioWithOid) => {
      val (data: Option[JValue], virheet: Option[JValue]) = error.map(e => (Some(e.data), Some(e.virheet))).getOrElse((None, None))

      runDbSync {
        Tiedonsiirto.map { row => (row.kayttajaOid, row.tallentajaOrganisaatioOid, row.oppija, row.oppilaitos, row.data, row.virheet, row.lahdejarjestelma) } +=
          (koskiSession.oid, org.oid, henkilö, oppilaitokset.map(toJValue), data, virheet, lahdejarjestelma)
      }

      storeToElasticSearch(henkilö, org, oppilaitokset, data, virheet, lahdejarjestelma)

      if (error.isDefined) {
        tiedonSiirtoVirheet.inc
        mailer.sendMail(org.oid)
      }
    })
  }


  private def storeToElasticSearch(henkilö: Option[JValue] /*TODO: why not tiedonsiirtooppija*/, org: OrganisaatioWithOid,
                                   oppilaitokset: Option[List[OidOrganisaatio]], data: Option[JValue],
                                   virheet: Option[JValue], lahdejarjestelma: Option[String])(implicit koskiSession: KoskiSession) = {
    val idValue: String = henkilö.flatMap { henkilö =>
      val hetuTaiOid = henkilö.extract[HetuTaiOid]
      hetuTaiOid.hetu.orElse(hetuTaiOid.oid)
    }.getOrElse("")

    val document = TiedonsiirtoDocument(koskiSession.oid, org.oid, henkilö.map(_.extract[TiedonsiirtoOppija]), oppilaitokset, data, virheet, lahdejarjestelma)

    val jsonLines: Seq[Map[String, Any]] = List(
      Map("update" -> Map("_id" -> (org.oid + "_" + idValue), "_index" -> "koski", "_type" -> "tiedonsiirto")),
      Map("doc_as_upsert" -> true, "doc" -> document)
    )

    val response = Http.runTask(elasticSearch.http.post(uri"/koski/_bulk", jsonLines)(Json4sHttp4s.multiLineJson4sEncoderOf[Map[String, Any]])(Http.parseJson[JValue]))


    val errors = (response \ "errors").extract[Boolean]
    if (errors) {
      val msg = s"Elasticsearch indexing failed: ${Json.writePretty(response)}"
      logger.error(msg)
      Left(KoskiErrorCategory.internalError(msg))
    } else {
      val itemResults = (response \ "items").extract[List[JValue]].map(_ \ "update" \ "_shards" \ "successful").map(_.extract[Int])
      Right(itemResults.sum)
    }
  }

  def yhteenveto(implicit koskiSession: KoskiSession, sorting: SortOrder): Seq[TiedonsiirtoYhteenveto] = {
    def getOrganisaatio(oid: String) = {
      organisaatioRepository.getOrganisaatio(oid) match {
        case s@Some(org) => s
        case None =>
          logger.warn(s"Organisaatiota $oid ei löydy organisaatiopalvelusta.")
          None
      }
    }

    var ordering: Ordering[TiedonsiirtoYhteenveto] = sorting match {
      case order if order.field == "oppilaitos" => Ordering.by(_.oppilaitos.nimi.map(_.get("fi")))
      case order if order.field == "aika" => Ordering.fromLessThan((x, y) => x.viimeisin.isBefore(y.viimeisin))
      case _ => throw new InvalidRequestException(KoskiErrorCategory.badRequest.queryParam("Epäkelpo järjestyskriteeri: order.field"))
    }
    if (sorting.descending) {
      ordering = ordering.reverse
    }

    runDbSync(Tables.TiedonsiirtoYhteenvetoWithAccessCheck(koskiSession).result).par.flatMap { row =>
      val käyttäjä = userRepository.findByOid(row.kayttaja) getOrElse {
        logger.warn(s"Käyttäjää ${row.kayttaja} ei löydy henkilöpalvelusta")
        KoskiUserInfo(row.kayttaja, None, None)
      }
      (getOrganisaatio(row.tallentajaOrganisaatio), getOrganisaatio(row.oppilaitos)) match {
        case (Some(tallentajaOrganisaatio), Some(oppilaitos)) =>
          val lähdejärjestelmä = row.lahdejarjestelma.flatMap(koodistoviitePalvelu.getKoodistoKoodiViite("lahdejarjestelma", _))
          Some(TiedonsiirtoYhteenveto(tallentajaOrganisaatio.toOidOrganisaatio, oppilaitos.toOidOrganisaatio, käyttäjä, row.viimeisin.toLocalDateTime, row.siirretyt, row.virheet, row.opiskeluoikeudet.getOrElse(0), lähdejärjestelmä))
        case _ =>
          None
      }
    }.toList.sorted(ordering)
  }

  private def jsonStringList(value: JValue) = value match {
    case JArray(xs) => xs.collect { case JString(x) => x }
    case JString(x) => List(x)
    case JNothing => Nil
    case JNull => Nil
  }

  private def extractLahdejarjestelma(data: JValue): Option[String] = {
    data \ "opiskeluoikeudet" match {
      case JArray(opiskeluoikeudet) =>
        val lähdejärjestelmä: List[String] = opiskeluoikeudet.flatMap { opiskeluoikeus: JValue =>
          opiskeluoikeus \ "lähdejärjestelmänId" \ "lähdejärjestelmä" \ "koodiarvo" match {
            case JString(lähdejärjestelmä) => Some(lähdejärjestelmä)
            case _ => None
          }
        }
        lähdejärjestelmä.headOption
      case _ => None
    }
  }

  private def extractHenkilö(data: JValue, oidHenkilö: Option[OidHenkilö])(implicit user: KoskiSession): Option[JValue] = {
    val annetutHenkilötiedot: JValue = data \ "henkilö"
    val annettuTunniste: HetuTaiOid = Json.fromJValue[HetuTaiOid](annetutHenkilötiedot)
    val oid: Option[String] = oidHenkilö.map(_.oid).orElse(annettuTunniste.oid)
    val haetutTiedot: Option[HenkilötiedotJaOid] = (oid, annettuTunniste.hetu) match {
      case (Some(oid), None) => henkilöRepository.findByOid(oid).map(_.toHenkilötiedotJaOid)
      case (None, Some(hetu)) => henkilöRepository.findOppijat(hetu).headOption
      case _ => None
    }
    haetutTiedot.map(toJValue).orElse(oidHenkilö match {
      case Some(oidHenkilö) => Some(annetutHenkilötiedot.merge(toJValue(oidHenkilö)))
      case None => annetutHenkilötiedot.toOption
    })
  }

  private def toHenkilönTiedonsiirrot(tiedonsiirrot: Seq[TiedonsiirtoDocument]): List[HenkilönTiedonsiirrot] = {
    tiedonsiirrot.map { row =>
      val rivi = TiedonsiirtoRivi(Math.random().toInt /*TODO tarvitaanko id?*/, row.aikaleima.toLocalDateTime, row.oppija, row.oppilaitokset.getOrElse(Nil), row.virheet, row.data, row.lähdejärjestelmä)
      HenkilönTiedonsiirrot(row.oppija, List(rivi))
    }.toList
  }
/*
  private def toHenkilönTiedonsiirrot(tiedonsiirrot: Seq[TiedonsiirtoRow]): List[HenkilönTiedonsiirrot] = {
    implicit val ordering = DateOrdering.localDateTimeReverseOrdering
    tiedonsiirrot.groupBy { t =>
      val oppijanTunniste = t.oppija.map(Json.fromJValue[HetuTaiOid])
      oppijanTunniste.flatMap(_.oid).orElse(oppijanTunniste.map(_.hetu))
    }.map {
      case (x, rows) =>
        val oppija = rows.head.oppija.flatMap(_.extractOpt[TiedonsiirtoOppija])
        val rivit = rows.map { row =>
          val oppilaitos: List[OidOrganisaatio] = row.oppilaitos.flatMap(_.extractOpt[List[OidOrganisaatio]]).toList.flatten.distinct
          TiedonsiirtoRivi(row.id, row.aikaleima.toLocalDateTime, oppija, oppilaitos, row.virheet, row.data, row.lahdejarjestelma)
        }
        HenkilönTiedonsiirrot(oppija, rivit.sortBy(_.aika))
    }.toList.sortBy(_.rivit.head.aika)
  }*/
}

case class Tiedonsiirrot(henkilöt: List[HenkilönTiedonsiirrot], oppilaitos: Option[OidOrganisaatio])
case class HenkilönTiedonsiirrot(oppija: Option[TiedonsiirtoOppija], rivit: Seq[TiedonsiirtoRivi])
case class TiedonsiirtoRivi(id: Int, aika: LocalDateTime, oppija: Option[TiedonsiirtoOppija], oppilaitos: List[OidOrganisaatio], virhe: Option[AnyRef], inputData: Option[AnyRef], lähdejärjestelmä: Option[String])
case class TiedonsiirtoOppija(oid: Option[String], hetu: Option[String], etunimet: Option[String], kutsumanimi: Option[String], sukunimi: Option[String], äidinkieli: Option[Koodistokoodiviite])
case class HetuTaiOid(oid: Option[String], hetu: Option[String])
case class TiedonsiirtoYhteenveto(tallentajaOrganisaatio: OidOrganisaatio, oppilaitos: OidOrganisaatio, käyttäjä: KoskiUserInfo, viimeisin: LocalDateTime, siirretyt: Int, virheelliset: Int, opiskeluoikeudet: Int, lähdejärjestelmä: Option[Koodistokoodiviite])
case class TiedonsiirtoQuery(oppilaitos: Option[String], paginationSettings: Option[PaginationSettings])
case class TiedonsiirtoKäyttäjä(oid: String, nimi: Option[String])
case class TiedonsiirtoError(data: JValue, virheet: JValue)

case class TiedonsiirtoDocument(tallentajaKäyttäjäOid: String, tallentajaOrganisaatioOid: String, oppija: Option[TiedonsiirtoOppija], oppilaitokset: Option[List[OidOrganisaatio]], data: Option[JValue], virheet: Option[JValue], lähdejärjestelmä: Option[String], aikaleima: Timestamp = new Timestamp(System.currentTimeMillis))