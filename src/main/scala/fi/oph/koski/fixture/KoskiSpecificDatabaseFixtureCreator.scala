package fi.oph.koski.fixture

import java.time.LocalDate
import java.time.LocalDate.{of => date}

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.db.PostgresDriverWithJsonSupport.api._
import fi.oph.koski.db.KoskiTables._
import fi.oph.koski.db._
import fi.oph.koski.documentation.ExampleData.{opiskeluoikeusMitätöity, suomenKieli}
import fi.oph.koski.documentation.ExamplesEsiopetus.{ostopalveluOpiskeluoikeus, päiväkotisuoritus}
import fi.oph.koski.documentation.ExamplesPerusopetus.ysinOpiskeluoikeusKesken
import fi.oph.koski.documentation.YleissivistavakoulutusExampleData.oppilaitos
import fi.oph.koski.documentation.{ExamplesEsiopetus, _}
import fi.oph.koski.henkilo.{KoskiSpecificMockOppijat, MockOppijat, OppijaHenkilö, VerifiedHenkilöOid}
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.koskiuser.{AccessType, KoskiSpecificSession, MockUsers}
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.organisaatio.MockOrganisaatiot.päiväkotiMajakka
import fi.oph.koski.perustiedot.{OpiskeluoikeudenOsittaisetTiedot, OpiskeluoikeudenPerustiedot}
import fi.oph.koski.schema._
import fi.oph.koski.util.Timing
import slick.dbio.DBIO

import scala.reflect.runtime.universe.TypeTag

class KoskiSpecificDatabaseFixtureCreator(application: KoskiApplication) extends DatabaseFixtureCreator(application, "opiskeluoikeus_fixture", "opiskeluoikeushistoria_fixture") {
  protected def oppijat = KoskiSpecificMockOppijat.defaultOppijat

  protected lazy val validatedOpiskeluoikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = {
    defaultOpiskeluOikeudet.zipWithIndex.map { case ((henkilö, oikeus), index) =>
      timed(s"Validating fixture ${index}", 500) {
        validator.validateAsJson(Oppija(henkilö.toHenkilötiedotJaOid, List(oikeus))) match {
          case Right(oppija) => (henkilö, oppija.tallennettavatOpiskeluoikeudet.head)
          case Left(status) => throw new RuntimeException(
            s"Fixture insert failed for ${henkilö.etunimet} ${henkilö.sukunimi} with data ${JsonSerializer.write(oikeus)}: ${status}"
          )
        }
      }
    }
  }

  protected lazy val invalidOpiskeluoikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = {
    val validOpiskeluoikeus: AmmatillinenOpiskeluoikeus = validateOpiskeluoikeus(AmmatillinenExampleData.opiskeluoikeus())
    val opiskeluoikeusJostaTunnisteenKoodiarvoPoistettu = validOpiskeluoikeus.copy(
      suoritukset = validOpiskeluoikeus.suoritukset.map(s => {
        val tutkinnonSuoritus = s.asInstanceOf[AmmatillisenTutkinnonSuoritus]
        tutkinnonSuoritus.copy(koulutusmoduuli = tutkinnonSuoritus.koulutusmoduuli.copy(
          tutkinnonSuoritus.koulutusmoduuli.tunniste.copy(koodiarvo = "123456")
        ))
      })
    )
    val hkiTallentaja = MockUsers.helsinkiTallentaja.toKoskiSpecificSession(application.käyttöoikeusRepository)
    List(
      (KoskiSpecificMockOppijat.organisaatioHistoria, validOpiskeluoikeus.copy(organisaatiohistoria = Some(AmmatillinenExampleData.opiskeluoikeudenOrganisaatioHistoria))),
      (
        KoskiSpecificMockOppijat.organisaatioHistoriallinen,
        validateOpiskeluoikeus(PerusopetusOppijaMaaratRaporttiFixtures.eriOppilaitoksessa).copy(
          organisaatiohistoria = PerusopetusOppijaMaaratRaporttiFixtures.organisaatiohistoria
        )
      ),
      (
        KoskiSpecificMockOppijat.organisaatioHistoriallinen,
        validateOpiskeluoikeus(PerusopetusOppijaMaaratRaporttiFixtures.eriOppilaitoksessaLisäopetus).copy(
          organisaatiohistoria = PerusopetusOppijaMaaratRaporttiFixtures.organisaatiohistoria
        )
      ),
      (KoskiSpecificMockOppijat.tunnisteenKoodiarvoPoistettu, opiskeluoikeusJostaTunnisteenKoodiarvoPoistettu),
      (KoskiSpecificMockOppijat.eskari, validateOpiskeluoikeus(ostopalveluOpiskeluoikeus, hkiTallentaja)),
      (KoskiSpecificMockOppijat.eskari, validateOpiskeluoikeus(ostopalveluOpiskeluoikeus.copy(
        suoritukset = List(päiväkotisuoritus(oppilaitos(päiväkotiMajakka))),
        tila = NuortenPerusopetuksenOpiskeluoikeudenTila(
          ostopalveluOpiskeluoikeus.tila.opiskeluoikeusjaksot.map(j => j.copy(alku = j.alku.minusDays(1)))
        )
      ), hkiTallentaja)),
    )
  }

