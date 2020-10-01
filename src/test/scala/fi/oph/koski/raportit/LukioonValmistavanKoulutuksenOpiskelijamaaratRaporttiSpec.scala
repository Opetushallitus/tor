package fi.oph.koski.raportit

import fi.oph.koski.KoskiApplicationForTests
import fi.oph.koski.fixture.LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures
import fi.oph.koski.koskiuser.MockUsers
import fi.oph.koski.log.AuditLogTester
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.raportointikanta.RaportointikantaTestMethods
import org.scalatest.{BeforeAndAfterAll, FreeSpec}

class LukioonValmistavanKoulutuksenOpiskelijamaaratRaporttiSpec extends FreeSpec with RaportointikantaTestMethods with BeforeAndAfterAll {

  override def defaultUser = MockUsers.helsinginKaupunkiPalvelukäyttäjä

  override def beforeAll() = {
    loadRaportointikantaFixtures
  }

  "Lukioon valmistavan koulutuksen opiskelijamaarat" - {

    "Raportin voi ladata ja se tuottaa auditlogin" in {
      AuditLogTester.clearMessages
      authGet(s"api/raportit/luvaopiskelijamaarat?oppilaitosOid=${MockOrganisaatiot.helsinginKaupunki}&paiva=2018-01-01&password=salasana") {
        verifyResponseStatusOk()
        response.headers("Content-Disposition").head should equal(s"""attachment; filename="lukioon_valmistavan_koulutuksen_opiskelijamaarat_20180101.xlsx"""")
        response.bodyBytes.take(ENCRYPTED_XLSX_PREFIX.length) should equal(ENCRYPTED_XLSX_PREFIX)
        AuditLogTester.verifyAuditLogMessage(Map("operation" -> "OPISKELUOIKEUS_RAPORTTI", "target" -> Map("hakuEhto" -> s"raportti=luvaopiskelijamaarat&oppilaitosOid=${MockOrganisaatiot.helsinginKaupunki}&paiva=2018-01-01")))
      }
    }

    lazy val rows: Seq[LukioonValmistavanKoulutuksenOpiskelijamaaratRaporttiRow] = loadRaportti
    lazy val ressu = rows.find(_.oppilaitos == "Ressun lukio").get

    "Valtionosuus rahoitteisia" in {
      ressu.opiskelijoidenMaara_VOSRahoitteisia shouldBe(1)
      ressu.nuortenOppimaaranSuorittajia_VOSRahoitteisia shouldBe(1)
      ressu.aikuistenOppimaaranSuorittajia_VOSRahoitteisia shouldBe(0)
    }
    "Muuta kautta rahoitettu" in {
      ressu.opiskelijoidenMaara_MuutaKauttaRahoitettu shouldBe(1)
      ressu.nuortenOppimaaranSuorittajia_MuutaKauttaRahoitettu shouldBe(0)
      ressu.aikuistenOppimaaranSuorittajia_MuutaKauttaRahoitettu shouldBe(1)
    }
    "Kotikunta" in {
      ressu.nuortenOppimaaranSuorittajia_EiKotikuntaa shouldBe(0)
      ressu.nuortenOppimaaranSuorittajia_KotikuntaAhvenanmaa shouldBe(1)

      ressu.aikuistenOppimaaranSuorittajia_EiKotikuntaa shouldBe(1)
      ressu.aikuistenOppimaaranSuorittajia_KotikuntaAhvenanmaa shouldBe(0)
    }
    "Sisäoppilaitosmainen majoitus" in {
      ressu.opiskelijoidenMaara_SisaoppilaitosmainenMajoitus shouldBe(1)
    }
  }

  private def loadRaportti = {
    val request = RaporttiPäivältäRequest(
      oppilaitosOid = MockOrganisaatiot.ressunLukio,
      downloadToken = None,
      password = "bassword",
      paiva = LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures.fixedDate
    )
    new RaportitService(KoskiApplicationForTests)
      .lukioonValmistavanKoulutuksenOpiskelijaMaaratRaportti(request)
      .sheets.collectFirst { case d: DataSheet => d.rows.collect {
        case r: LukioonValmistavanKoulutuksenOpiskelijamaaratRaporttiRow => r
    }}.get
  }
}