package fi.oph.koski.documentation

import java.time.LocalDate.{of => date}

import fi.oph.koski.documentation.ExampleData._
import fi.oph.koski.documentation.PerusopetusExampleData._
import fi.oph.koski.documentation.YleissivistavakoulutusExampleData.jyväskylänNormaalikoulu
import fi.oph.koski.henkilo.MockOppijat
import fi.oph.koski.henkilo.MockOppijat.asUusiOppija
import fi.oph.koski.localization.LocalizedStringImplicits._
import fi.oph.koski.schema._


object ExamplesAikuistenPerusopetus {
  val examples = List(
    Example("perusopetuksen oppiaineen oppimäärä - päättötodistus", "Aikuisopiskelija on suorittanut peruskoulun äidinkielen oppimäärän", aineopiskelija),
    Example("aikuisten perusopetuksen oppimäärä 2015", "Aikuisopiskelija on suorittanut aikuisten perusopetuksen oppimäärän opetussuunnitelman 2015 mukaisesti", aikuistenPerusopetuksenOppimäärä2015),
    Example("aikuisten perusopetuksen oppimäärä 2017", "Aikuisopiskelija on suorittanut aikuisten perusopetuksen oppimäärän alkuvaiheineen opetussuunnitelman 2017 mukaisesti", Oppija(exampleHenkilö, List(aikuistenPerusopetuksenOpiskeluoikeusAlkuvaiheineen)))
  )