  private def defaultOpiskeluOikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = {
    List(
      (KoskiSpecificMockOppijat.eero, AmmatillinenOpiskeluoikeusTestData.opiskeluoikeus(MockOrganisaatiot.stadinAmmattiopisto)),
      (KoskiSpecificMockOppijat.eero, AmmatillinenOpiskeluoikeusTestData.mitätöityOpiskeluoikeus),
      (KoskiSpecificMockOppijat.eerola, AmmatillinenOpiskeluoikeusTestData.opiskeluoikeus(MockOrganisaatiot.stadinAmmattiopisto)),
      (KoskiSpecificMockOppijat.teija, AmmatillinenOpiskeluoikeusTestData.opiskeluoikeus(MockOrganisaatiot.stadinAmmattiopisto)),
      (KoskiSpecificMockOppijat.syntymäajallinen, AmmatillinenOpiskeluoikeusTestData.opiskeluoikeus(MockOrganisaatiot.stadinAmmattiopisto)),
      (KoskiSpecificMockOppijat.markkanen, AmmatillinenOpiskeluoikeusTestData.opiskeluoikeus(MockOrganisaatiot.omnia).copy(ostettu = true)),
      (KoskiSpecificMockOppijat.eskari, ExamplesEsiopetus.esioppilas.tallennettavatOpiskeluoikeudet.head),
      (KoskiSpecificMockOppijat.eskariAikaisillaLisätiedoilla, ExamplesEsiopetus.esioppilasAikaisillaLisätiedoilla.tallennettavatOpiskeluoikeudet.head),
      (KoskiSpecificMockOppijat.ysiluokkalainen, ysinOpiskeluoikeusKesken),
      (KoskiSpecificMockOppijat.hetuton, ysinOpiskeluoikeusKesken),
      (KoskiSpecificMockOppijat.monessaKoulussaOllut, ysinOpiskeluoikeusKesken),
      (KoskiSpecificMockOppijat.monessaKoulussaOllut, ExamplesPerusopetus.seiskaTuplattuOpiskeluoikeus),
      (KoskiSpecificMockOppijat.koululainen, ExamplesEsiopetus.opiskeluoikeusHelsingissä),
      (KoskiSpecificMockOppijat.koululainen, PerusopetusExampleData.päättötodistusOpiskeluoikeus()),
      (KoskiSpecificMockOppijat.suoritusTuplana, PerusopetusExampleData.suoritusTuplana()),
      (KoskiSpecificMockOppijat.koululainen, ExamplesPerusopetukseenValmistavaOpetus.perusopetukseenValmistavaOpiskeluoikeus),
      (KoskiSpecificMockOppijat.luokallejäänyt, PerusopetusExampleData.päättötodistusLuokanTuplauksellaOpiskeluoikeus()),
      (KoskiSpecificMockOppijat.vuosiluokkalainen, PerusopetusExampleData.vuosiluokanOpiskeluoikeus()),
      (KoskiSpecificMockOppijat.toimintaAlueittainOpiskelija, ExamplesPerusopetus.toimintaAlueittainOpiskelija.tallennettavatOpiskeluoikeudet.head),
      (KoskiSpecificMockOppijat.oppiaineenKorottaja, ExamplesAikuistenPerusopetus.oppiaineenOppimääräOpiskeluoikeus),
      (KoskiSpecificMockOppijat.montaOppiaineenOppimäärääOpiskeluoikeudessa, ExamplesAikuistenPerusopetus.montaOppiaineenOppimääränSuoritustaOpiskeluoikeus),
      (KoskiSpecificMockOppijat.aikuisOpiskelija, ExamplesAikuistenPerusopetus.aikuistenPerusopetuksenOpiskeluoikeusAlkuvaiheineen),
      (KoskiSpecificMockOppijat.aikuisAineOpiskelijaMuuKuinVos, AikuistenPerusopetusOppijaMaaratRaporttiFixtures.aineOpiskelijaMuuKuinVos),
      (KoskiSpecificMockOppijat.aikuisOpiskelijaMuuKuinVos, AikuistenPerusopetusOppijaMaaratRaporttiFixtures.oppimääränSuorittajaMuuKuinVos),
      (KoskiSpecificMockOppijat.aikuisOpiskelijaVieraskielinen, AikuistenPerusopetusOppijaMaaratRaporttiFixtures.oppimääränSuorittaja),
      (KoskiSpecificMockOppijat.aikuisOpiskelijaVieraskielinenMuuKuinVos, AikuistenPerusopetusOppijaMaaratRaporttiFixtures.oppimääränSuorittajaMuuKuinVos),
      (KoskiSpecificMockOppijat.aikuisOpiskelijaMuuRahoitus, ExamplesAikuistenPerusopetus.oppiaineenOppimääräOpiskeluoikeusMuuRahoitus),
      (KoskiSpecificMockOppijat.kymppiluokkalainen, ExamplesPerusopetuksenLisaopetus.lisäopetuksenPäättötodistus.tallennettavatOpiskeluoikeudet.head),
      (KoskiSpecificMockOppijat.lukiolainen, PerusopetusExampleData.päättötodistusOpiskeluoikeus(luokka = "B")),
      (KoskiSpecificMockOppijat.lukiolainen, ExamplesLukio.päättötodistus()),
      (KoskiSpecificMockOppijat.lukioKesken, ExamplesLukio.lukioKesken),
      (KoskiSpecificMockOppijat.lukionAineopiskelija, ExamplesLukio.aineopiskelija),
      (KoskiSpecificMockOppijat.lukionAineopiskelijaAktiivinen, ExamplesLukio.aineOpiskelijaAktiivinen),
      (KoskiSpecificMockOppijat.lukionEiTiedossaAineopiskelija, ExamplesLukio.aineOpiskelijaEiTiedossaOppiaineella),
      (KoskiSpecificMockOppijat.uusiLukio, ExamplesLukio2019.opiskeluoikeus),
      (KoskiSpecificMockOppijat.uusiLukionAineopiskelija, ExamplesLukio2019.oppiaineenOppimääräOpiskeluoikeus),
      (KoskiSpecificMockOppijat.luva, ExamplesLukioonValmistavaKoulutus.luvaTodistus.tallennettavatOpiskeluoikeudet.head),
      (KoskiSpecificMockOppijat.ammattilainen, AmmatillinenExampleData.perustutkintoOpiskeluoikeusValmis()),
      (KoskiSpecificMockOppijat.tutkinnonOsaaPienempiKokonaisuus, TutkinnonOsaaPienempiKokonaisuusExample.opiskeluoikeus),
      (KoskiSpecificMockOppijat.muuAmmatillinen, MuunAmmatillisenKoulutuksenExample.muuAmmatillinenKoulutusOpiskeluoikeus),
      (KoskiSpecificMockOppijat.muuAmmatillinenKokonaisuuksilla, MuunAmmatillisenKoulutuksenExample.muuAmmatillinenKoulutusKokonaisuuksillaOpiskeluoikeus),
      (KoskiSpecificMockOppijat.ammatilliseenTetäväänValmistavaMuuAmmatillinen, MuunAmmatillisenKoulutuksenExample.ammatilliseenTehtäväänValmistavaKoulutusOpiskeluoikeus),
      (KoskiSpecificMockOppijat.amis, AmmatillinenExampleData.perustutkintoOpiskeluoikeusKesken()),
      (KoskiSpecificMockOppijat.liiketalous, AmmatillinenOpiskeluoikeusTestData.opiskeluoikeus(MockOrganisaatiot.stadinAmmattiopisto, koulutusKoodi = 331101, diaariNumero = "59/011/2014")),
      (KoskiSpecificMockOppijat.valma, ExamplesValma.valmaTodistus.tallennettavatOpiskeluoikeudet.head),
      (KoskiSpecificMockOppijat.telma, ExamplesTelma.telmaTodistus.tallennettavatOpiskeluoikeudet.head),
      (KoskiSpecificMockOppijat.ylioppilasLukiolainen, ExamplesLukio.päättötodistus()),
      (KoskiSpecificMockOppijat.erikoisammattitutkinto, AmmattitutkintoExample.opiskeluoikeus),
      (KoskiSpecificMockOppijat.reformitutkinto, ReforminMukainenErikoisammattitutkintoExample.opiskeluoikeus),
      (KoskiSpecificMockOppijat.osittainenammattitutkinto, AmmatillinenPerustutkintoExample.osittainenPerustutkintoOpiskeluoikeus),
      (KoskiSpecificMockOppijat.ammatillisenOsittainenRapsa,AmmatillinenPerustutkintoExample.osittainenPerustutkintoOpiskeluoikeus.copy(suoritukset = List(AmmatillinenExampleData.ammatillisenTutkinnonOsittainenAutoalanSuoritus))),
      (KoskiSpecificMockOppijat.paikallinenTunnustettu, AmmatillinenPerustutkintoExample.tunnustettuPaikallinenTutkinnonOsaOpiskeluoikeus),
      (KoskiSpecificMockOppijat.tiedonsiirto, AmmatillinenOpiskeluoikeusTestData.lähdejärjestelmällinenOpiskeluoikeus),
      (KoskiSpecificMockOppijat.perusopetuksenTiedonsiirto, PerusopetuksenOpiskeluoikeusTestData.lähdejärjestelmällinenOpiskeluoikeus),
      (KoskiSpecificMockOppijat.omattiedot, PerusopetusExampleData.päättötodistusOpiskeluoikeus(luokka = "D")),
      (KoskiSpecificMockOppijat.omattiedot, ExamplesLukio.päättötodistus()),
      (KoskiSpecificMockOppijat.omattiedotSlave.henkilö, AmmatillinenOldExamples.uusi.tallennettavatOpiskeluoikeudet(0)),
      (KoskiSpecificMockOppijat.ibFinal, ExamplesIB.opiskeluoikeus),
      (KoskiSpecificMockOppijat.ibPredicted, ExamplesIB.opiskeluoikeusPredictedGrades),
      (KoskiSpecificMockOppijat.ibPreIB2019, ExamplesIB.opiskeluoikeusPreIB2019),
      (KoskiSpecificMockOppijat.dia, ExamplesDIA.opiskeluoikeus),
      (KoskiSpecificMockOppijat.master, ExamplesPerusopetus.päättötodistus.tallennettavatOpiskeluoikeudet.head),
      (KoskiSpecificMockOppijat.slave.henkilö, ExamplesLukio.päättötodistus()),
      (KoskiSpecificMockOppijat.turvakielto, ExamplesLukio.päättötodistus()),
      (KoskiSpecificMockOppijat.erkkiEiperusteissa, AmmatillinenOpiskeluoikeusTestData.opiskeluoikeus(MockOrganisaatiot.stadinAmmattiopisto, koulutusKoodi = 334117, diaariNumero = "22/011/2004")),
      (KoskiSpecificMockOppijat.internationalschool, ExamplesInternationalSchool.opiskeluoikeus),
      (KoskiSpecificMockOppijat.montaKoulutuskoodiaAmis, AmmatillinenExampleData.puuteollisuusOpiskeluoikeusKesken()),
      (KoskiSpecificMockOppijat.valviraaKiinnostavaTutkinto, AmmatillinenExampleData.sosiaaliJaTerveysalaOpiskeluoikeus()),
      (KoskiSpecificMockOppijat.valviraaKiinnostavaTutkintoKesken, AmmatillinenExampleData.sosiaaliJaTerveysalaOpiskeluoikeusKesken()),
      (KoskiSpecificMockOppijat.kelaErityyppisiaOpiskeluoikeuksia, ExamplesEsiopetus.esioppilas.tallennettavatOpiskeluoikeudet.head),
      (KoskiSpecificMockOppijat.kelaErityyppisiaOpiskeluoikeuksia, PerusopetusExampleData.päättötodistusOpiskeluoikeus()),
      (KoskiSpecificMockOppijat.lukioDiaIbInternationalOpiskelijamaaratRaportti_nuortenOppimaara, LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures.lukionOppimaaraNuorten),
      (KoskiSpecificMockOppijat.lukioDiaIbInternationalOpiskelijamaaratRaportti_aikuistenOppimaara, LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures.lukionOppimaaraAikuisten),
      (KoskiSpecificMockOppijat.lukioDiaIbInternationalOpiskelijamaaratRaportti_aineopiskelija, LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures.lukionAineopiskelija),
      (KoskiSpecificMockOppijat.lukioDiaIbInternationalOpiskelijamaaratRaportti_dia, LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures.dia),
      (KoskiSpecificMockOppijat.lukioDiaIbInternationalOpiskelijamaaratRaportti_ib, LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures.ib),
      (KoskiSpecificMockOppijat.lukioDiaIbInternationalOpiskelijamaaratRaportti_international, LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures.international),
      (KoskiSpecificMockOppijat.perusopetusOppijaMaaratRaportti_tavallinen, PerusopetusOppijaMaaratRaporttiFixtures.tavallinen),
      (KoskiSpecificMockOppijat.perusopetusOppijaMaaratRaportti_erikois, PerusopetusOppijaMaaratRaporttiFixtures.erikois),
      (KoskiSpecificMockOppijat.perusopetusOppijaMaaratRaportti_virheellisestiSiirretty, PerusopetusOppijaMaaratRaporttiFixtures.virheellisestiSiirrettyVaikeastiVammainen),
      (KoskiSpecificMockOppijat.perusopetusOppijaMaaratRaportti_virheellisestiSiirrettyVieraskielinen, PerusopetusOppijaMaaratRaporttiFixtures.virheellisestiSiirrettyVammainen),
      (KoskiSpecificMockOppijat.perusopetusOppijaMaaratRaportti_tavallinen, PerusopetusOppijaMaaratRaporttiFixtures.tavallinenLisäopetus),
      (KoskiSpecificMockOppijat.perusopetusOppijaMaaratRaportti_erikois, PerusopetusOppijaMaaratRaporttiFixtures.erikoisLisäopetus),
      (KoskiSpecificMockOppijat.perusopetusOppijaMaaratRaportti_virheellisestiSiirretty, PerusopetusOppijaMaaratRaporttiFixtures.virheellisestiSiirrettyVaikeastiVammainenLisäopetus),
      (KoskiSpecificMockOppijat.perusopetusOppijaMaaratRaportti_virheellisestiSiirrettyVieraskielinen, PerusopetusOppijaMaaratRaporttiFixtures.virheellisestiSiirrettyVammainenLisäopetus),
      (KoskiSpecificMockOppijat.lukioKurssikertymaRaportti_oppimaara, LukioKurssikertymaRaporttiFixtures.oppimaara),
      (KoskiSpecificMockOppijat.lukioKurssikertymaRaportti_aineopiskelija_eronnut, LukioKurssikertymaRaporttiFixtures.aineopiskelijaEronnut),
      (KoskiSpecificMockOppijat.lukioKurssikertymaRaportti_aineopiskelija_valmistunut, LukioKurssikertymaRaporttiFixtures.aineopiskelijaValmistunut),
      (KoskiSpecificMockOppijat.luvaOpiskelijamaaratRaportti_nuortenOppimaara, LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures.nuortenOppimaaraLuva),
      (KoskiSpecificMockOppijat.luvaOpiskelijamaaratRaportti_aikuistenOppimaara, LukioDiaIbInternationalOpiskelijaMaaratRaporttiFixtures.aikuistenOppimaaraLuva),
      (KoskiSpecificMockOppijat.paallekkaisiOpiskeluoikeuksia, PaallekkaisetOpiskeluoikeudetFixtures.ensimmainenOpiskeluoikeus),
      (KoskiSpecificMockOppijat.paallekkaisiOpiskeluoikeuksia, PaallekkaisetOpiskeluoikeudetFixtures.keskimmainenOpiskeluoikeus),
      (KoskiSpecificMockOppijat.paallekkaisiOpiskeluoikeuksia, PaallekkaisetOpiskeluoikeudetFixtures.viimeinenOpiskeluoikeus),
      (KoskiSpecificMockOppijat.vapaaSivistystyöOppivelvollinen, VapaaSivistystyöExample.opiskeluoikeusKOPS),
      (KoskiSpecificMockOppijat.vapaaSivistystyöMaahanmuuttajienKotoutus, VapaaSivistystyöExample.opiskeluoikeusKOTO),
      (KoskiSpecificMockOppijat.vapaaSivistystyöVapaatavoitteinenKoulutus, VapaaSivistystyöExample.opiskeluoikeusVapaatavoitteinen),
      (KoskiSpecificMockOppijat.vapaaSivistystyöLukutaitoKotoutus, VapaaSivistystyöExample.opiskeluoikeusLukutaito),
      (KoskiSpecificMockOppijat.maksuttomuuttaPidennetty1, MaksuttomuusRaporttiFixtures.opiskeluoikeusAmmatillinenMaksuttomuuttaPidennetty),
      (KoskiSpecificMockOppijat.maksuttomuuttaPidennetty2, MaksuttomuusRaporttiFixtures.opiskeluoikeusAmmatillinenMaksuttomuuttaPidennetty),
      (KoskiSpecificMockOppijat.maksuttomuuttaPidennetty2, MaksuttomuusRaporttiFixtures.opiskeluoikeusLukioMaksuttomuuttaPidennetty)
    )
  }

}

