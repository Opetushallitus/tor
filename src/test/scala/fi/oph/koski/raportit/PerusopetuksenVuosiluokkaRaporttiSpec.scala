package fi.oph.koski.raportit

import java.time.LocalDate
import java.time.LocalDate.{of => date}

import fi.oph.koski.KoskiApplicationForTests
import fi.oph.koski.api.OpiskeluoikeusTestMethodsPerusopetus
import fi.oph.koski.documentation.ExampleData._
import fi.oph.koski.documentation.ExamplesPerusopetus._
import fi.oph.koski.documentation.PerusopetusExampleData
import fi.oph.koski.documentation.PerusopetusExampleData._
import fi.oph.koski.henkilo.{LaajatOppijaHenkilöTiedot, KoskiSpecificMockOppijat}
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.raportointikanta.RaportointikantaTestMethods
import fi.oph.koski.schema._
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

class PerusopetuksenVuosiluokkaRaporttiSpec
  extends FreeSpec
    with Matchers
    with RaportointikantaTestMethods
    with OpiskeluoikeusTestMethodsPerusopetus
    with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    reloadRaportointikanta
  }

  private lazy val repository = PerusopetuksenRaportitRepository(KoskiApplicationForTests.raportointiDatabase.db)

  "Perusopetuksenvuosiluokka raportti" - {

    "Raportin lataaminen toimii" in {
      verifyPerusopetukseVuosiluokkaRaportinLataaminen(
        queryString = defaultQuery,
        apiUrl = "api/raportit/perusopetuksenvuosiluokka",
        expectedRaporttiNimi = "perusopetuksenvuosiluokka",
        expectedFileNamePrefix = "Perusopetuksen_vuosiluokka")
    }

    "Tuottaa oikeat tiedot" in {
      withLisätiedotFixture(KoskiSpecificMockOppijat.ysiluokkalainen, perusopetuksenOpiskeluoikeudenLisätiedot) {
        val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), LocalDate.of(2014, 8, 15), vuosiluokka = "8")
        val ynjevinOpiskeluoikeusOid = lastOpiskeluoikeus(KoskiSpecificMockOppijat.ysiluokkalainen.oid).oid.get
        val rivi = result.find(_.opiskeluoikeusOid == ynjevinOpiskeluoikeusOid)

        rivi should equal(
          Some(ynjevinExpectedKasiLuokkaRowWithLisätiedot.copy(opiskeluoikeusOid = ynjevinOpiskeluoikeusOid))
        )
      }
    }

    "Ei näytetä laajuuksia kun päätason suorituksen arviointipäivä on 31.7.2020 tai ennen" in {
      withAdditionalSuoritukset(KoskiSpecificMockOppijat.ysiluokkalainen, List(seitsemännenLuokanSuoritusLaajuudet_ennen_1_8_2020)) {
        val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), LocalDate.of(2019, 8, 15), vuosiluokka = "7")
        val oppijaOpiskeluoikeusOid = lastOpiskeluoikeus(KoskiSpecificMockOppijat.ysiluokkalainen.oid).oid.get
        val rivi = result.find(_.opiskeluoikeusOid == oppijaOpiskeluoikeusOid)

        rivi.get.aidinkieli should equal("9")
        rivi.get.kieliA2 should equal("Oppiaine puuttuu")
        rivi.get.biologia should equal("9*")
        rivi.get.uskonto should equal("10")
      }
    }

    "Näytetään laajuudet kun päätason suorituksen arviointipäivä on 1.8.2020 tai jälkeen" in {
      withAdditionalSuoritukset(KoskiSpecificMockOppijat.ysiluokkalainen, List(seitsemännenLuokanSuoritusLaajuudet_jälkeen_1_8_2020)) {
        val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), LocalDate.of(2020, 8, 1), vuosiluokka = "7")
        val oppijaOpiskeluoikeusOid = lastOpiskeluoikeus(KoskiSpecificMockOppijat.ysiluokkalainen.oid).oid.get
        val rivi = result.find(_.opiskeluoikeusOid == oppijaOpiskeluoikeusOid)

        rivi.get.aidinkieli should equal("9 laajuus: 1.0")
        rivi.get.kieliA2 should equal("Oppiaine puuttuu")
        rivi.get.biologia should equal("9* laajuus: 1.0")
        rivi.get.uskonto should equal("10 laajuus: 1.0")
      }
    }


    "Monta saman vuosiluokan suoritusta eritellään omiksi riveiksi" in {
      withAdditionalSuoritukset(KoskiSpecificMockOppijat.ysiluokkalainen, List(kahdeksannenLuokanLuokalleJääntiSuoritus.copy(luokka = "8C"))) {
        val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), LocalDate.of(2014, 8, 15), vuosiluokka = "8")
        val ynjevinOpiskeluoikeusOid = lastOpiskeluoikeus(KoskiSpecificMockOppijat.ysiluokkalainen.oid).oid.get
        val rivit = result.filter(_.opiskeluoikeusOid == ynjevinOpiskeluoikeusOid)

        rivit.length should equal(2)
        rivit should contain(defaultYnjeviExpectedKasiLuokkaRow.copy(opiskeluoikeusOid = ynjevinOpiskeluoikeusOid))
        rivit should contain(kahdeksannenLuokanLuokalleJääntiRow.copy(opiskeluoikeusOid = ynjevinOpiskeluoikeusOid))
      }
    }

    "Raportille ei päädy vuosiluokkien suorituksia joiden vahvistuspäivä on menneisyydessä" in {
      val hakuDate = date(2015, 5, 30)
      val rows = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), hakuDate, "7")
      rows.map(_.suorituksenVahvistuspaiva).foreach(paivaStr => {
        if (!paivaStr.isEmpty) {
          val paiva = LocalDate.parse(paivaStr)
          paiva.isBefore(hakuDate) shouldBe (false)
        }
      })
    }

    "Toiminta alueiden suoritukset joilla on arvosana näytetään omassa kolumnissaan" in {
      val suoritusToimintaAlueenOsasuorituksilla = yhdeksännenLuokanSuoritus.copy(osasuoritukset = toimintaAlueOsasuoritukset, vahvistus = None)
      withAdditionalSuoritukset(KoskiSpecificMockOppijat.toimintaAlueittainOpiskelija, List(suoritusToimintaAlueenOsasuorituksilla)) {
        val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), date(2015, 9, 1), "9")
        val rows = result.filter(_.oppijaOid == KoskiSpecificMockOppijat.toimintaAlueittainOpiskelija.oid)
        rows.length should equal(1)
        val row = rows.head

        row.valinnaisetPaikalliset should equal("")
        row.valinnaisetValtakunnalliset should equal("")
        row.vahvistetutToimintaAlueidenSuoritukset should equal(
          "motoriset taidot (1),kieli ja kommunikaatio (2),sosiaaliset taidot (3),päivittäisten toimintojen taidot (4),kognitiiviset taidot (5)"
        )
      }
    }

    "Elämänkatsomustiedon opiskelijoille ei yritetä hakea uskonnon oppimäärää" in {
      val suoritusElämänkatsomustiedolla = kahdeksannenLuokanSuoritus.copy(osasuoritukset = Some(List(suoritus(uskonto(oppiaineenKoodiarvo = "ET")).copy(arviointi = arviointi(5)))), vahvistus = None)
      withAdditionalSuoritukset(KoskiSpecificMockOppijat.vuosiluokkalainen, List(suoritusElämänkatsomustiedolla)) {
        val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), date(2016, 1, 1), "8")
        val rows = result.filter(_.oppijaOid == KoskiSpecificMockOppijat.vuosiluokkalainen.oid)
        rows.length should equal(1)
        val row = rows.head

        row.uskonto should equal("5")
        row.uskonnonOppimaara should equal("")
      }
    }

    "Peruskoulun päättävät" - {

      "Hakee tiedot peruskoulun oppimäärän suorituksesta" in {
        val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), LocalDate.of(2016, 1, 1), "9")
        val kaisanOpiskeluoikeusOid = getOpiskeluoikeudet(KoskiSpecificMockOppijat.koululainen.oid).find(_.tyyppi.koodiarvo == "perusopetus").get.oid.get
        val rivit = result.filter(_.opiskeluoikeusOid == kaisanOpiskeluoikeusOid)
        rivit.length should equal(1)
        val kaisanRivi = rivit.head

        kaisanRivi should equal(kaisanPäättötodistusRow.copy(opiskeluoikeusOid = kaisanOpiskeluoikeusOid))
      }

      "Jos oppilas on jäämässä luokalle käytetään yhdeksännen luokan vuosiluokka suoritusta" in {
        withAdditionalSuoritukset(KoskiSpecificMockOppijat.vuosiluokkalainen, List(perusopetuksenOppimääränSuoritus, yhdeksännenLuokanLuokallejääntiSuoritus, kahdeksannenLuokanSuoritus)) {
          val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), date(2016, 1, 1), "9")
          val opiskeluoikeusOid = getOpiskeluoikeudet(KoskiSpecificMockOppijat.vuosiluokkalainen.oid).find(_.tyyppi.koodiarvo == "perusopetus").get.oid.get
          val rows = result.filter(_.opiskeluoikeusOid == opiskeluoikeusOid)
          rows.length should equal(1)
          rows.head should equal(yhdeksännenLuokanLuokalleJääntiRow.copy(opiskeluoikeusOid = opiskeluoikeusOid))
        }
      }

      "Jos oppilas on jäämässä luokalle käytetään yhdeksännen luokan vuosiluokka suoritusta (ei vahvistusta luokalle jäänti suorituksella)" in {
        withAdditionalSuoritukset(KoskiSpecificMockOppijat.vuosiluokkalainen, List(perusopetuksenOppimääränSuoritus.copy(vahvistus = None), yhdeksännenLuokanLuokallejääntiSuoritus.copy(vahvistus = None), kahdeksannenLuokanSuoritus)) {
          val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), date(2016, 1, 1), "9")
          val opiskeluoikeusOid = getOpiskeluoikeudet(KoskiSpecificMockOppijat.vuosiluokkalainen.oid).find(_.tyyppi.koodiarvo == "perusopetus").get.oid.get
          val rows = result.filter(_.opiskeluoikeusOid == opiskeluoikeusOid)
          rows.length should equal(1)
          rows.head should equal(yhdeksännenLuokanLuokalleJääntiRow.copy(opiskeluoikeusOid = opiskeluoikeusOid, suorituksenTila = "kesken", suorituksenVahvistuspaiva = "", voimassaolevatVuosiluokat = "9"))
        }
      }

      "Jos oppilas on jäänyt aikaisemmin luokalle mutta nyt suorittanut perusopetuksen oppimäärän" in {
        withAdditionalSuoritukset(KoskiSpecificMockOppijat.vuosiluokkalainen, List(perusopetuksenOppimääränSuoritus.copy(vahvistus = None), yhdeksännenLuokanSuoritus, yhdeksännenLuokanLuokallejääntiSuoritus.copy(vahvistus = vahvistusPaikkakunnalla(date(2015, 1, 1))), kahdeksannenLuokanSuoritus)) {
          val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), date(2016, 1, 1), "9")
          val opiskeluoikeusOid = getOpiskeluoikeudet(KoskiSpecificMockOppijat.vuosiluokkalainen.oid).find(_.tyyppi.koodiarvo == "perusopetus").get.oid.get
          val rows = result.filter(_.opiskeluoikeusOid == opiskeluoikeusOid)
          rows.length should equal(1)
          rows.head should equal(kaisanPäättötodistusRow.copy(opiskeluoikeusOid = opiskeluoikeusOid, oppijaOid = KoskiSpecificMockOppijat.vuosiluokkalainen.oid, hetu = KoskiSpecificMockOppijat.vuosiluokkalainen.hetu, sukunimi = KoskiSpecificMockOppijat.vuosiluokkalainen.sukunimi, etunimet = KoskiSpecificMockOppijat.vuosiluokkalainen.etunimet, viimeisinTila = "lasna", suorituksenTila = "kesken", suorituksenVahvistuspaiva = "", luokka = Some("9A,9C")))
        }
      }

      "Ei tulosta tulosta päättötodistusta oppijoilla joilla ei ole yhdeksännen luokan opintoja" in {
        withAdditionalSuoritukset(KoskiSpecificMockOppijat.vuosiluokkalainen, List(perusopetuksenOppimääränSuoritus)) {
          val result = PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), date(2016, 6, 1), "9")
          result.map(_.oppijaOid) shouldNot contain(KoskiSpecificMockOppijat.vuosiluokkalainen.oid)
        }
      }

      "Suorituksen alkamispäivä huomioidaan" in {
        withAdditionalSuoritukset(
          KoskiSpecificMockOppijat.vuosiluokkalainen,
          List(
            yhdeksännenLuokanSuoritus.copy(alkamispäivä = Some(date(2015, 10, 1))),
            perusopetuksenOppimääränSuoritus
          )
        ) {
          PerusopetuksenVuosiluokkaRaportti
            .buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), date(2015, 9, 1), "9")
            .map(_.oppijaOid) shouldNot contain(KoskiSpecificMockOppijat.vuosiluokkalainen.oid)

          PerusopetuksenVuosiluokkaRaportti
            .buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), date(2015, 11, 1), "9")
            .map(_.oppijaOid) should contain(KoskiSpecificMockOppijat.vuosiluokkalainen.oid)
        }
      }
    }

    "Raportilla näytettävien vuosiluokan suoritusten valinta" - {
      lazy val raportti = {
        insertTestData
        PerusopetuksenVuosiluokkaRaportti.buildRaportti(repository, Seq(MockOrganisaatiot.jyväskylänNormaalikoulu), date(2015, 2, 2), "8")
      }
      "Viimeisin suoritus on vahvistettu suoritus haetulta vuosiluokalta" - {
        "Näytetään, vaikka hakupäivä ei osu alku- ja vahvistuspäivän väliin" in {
          raportti.filter(_.oppijaOid == KoskiSpecificMockOppijat.eero.oid).length shouldBe(1)
        }
        "Ei näytetä duplikaattina jos hakupäivä osuu alku- ja vahvistuspäivän väliin" in {
          raportti.filter(_.oppijaOid == KoskiSpecificMockOppijat.eerola.oid).length shouldBe(1)
        }
      }
      "Kaksi saman vuosiluokan suoritusta. Näytetään molemmat, kun hakupäivä osuu suoritusten alku- ja vahvistuspäivien väliin" in {
        raportti.filter(_.oppijaOid == KoskiSpecificMockOppijat.tero.oid).length shouldBe(2)
      }
      "Jos uusii vuosiluokkaa, ei poimita vanhaa suoritusta mukaan, kun hakupäivä ei osu vanhan suorituksen alku- ja vahvistuspäivän väliin" in {
        raportti.filter(_.oppijaOid === KoskiSpecificMockOppijat.teija.oid).length shouldBe(1)
      }
      "Jos löytyy ylemmän vuosiluokan suoritus, ei poimita mukaan" in {
        raportti.filter(_.oppijaOid === KoskiSpecificMockOppijat.markkanen.oid).length shouldBe(0)
      }
    }

    "Koulutustoimijalla voidaan hakea sen alaisuudessa olevien oppilaitosten suoritukset" - {

      lazy val raportitService = new RaportitService(KoskiApplicationForTests)

      "Vuosiluokan suoritus" in {
        val request = PerusopetuksenVuosiluokkaRequest(MockOrganisaatiot.helsinginKaupunki, None, "", date(2015, 1, 1), "8")
        val rows = raportitService.perusopetuksenVuosiluokka(request).sheets.collect { case dSheet: DataSheet => dSheet }
        rows.flatMap(_.rows.map(_.asInstanceOf[PerusopetusRow])).map(_.oppilaitoksenNimi).toSet should equal(Set("Stadin ammatti- ja aikuisopisto"))
      }

      "Peruskoulun päättävät" in {
        val request = PerusopetuksenVuosiluokkaRequest(MockOrganisaatiot.helsinginKaupunki, None, "", date(2015, 9, 1), "9")
        val rows = raportitService.perusopetuksenVuosiluokka(request).sheets.collect { case dSheet: DataSheet => dSheet }
        rows.flatMap(_.rows.map(_.asInstanceOf[PerusopetusRow])).map(_.oppilaitoksenNimi).toSet should equal(Set("Stadin ammatti- ja aikuisopisto"))
      }
    }

    "Näyttää raportin tulostushetken oppilaitoksen jos eri kuin nykyinen" in {
      lazy val raportti = PerusopetuksenVuosiluokkaRaportti.buildRaportti(
        repository,
        Seq(MockOrganisaatiot.kulosaarenAlaAste),
        LocalDate.of(2012, 8, 15),
        vuosiluokka = "6"
      )
      val oppijaOid = KoskiSpecificMockOppijat.organisaatioHistoriallinen.oid
      val rivi = raportti.find(_.oppijaOid == oppijaOid).get
      rivi.oppilaitosRaportointipäivänä should equal(Some("1.1.2006-31.12.2012: Jyväskylän normaalikoulu (Helsingin kaupunki)"))
    }
  }

  val defaultYnjeviExpectedKasiLuokkaRow = PerusopetusRow(
    opiskeluoikeusOid = "",
    oppilaitoksenNimi = "Jyväskylän normaalikoulu",
    oppilaitosRaportointipäivänä = None,
    lähdejärjestelmä = None,
    lähdejärjestelmänId = None,
    yksiloity = true,
    oppijaOid = KoskiSpecificMockOppijat.ysiluokkalainen.oid,
    hetu = KoskiSpecificMockOppijat.ysiluokkalainen.hetu,
    sukunimi = KoskiSpecificMockOppijat.ysiluokkalainen.sukunimi,
    etunimet = KoskiSpecificMockOppijat.ysiluokkalainen.etunimet,
    sukupuoli = None,
    kotikunta = Some("Jyväskylä"),
    luokka = Some("8C"),
    opiskeluoikeudenAlkamispäivä = Some(date(2008, 8, 15)),
    viimeisinTila = "lasna",
    tilaHakupaivalla = "lasna",
    suorituksenTila = "valmis",
    suorituksenAlkamispaiva = "2014-08-15",
    suorituksenVahvistuspaiva = "2015-05-30",
    voimassaolevatVuosiluokat = "9",
    jaaLuokalle = false,
    aidinkieli = "9",
    pakollisenAidinkielenOppimaara = "Suomen kieli ja kirjallisuus",
    kieliA1 = "8",
    kieliA1Oppimaara = "englanti",
    kieliA2 = "Oppiaine puuttuu",
    kieliA2Oppimaara = "Oppiaine puuttuu",
    kieliB = "8",
    kieliBOppimaara = "ruotsi",
    uskonto = "10",
    uskonnonOppimaara = "Ortodoksinen uskonto",
    historia = "8",
    yhteiskuntaoppi = "10",
    matematiikka = "9",
    kemia = "7",
    fysiikka = "9",
    biologia = "9*",
    maantieto = "9",
    musiikki = "7",
    kuvataide = "8",
    kotitalous = "8",
    terveystieto = "8",
    kasityo = "9",
    liikunta = "9",
    ymparistooppi = "Oppiaine puuttuu",
    opintoohjaus = "Oppiaine puuttuu",
    kayttaymisenArvio = "S",
    paikallistenOppiaineidenKoodit = "TH",
    pakollisetPaikalliset = "",
    valinnaisetPaikalliset = "Tietokoneen hyötykäyttö (TH) 9",
    valinnaisetValtakunnalliset = "ruotsi (B1) S,Kotitalous (KO) S,Liikunta (LI) S,saksa (B2) 9",
    valinnaisetLaajuus_SuurempiKuin_2Vuosiviikkotuntia = "saksa (B2) 4.0 9",
    valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia = "ruotsi (B1) 1.0 S,Kotitalous (KO) 1.0 S,Liikunta (LI) 0.5 S",
    numeroarviolliset_valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia = "",
    valinnaisetEiLaajuutta = "Tietokoneen hyötykäyttö (TH)",
    vahvistetutToimintaAlueidenSuoritukset = "",
    majoitusetu = false,
    kuljetusetu = false,
    kotiopetus = false,
    ulkomailla = false,
    perusopetuksenAloittamistaLykatty = false,
    aloittanutEnnenOppivelvollisuutta = false,
    pidennettyOppivelvollisuus = false,
    tehostetunTuenPaatos = false,
    joustavaPerusopetus = false,
    vuosiluokkiinSitoutumatonOpetus = false,
    vammainen = false,
    vaikeastiVammainen = false,
    oikeusMaksuttomaanAsuntolapaikkaan = false,
    sisäoppilaitosmainenMajoitus = false,
    koulukoti = false,
    erityisenTuenPaatosVoimassa = false,
    erityisenTuenPaatosToimialueittain = false,
    erityisenTuenPaatosToteutuspaikat = "",
    tukimuodot = ""
  )

  val ynjevinExpectedKasiLuokkaRowWithLisätiedot = defaultYnjeviExpectedKasiLuokkaRow.copy(
    majoitusetu = true,
    kuljetusetu = false,
    kotiopetus = false,
    ulkomailla = false,
    perusopetuksenAloittamistaLykatty = true,
    aloittanutEnnenOppivelvollisuutta = false,
    pidennettyOppivelvollisuus = true,
    tehostetunTuenPaatos = true,
    joustavaPerusopetus = true,
    vuosiluokkiinSitoutumatonOpetus = true,
    vammainen = true,
    vaikeastiVammainen = true,
    oikeusMaksuttomaanAsuntolapaikkaan = true,
    sisäoppilaitosmainenMajoitus = true,
    koulukoti = true,
    erityisenTuenPaatosVoimassa = true,
    erityisenTuenPaatosToimialueittain = true,
    erityisenTuenPaatosToteutuspaikat = "Opetus on kokonaan erityisryhmissä tai -luokassa,Opetuksesta 20-49 % on yleisopetuksen ryhmissä",
    tukimuodot = "Osa-aikainen erityisopetus"
  )

  val kahdeksannenLuokanLuokalleJääntiRow = defaultYnjeviExpectedKasiLuokkaRow.copy(
    jaaLuokalle = true,
    viimeisinTila = "lasna",
    suorituksenAlkamispaiva = "2013-08-15",
    suorituksenTila = "valmis",
    voimassaolevatVuosiluokat = "9",
    aidinkieli = "4",
    pakollisenAidinkielenOppimaara = "Suomen kieli ja kirjallisuus",
    kieliA1 = "4",
    kieliA1Oppimaara = "englanti",
    kieliB = "4",
    kieliBOppimaara = "ruotsi",
    uskonto = "4",
    historia = "4",
    yhteiskuntaoppi = "4",
    matematiikka = "4",
    kemia = "4",
    fysiikka = "4",
    biologia = "4",
    maantieto = "4",
    musiikki = "4",
    kuvataide = "4",
    kotitalous = "4",
    terveystieto = "4",
    kasityo = "4",
    liikunta = "4",
    kayttaymisenArvio = "",
    paikallistenOppiaineidenKoodit = "",
    pakollisetPaikalliset = "",
    valinnaisetPaikalliset = "",
    valinnaisetValtakunnalliset = "",
    valinnaisetLaajuus_SuurempiKuin_2Vuosiviikkotuntia = "",
    valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia = "",
    numeroarviolliset_valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia = "",
    valinnaisetEiLaajuutta = ""
  )

  val yhdeksännenLuokanLuokalleJääntiRow = kahdeksannenLuokanLuokalleJääntiRow.copy(
    oppijaOid = KoskiSpecificMockOppijat.vuosiluokkalainen.oid,
    hetu = KoskiSpecificMockOppijat.vuosiluokkalainen.hetu,
    sukunimi = KoskiSpecificMockOppijat.vuosiluokkalainen.sukunimi,
    etunimet = KoskiSpecificMockOppijat.vuosiluokkalainen.etunimet,
    kotikunta = None,
    luokka = Some("9A"),
    viimeisinTila = "lasna",
    suorituksenTila = "valmis",
    voimassaolevatVuosiluokat = "",
    suorituksenAlkamispaiva = "2014-08-16",
    suorituksenVahvistuspaiva = "2016-05-30"
  )

  val kaisanPäättötodistusRow = defaultYnjeviExpectedKasiLuokkaRow.copy(
    oppijaOid = KoskiSpecificMockOppijat.koululainen.oid,
    hetu = KoskiSpecificMockOppijat.koululainen.hetu,
    sukunimi = KoskiSpecificMockOppijat.koululainen.sukunimi,
    etunimet = KoskiSpecificMockOppijat.koululainen.etunimet,
    kotikunta = None,
    sukupuoli = None,
    luokka = Some("9C"),
    viimeisinTila = "valmistunut",
    tilaHakupaivalla = "lasna",
    suorituksenTila = "valmis",
    suorituksenAlkamispaiva = "",
    suorituksenVahvistuspaiva = "2016-06-04",
    voimassaolevatVuosiluokat = "",
    kayttaymisenArvio = ""
  )

  private def insertTestData = {
    val alkupäivä = Some(date(2015, 1, 1))
    val loppupäivä = Some(date(2016, 1, 1))
    addPerusopetus(KoskiSpecificMockOppijat.eero, createVuosiluokanSuoritus(Some(date(2014, 1, 1)), Some(date(2014, 12, 12))))
    addPerusopetus(KoskiSpecificMockOppijat.eerola, createVuosiluokanSuoritus(alkupäivä, loppupäivä))
    addPerusopetus(KoskiSpecificMockOppijat.markkanen, createVuosiluokanSuoritus(Some(date(2015, 1, 1)), None, vuosiluokka = 9), createVuosiluokanSuoritus(Some(date(2014, 1, 1)), Some(date(2015, 1, 1))))
    addPerusopetus(KoskiSpecificMockOppijat.teija, createVuosiluokanSuoritus(Some(date(2014, 1, 1)), Some(date(2014, 12, 12))), createVuosiluokanSuoritus(alkupäivä, None, vuosiluokka = 8))
    addPerusopetus(KoskiSpecificMockOppijat.tero, createVuosiluokanSuoritus(alkupäivä, loppupäivä), createVuosiluokanSuoritus(Some(date(2014, 1, 1)), Some(date(2015, 2, 2)), vuosiluokka = 8))
    reloadRaportointikanta
  }

  private def addPerusopetus(oppija: Henkilö, suoritukset: PerusopetuksenPäätasonSuoritus*) = {
    val opiskeluoikeus = PerusopetusExampleData.opiskeluoikeus(suoritukset = suoritukset.toList).copy(tila = opiskeluoikeusKesken)
    putOpiskeluoikeus(opiskeluoikeus, oppija) { verifyResponseStatusOk() }
  }

  private def opiskeluoikeusKesken = {
    NuortenPerusopetuksenOpiskeluoikeudenTila(List(NuortenPerusopetuksenOpiskeluoikeusjakso(date(2008, 1, 1), opiskeluoikeusLäsnä)))
  }

  private def createVuosiluokanSuoritus(alku: Option[LocalDate], loppu: Option[LocalDate], vuosiluokka: Int = 8) = {
    kahdeksannenLuokanSuoritus.copy(alkamispäivä = alku, vahvistus = loppu.map(vahvistusPaikkakunnalla(_)).getOrElse(None), koulutusmoduuli = PerusopetuksenLuokkaAste(vuosiluokka, perusopetuksenDiaarinumero))
  }

  private def withAdditionalSuoritukset(oppija: LaajatOppijaHenkilöTiedot, vuosiluokanSuoritus: List[PerusopetuksenPäätasonSuoritus])(f: => Any) = {
    resetFixtures
    val oo = getOpiskeluoikeudet(oppija.oid).collect { case oo: PerusopetuksenOpiskeluoikeus => oo }.head
    val lisatyllaVuosiluokanSuorituksella = oo.copy(suoritukset = (vuosiluokanSuoritus ::: oo.suoritukset), tila = NuortenPerusopetuksenOpiskeluoikeudenTila(List(NuortenPerusopetuksenOpiskeluoikeusjakso(date(2008, 8, 15), opiskeluoikeusLäsnä))))
    putOppija(Oppija(oppija, List(lisatyllaVuosiluokanSuorituksella))) {
      verifyResponseStatusOk()
      reloadRaportointikanta
      f
    }
  }

  private def withLisätiedotFixture[T <: PerusopetuksenOpiskeluoikeus](oppija: LaajatOppijaHenkilöTiedot, lisätiedot: PerusopetuksenOpiskeluoikeudenLisätiedot)(f: => Any) = {
    val oo = lastOpiskeluoikeus(oppija.oid).asInstanceOf[T].copy(lisätiedot = Some(lisätiedot))
    putOppija(Oppija(oppija, List(oo))) {
      verifyResponseStatusOk()
      reloadRaportointikanta
      f
    }
  }

  private val toimintaAlueOsasuoritukset = Some(List(
    toimintaAlueenSuoritus("1").copy(arviointi = arviointi("S", kuvaus = None)),
    toimintaAlueenSuoritus("2").copy(arviointi = arviointi("S", kuvaus = None)),
    toimintaAlueenSuoritus("3"),
    toimintaAlueenSuoritus("4")
  ))

  private val defaultQuery = makeQueryString(MockOrganisaatiot.jyväskylänNormaalikoulu, LocalDate.of(2016, 1, 1), "9")

  private def makeQueryString(oppilaitosOid: String, paiva: LocalDate, vuosiluokka: String) = {
    s"oppilaitosOid=$oppilaitosOid&paiva=${paiva.toString}&vuosiluokka=$vuosiluokka"
  }

  private val mennytAikajakso = Aikajakso(date(2000, 1, 1), Some(date(2001, 1, 1)))
  private val voimassaolevaAikajakso = Aikajakso(date(2008, 1, 1), None)
  private val aikajakso = voimassaolevaAikajakso.copy(loppu = Some(date(2018, 1, 1)))
  private val aikajaksot = Some(List(aikajakso))
  private val tukimuodot = Some(List(Koodistokoodiviite("1", "perusopetuksentukimuoto")))
  private val erityisenTuenPäätös = ErityisenTuenPäätös(
    alku = Some(date(2014, 1, 1)),
    loppu = Some(date(2018, 1, 1)),
    opiskeleeToimintaAlueittain = true,
    erityisryhmässä = Some(true),
    toteutuspaikka = Some(Koodistokoodiviite("1", "erityisopetuksentoteutuspaikka"))
  )

  private val perusopetuksenOpiskeluoikeudenLisätiedot = PerusopetuksenOpiskeluoikeudenLisätiedot(
    perusopetuksenAloittamistaLykätty = true,
    pidennettyOppivelvollisuus = Some(voimassaolevaAikajakso),
    tukimuodot = tukimuodot,
    erityisenTuenPäätös = Some(erityisenTuenPäätös),
    erityisenTuenPäätökset = Some(List(
      erityisenTuenPäätös.copy(alku = Some(date(2016, 1, 1)), toteutuspaikka = Some(Koodistokoodiviite("2", "erityisopetuksentoteutuspaikka"))),
      erityisenTuenPäätös.copy(toteutuspaikka = Some(Koodistokoodiviite("3", "erityisopetuksentoteutuspaikka")))
    )),
    tehostetunTuenPäätös = Some(tehostetunTuenPäätös.copy(alku = voimassaolevaAikajakso.alku, loppu = voimassaolevaAikajakso.loppu)),
    tehostetunTuenPäätökset = Some(List(tehostetunTuenPäätös.copy(alku = aikajakso.alku, loppu = aikajakso.loppu))),
    joustavaPerusopetus = Some(voimassaolevaAikajakso),
    vuosiluokkiinSitoutumatonOpetus = true,
    vammainen = aikajaksot,
    vaikeastiVammainen = aikajaksot,
    majoitusetu = Some(voimassaolevaAikajakso),
    kuljetusetu = Some(mennytAikajakso),
    oikeusMaksuttomaanAsuntolapaikkaan = Some(aikajakso),
    sisäoppilaitosmainenMajoitus = aikajaksot,
    koulukoti = aikajaksot
  )
}
