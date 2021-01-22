package fi.oph.koski.opiskeluoikeus

import java.sql.SQLException

import fi.oph.koski.db.KoskiDatabase.DB
import fi.oph.koski.db.PostgresDriverWithJsonSupport.api._
import fi.oph.koski.db.Tables._
import fi.oph.koski.db._
import fi.oph.koski.henkilo._
import fi.oph.koski.history.{JsonPatchException, OpiskeluoikeusHistory, OpiskeluoikeusHistoryRepository}
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.json.JsonDiff.jsonDiff
import fi.oph.koski.koskiuser.KoskiSession
import fi.oph.koski.log.Logging
import fi.oph.koski.opiskeluoikeus.OpiskeluoikeusChangeValidator.validateOpiskeluoikeusChange
import fi.oph.koski.perustiedot.{OpiskeluoikeudenPerustiedot, PerustiedotSyncRepository}
import fi.oph.koski.schema.Henkilö.Oid
import fi.oph.koski.schema.Opiskeluoikeus.VERSIO_1
import fi.oph.koski.schema._
import org.json4s.jackson.JsonMethods
import org.json4s.{JArray, JObject, JString, JValue}
import slick.dbio
import slick.dbio.DBIOAction.sequence
import slick.dbio.Effect.{Read, Transactional, Write}
import slick.dbio.{DBIOAction, NoStream}

class PostgresOpiskeluoikeusRepository(val db: DB, historyRepository: OpiskeluoikeusHistoryRepository, henkilöCache: KoskiHenkilöCache, oidGenerator: OidGenerator, henkilöRepository: OpintopolkuHenkilöRepository, perustiedotSyncRepository: PerustiedotSyncRepository) extends KoskiOpiskeluoikeusRepository with DatabaseExecutionContext with KoskiDatabaseMethods with Logging {
  override def filterOppijat[A <: HenkilönTunnisteet](oppijat: List[A])(implicit user: KoskiSession): List[A] = {
    val queryOppijaOids = sequence(oppijat.map { o =>
      findByOppijaOidsAction(o.oid :: o.linkitetytOidit).map(opiskeluoikeusOids => (o.oid, opiskeluoikeusOids))
    })

    val oppijatJoillaOpiskeluoikeuksia: Set[Oid] = runDbSync(queryOppijaOids)
      .collect { case (oppija, opiskeluoikeudet) if opiskeluoikeudet.nonEmpty => oppija }
      .toSet

    oppijat.filter { oppija => oppijatJoillaOpiskeluoikeuksia.contains(oppija.oid)}
  }


  override def findByOppijaOids(oids: List[String])(implicit user: KoskiSession): Seq[Opiskeluoikeus] = {
    runDbSync(findByOppijaOidsAction(oids).map(rows => rows.sortBy(_.id).map(_.toOpiskeluoikeus)))
  }

  override def findByCurrentUserOids(oids: List[String])(implicit user: KoskiSession): Seq[Opiskeluoikeus] = {
    assert(oids.contains(user.oid), "Käyttäjän oid: " + user.oid + " ei löydy etsittävän oppijan oideista: " + oids)
    findKansalaisenOpiskeluoikeudet(oids)
  }

  override def findHuollettavaByOppijaOids(oids: List[String])(implicit user: KoskiSession): Seq[Opiskeluoikeus] = {
    assert(oids.exists(user.isUsersHuollettava), "Käyttäjän oid: " + user.oid + " ei löydy etsittävän oppijan oideista: " + oids)
    findKansalaisenOpiskeluoikeudet(oids)
  }

  private def findKansalaisenOpiskeluoikeudet(oids: List[String]) = {
    val query = OpiskeluOikeudet
      .filterNot(_.mitätöity)
      .filter(_.oppijaOid inSetBind oids)

    runDbSync(query.result.map(rows => rows.sortBy(_.id).map(_.toOpiskeluoikeus)))
  }

