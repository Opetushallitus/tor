package fi.oph.koski.oppija

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.db.GlobalExecutionContext
import fi.oph.koski.henkilo.HenkilöOid
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.json.SensitiveDataFilter
import fi.oph.koski.koskiuser._
import fi.oph.koski.log._
import fi.oph.koski.opiskeluoikeus.OpiskeluoikeusQueries
import fi.oph.koski.schema._
import fi.oph.koski.servlet.RequestDescriber.logSafeDescription
import fi.oph.koski.servlet.{ApiServlet, NoCache}
import fi.oph.koski.tiedonsiirto.TiedonsiirtoError
import fi.oph.koski.util.{Pagination, Timing, WithWarnings, XML}
import fi.oph.koski.virta.{VirtaHakuehtoHetu, VirtaHakuehtoKansallinenOppijanumero}
import javax.servlet.http.HttpServletRequest
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.{JArray, JValue}
import org.scalatra.ContentEncodingSupport

class OppijaServlet(implicit val application: KoskiApplication) extends ApiServlet with Logging with GlobalExecutionContext with OpiskeluoikeusQueries with RequiresVirkailijaOrPalvelukäyttäjä with ContentEncodingSupport with NoCache with Timing with Pagination {

  post("/") { putSingle(false) }

  put("/") { putSingle(true) }

  private def putSingle(allowUpdate: Boolean) = {
    withTracking { withJsonBody { (oppijaJson: JValue) =>
      val validationResult: Either[HttpStatus, Oppija] = application.validator.extractAndValidateOppija(oppijaJson)(koskiSession, AccessType.write, Some(application))
      val result: Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = UpdateContext(koskiSession, application, request).putSingle(validationResult, oppijaJson, allowUpdate)
      renderEither[HenkilönOpiskeluoikeusVersiot](result)
    }(parseErrorHandler = handleUnparseableJson)}
  }

  put("/batch") {
    withTracking { withJsonBody { parsedJson =>
      val putter = UpdateContext(koskiSession, application, request)

      val validationResults: List[(Either[HttpStatus, Oppija], JValue)] = application.validator.extractAndValidateBatch(parsedJson.asInstanceOf[JArray])(koskiSession, AccessType.write)

      val batchResults: List[Either[HttpStatus, HenkilönOpiskeluoikeusVersiot]] = validationResults.par.map { results =>
        putter.putSingle(results._1, results._2, true)
      }.toList

      response.setStatus(batchResults.map {
        case Left(status) => status.statusCode
        case _ => 200
      }.max)

      batchResults
    }(parseErrorHandler = handleUnparseableJson)}
  }

  get("/") {
    val serializer = SensitiveDataFilter(koskiSession).rowSerializer

    val oppijat = performOpiskeluoikeudetQueryLaajoillaHenkilötiedoilla.map(observable => observable
      .map(x => (application.henkilöRepository.oppijaHenkilöToTäydellisetHenkilötiedot(x._1), x._2))
      .map(serializer)
    )

    streamResponse(oppijat, koskiSession)
  }

  // TODO: tarkista lokeista voiko tämän poistaa
  get("/:oid") {
    renderEither[Oppija](HenkilöOid.validateHenkilöOid(params("oid")).right.flatMap { oid =>
      application.oppijaFacade.findOppija(oid, findMasterIfSlaveOid = false)(koskiSession)
    }.flatMap(_.warningsToLeft))
  }

  get("/:oid/opintotiedot-json") {
    renderEither[Oppija](HenkilöOid.validateHenkilöOid(params("oid")).right.flatMap { oid =>
      application.oppijaFacade.findOppija(oid, findMasterIfSlaveOid = true)(koskiSession)
    }.flatMap(_.warningsToLeft))
  }

  get("/:oid/virta-opintotiedot-xml") {
    if (!koskiSession.hasGlobalReadAccess) {
      haltWithStatus(KoskiErrorCategory.forbidden())
    }
    virtaOpinnot(params("oid")) match {
      case Right(elements) =>
        contentType = "text/plain"
        response.writer.print(XML.prettyPrintNodes(elements))
      case Left(status) => haltWithStatus(status)
    }
  }

  get("/oids") {
    streamResponse[String](application.opiskeluoikeusQueryRepository.oppijaOidsQuery(paginationSettings)(koskiSession), koskiSession)
  }

  private def virtaOpinnot(oid: String) =
    application.opintopolkuHenkilöFacade.findMasterOppija(oid).toRight(KoskiErrorCategory.notFound.oppijaaEiLöydy()).map { oppijaHenkilö =>
      val byHetu = (oppijaHenkilö.hetu.toList ++ oppijaHenkilö.vanhatHetut).sorted.map(VirtaHakuehtoHetu)
      val byOid = (oppijaHenkilö.oid :: oppijaHenkilö.linkitetytOidit).sorted.map(VirtaHakuehtoKansallinenOppijanumero)
      application.virtaClient.opintotiedotMassahaku(byHetu) ++ application.virtaClient.opintotiedotMassahaku(byOid)
    }.map(_.toList.distinct)

  private def handleUnparseableJson(status: HttpStatus) = {
    application.tiedonsiirtoService.storeTiedonsiirtoResult(koskiSession, None, None, None, Some(TiedonsiirtoError(JObject("unparseableJson" -> JString(request.body)), status.errors)))
    haltWithStatus(status)
  }

  private def withTracking[T](f: => T) = {
    if (koskiSession.isPalvelukäyttäjä) {
      application.ipService.trackIPAddress(koskiSession)
    }
    try {
      f
    } catch {
      case e: Exception =>
        application.tiedonsiirtoService.storeTiedonsiirtoResult(koskiSession, None, None, None, Some(TiedonsiirtoError(JObject("unparseableJson" -> JString(request.body)), KoskiErrorCategory.internalError().errors)))
        logger.error(e)("virhe aiheutti unparseableJson merkinnän tiedonsiirtoihin")
        throw e
    }
  }
}

/**
  *  Operating context for data updates. Operates outside the lecixal scope of OppijaServlet to ensure that none of the
  *  Scalatra threadlocals are used. This must be done because in batch mode, we are running in several threads.
  */
case class UpdateContext(user: KoskiSession, application: KoskiApplication, request: HttpServletRequest) extends Logging {
  def putSingle(validationResult: Either[HttpStatus, Oppija], oppijaJsonFromRequest: JValue, allowUpdate: Boolean): Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = {

    val result: Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = validationResult.flatMap(application.oppijaFacade.createOrUpdate(_, allowUpdate)(user))

    result.left.foreach { case HttpStatus(code, errors) =>
      logger(user).warn("Opiskeluoikeuden lisäys/päivitys estetty: " + code + " " + errors + " for request " + logSafeDescription(request))
    }

    val error = result.left.toOption.map(status => TiedonsiirtoError(oppijaJsonFromRequest, status.errors))
    application.tiedonsiirtoService
      .storeTiedonsiirtoResult(user, result.toOption.map(_.henkilö), validationResult.toOption, Some(oppijaJsonFromRequest), error)

    result
  }
}

