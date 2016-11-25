package fi.oph.koski.tiedonsiirto

import java.sql.Timestamp
import java.time.LocalDateTime

import fi.oph.koski.db.TiedonsiirtoRow
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.json.Json
import fi.oph.koski.json.Json._
import fi.oph.koski.koodisto.KoodistoViitePalvelu
import fi.oph.koski.koskiuser.{KoskiSession, KoskiUserInfo, KoskiUserRepository}
import fi.oph.koski.log.KoskiMessageField._
import fi.oph.koski.log.KoskiOperation._
import fi.oph.koski.log.{AuditLog, AuditLogMessage, Logging}
import fi.oph.koski.oppija.OppijaRepository
import fi.oph.koski.organisaatio.{OrganisaatioHierarkia, OrganisaatioRepository}
import fi.oph.koski.schema._
import fi.oph.koski.util.{DateOrdering, PageInfo}
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.{JValue, _}

class TiedonsiirtoService(tiedonsiirtoRepository: TiedonsiirtoRepository, organisaatioRepository: OrganisaatioRepository, oppijaRepository: OppijaRepository, koodistoviitePalvelu: KoodistoViitePalvelu, userRepository: KoskiUserRepository) extends Logging {
  def haeTiedonsiirrot(query: TiedonsiirtoQuery)(implicit koskiSession: KoskiSession): Either[HttpStatus, Tiedonsiirrot] = {
    AuditLog.log(AuditLogMessage(TIEDONSIIRTO_KATSOMINEN, koskiSession, Map(juuriOrganisaatio -> koskiSession.juuriOrganisaatio.map(_.oid).getOrElse("ei juuriorganisaatiota"))))

    query.oppilaitos match {
      case Some(oppilaitosOid) =>
        def oidPath(oid: String, hierarkia: OrganisaatioHierarkia): List[Organisaatio.Oid] = {
          if (hierarkia.find(oid).isDefined) {
            hierarkia.oid :: hierarkia.children.flatMap(child => oidPath(oid, child))
          } else if (hierarkia.oid == oid) {
            List(oid)
          } else {
            Nil
          }
        }
        val hierarkia: Option[OrganisaatioHierarkia] = organisaatioRepository.getOrganisaatioHierarkiaIncludingParents(oppilaitosOid)
        hierarkia.map(oidPath(oppilaitosOid, _)) match {
          case Some(oids) =>
            val henkilöt: List[HenkilönTiedonsiirrot] = toHenkilönTiedonsiirrot(tiedonsiirtoRepository.find(Some(oids), query.errorsOnly, query.pageInfo))
              .map { siirrot => siirrot.copy(rivit = siirrot.rivit.filter(_.oppilaitos.toList.flatten.map(_.oid).contains(oppilaitosOid))) }
              .filter { siirrot => siirrot.rivit.nonEmpty }
            Right(Tiedonsiirrot(henkilöt, oppilaitos = hierarkia.flatMap(_.find(oppilaitosOid).flatMap(_.toOppilaitos))))
          case None =>
            Left(KoskiErrorCategory.notFound.oppilaitostaEiLöydy())
        }
      case None =>
        Right(Tiedonsiirrot(toHenkilönTiedonsiirrot(tiedonsiirtoRepository.find(None, query.errorsOnly, query.pageInfo)), oppilaitos = None))
    }
  }

  def virheelliset(query: TiedonsiirtoQuery)(implicit koskiSession: KoskiSession): Either[HttpStatus, Tiedonsiirrot] = {
    haeTiedonsiirrot(query).right.map { tiedonsiirrot =>
      tiedonsiirrot.copy(henkilöt = tiedonsiirrot.henkilöt
        .filter { siirrot => siirrot.rivit.groupBy(_.oppilaitos).exists { case (_, rivit) => rivit.headOption.exists(_.virhe.isDefined) } }
        .map(v => v.copy(rivit = v.rivit.filter(_.virhe.isDefined)))
      )
    }
  }


  def storeTiedonsiirtoResult(implicit koskiSession: KoskiSession, oppijaOid: Option[OidHenkilö], validatedOppija: Option[Oppija], data: Option[JValue], error: Option[TiedonsiirtoError]) {
    if (!koskiSession.isPalvelukäyttäjä && !koskiSession.isRoot) {
      return
    }

    val oppija = data.flatMap(extractHenkilö(_, oppijaOid))
    val lahdejarjestelma = data.flatMap(extractLahdejarjestelma)
    val oppilaitokset = data.map(_ \ "opiskeluoikeudet" \ "oppilaitos" \ "oid").map(jsonStringList).map(_.flatMap(organisaatioRepository.getOrganisaatio)).map(toJValue)
    val koulutustoimija: Option[Koulutustoimija] = validatedOppija.flatMap(_.opiskeluoikeudet.headOption.flatMap(_.koulutustoimija))

    val juuriOrganisaatio = if (koskiSession.isRoot) koulutustoimija else koskiSession.juuriOrganisaatio

    juuriOrganisaatio.foreach(org => tiedonsiirtoRepository.create(koskiSession.oid, org.oid, oppija, oppilaitokset, error, lahdejarjestelma))
  }