  lazy val aineopiskelija = Oppija(
    asUusiOppija(MockOppijat.eero),
    List(AikuistenPerusopetuksenOpiskeluoikeus(
      päättymispäivä = Some(date(2016, 6, 4)),
      oppilaitos = Some(jyväskylänNormaalikoulu),
      koulutustoimija = None,
      suoritukset = List(
        AikuistenPerusopetuksenOppiaineenOppimääränSuoritus(
          koulutusmoduuli = äidinkieli("AI1", diaarinumero = Some("19/011/2015")),
          toimipiste = jyväskylänNormaalikoulu,
          arviointi = arviointi(9),
          suoritustapa = suoritustapaErityinenTutkinto,
          vahvistus = vahvistusPaikkakunnalla(),
          suorituskieli = suomenKieli
        )),
      tila = AikuistenPerusopetuksenOpiskeluoikeudenTila(
        List(
          AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2008, 8, 15), opiskeluoikeusLäsnä),
          AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2016, 6, 4), opiskeluoikeusValmistunut)
        )
      ),
      lisätiedot = Some(AikuistenPerusopetuksenOpiskeluoikeudenLisätiedot(vaikeastiVammainen = Some(List(Aikajakso(date(2014, 6, 6), None)))))
    ))
  )

  def aikuistenPerusopetuksenOppimäärä2015 = Oppija(
    exampleHenkilö,
    List(AikuistenPerusopetuksenOpiskeluoikeus(
      päättymispäivä = Some(date(2016, 6, 4)),
      oppilaitos = Some(jyväskylänNormaalikoulu),
      koulutustoimija = None,
      suoritukset = List(aikuistenPerusopetukseOppimääränSuoritus(aikuistenPerusopetus2015, oppiaineidenSuoritukset2015)),
      tila = AikuistenPerusopetuksenOpiskeluoikeudenTila(
        List(
          AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2008, 8, 15), opiskeluoikeusLäsnä),
          AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2016, 6, 4), opiskeluoikeusValmistunut)
        )
      )
    ))
  )

  def aikuistenPerusopetuksenOpiskeluoikeusAlkuvaiheineen = AikuistenPerusopetuksenOpiskeluoikeus(
    päättymispäivä = Some(date(2016, 6, 4)),
    oppilaitos = Some(jyväskylänNormaalikoulu),
    koulutustoimija = None,
    suoritukset = List(
      aikuistenPerusopetuksenAlkuvaiheenSuoritus,
      aikuistenPerusopetukseOppimääränSuoritus(aikuistenPerusopetus2017, oppiaineidenSuoritukset2017)
    ),
    tila = AikuistenPerusopetuksenOpiskeluoikeudenTila(
      List(
        AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2008, 8, 15), opiskeluoikeusLäsnä, Some(Koodistokoodiviite("1", Some("Valtionosuusrahoitteinen koulutus"), "opintojenrahoitus", None))),
        AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2016, 6, 4), opiskeluoikeusValmistunut)
      )
    )
  )

  def aikuistenPerusopetukseOppimääränSuoritus(koulutus: AikuistenPerusopetus, oppiaineet: Option[List[AikuistenPerusopetuksenOppiaineenSuoritus]]) = {
    AikuistenPerusopetuksenOppimääränSuoritus(
      koulutusmoduuli = koulutus,
      suorituskieli = suomenKieli,
      toimipiste = jyväskylänNormaalikoulu,
      vahvistus = vahvistusPaikkakunnalla(),
      suoritustapa = suoritustapaErityinenTutkinto,
      osasuoritukset = oppiaineet
    )
  }

  def oppiaineenSuoritus(aine: AikuistenPerusopetuksenOppiaine) = AikuistenPerusopetuksenOppiaineenSuoritus(
    koulutusmoduuli = aine,
    suorituskieli = None,
    arviointi = None
  )

  lazy val aikuistenPerusopetus2015 = AikuistenPerusopetus(Some("19/011/2015"))
  lazy val aikuistenPerusopetus2017 = AikuistenPerusopetus(Some("OPH-1280-2017"))
  lazy val aikuistenPerusopetuksenAlkuvaihe2017 = AikuistenPerusopetuksenAlkuvaihe(Some("OPH-1280-2017"))

  lazy val oppiaineidenSuoritukset2015 = Some(
    List(
      oppiaineenSuoritus(äidinkieli("AI1")).copy(
        arviointi = arviointi(9),
        osasuoritukset = Some(List(
          kurssinSuoritus2015("ÄI1"),
          kurssinSuoritus2015("ÄI2"),
          kurssinSuoritus2015("ÄI3", laajuus = LaajuusVuosiviikkotunneissa(1)),
          kurssinSuoritusPaikallinen("ÄI10", "Paikallinen äidinkielen kurssi"),
          kurssinSuoritusPaikallinen("ÄI11", "Toinen paikallinen äidinkielen kurssi", laajuus = LaajuusVuosiviikkotunneissa(1))
        ))
      ),
      oppiaineenSuoritus(aikuistenPerusopetuksenKieli("B1", "SV")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenPerusopetuksenKieli("B1", "SV").copy(pakollinen = false, laajuus = vuosiviikkotuntia(1))).copy(arviointi = hyväksytty),
      oppiaineenSuoritus(aikuistenPerusopetuksenKieli("A1", "EN")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("KT")).copy(arviointi = arviointi(10)),
      oppiaineenSuoritus(aikuistenOppiaine("HI")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("YH")).copy(arviointi = arviointi(10)),
      oppiaineenSuoritus(aikuistenOppiaine("MA")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("KE")).copy(arviointi = arviointi(7)),
      oppiaineenSuoritus(aikuistenOppiaine("FY")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("BI")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("GE")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("MU")).copy(arviointi = arviointi(7)),
      oppiaineenSuoritus(aikuistenOppiaine("KU")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("KO")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("KO").copy(pakollinen = false, laajuus = vuosiviikkotuntia(1))).copy(arviointi = hyväksytty),
      oppiaineenSuoritus(aikuistenOppiaine("TE")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("KS")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("LI")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("LI").copy(pakollinen = false, laajuus = vuosiviikkotuntia(0.5))).copy(arviointi = hyväksytty),
      oppiaineenSuoritus(aikuistenPerusopetuksenKieli("B2", "DE").copy(pakollinen = false, laajuus = vuosiviikkotuntia(4))).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(valinnainenAikuistenOppiaine("TH", "Tietokoneen hyötykäyttö", "Kurssilla tarjotaan yksityiskohtaisempaa tietokoneen, oheislaitteiden sekä käyttöjärjestelmän ja ohjelmien tuntemusta.")).copy(arviointi = arviointi(9))
    ))

  lazy val oppiaineidenSuoritukset2017 = Some(
    List(
      oppiaineenSuoritus(äidinkieli("AI1")).copy(
        arviointi = arviointi(9),
        osasuoritukset = Some(List(
          kurssinSuoritus2017("ÄI1").copy(tunnustettu = Some(OsaamisenTunnustaminen(None, "Osoittanut osaamisen käytännössä."))),
          kurssinSuoritus2017("ÄI2"),
          kurssinSuoritus2017("ÄI3", laajuus = LaajuusVuosiviikkotunneissa(1)),
          kurssinSuoritusPaikallinen("ÄI10", "Paikallinen äidinkielen kurssi")
        ))
      ),
      oppiaineenSuoritus(aikuistenPerusopetuksenKieli("B1", "SV")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenPerusopetuksenKieli("B1", "SV").copy(pakollinen = false, laajuus = vuosiviikkotuntia(1))).copy(arviointi = hyväksytty),
      oppiaineenSuoritus(aikuistenPerusopetuksenKieli("A1", "EN").copy(laajuus = Some(LaajuusKursseissa(1)))).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("KT")).copy(arviointi = arviointi(10)),
      oppiaineenSuoritus(aikuistenOppiaine("HI")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("YH")).copy(arviointi = arviointi(10)),
      oppiaineenSuoritus(aikuistenOppiaine("MA")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("KE")).copy(arviointi = arviointi(7)),
      oppiaineenSuoritus(aikuistenOppiaine("FY")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("BI")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("GE")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("MU")).copy(arviointi = arviointi(7)),
      oppiaineenSuoritus(aikuistenOppiaine("KU")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("KO")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("KO").copy(pakollinen = false, laajuus = vuosiviikkotuntia(1))).copy(arviointi = hyväksytty),
      oppiaineenSuoritus(aikuistenOppiaine("TE")).copy(arviointi = arviointi(8)),
      oppiaineenSuoritus(aikuistenOppiaine("KS")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("LI")).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(aikuistenOppiaine("LI").copy(pakollinen = false, laajuus = vuosiviikkotuntia(0.5))).copy(arviointi = hyväksytty),
      oppiaineenSuoritus(aikuistenPerusopetuksenKieli("B2", "DE").copy(pakollinen = false, laajuus = vuosiviikkotuntia(4))).copy(arviointi = arviointi(9)),
      oppiaineenSuoritus(valinnainenAikuistenOppiaine("TH", "Tietokoneen hyötykäyttö", "Kurssilla tarjotaan yksityiskohtaisempaa tietokoneen, oheislaitteiden sekä käyttöjärjestelmän ja ohjelmien tuntemusta.")).copy(arviointi = arviointi(9))
    ))

  def kurssinSuoritus2015(koodiarvo: String, laajuus: LaajuusVuosiviikkotunneissaTaiKursseissa = LaajuusKursseissa(1)) = AikuistenPerusopetuksenKurssinSuoritus(
    ValtakunnallinenAikuistenPerusopetuksenKurssi2015(Koodistokoodiviite(koodiarvo, "aikuistenperusopetuksenkurssit2015"), Some(laajuus)),
    arviointi = arviointi(9)
  )

  def kurssinSuoritus2017(koodiarvo: String, laajuus: LaajuusVuosiviikkotunneissaTaiKursseissa = LaajuusKursseissa(1)) = AikuistenPerusopetuksenKurssinSuoritus(
    ValtakunnallinenAikuistenPerusopetuksenPäättövaiheenKurssi2017(Koodistokoodiviite(koodiarvo, "aikuistenperusopetuksenpaattovaiheenkurssit2017"), Some(laajuus)),
    arviointi = arviointi(9)
  )

  def kurssinSuoritusPaikallinen(koodiarvo: String, kuvaus: String, laajuus: LaajuusVuosiviikkotunneissaTaiKursseissa = LaajuusKursseissa(1)) = AikuistenPerusopetuksenKurssinSuoritus(
    PaikallinenAikuistenPerusopetuksenKurssi(PaikallinenKoodi(koodiarvo, kuvaus), Some(laajuus)),
    arviointi = arviointi(9)
  )

  def aikuistenPerusopetuksenAlkuvaihe = {
    Oppija(
      exampleHenkilö,
      List(AikuistenPerusopetuksenOpiskeluoikeus(
        päättymispäivä = Some(date(2016, 6, 4)),
        oppilaitos = Some(jyväskylänNormaalikoulu),
        koulutustoimija = None,
        suoritukset = List(aikuistenPerusopetuksenAlkuvaiheenSuoritus),
        tila = AikuistenPerusopetuksenOpiskeluoikeudenTila(
          List(
            AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2008, 8, 15), opiskeluoikeusLäsnä),
            AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2016, 6, 4), opiskeluoikeusValmistunut)
          )
        )
      ))
    )
  }

  def aikuistenPerusopetuksenAlkuvaiheenSuoritus = AikuistenPerusopetuksenAlkuvaiheenSuoritus(
    aikuistenPerusopetuksenAlkuvaihe2017,
    suorituskieli = suomenKieli,
    toimipiste = jyväskylänNormaalikoulu,
    vahvistus = vahvistusPaikkakunnalla(),
    suoritustapa = suoritustapaErityinenTutkinto,
    osasuoritukset = alkuvaiheenOppiaineet
  )

  def kieli(oppiaine: String, kieli: String) = NuortenPerusopetuksenVierasTaiToinenKotimainenKieli(
    tunniste = Koodistokoodiviite(koodiarvo = oppiaine, koodistoUri = "koskioppiaineetyleissivistava"),
    kieli = Koodistokoodiviite(koodiarvo = kieli, koodistoUri = "kielivalikoima"))


  def alkuvaiheenOppiaineet = Some(List(
    alkuvaiheenOppiaineenSuoritus(AikuistenPerusopetuksenAlkuvaiheenÄidinkieliJaKirjallisuus(kieli = Koodistokoodiviite(koodiarvo = "AI1", koodistoUri = "oppiaineaidinkielijakirjallisuus"))).copy(arviointi = arviointi(9), osasuoritukset = Some(List(
      alkuvaiheenKurssinSuoritus("LÄI1").copy(tunnustettu = Some(OsaamisenTunnustaminen(None, "Osoittanut osaamisen käytännössä."))),
      alkuvaiheenKurssinSuoritus("LÄI2"),
      alkuvaiheenKurssinSuoritus("LÄI3"),
      alkuvaiheenKurssinSuoritus("LÄI4"),
      alkuvaiheenKurssinSuoritus("LÄI5"),
      alkuvaiheenKurssinSuoritus("LÄI6"),
      alkuvaiheenKurssinSuoritus("LÄI7"),
      alkuvaiheenKurssinSuoritus("LÄI8"),
      alkuvaiheenKurssinSuoritus("LÄI9"),
      alkuvaiheenKurssinSuoritus("AÄI1"),
      alkuvaiheenKurssinSuoritus("AÄI2"),
      alkuvaiheenKurssinSuoritus("AÄI3"),
      alkuvaiheenKurssinSuoritus("AÄI4"),
      alkuvaiheenKurssinSuoritus("AÄI5"),
      alkuvaiheenKurssinSuoritus("AÄI6")
    ))),
    alkuvaiheenOppiaineenSuoritus(AikuistenPerusopetuksenAlkuvaiheenVierasKieli(kieli=Koodistokoodiviite(koodiarvo = "EN", koodistoUri = "kielivalikoima"))).copy(arviointi = arviointi(7), osasuoritukset = Some(List(
      alkuvaiheenKurssinSuoritus("AENA1"),
      alkuvaiheenKurssinSuoritus("AENA2"),
      alkuvaiheenKurssinSuoritus("AENA3"),
      alkuvaiheenKurssinSuoritus("AENA4")
    ))),
    alkuvaiheenOppiaineenSuoritus(alkuvaiheenOppiaine("MA")).copy(arviointi = arviointi(10), osasuoritukset = Some(List(
      alkuvaiheenKurssinSuoritus("LMA1"),
      alkuvaiheenKurssinSuoritus("LMA2"),
      alkuvaiheenKurssinSuoritus("LMA3")
    ))),
    // Yhteiskuntatietous ja kulttuurintuntemus
    alkuvaiheenOppiaineenSuoritus(alkuvaiheenOppiaine("YH")).copy(arviointi = arviointi(8), osasuoritukset = Some(List(
      alkuvaiheenKurssinSuoritus("LYK1"),
      alkuvaiheenKurssinSuoritus("LYK2", laajuus = LaajuusVuosiviikkotunneissa(1)),
      alkuvaiheenPaikallisenKurssinSuoritus("LYKX", "Kulttuurinen moniarvoisuus"),
      alkuvaiheenPaikallisenKurssinSuoritus("LYKY", "Tasa-arvo yhteiskunnassa", laajuus = LaajuusVuosiviikkotunneissa(1))
    ))),
    // Ympäristö- ja luonnontieto
    alkuvaiheenOppiaineenSuoritus(alkuvaiheenOppiaine("YL")).copy(arviointi = arviointi(8), osasuoritukset = Some(List(
      alkuvaiheenKurssinSuoritus("LYL1")
    ))),
    // Terveystieto
    alkuvaiheenOppiaineenSuoritus(alkuvaiheenOppiaine("TE")).copy(arviointi = arviointi(10), osasuoritukset = Some(List(
      alkuvaiheenKurssinSuoritus("ATE1")
    ))),
    // Opinto-ohjaus
    alkuvaiheenOppiaineenSuoritus(alkuvaiheenOppiaine("OP")).copy(arviointi = arviointi("S"))
  ))

  def alkuvaiheenOppiaine(aine: String) = MuuAikuistenPerusopetuksenAlkuvaiheenOppiaine(tunniste = Koodistokoodiviite(koodistoUri = "aikuistenperusopetuksenalkuvaiheenoppiaineet", koodiarvo = aine))

  def alkuvaiheenOppiaineenSuoritus(aine: AikuistenPerusopetuksenAlkuvaiheenOppiaine) = AikuistenPerusopetuksenAlkuvaiheenOppiaineenSuoritus(
    koulutusmoduuli = aine,
    suorituskieli = None,
    arviointi = None
  )

  def alkuvaiheenKurssinSuoritus(koodiarvo: String, laajuus: LaajuusVuosiviikkotunneissaTaiKursseissa = LaajuusKursseissa(1)) = AikuistenPerusopetuksenAlkuvaiheenKurssinSuoritus(
    ValtakunnallinenAikuistenPerusopetuksenAlkuvaiheenKurssi2017(Koodistokoodiviite(koodiarvo, "aikuistenperusopetuksenalkuvaiheenkurssit2017"), Some(laajuus)),
    arviointi = arviointi(9)
  )

  def alkuvaiheenPaikallisenKurssinSuoritus(koodiarvo: String, nimi: String, laajuus: LaajuusVuosiviikkotunneissaTaiKursseissa = LaajuusKursseissa(1)) = AikuistenPerusopetuksenAlkuvaiheenKurssinSuoritus(
    PaikallinenAikuistenPerusopetuksenAlkuvaiheenKurssi(PaikallinenKoodi(koodiarvo, nimi), Some(laajuus)),
    arviointi = arviointi(9)
  )

  def äidinkieli(kieli: String, diaarinumero: Option[String] = None) = AikuistenPerusopetuksenÄidinkieliJaKirjallisuus(
    perusteenDiaarinumero = diaarinumero,
    kieli = Koodistokoodiviite(koodiarvo = kieli, koodistoUri = "oppiaineaidinkielijakirjallisuus")
  )

  def aikuistenPerusopetuksenKieli(oppiaine: String, kieli: String) = AikuistenPerusopetuksenVierasTaiToinenKotimainenKieli(
    tunniste = Koodistokoodiviite(koodiarvo = oppiaine, koodistoUri = "koskioppiaineetyleissivistava"),
    kieli = Koodistokoodiviite(koodiarvo = kieli, koodistoUri = "kielivalikoima"))

  def aikuistenOppiaine(aine: String, laajuus: Option[LaajuusVuosiviikkotunneissa] = None) = MuuAikuistenPerusopetuksenOppiaine(tunniste = Koodistokoodiviite(koodistoUri = "koskioppiaineetyleissivistava", koodiarvo = aine), laajuus = laajuus)

 def valinnainenAikuistenOppiaine(aine: String, nimi: String, kuvaus: String, laajuus: Option[LaajuusVuosiviikkotunneissa] = None) =
    AikuistenPerusopetuksenPaikallinenOppiaine(tunniste = PaikallinenKoodi(koodiarvo = aine, nimi = nimi), laajuus = laajuus, kuvaus = kuvaus)
}