object AmmatillinenOpiskeluoikeusTestData {
  def opiskeluoikeus(oppilaitosId: String, koulutusKoodi: Int = 351301, diaariNumero: String = "39/011/2014"): AmmatillinenOpiskeluoikeus = {
    val oppilaitos: Oppilaitos = Oppilaitos(oppilaitosId, None, None)
    val koulutusKoodiViite = Koodistokoodiviite(koulutusKoodi.toString, None, "koulutus", None)

    AmmatillinenOpiskeluoikeus(
      oppilaitos = Some(oppilaitos),
      suoritukset = List(AmmatillisenTutkinnonSuoritus(
        koulutusmoduuli = AmmatillinenTutkintoKoulutus(koulutusKoodiViite, Some(diaariNumero)),
        toimipiste = oppilaitos,
        suorituskieli = suomenKieli,
        suoritustapa = AmmatillinenExampleData.suoritustapaOps
      )),
      tila = AmmatillinenOpiskeluoikeudenTila(List(AmmatillinenOpiskeluoikeusjakso(date(2000, 1, 1), ExampleData.opiskeluoikeusLäsnä, Some(ExampleData.valtionosuusRahoitteinen)))),
      lisätiedot = Some(AmmatillisenOpiskeluoikeudenLisätiedot(
        hojks = None,
        erityinenTuki = Some(List(Aikajakso(date(2001, 1, 1), None))),
        vaikeastiVammainen = Some(List(Aikajakso(date(2001, 1, 1), None))),
        vankilaopetuksessa = Some(List(Aikajakso(date(2001, 1, 1), None)))
      ))
    )
  }