  override def findByOid(oid: String)(implicit user: KoskiSession): Either[HttpStatus, OpiskeluoikeusRow] = withOidCheck(oid) {
    withExistenceCheck(runDbSync(OpiskeluOikeudetWithAccessCheck.filter(_.oid === oid).result))
  }

  override def getOppijaOidsForOpiskeluoikeus(opiskeluoikeusOid: String)(implicit user: KoskiSession): Either[HttpStatus, List[Oid]] = withOidCheck(opiskeluoikeusOid) {
    withExistenceCheck(runDbSync(OpiskeluOikeudetWithAccessCheck
      .filter(_.oid === opiskeluoikeusOid)
      .flatMap(row => Henkilöt.filter(_.oid === row.oppijaOid))
      .result)).map(henkilö => henkilö.oid :: henkilö.masterOid.toList)
  }

  private def withExistenceCheck[T](things: Iterable[T]): Either[HttpStatus, T] = things.headOption.toRight(KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia())

  private def withOidCheck[T](oid: String)(f: => Either[HttpStatus, T]) = {
    OpiskeluoikeusOid.validateOpiskeluoikeusOid(oid).right.flatMap(_ => f)
  }

  private def oppijaOidsByOppijaOid(oid: String): DBIOAction[List[String], NoStream, Read] = {
    Henkilöt.filter(_.oid === oid).result.map { henkilöt =>
      henkilöt.headOption match {
        case Some(h) =>
          h.oid :: h.masterOid.toList
        case None =>
          throw new RuntimeException(s"Oppija not found: $oid")
      }
    }
  }