  def yhteenveto(implicit koskiSession: KoskiSession): Seq[TiedonsiirtoYhteenveto] = {
    def getOrganisaatio(oid: String) = {
      organisaatioRepository.getOrganisaatio(oid) match {
        case s@Some(org) => s
        case None =>
          logger.warn(s"Organisaatiota $oid ei löydy organisaatiopalvelusta.")
          None
      }
    }
    tiedonsiirtoRepository.yhteenveto(koskiSession).par.flatMap { row =>
      val käyttäjä = userRepository.findByOid(row.kayttaja) getOrElse {
        logger.warn(s"Käyttäjää ${row.kayttaja} ei löydy henkilöpalvelusta")
        KoskiUserInfo(row.kayttaja, None, None)
      }
      (getOrganisaatio(row.tallentajaOrganisaatio), getOrganisaatio(row.oppilaitos)) match {
        case (Some(tallentajaOrganisaatio), Some(oppilaitos)) =>
          val lähdejärjestelmä = row.lahdejarjestelma.flatMap(koodistoviitePalvelu.getKoodistoKoodiViite("lahdejarjestelma", _))
          Some(TiedonsiirtoYhteenveto(tallentajaOrganisaatio, oppilaitos, käyttäjä, row.viimeisin, row.siirretyt, row.virheet, row.opiskeluoikeudet.getOrElse(0), lähdejärjestelmä))
        case _ =>
          None
      }
    }.toList
     .sortBy(_.oppilaitos.nimi.map(_.get("fi")))
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
      case (Some(oid), None) => oppijaRepository.findByOid(oid).map(_.toHenkilötiedotJaOid)
      case (None, Some(hetu)) => oppijaRepository.findOppijat(hetu).headOption
      case _ => None
    }
    haetutTiedot.map(toJValue).orElse(oidHenkilö match {
      case Some(oidHenkilö) => Some(annetutHenkilötiedot.merge(toJValue(oidHenkilö)))
      case None => annetutHenkilötiedot.toOption
    })
  }

  private def toHenkilönTiedonsiirrot(tiedonsiirrot: Seq[TiedonsiirtoRow]): List[HenkilönTiedonsiirrot] = {
    implicit val ordering = DateOrdering.localDateTimeReverseOrdering
    tiedonsiirrot.groupBy { t =>
      val oppijanTunniste = t.oppija.map(Json.fromJValue[HetuTaiOid])
      oppijanTunniste.flatMap(_.hetu).orElse(oppijanTunniste.map(_.oid))
    }.map {
      case (_, rows) =>
        val oppija = rows.head.oppija.flatMap(_.extractOpt[Henkilö])
        val rivit = rows.map { row =>
          val oppilaitos = row.oppilaitos.flatMap(_.extractOpt[List[OrganisaatioWithOid]])
          TiedonsiirtoRivi(row.id, row.aikaleima.toLocalDateTime, oppija, oppilaitos, row.virheet, row.data, row.lahdejarjestelma)
        }
        HenkilönTiedonsiirrot(oppija, rivit.sortBy(_.aika))
    }.toList.sortBy(_.rivit.head.aika)
  }
}

case class Tiedonsiirrot(henkilöt: List[HenkilönTiedonsiirrot], oppilaitos: Option[Oppilaitos])
case class HenkilönTiedonsiirrot(oppija: Option[Henkilö], rivit: Seq[TiedonsiirtoRivi])
case class TiedonsiirtoRivi(id: Int, aika: LocalDateTime, oppija: Option[Henkilö], oppilaitos: Option[List[OrganisaatioWithOid]], virhe: Option[AnyRef], inputData: Option[AnyRef], lähdejärjestelmä: Option[String])
case class Henkilö(oid: Option[String], hetu: Option[String], etunimet: Option[String], kutsumanimi: Option[String], sukunimi: Option[String], äidinkieli: Option[Koodistokoodiviite])
case class HetuTaiOid(oid: Option[String], hetu: Option[String])
case class TiedonsiirtoYhteenveto(tallentajaOrganisaatio: OrganisaatioWithOid, oppilaitos: OrganisaatioWithOid, käyttäjä: KoskiUserInfo, viimeisin: Timestamp, siirretyt: Int, virheelliset: Int, opiskeluoikeudet: Int, lähdejärjestelmä: Option[Koodistokoodiviite])
case class TiedonsiirtoQuery(oppilaitos: Option[String], errorsOnly: Boolean, pageInfo: PageInfo)
case class TiedonsiirtoKäyttäjä(oid: String, nimi: Option[String])