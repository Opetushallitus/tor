package fi.oph.koski.valpas

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.http.HttpStatus
import fi.oph.koski.servlet.NoCache
import fi.oph.koski.util.ChainingSyntax._
import fi.oph.koski.valpas.log.ValpasAuditLog.{auditLogOppijaKatsominen, auditLogOppijaKuntailmoitus}
import fi.oph.koski.valpas.servlet.ValpasApiServlet
import fi.oph.koski.valpas.valpasrepository.{ValpasKuntailmoitusLaajatTiedot, ValpasKuntailmoitusLaajatTiedotJaOppijaOid, ValpasKuntailmoitusPohjatiedot, ValpasKuntailmoitusPohjatiedotInput}
import fi.oph.koski.valpas.valpasuser.RequiresValpasSession
import org.json4s._

class ValpasKuntailmoitusApiServlet(implicit val application: KoskiApplication)
  extends ValpasApiServlet
    with NoCache
    with RequiresValpasSession {

  private lazy val kuntailmoitusValidator = new ValpasKuntailmoitusInputValidator(
    application.organisaatioRepository,
    application.valpasRajapäivätService,
    application.directoryClient
  )
  private lazy val kuntailmoitusService = application.valpasKuntailmoitusService

  post("/") {
    withJsonBody { (kuntailmoitusInputJson: JValue) =>
      val result: Either[HttpStatus, ValpasKuntailmoitusLaajatTiedot] =
        extractAndValidateKuntailmoitus(kuntailmoitusInputJson)
          .flatMap(kuntailmoitusService.createKuntailmoitus)
          .tap(auditLogOppijaKuntailmoitus)
          .map(_.kuntailmoitus)
      renderEither[ValpasKuntailmoitusLaajatTiedot](result)
    }(parseErrorHandler = handleUnparseableJson)
  }

  private def extractAndValidateKuntailmoitus(kuntailmoitusInputJson: JValue) = {
    application.validatingAndResolvingExtractor.extract[ValpasKuntailmoitusLaajatTiedotJaOppijaOid](kuntailmoitusInputJson)
      .flatMap(kuntailmoitusInput =>
        Either.cond(
          kuntailmoitusInput.kuntailmoitus.id.isEmpty,
          kuntailmoitusInput,
          ValpasErrorCategory.notImplemented.kuntailmoituksenMuokkaus()
        )
      )
      .flatMap(kuntailmoitusValidator.validateKuntailmoitusInput)
  }

  post("/pohjatiedot") {
    withJsonBody { (pohjatiedotInputJson: JValue) =>
      val input = extractAndValidatePohjatiedot(pohjatiedotInputJson)

      val result = input.flatMap(kuntailmoitusService.haePohjatiedot)
        .tap(result => result.oppijat.map(_.oppijaOid).foreach(auditLogOppijaKatsominen))

      renderEither[ValpasKuntailmoitusPohjatiedot](result)
    }(parseErrorHandler = handleUnparseableJson)
  }

  private def extractAndValidatePohjatiedot(pohjatiedotInputJson: JValue): Either[HttpStatus, ValpasKuntailmoitusPohjatiedotInput] = {
    application.validatingAndResolvingExtractor.extract[ValpasKuntailmoitusPohjatiedotInput](pohjatiedotInputJson)
  }

  private def handleUnparseableJson(status: HttpStatus) = {
    haltWithStatus(status)
  }
}