  override def createOrUpdate(oppijaOid: PossiblyUnverifiedHenkilöOid, opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus, allowUpdate: Boolean, allowDeleteCompleted: Boolean = false)(implicit user: KoskiSession): Either[HttpStatus, CreateOrUpdateResult] = {
    def createOrUpdateWithRetry: Either[HttpStatus, CreateOrUpdateResult] = {
      val result = try {
        runDbSync {
          (for {
            result <- createOrUpdateAction(oppijaOid, opiskeluoikeus, allowUpdate, allowDeleteCompleted)
            syncAction <- result match {
              case Right(result) if result.changed =>
                syncHenkilötiedotAction(result.id, oppijaOid.oppijaOid, opiskeluoikeus, result.henkilötiedot)
              case _ =>
                DBIO.successful(Unit)
            }
          } yield result).transactionally
        }
      } catch {
        case e: SQLException if e.getSQLState == "23505" => // 23505 = Unique constraint violation
          if (e.getMessage.contains("""duplicate key value violates unique constraint "opiskeluoikeus_oid_key"""")) {
            Left(KoskiErrorCategory.conflict("duplicate oid"))
          } else {
            Left(KoskiErrorCategory.conflict.samanaikainenPäivitys())
          }
        case e: SQLException if e.getMessage.contains("unsupported Unicode escape sequence") =>
          Left(KoskiErrorCategory.badRequest.format.json("unsupported unicode escape sequence in data"))
      }

      if (result.left.exists(_ == KoskiErrorCategory.conflict("duplicate oid"))) {
        createOrUpdateWithRetry
      } else {
        result
      }
    }

    if (!allowUpdate && opiskeluoikeus.oid.isDefined) {
      Left(KoskiErrorCategory.badRequest("Uutta opiskeluoikeutta luotaessa ei hyväksytä arvoja oid-kenttään"))
    } else {
      createOrUpdateWithRetry
    }
  }

  private def findByOppijaOidsAction(oids: List[String])(implicit user: KoskiSession): dbio.DBIOAction[Seq[OpiskeluoikeusRow], NoStream, Read] = {
    OpiskeluOikeudetWithAccessCheck.filter(_.oppijaOid inSetBind oids).result
  }

  private def syncHenkilötiedotAction(id: Int, oppijaOid: String, opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus, henkilötiedot: Option[OppijaHenkilöWithMasterInfo]) = {
    henkilötiedot match {
      case Some(henkilö) =>
        val perustiedot = OpiskeluoikeudenPerustiedot.makePerustiedot(id, opiskeluoikeus, henkilö)
        perustiedotSyncRepository.syncAction(perustiedot, true)
      case None =>
        henkilöCache.getCachedAction(oppijaOid).flatMap {
          case Some(HenkilöRowWithMasterInfo(henkilöRow, masterHenkilöRow)) =>
            val perustiedot = OpiskeluoikeudenPerustiedot.makePerustiedot(id, opiskeluoikeus, henkilöRow, masterHenkilöRow)
            perustiedotSyncRepository.syncAction(perustiedot, true)
          case None =>
            throw new RuntimeException(s"Oppija not found: $oppijaOid")
        }
    }
  }

  def findByIdentifierAction(identifier: OpiskeluoikeusIdentifier)(implicit user: KoskiSession): dbio.DBIOAction[Either[HttpStatus, List[OpiskeluoikeusRow]], NoStream, Read] = {
    identifier match {
      case OpiskeluoikeusByOid(oid) => OpiskeluOikeudetWithAccessCheck.filter(_.oid === oid).result.map { rows =>
        rows.headOption match {
          case Some(oikeus) => Right(List(oikeus))
          case None => Left(KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia("Opiskeluoikeutta " + oid + " ei löydy tai käyttäjällä ei ole oikeutta sen katseluun"))
        }
      }

      case OppijaOidJaLähdejärjestelmänId(oppijaOid, lähdejärjestelmäId) =>
        findOpiskeluoikeudetWithSlaves(oppijaOid).map(_.filter { row =>
          row.toOpiskeluoikeus.lähdejärjestelmänId == Some(lähdejärjestelmäId)
        }).map(_.toList).map(Right(_))

      case i:OppijaOidOrganisaatioJaTyyppi =>
        findOpiskeluoikeudetWithSlaves(i.oppijaOid).map(_.filter { row =>
          val opiskeluoikeus = row.toOpiskeluoikeus
          OppijaOidOrganisaatioJaTyyppi(i.oppijaOid, opiskeluoikeus.getOppilaitos.oid, opiskeluoikeus.koulutustoimija.map(_.oid), opiskeluoikeus.tyyppi.koodiarvo, opiskeluoikeus.lähdejärjestelmänId) == identifier
        }).map(_.toList).map(Right(_))
    }
  }

  private def findOpiskeluoikeudetWithSlaves(oid: String)(implicit user: KoskiSession): dbio.DBIOAction[Seq[OpiskeluoikeusRow], NoStream, Read] =
    (Henkilöt.filter(_.masterOid === oid) ++ Henkilöt.filter(_.oid === oid)).map(_.oid)
      .flatMap(oid => OpiskeluOikeudetWithAccessCheck.filter(_.oppijaOid === oid))
      .result

  private def createOrUpdateAction(oppijaOid: PossiblyUnverifiedHenkilöOid, opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus, allowUpdate: Boolean, allowDeleteCompleted: Boolean = false)(implicit user: KoskiSession): dbio.DBIOAction[Either[HttpStatus, CreateOrUpdateResult], NoStream, Read with Write with Transactional] = {
    findByIdentifierAction(OpiskeluoikeusIdentifier(oppijaOid.oppijaOid, opiskeluoikeus)).flatMap { rows: Either[HttpStatus, List[OpiskeluoikeusRow]] => createOrUpdateActionBasedOnDbResult(oppijaOid, opiskeluoikeus, allowUpdate, allowDeleteCompleted, rows) }
  }

  protected def createOrUpdateActionBasedOnDbResult(oppijaOid: PossiblyUnverifiedHenkilöOid, opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus, allowUpdate: Boolean, allowDeleteCompleted: Boolean, rows: Either[HttpStatus, List[OpiskeluoikeusRow]])(implicit user: KoskiSession): dbio.DBIOAction[Either[HttpStatus, CreateOrUpdateResult], NoStream, Read with Write with Transactional] = (allowUpdate, rows) match {
    case (_, Right(Nil)) => createAction(oppijaOid, opiskeluoikeus)
    case (true, Right(List(vanhaOpiskeluoikeus))) =>
      if (oppijaOid.oppijaOid == vanhaOpiskeluoikeus.oppijaOid) {
        updateAction(vanhaOpiskeluoikeus, opiskeluoikeus, allowDeleteCompleted)
      } else { // Check if oppija oid belongs to master of slave oppija oids
        oppijaOidsByOppijaOid(vanhaOpiskeluoikeus.oppijaOid).flatMap { oids =>
          if (oids.contains(oppijaOid.oppijaOid)) {
            updateAction(vanhaOpiskeluoikeus, opiskeluoikeus)
          } else {
            DBIO.successful(Left(KoskiErrorCategory.forbidden.oppijaOidinMuutos("Oppijan oid: " + oppijaOid.oppijaOid + " ei löydy opiskeluoikeuden oppijan oideista: " + oids.mkString(", "))))
          }
        }
      }
    case (true, Right(rows)) =>
      DBIO.successful(Left(KoskiErrorCategory.internalError(s"Löytyi enemmän kuin yksi rivi päivitettäväksi (${rows.map(_.oid)})")))
    case (false, Right(rows)) =>
      rows.find(!_.toOpiskeluoikeus.tila.opiskeluoikeusjaksot.last.opiskeluoikeusPäättynyt) match {
        case None => createAction(oppijaOid, opiskeluoikeus) // Tehdään uusi opiskeluoikeus, koska vanha on päättynyt
        case Some(_) => DBIO.successful(Left(KoskiErrorCategory.conflict.exists())) // Ei tehdä uutta, koska vanha vastaava opiskeluoikeus on voimassa
      }
    case (_, Left(err)) => DBIO.successful(Left(err))
  }

  protected def createAction(oppijaOid: PossiblyUnverifiedHenkilöOid, opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus)(implicit user: KoskiSession): dbio.DBIOAction[Either[HttpStatus, CreateOrUpdateResult], NoStream, Read with Write] = {
    oppijaOid.verified match {
      case Some(henkilö) =>
        val withMasterInfo = henkilöRepository.withMasterInfo(henkilö)
        henkilöCache.addHenkilöAction(withMasterInfo).flatMap { _ =>
          createAction(withMasterInfo, opiskeluoikeus)
        }
      case None => DBIO.successful(Left(KoskiErrorCategory.notFound.oppijaaEiLöydy("Oppijaa " + oppijaOid.oppijaOid + " ei löydy.")))
    }
  }

  private def createAction(oppija: OppijaHenkilöWithMasterInfo, opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus)(implicit user: KoskiSession): dbio.DBIOAction[Either[HttpStatus, CreateOrUpdateResult], NoStream, Write] = {
    opiskeluoikeus.versionumero match {
      case Some(versio) if (versio != VERSIO_1) =>
        DBIO.successful(Left(KoskiErrorCategory.conflict.versionumero(s"Uudelle opiskeluoikeudelle annettu versionumero $versio")))
      case _ =>
        val tallennettavaOpiskeluoikeus = opiskeluoikeus
        val oid = oidGenerator.generateOid(oppija.henkilö.oid)
        val row: OpiskeluoikeusRow = Tables.OpiskeluoikeusTable.makeInsertableRow(oppija.henkilö.oid, oid, tallennettavaOpiskeluoikeus)
        for {
          opiskeluoikeusId <- Tables.OpiskeluOikeudet.returning(OpiskeluOikeudet.map(_.id)) += row
          diff = JArray(List(JObject("op" -> JString("add"), "path" -> JString(""), "value" -> row.data)))
          _ <- historyRepository.createAction(opiskeluoikeusId, VERSIO_1, user.oid, diff)
        } yield {
          Right(Created(opiskeluoikeusId, oid, opiskeluoikeus.lähdejärjestelmänId, oppija, VERSIO_1, diff, row.data))
        }
    }
  }

  private def updateAction[A <: PäätasonSuoritus](oldRow: OpiskeluoikeusRow, uusiOpiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus, allowDeleteCompletedSuoritukset: Boolean = false)(implicit user: KoskiSession) = {
    val (id, oid, versionumero) = (oldRow.id, oldRow.oid, oldRow.versionumero)
    val nextVersionumero = versionumero + 1

    uusiOpiskeluoikeus.versionumero match {
      case Some(requestedVersionumero) if requestedVersionumero != versionumero =>
        DBIO.successful(Left(KoskiErrorCategory.conflict.versionumero("Annettu versionumero " + requestedVersionumero + " <> " + versionumero)))
      case _ =>
        val vanhaOpiskeluoikeus = oldRow.toOpiskeluoikeus

        val tallennettavaOpiskeluoikeus = OpiskeluoikeusChangeMigrator.migrate(vanhaOpiskeluoikeus, uusiOpiskeluoikeus, allowDeleteCompletedSuoritukset)

        validateOpiskeluoikeusChange(vanhaOpiskeluoikeus, tallennettavaOpiskeluoikeus) match {
          case HttpStatus.ok =>
            val updatedValues@(newData, _, _, _, _, _, _, _, _, _) = Tables.OpiskeluoikeusTable.updatedFieldValues(tallennettavaOpiskeluoikeus, nextVersionumero)
            val diff: JArray = jsonDiff(oldRow.data, newData)
            diff.values.length match {
              case 0 =>
                DBIO.successful(Right(NotChanged(id, oid, uusiOpiskeluoikeus.lähdejärjestelmänId, oldRow.oppijaOid, versionumero, diff, newData)))
              case _ =>
                for {
                  rowsUpdated <- OpiskeluOikeudetWithAccessCheck.filter(_.id === id).map(_.updateableFields).update(updatedValues)
                  _ <- historyRepository.createAction(id, nextVersionumero, user.oid, diff)
                  hist <- historyRepository.findByOpiskeluoikeusOidAction(oid, nextVersionumero)
                } yield {
                  rowsUpdated match {
                    case 1 =>
                      verifyHistoria(newData, hist)
                      Right(Updated(id, oid, uusiOpiskeluoikeus.lähdejärjestelmänId, oldRow.oppijaOid, nextVersionumero, diff, newData, vanhaOpiskeluoikeus))
                    case x: Int =>
                      throw new RuntimeException("Unexpected number of updated rows: " + x) // throw exception to cause rollback!
                  }
                }
            }
          case nonOk => DBIO.successful(Left(nonOk))
        }
    }
  }

  private def verifyHistoria(opiskeluoikeusJson: JValue, hist: Option[OpiskeluoikeusHistory]): Unit =
    hist.flatMap(validateHistoria(opiskeluoikeusJson, _))
        .foreach(logger.warn(_))

  private def validateHistoria(opiskeluoikeusJson: JValue, historia: OpiskeluoikeusHistory): Option[String] = try {
    val opiskeluoikeusDiffHistoria = jsonDiff(opiskeluoikeusJson, historia.asOpiskeluoikeusJson)
    if (opiskeluoikeusDiffHistoria.values.nonEmpty) {
      Some(s"Virhe opiskeluoikeushistoriarivin tuottamisessa opiskeluoikeudelle ${historia.oid}/${historia.version}: ${JsonMethods.pretty(opiskeluoikeusDiffHistoria)}")
    } else {
      None
    }
  } catch {
    case e: JsonPatchException => Some(s"Virhe opiskeluoikeushistorian validoinnissa: ${e.getMessage}")
  }
}
