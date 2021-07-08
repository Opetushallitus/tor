package fi.oph.koski.valpas

import fi.oph.koski.config.{Environment, KoskiApplication}
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.koskiuser.Unauthenticated
import fi.oph.koski.schema.LocalizedString
import fi.oph.koski.servlet.NoCache
import fi.oph.koski.valpas.servlet.ValpasApiServlet

class ValpasBootstrapServlet(implicit val application: KoskiApplication) extends ValpasApiServlet with NoCache with Unauthenticated {
  get("/window-properties") {
    WindowProperties(
      valpasLocalizationMap = application.valpasLocalizationRepository.localizations,
      environment = Environment.currentEnvironment(application.config),
      opintopolkuVirkailijaUrl = application.config.getString("opintopolku.virkailija.url"),
    )
  }
}

case class WindowProperties(
  valpasLocalizationMap: Map[String, LocalizedString],
  environment: String,
  opintopolkuVirkailijaUrl: String,
)
