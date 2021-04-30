package fi.oph.koski.raportit

import java.time.LocalDate
import java.time.LocalDate.{of => date}

import fi.oph.koski.KoskiApplicationForTests
import fi.oph.koski.api.PutOpiskeluoikeusTestMethods
import fi.oph.koski.documentation.ExampleData.{longTimeAgo, opiskeluoikeusLäsnä, opiskeluoikeusValmistunut, valtionosuusRahoitteinen}
import fi.oph.koski.documentation.ExamplesAikuistenPerusopetus
import fi.oph.koski.documentation.ExamplesAikuistenPerusopetus.{aikuistenPerusopetukseOppimääränSuoritus, aikuistenPerusopetuksenAlkuvaiheenSuoritus, aikuistenPerusopetus2017, oppiaineidenSuoritukset2017}
import fi.oph.koski.henkilo.LaajatOppijaHenkilöTiedot
import fi.oph.koski.henkilo.KoskiSpecificMockOppijat.aikuisOpiskelija
import fi.oph.koski.koskiuser.{KoskiMockUser, MockUser}
import fi.oph.koski.log.AuditLogTester
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.organisaatio.MockOrganisaatiot.jyväskylänNormaalikoulu
import fi.oph.koski.raportit.aikuistenperusopetus._
import fi.oph.koski.raportointikanta.RaportointikantaTestMethods
import fi.oph.koski.schema._
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

class AikuistenPerusopetuksenOpiskeluoikeudenUlkopuolisetKurssitSpec extends FreeSpec with Matchers with RaportointikantaTestMethods with BeforeAndAfterAll with PutOpiskeluoikeusTestMethods[AikuistenPerusopetuksenOpiskeluoikeus] {
  private val application = KoskiApplicationForTests
  private val raporttiBuilder = AikuistenPerusopetuksenOpiskeluoikeudenUlkopuolisetKurssit(application.raportointiDatabase.db)
  private lazy val raportti =
    raporttiBuilder.build(List(jyväskylänNormaalikoulu), date(2006, 1, 1), date(2016, 12, 30))(session(defaultUser)).rows.map(_.asInstanceOf[AikuistenPerusopetuksenOpiskeluoikeudenUlkopuolisetKurssitRow])

  override def beforeAll(): Unit = {
    lisääPäätasonSuorituksia(
      aikuisOpiskelija,
      List(
        ExamplesAikuistenPerusopetus.oppiaineenOppimääränSuoritusAI1,
        ExamplesAikuistenPerusopetus.oppiaineenOppimääränSuoritusYH
      )
    )
    reloadRaportointikanta
  }

  override def afterAll: Unit = resetFixtures


  def tag = implicitly[reflect.runtime.universe.TypeTag[AikuistenPerusopetuksenOpiskeluoikeus]]
  override def defaultOpiskeluoikeus = makeOpiskeluoikeus(alkamispäivä = longTimeAgo)

  def makeOpiskeluoikeus(alkamispäivä: LocalDate = longTimeAgo, oppilaitos: Oppilaitos = Oppilaitos(MockOrganisaatiot.jyväskylänNormaalikoulu)) = AikuistenPerusopetuksenOpiskeluoikeus(
    tila = AikuistenPerusopetuksenOpiskeluoikeudenTila(
      List(
        AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2008, 8, 15), opiskeluoikeusLäsnä, Some(valtionosuusRahoitteinen)),
        AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2016, 6, 4), opiskeluoikeusValmistunut, Some(valtionosuusRahoitteinen))
      )
    ),
    koulutustoimija = None,
    oppilaitos = Some(oppilaitos),
    suoritukset = List(
      aikuistenPerusopetuksenAlkuvaiheenSuoritus,
      aikuistenPerusopetukseOppimääränSuoritus(aikuistenPerusopetus2017, oppiaineidenSuoritukset2017)
    ),
    lisätiedot = Some(AikuistenPerusopetuksenOpiskeluoikeudenLisätiedot(vaikeastiVammainen = Some(List(Aikajakso(date(2014, 6, 6), None)))))
  )

  "Aikuisten perusopetuksen aineopiskelijoiden kurssikertymien raportti" - {
    "Raportti voidaan ladata ja lataaminen tuottaa auditlogin" in {
      authGet(s"api/raportit/aikuistenperusopetuksenkurssikertymaraportti?oppilaitosOid=$jyväskylänNormaalikoulu&alku=2006-01-01&loppu=2016-12-30&password=salasana") {
        verifyResponseStatusOk()
        response.headers("Content-Disposition").head should equal(s"""attachment; filename="aikuisten_perusopetuksen_kurssikertymät_raportti-20060101-20161230.xlsx"""")
        response.bodyBytes.take(ENCRYPTED_XLSX_PREFIX.length) should equal(ENCRYPTED_XLSX_PREFIX)
        AuditLogTester.verifyAuditLogMessage(Map("operation" -> "OPISKELUOIKEUS_RAPORTTI", "target" -> Map("hakuEhto" -> s"raportti=aikuistenperusopetuksenkurssikertymaraportti&oppilaitosOid=$jyväskylänNormaalikoulu&alku=2006-01-01&loppu=2016-12-30")))
      }
    }

    "Raportin kolumnit" in {
      lazy val r = findSingle(raportti)
      r.oppilaitos should equal("Jyväskylän normaalikoulu")
      r.kurssikoodi should (equal("ÄI2") or equal("AÄI1"))
      r.kurssinNimi should (equal("Monimuotoiset tekstit") or equal("Suomen kielen ja kirjallisuuden opiskelun perustaidot"))
      r.päätasonSuorituksenTyyppi should (equal("aikuistenperusopetuksenoppimaara") or equal("aikuistenperusopetuksenoppimaaranalkuvaihe"))
      r.kurssinSuorituksenTyyppi should (equal("aikuistenperusopetuksenkurssi") or equal("aikuistenperusopetuksenalkuvaiheenkurssi"))
    }
  }

  private def findSingle(rows: Seq[AikuistenPerusopetuksenOpiskeluoikeudenUlkopuolisetKurssitRow]) = {
    val found = rows.filter(_.oppilaitos.equals("Jyväskylän normaalikoulu"))
    found.length should be(3)
    found.head
  }

  private def session(user: KoskiMockUser)= user.toKoskiSpecificSession(application.käyttöoikeusRepository)

  private def lisääPäätasonSuorituksia(oppija: LaajatOppijaHenkilöTiedot, päätasonSuoritukset: List[AikuistenPerusopetuksenPäätasonSuoritus]) = {
    val oo = getOpiskeluoikeus(oppija.oid, OpiskeluoikeudenTyyppi.aikuistenperusopetus.koodiarvo).asInstanceOf[AikuistenPerusopetuksenOpiskeluoikeus]
    putOppija(Oppija(oppija, List(oo.copy(suoritukset = päätasonSuoritukset ::: oo.suoritukset)))) {
      verifyResponseStatusOk()
    }
  }
}