  lazy val mitätöityOpiskeluoikeus: PerusopetuksenOpiskeluoikeus =
    ysinOpiskeluoikeusKesken.copy(tila =
      ysinOpiskeluoikeusKesken.tila.copy(opiskeluoikeusjaksot =
        ysinOpiskeluoikeusKesken.tila.opiskeluoikeusjaksot :+ NuortenPerusopetuksenOpiskeluoikeusjakso(alku = LocalDate.now, opiskeluoikeusMitätöity)
      )
    )

  lazy val lähdejärjestelmällinenOpiskeluoikeus: AmmatillinenOpiskeluoikeus =
    opiskeluoikeus(MockOrganisaatiot.stadinAmmattiopisto).copy(lähdejärjestelmänId = Some(AmmatillinenExampleData.winnovaLähdejärjestelmäId))
}

object PerusopetuksenOpiskeluoikeusTestData {
  lazy val lähdejärjestelmällinenOpiskeluoikeus: PerusopetuksenOpiskeluoikeus =
    PerusopetusExampleData.päättötodistusOpiskeluoikeus(oppilaitos = Oppilaitos(MockOrganisaatiot.stadinAmmattiopisto, None, None)).copy(
      lähdejärjestelmänId = Some(AmmatillinenExampleData.primusLähdejärjestelmäId)
    )
}
