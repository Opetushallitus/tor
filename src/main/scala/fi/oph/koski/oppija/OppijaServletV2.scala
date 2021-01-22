package fi.oph.koski.oppija

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.koskiuser.{AccessType, KoskiSession, RequiresVirkailijaOrPalvelukäyttäjä}
import fi.oph.koski.log.Logging
import fi.oph.koski.opiskeluoikeus.OpiskeluoikeusQueries
import fi.oph.koski.schema.Oppija
import fi.oph.koski.servlet.RequestDescriber.logSafeDescription
import fi.oph.koski.servlet.{ApiServlet, NoCache}
import fi.oph.koski.tiedonsiirto.TiedonsiirtoError
import javax.servlet.http.HttpServletRequest
import org.json4s.JValue
import org.json4s.JsonAST.{JObject, JString}

class OppijaServletV2(implicit val application: KoskiApplication) extends ApiServlet with RequiresVirkailijaOrPalvelukäyttäjä with NoCache {
  post("/") { putSingle(false) }

  put("/") { putSingle(true) }

  private def putSingle(allowUpdate: Boolean) = {
    withTracking { withJsonBody { (oppijaJson: JValue) =>
      val validationResult: Either[HttpStatus, Oppija] = application.validator.extractAndValidateOppija(oppijaJson)(koskiSession, AccessType.write, Some(application))
      val result: Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = UpdateContextV2(koskiSession, application, request).putSingle(validationResult, oppijaJson, allowUpdate)
      renderEither[HenkilönOpiskeluoikeusVersiot](result)
    }(parseErrorHandler = handleUnparseableJson)}
  }

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
        throw e
    }
  }
}

/**
  *  Operating context for data updates. Operates outside the lecixal scope of OppijaServlet to ensure that none of the
  *  Scalatra threadlocals are used. This must be done because in batch mode, we are running in several threads.
  */
case class UpdateContextV2(user: KoskiSession, application: KoskiApplication, request: HttpServletRequest) extends Logging {
  def putSingle(validationResult: Either[HttpStatus, Oppija], oppijaJsonFromRequest: JValue, allowUpdate: Boolean): Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = {

    val result: Either[HttpStatus, HenkilönOpiskeluoikeusVersiot] = validationResult.flatMap(application.oppijaFacadeV2.createOrUpdate(_, allowUpdate)(user))

    result.left.foreach { case HttpStatus(code, errors) =>
      logger(user).warn("Opiskeluoikeuden lisäys/päivitys estetty: " + code + " " + errors + " for request " + logSafeDescription(request))
    }

    val error = result.left.toOption.map(status => TiedonsiirtoError(oppijaJsonFromRequest, status.errors))
    application.tiedonsiirtoService
      .storeTiedonsiirtoResult(user, result.toOption.map(_.henkilö), validationResult.toOption, Some(oppijaJsonFromRequest), error)

    result
  }
}
