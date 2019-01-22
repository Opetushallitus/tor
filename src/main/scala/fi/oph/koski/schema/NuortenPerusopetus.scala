package fi.oph.koski.schema

import java.time.{LocalDate, LocalDateTime}

import fi.oph.koski.koskiuser.Rooli
import fi.oph.koski.schema.annotation._
import fi.oph.scalaschema.annotation._
import mojave.{Traversal, traversal}

@Description("Perusopetuksen opiskeluoikeus")
case class PerusopetuksenOpiskeluoikeus(
  oid: Option[String] = None,
  versionumero: Option[Int]  = None,
  aikaleima: Option[LocalDateTime] = None,
  lähdejärjestelmänId: Option[LähdejärjestelmäId] = None,
  oppilaitos: Option[Oppilaitos],
  koulutustoimija: Option[Koulutustoimija] = None,
  @Hidden
  sisältyyOpiskeluoikeuteen: Option[SisältäväOpiskeluoikeus] = None,
  @Description("Oppijan oppimäärän päättymispäivä")
  päättymispäivä: Option[LocalDate] = None,
  tila: NuortenPerusopetuksenOpiskeluoikeudenTila,
  lisätiedot: Option[PerusopetuksenOpiskeluoikeudenLisätiedot] = None,
  suoritukset: List[PerusopetuksenPäätasonSuoritus],
  @KoodistoKoodiarvo(OpiskeluoikeudenTyyppi.perusopetus.koodiarvo)
  tyyppi: Koodistokoodiviite = OpiskeluoikeudenTyyppi.perusopetus
) extends KoskeenTallennettavaOpiskeluoikeus {
  override def withOppilaitos(oppilaitos: Oppilaitos) = this.copy(oppilaitos = Some(oppilaitos))
  override def withKoulutustoimija(koulutustoimija: Koulutustoimija) = this.copy(koulutustoimija = Some(koulutustoimija))
  override def arvioituPäättymispäivä = None
}

object PerusopetuksenOpiskeluoikeus {
  val päätasonSuorituksetTraversal: Traversal[Oppija, Suoritus] = traversal[Oppija]
    .field[Seq[Opiskeluoikeus]]("opiskeluoikeudet").items
    .ifInstanceOf[PerusopetuksenOpiskeluoikeus]
    .field[List[Suoritus]]("suoritukset").items

  val oppimääränArvioinnitTraversal: Traversal[Suoritus, Option[List[Arviointi]]] = traversal[Suoritus]
    .ifInstanceOf[PerusopetuksenOppimääränSuoritus]
    .field[Option[List[Suoritus]]]("osasuoritukset").items.items
    .field[Option[List[Arviointi]]]("arviointi")

  val vuosiluokanArvioinnitTraversal: Traversal[Suoritus, Option[List[Arviointi]]] = {
    traversal[Suoritus]
      .ifInstanceOf[PerusopetuksenVuosiluokanSuoritus]
      .field[Option[List[OppiaineenTaiToiminta_AlueenSuoritus]]]("osasuoritukset").items.items
      .field[Option[List[Arviointi]]]("arviointi")
  }

  val käyttäytymisenArviointiTraversal: Traversal[Suoritus, Option[PerusopetuksenKäyttäytymisenArviointi]] = {
    traversal[Suoritus]
      .ifInstanceOf[PerusopetuksenVuosiluokanSuoritus]
      .field[Option[PerusopetuksenKäyttäytymisenArviointi]]("käyttäytymisenArvio")
  }

  val oppiaineenOppimääränArvioinnitTraversal: Traversal[Suoritus, Option[List[Arviointi]]] = traversal[Suoritus]
    .ifInstanceOf[NuortenPerusopetuksenOppiaineenOppimääränSuoritus]
    .field[Option[List[Arviointi]]]("arviointi")
}

case class PerusopetuksenOpiskeluoikeudenLisätiedot(
  @Description("Perusopetuksen aloittamisesta lykkäys (true/false). Oppilas saanut luvan aloittaa perusopetuksen myöhemmin.")
  @Tooltip("Perusopetuksen aloittamisesta lykkäys. Oppilas saanut luvan aloittaa perusopetuksen myöhemmin.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  @OksaUri("tmpOKSAID242", "koulunkäynnin aloittamisen lykkääminen")
  @DefaultValue(false)
  perusopetuksenAloittamistaLykätty: Boolean = false,
  @Description("Perusopetuksen aloituksen aikaistaminen (true/false). Oppilas aloittanut perusopetuksen ennen oppivelvollisuusikää.")
  @Tooltip("Perusopetuksen aloitusta aikaistettu, eli oppilas aloittanut peruskoulun ennen oppivelvollisuusikää.")
  @DefaultValue(false)
  aloittanutEnnenOppivelvollisuutta: Boolean = false,
  @Description("Pidennetty oppivelvollisuus alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, että oppilaalla ei ole pidennettyä oppivelvollisuutta. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen pidennetyn oppivelvollisuuden alkamis- ja päättymispäivät. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  @OksaUri("tmpOKSAID517", "pidennetty oppivelvollisuus")
  pidennettyOppivelvollisuus: Option[Aikajakso] = None,
  @KoodistoUri("perusopetuksentukimuoto")
  @Description("Oppilaan saamat laissa säädetyt tukimuodot.")
  @Tooltip("Oppilaan saamat laissa säädetyt tukimuodot. Voi olla useita.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tukimuodot: Option[List[Koodistokoodiviite]] = None,
  @Description("Erityisen tuen päätös alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, että päätöstä ei ole tehty. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen erityisen tuen päätöksen alkamis- ja päättymispäivät. Rahoituksen laskennassa käytettävä tieto.")
  @OksaUri("tmpOKSAID281", "henkilökohtainen opetuksen järjestämistä koskeva suunnitelma")
  @Deprecated("Käytä korvaavaa kenttää Erityisen tuen päätökset")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  erityisenTuenPäätös: Option[ErityisenTuenPäätös] = None,
  @Description("Erityisen tuen päätökset alkamis- ja päättymispäivineen. Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen erityisen tuen päätösten alkamis- ja päättymispäivät. Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @OksaUri("tmpOKSAID281", "henkilökohtainen opetuksen järjestämistä koskeva suunnitelma")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  erityisenTuenPäätökset: Option[List[ErityisenTuenPäätös]] = None,
  @Description("Tehostetun tuen päätös alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, että päätöstä ei ole tehty.")
  @Tooltip("Mahdollisen tehostetun tuen päätös päätöksen alkamis- ja päättymispäivät.")
  @OksaUri("tmpOKSAID511", "tehostettu tuki")
  @Deprecated("Käytä korvaavaa kenttää Tehostetun tuen päätökset")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätös: Option[Aikajakso] = None,
  @Description("Tehostetun tuen päätös. Lista alku-loppu päivämääräpareja.")
  @Tooltip("Mahdollisen tehostetun tuen päätösten alkamis- ja päättymispäivät. Voi olla useita erillisiä jaksoja.")
  @OksaUri("tmpOKSAID511", "tehostettu tuki")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätökset: Option[List[Aikajakso]] = None,
  @Description("Opiskelu joustavassa perusopetuksessa (JOPO) alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, ettei oppilas ole joustavassa perusopetuksessa. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen joustavan perusopetuksen (JOPO) alkamis- ja päättymispäivät. Rahoituksen laskennassa käytettävä tieto.")
  @OksaUri("tmpOKSAID453", "joustava perusopetus")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  joustavaPerusopetus: Option[Aikajakso] = None,
  @Description("Tieto opiskelusta kotiopetuksessa huoltajan päätöksestä alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, ettei oppilas ole kotiopetuksessa. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto mahdollisesta opiskelusta kotiopetuksessa huoltajan päätöksestä alkamis- ja päättymispäivineen. Rahoituksen laskennassa käytettävä tieto.")
  kotiopetus: Option[Aikajakso] = None,
  @Description("Tieto opiskelusta ulkomailla huoltajan ilmoituksesta alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, ettei oppilas ole ulkomailla. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto opiskelusta ulkomailla huoltajan ilmoituksesta alkamis- ja päättymispäivineen. Rahoituksen laskennassa käytettävä tieto.")
  ulkomailla: Option[Aikajakso] = None,
  @Description("Oppilas on vuosiluokkiin sitomattomassa opetuksessa (kyllä/ei).")
  @Tooltip("Onko oppilas vuosiluokkiin sitomattomassa opetuksessa.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  @DefaultValue(false)
  @Title("Vuosiluokkiin sitomaton opetus")
  vuosiluokkiinSitoutumatonOpetus: Boolean = false,
  @Description("Onko oppija muu kuin vaikeimmin kehitysvammainen. Lista alku-loppu päivämääräpareja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto siitä, onko oppija muu kuin vaikeimmin kehitysvammainen (alku- ja loppupäivämäärät). Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  vammainen: Option[List[Aikajakso]] = None,
  @Description("Onko oppija vaikeasti kehitysvammainen. Lista alku-loppu päivämääräpareja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto siitä, onko oppija vaikeasti kehitysvammainen (alku- ja loppupäivämäärät). Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  vaikeastiVammainen: Option[List[Aikajakso]] = None,
  @Description("Oppilaalla on majoitusetu (alku- ja loppupäivämäärä). Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto siitä, jos oppilaalla on majoitusetu (alku- ja loppupäivämäärät). Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  majoitusetu: Option[Aikajakso] = None,
  @Description("Oppilaalla on kuljetusetu (alku- ja loppupäivämäärät).")
  @Tooltip("Tieto siitä, jos oppilaalla on kuljetusetu (alku- ja loppupäivämäärät).")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  kuljetusetu: Option[Aikajakso] = None,
  @Description("Oppilaalla on oikeus maksuttomaan asuntolapaikkaan (alku- ja loppupäivämäärät).")
  @Tooltip("Tieto siitä, jos oppilaalla on oikeus maksuttomaan asuntolapaikkaan (alku- ja loppupäivämäärät).")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN, Rooli.LUOTTAMUKSELLINEN_KELA_SUPPEA, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  oikeusMaksuttomaanAsuntolapaikkaan: Option[Aikajakso] = None,
  @Description("Sisäoppilaitosmuotoinen majoitus, aloituspäivä ja loppupäivä. Lista alku-loppu päivämääräpareja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen sisäoppilaitosmuotoisen majoituksen aloituspäivä ja loppupäivä. Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN, Rooli.LUOTTAMUKSELLINEN_KELA_SUPPEA, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  sisäoppilaitosmainenMajoitus: Option[List[Aikajakso]] = None,
  @Description("Oppija on koulukotikorotuksen piirissä, aloituspäivä ja loppupäivä. Lista alku-loppu päivämääräpareja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto siitä, jos oppija on koulukotikorotuksen piirissä (aloituspäivä ja loppupäivä). Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  koulukoti: Option[List[Aikajakso]] = None
) extends OpiskeluoikeudenLisätiedot

@Description("Oppivelvollisen erityisen tuen päätöstiedot")
case class ErityisenTuenPäätös(
  @Description("Jakson alkamispäivämäärä. Muoto YYYY-MM-DD")
  alku: Option[LocalDate],
  @Description("Jakson loppumispäivämäärä. Muoto YYYY-MM-DD")
  loppu: Option[LocalDate],
  @Description("""Oppilas opiskelee toiminta-alueittain (true/false).
Toiminta-alueittain opiskelussa oppilaalla on yksilöllistetty oppimäärä ja opetus järjestetty toiminta-alueittain.
Tuolloin oppilaalla on aina erityisen tuen päätös.
Oppilaan opetussuunnitelmaan kuuluvat toiminta-alueet ovat motoriset taidot, kieli ja kommunikaatio, sosiaaliset taidot, päivittäisten toimintojen taidot ja kognitiiviset taidot.
Huom: toiminta-alue arviointeineen on kuvattu oppiaineen suorituksessa.""")
  @Tooltip("Opiskeleeko oppilas toiminta-alueittain? Toiminta-alueittain opiskelussa oppilaalla on yksilöllistetty oppimäärä ja opetus järjestetty toiminta-alueittain. Tuolloin oppilaalla on aina erityisen tuen päätös. Oppilaan opetussuunnitelmaan kuuluvat toiminta-alueet ovat motoriset taidot, kieli ja kommunikaatio, sosiaaliset taidot, päivittäisten toimintojen taidot ja kognitiiviset taidot.")
  @Title("Opiskelee toiminta-alueittain")
  opiskeleeToimintaAlueittain: Boolean = false,
  @Description("Tätä tietoa ei tarvitse jatkossa välittää. Tieto erityisryhmän toteutuspaikasta välitetään toteutuspaikka-rakenteessa.")
  @Tooltip("Suorittaako erityisoppilas koulutusta omassa erityisryhmässään vai inklusiivisesti opetuksen mukana.")
  @OksaUri("tmpOKSAID444", "opetusryhmä")
  @Title("Opiskelee erityisryhmässä")
  erityisryhmässä: Option[Boolean],
  @Description("Tieto erityisopetuksen toteutuspaikasta välitetään tämän rakenteen koodiston mukaisesti.")
  @Tooltip("Tieto erityisopetuksen toteutuspaikasta.")
  @KoodistoUri("erityisopetuksentoteutuspaikka")
  @Title("Erityisopetuksen toteutuspaikka")
  toteutuspaikka: Option[Koodistokoodiviite] = None
)

trait PerusopetuksenPäätasonSuoritus extends KoskeenTallennettavaPäätasonSuoritus with Toimipisteellinen with MonikielinenSuoritus with Suorituskielellinen

@Description("Perusopetuksen vuosiluokan suoritus. Nämä suoritukset näkyvät lukuvuositodistuksella")
case class PerusopetuksenVuosiluokanSuoritus(
  @Description("Luokka-aste ilmaistaan perusopetuksenluokkaaste-koodistolla.")
  @Tooltip("Vuosiluokkasuorituksen luokka-aste ja vuosiluokkasuorituksen opetussuunnitelman perusteen diaarinumero.")
  @Title("Luokka-aste")
  koulutusmoduuli: PerusopetuksenLuokkaAste,
  @Description("Luokan tunniste, esimerkiksi 9C.")
  @Tooltip("Luokan tunniste, esimerkiksi 9C.")
  luokka: String,
  toimipiste: OrganisaatioWithOid,
  @Description("Vuosiluokan alkamispäivä")
  @Tooltip("Vuosiluokan alkamispäivä")
  override val alkamispäivä: Option[LocalDate] = None,
  @Description("Varsinaisen todistuksen saantipäivämäärä")
  vahvistus: Option[HenkilövahvistusPaikkakunnalla] = None,
  suorituskieli: Koodistokoodiviite,
  @Tooltip("Mahdolliset muut suorituskielet.")
  muutSuorituskielet: Option[List[Koodistokoodiviite]] = None,
  @Description("Oppilaan kotimaisten kielten kielikylvyn kieli.")
  @Tooltip("Oppilaan kotimaisten kielten kielikylvyn kieli.")
  @KoodistoUri("kieli")
  @OksaUri("tmpOKSAID439", "kielikylpy")
  kielikylpykieli: Option[Koodistokoodiviite] = None,
  @Description("Tieto siitä, että oppilas jää luokalle")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  @DefaultValue(false)
  @Title("Oppilas jää luokalle")
  jääLuokalle: Boolean = false,
  käyttäytymisenArvio: Option[PerusopetuksenKäyttäytymisenArviointi] = None,
  @Description("Vuosiluokan suoritukseen liittyvät oppiaineen suoritukset")
  @Title("Oppiaineet")
  override val osasuoritukset: Option[List[OppiaineenTaiToiminta_AlueenSuoritus]] = None,
  @Tooltip("Todistuksella näkyvät lisätiedot. Esimerkiksi, jos oppilaan oppitunneista lukuvuoden aikana vähintään 25 % on opetettu muulla kuin koulun opetuskielellä.")
  todistuksellaNäkyvätLisätiedot: Option[LocalizedString] = None,
  @KoodistoKoodiarvo("perusopetuksenvuosiluokka")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("perusopetuksenvuosiluokka", koodistoUri = "suorituksentyyppi"),
  @Tooltip("Perusopetuksen vuosiluokkatodistuksen liitetieto (liitteenä annettu arvio käyttäytymisestä tai työskentelystä).")
  liitetiedot: Option[List[PerusopetuksenVuosiluokanSuorituksenLiite]] = None
) extends PerusopetuksenPäätasonSuoritus with Todistus with Arvioinniton

trait PerusopetuksenOppimääränSuoritus extends Suoritus with Todistus with Arvioinniton with SuoritustavallinenPerusopetuksenSuoritus {
  @Title("Koulutus")
  override def koulutusmoduuli: Perusopetus
  def suorituskieli: Koodistokoodiviite
  def muutSuorituskielet: Option[List[Koodistokoodiviite]]
  @Description("Päättötodistukseen liittyvät oppiaineen suoritukset")
  @Title("Oppiaineet")
  override def osasuoritukset: Option[List[Suoritus]] = None
  def todistuksellaNäkyvätLisätiedot: Option[LocalizedString]
  def oppiaineet = osasuoritukset.toList.flatten
}

trait SuoritustavallinenPerusopetuksenSuoritus extends Suoritus {
  @KoodistoUri("perusopetuksensuoritustapa")
  @Description("Tieto siitä, suoritetaanko perusopetusta normaalina koulutuksena vai erityisenä tutkintona.")
  @Tooltip("Tieto siitä, suoritetaanko perusopetusta normaalina koulutuksena vai erityisenä tutkintona.")
  def suoritustapa: Koodistokoodiviite
}

trait SuoritustapanaMahdollisestiErityinenTutkinto extends Suoritus {
  @Description("Käytetään tilanteessa jossa oppija opiskelee yksittäisen oppiaineen erityisenä tutkintona")
  @KoodistoUri("perusopetuksensuoritustapa")
  @KoodistoKoodiarvo("erityinentutkinto")
  def suoritustapa: Option[Koodistokoodiviite]
}

@Description("Perusopetuksen koko oppimäärän suoritus. Nämä suoritukset näkyvät päättötodistuksella")
case class NuortenPerusopetuksenOppimääränSuoritus(
  @Tooltip("Suoritettava koulutus ja koulutuksen opetussuunnitelman perusteiden diaarinumero.")
  koulutusmoduuli: NuortenPerusopetus,
  toimipiste: OrganisaatioWithOid,
  vahvistus: Option[HenkilövahvistusPaikkakunnalla] = None,
  suoritustapa: Koodistokoodiviite,
  suorituskieli: Koodistokoodiviite,
  @Tooltip("Mahdolliset muut suorituskielet.")
  muutSuorituskielet: Option[List[Koodistokoodiviite]] = None,
  override val osasuoritukset: Option[List[OppiaineenTaiToiminta_AlueenSuoritus]] = None,
  @Description("Vapaamuotoinen tekstikenttä")
  @Tooltip("Todistuksella näkyvät lisätiedot. Esimerkiksi merkintä siitä, että oppilas on opiskellut tähdellä (*) merkityt oppiaineet yksilöllistetyn oppimäärän mukaan.")
  todistuksellaNäkyvätLisätiedot: Option[LocalizedString] = None,
  @KoodistoKoodiarvo("perusopetuksenoppimaara")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("perusopetuksenoppimaara", koodistoUri = "suorituksentyyppi")
) extends PerusopetuksenPäätasonSuoritus with PerusopetuksenOppimääränSuoritus

@Description("Vuosiluokan todistuksen liitetieto")
case class PerusopetuksenVuosiluokanSuorituksenLiite(
  @Description("Liitetiedon tyyppi kooditettuna.")
  @Tooltip("Liitetiedon tyyppi, eli onko kyseessä käyttäytymisen vai työskentelyn arviointi.")
  @KoodistoUri("perusopetuksentodistuksenliitetieto")
  @KoodistoKoodiarvo("kayttaytyminen")
  @KoodistoKoodiarvo("tyoskentely")
  tunniste: Koodistokoodiviite,
  @Description("Liitetiedon kuvaus.")
  @Tooltip("Liitetiedon kuvaus, eli sanallinen käyttäytymisen tai työskentelyn arviointi.")
  kuvaus: LocalizedString
)

trait Toiminta_AlueenSuoritus extends Suoritus

sealed trait OppiaineenTaiToiminta_AlueenSuoritus extends Suoritus with MahdollisestiSuorituskielellinen

@Description("Perusopetuksen oppiaineen suoritus osana perusopetuksen oppimäärän tai vuosiluokan suoritusta")
case class NuortenPerusopetuksenOppiaineenSuoritus(
  koulutusmoduuli: NuortenPerusopetuksenOppiaine,
  @Description("Jos oppilas opiskelee yhdessä yksilöllistetyn oppimäärän mukaan, myös päättöarviointi voi näissä aineissa olla sanallinen.")
  @Tooltip("Onko oppilas opiskellut oppiaineessa yksilöllisen oppimäärän. Jos oppilas opiskelee yhdessä yksilöllistetyn oppimäärän mukaan, myös päättöarviointi voi näissä aineissa olla sanallinen.")
  yksilöllistettyOppimäärä: Boolean = false,
  @Description("Tieto siitä, onko oppiaineen opetus painotettu (true/false). Painotetun opetuksen (oppiaine tai oppiainekokonaisuus, kaksikielinen opetus) tavoitteet ja arviointiperusteet ovat valtakunnallisen opetussuunnitelman perusteiden mukaiset.")
  @Tooltip("Onko oppilas ollut oppiaineessa painotetussa opetuksessa. Painotetun opetuksen (oppiaine tai oppiainekokonaisuus, kaksikielinen opetus) tavoitteet ja arviointiperusteet ovat valtakunnallisen opetussuunnitelman perusteiden mukaiset.")
  painotettuOpetus: Boolean = false,
  arviointi: Option[List[PerusopetuksenOppiaineenArviointi]] = None,
  suorituskieli: Option[Koodistokoodiviite] = None,
  @KoodistoKoodiarvo("perusopetuksenoppiaine")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "perusopetuksenoppiaine", koodistoUri = "suorituksentyyppi"),
  suoritustapa: Option[Koodistokoodiviite] = None
) extends PerusopetuksenOppiaineenSuoritus with OppiaineenTaiToiminta_AlueenSuoritus with Vahvistukseton with Yksilöllistettävä with MahdollisestiSuorituskielellinen with SuoritustapanaMahdollisestiErityinenTutkinto with PerusopetukseenValmistavanOpetuksenOsasuoritus

@Description("Perusopetuksen yksittäisen oppiaineen oppimäärän suoritus erillisenä kokonaisuutena")
trait PerusopetuksenOppiaineenOppimääränSuoritus {
  @Description("Päättötodistukseen liittyvät oppiaineen suoritukset.")
  @Tooltip("Päättötodistukseen liittyvät oppiaineen suoritukset.")
  @Title("Oppiaine")
  @FlattenInUI
  def koulutusmoduuli: PerusopetuksenOppiaine
  @Title("Arvosana")
  @Tooltip("Oppiaineen kokonaisarvosana")
  @FlattenInUI
  def arviointi: Option[List[PerusopetuksenOppiaineenArviointi]]
  @Tooltip("Mahdolliset muut suorituskielet.")
  def muutSuorituskielet: Option[List[Koodistokoodiviite]]
  @Tooltip("Mahdolliset todistuksella näkyvät lisätiedot.")
  def todistuksellaNäkyvätLisätiedot: Option[LocalizedString]
}

case class NuortenPerusopetuksenOppiaineenOppimääränSuoritus(
  koulutusmoduuli: NuortenPerusopetuksenOppiaine,
  toimipiste: OrganisaatioWithOid,
  arviointi: Option[List[PerusopetuksenOppiaineenArviointi]] = None,
  override val vahvistus: Option[HenkilövahvistusPaikkakunnalla] = None,
  suoritustapa: Koodistokoodiviite,
  suorituskieli: Koodistokoodiviite,
  muutSuorituskielet: Option[List[Koodistokoodiviite]] = None,
  todistuksellaNäkyvätLisätiedot: Option[LocalizedString] = None,
  @KoodistoKoodiarvo("nuortenperusopetuksenoppiaineenoppimaara")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("nuortenperusopetuksenoppiaineenoppimaara", koodistoUri = "suorituksentyyppi")
) extends PerusopetuksenPäätasonSuoritus with OppiaineenSuoritus with Todistus with SuoritustavallinenPerusopetuksenSuoritus with PerusopetuksenOppiaineenOppimääränSuoritus {
  override def osasuoritukset = None
}

trait PerusopetuksenOppiaineenSuoritus extends OppiaineenSuoritus with PakollisenTaiValinnaisenSuoritus {
  override def salliDuplikaatit = !koulutusmoduuli.pakollinen
}

@Description("Perusopetuksen toiminta-alueen suoritus osana perusopetuksen oppimäärän tai vuosiluokan suoritusta. Suoritukset voidaan kirjata oppiaineiden sijaan toiminta-alueittain, jos opiskelijalle on tehty erityisen tuen päätös")
case class PerusopetuksenToiminta_AlueenSuoritus(
  @Description("Toiminta-alueet voivat sisältää yksittäisen oppiaineen tavoitteita ja sisältöjä, jos oppilaalla on vahvuuksia jossakin yksittäisessä oppiaineessa. Opetuksen toteuttamisessa eri toiminta-alueiden sisältöjä voidaan yhdistää. Toiminta-alueesta muodostuu oppiaineen kaltaisia suorituksia")
  @Title("Toiminta-alue")
  koulutusmoduuli: PerusopetuksenToiminta_Alue,
  arviointi: Option[List[PerusopetuksenOppiaineenArviointi]] = None,
  suorituskieli: Option[Koodistokoodiviite] = None,
  @KoodistoKoodiarvo("perusopetuksentoimintaalue")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "perusopetuksentoimintaalue", koodistoUri = "suorituksentyyppi")
) extends OppiaineenTaiToiminta_AlueenSuoritus with Vahvistukseton with Toiminta_AlueenSuoritus

trait PerusopetuksenOppiaineenArviointi extends YleissivistävänKoulutuksenArviointi

@Description("Numeerinen arviointi asteikolla 4 (hylätty) - 10 (erinomainen)")
case class NumeerinenPerusopetuksenOppiaineenArviointi(
  arvosana: Koodistokoodiviite,
  @Description("Päivämäärä, jolloin arviointi on annettu. Muoto YYYY-MM-DD.")
  päivä: Option[LocalDate]
) extends PerusopetuksenOppiaineenArviointi with NumeerinenYleissivistävänKoulutuksenArviointi {
  def arviointipäivä = päivä
}

@Description("Sanallisessa arvioinnissa suorituksen hyväksymisen ilmaisuun käytetään koodiarvoja S (suoritettu), H (hylätty) ja O (osallistunut). Koodiarvon lisäksi voidaan liittää sanallinen arviointi vapaana tekstinä kuvaus-kenttään")
case class SanallinenPerusopetuksenOppiaineenArviointi(
  arvosana: Koodistokoodiviite = Koodistokoodiviite("S", "arviointiasteikkoyleissivistava"),
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  @Tooltip("Oppiaineen sanallinen arviointi.")
  kuvaus: Option[LocalizedString],
  @Description("Päivämäärä, jolloin arviointi on annettu. Muoto YYYY-MM-DD")
  päivä: Option[LocalDate] = None
) extends PerusopetuksenOppiaineenArviointi with SanallinenYleissivistävänKoulutuksenArviointi {
  def arviointipäivä = päivä
}

@Description("Käyttäytymisen arviointi. Koodiarvon lisäksi voidaan liittää sanallinen arviointi vapaana tekstinä kuvaus-kenttään")
@IgnoreInAnyOfDeserialization
case class PerusopetuksenKäyttäytymisenArviointi(
  @Tooltip("Käyttäytymisen arvosana.")
  arvosana: Koodistokoodiviite = Koodistokoodiviite("S", "arviointiasteikkoyleissivistava"),
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN))
  @Tooltip("Käyttäytymisen sanallinen arviointi.")
  kuvaus: Option[LocalizedString] = None,
  @Description("Päivämäärä, jolloin arviointi on annettu. Muoto YYYY-MM-DD.")
  @Hidden
  päivä: Option[LocalDate] = None
) extends YleissivistävänKoulutuksenArviointi with SanallinenArviointi {
  def arviointipäivä = päivä
}

object PerusopetuksenOppiaineenArviointi {
  def apply(arvosana: String, kuvaus: Option[LocalizedString] = None) = new SanallinenPerusopetuksenOppiaineenArviointi(
    arvosana = Koodistokoodiviite(koodiarvo = arvosana, koodistoUri = "arviointiasteikkoyleissivistava"),
    päivä = None,
    kuvaus = kuvaus
  )
  def apply(arvosana: Int) = new NumeerinenPerusopetuksenOppiaineenArviointi(
    arvosana = Koodistokoodiviite(koodiarvo = arvosana.toString, koodistoUri = "arviointiasteikkoyleissivistava"),
    päivä = None
  )
}

@Description("Perusopetuksen toiminta-alueen tunnistetiedot")
case class PerusopetuksenToiminta_Alue(
  @Description("Toiminta-alueen tunniste")
  @KoodistoUri("perusopetuksentoimintaalue")
  tunniste: Koodistokoodiviite
) extends KoodistostaLöytyväKoulutusmoduuli {
  def laajuus = None
}

@Description("Nuorten perusopetuksen tunnistetiedot")
case class NuortenPerusopetus(
 perusteenDiaarinumero: Option[String],
 @KoodistoKoodiarvo("201101")
 tunniste: Koodistokoodiviite = Koodistokoodiviite("201101", koodistoUri = "koulutus"),
 koulutustyyppi: Option[Koodistokoodiviite] = None
) extends Perusopetus

trait Perusopetus extends Koulutus with Laajuudeton with Tutkinto with PerusopetuksenDiaarinumerollinenKoulutus

@Title("Perusopetuksen luokka-aste")
@Description("Perusopetuksen luokka-asteen (1-9) tunnistetiedot")
case class PerusopetuksenLuokkaAste(
 @Description("Luokka-asteen tunniste (1-9)")
 @KoodistoUri("perusopetuksenluokkaaste")
 @Title("Luokka-aste")
 tunniste: Koodistokoodiviite,
 perusteenDiaarinumero: Option[String],
 koulutustyyppi: Option[Koodistokoodiviite] = None
) extends KoodistostaLöytyväKoulutusmoduuli with Laajuudeton with PerusopetuksenDiaarinumerollinenKoulutus {
  override def laajuus = None
  def luokkaAste = tunniste.koodiarvo
}

trait PerusopetuksenDiaarinumerollinenKoulutus extends DiaarinumerollinenKoulutus

object PerusopetuksenLuokkaAste {
  def apply(luokkaAste: Int, diaarinumero: String): PerusopetuksenLuokkaAste = PerusopetuksenLuokkaAste(Koodistokoodiviite(luokkaAste.toString, "perusopetuksenluokkaaste"), Some(diaarinumero))
}

@Description("Perusopetuksen oppiaineen tunnistetiedot")
trait PerusopetuksenOppiaine extends Koulutusmoduuli with Valinnaisuus with Diaarinumerollinen {
  @Description("Oppiaineen tunnistetiedot")
  @Title("Oppiaine")
  def tunniste: KoodiViite
}

@Title("Muu oppiaine")
trait MuuPerusopetuksenOppiaine extends PerusopetuksenOppiaine {
  @KoodistoKoodiarvo("HI")
  @KoodistoKoodiarvo("MU")
  @KoodistoKoodiarvo("BI")
  @KoodistoKoodiarvo("PS")
  @KoodistoKoodiarvo("KT")
  @KoodistoKoodiarvo("ET")
  @KoodistoKoodiarvo("KO")
  @KoodistoKoodiarvo("FI")
  @KoodistoKoodiarvo("KE")
  @KoodistoKoodiarvo("YH")
  @KoodistoKoodiarvo("TE")
  @KoodistoKoodiarvo("KS")
  @KoodistoKoodiarvo("FY")
  @KoodistoKoodiarvo("GE")
  @KoodistoKoodiarvo("LI")
  @KoodistoKoodiarvo("KU")
  @KoodistoKoodiarvo("MA")
  @KoodistoKoodiarvo("YL")
  @KoodistoKoodiarvo("OP")
  def tunniste: Koodistokoodiviite
}

@Title("Äidinkieli ja kirjallisuus")
@Description("Oppiaineena äidinkieli ja kirjallisuus")
trait PerusopetuksenÄidinkieliJaKirjallisuus extends PerusopetuksenOppiaine with Äidinkieli {
  @KoodistoKoodiarvo("AI")
  def tunniste: Koodistokoodiviite
  @Description("Mikä kieli on kyseessä")
  @KoodistoUri("oppiaineaidinkielijakirjallisuus")
  def kieli: Koodistokoodiviite
}

@Title("Vieras tai toinen kotimainen kieli")
@Description("Oppiaineena vieras tai toinen kotimainen kieli")
trait PerusopetuksenVierasTaiToinenKotimainenKieli extends PerusopetuksenOppiaine with Kieliaine {
  @KoodistoKoodiarvo("A1")
  @KoodistoKoodiarvo("A2")
  @KoodistoKoodiarvo("B1")
  @KoodistoKoodiarvo("B2")
  @KoodistoKoodiarvo("B3")
  def tunniste: Koodistokoodiviite
  @Description("Mikä kieli on kyseessä")
  @KoodistoUri("kielivalikoima")
  def kieli: Koodistokoodiviite
  override def description = kieliaineDescription
}

@Title("Paikallinen oppiaine")
trait PerusopetuksenPaikallinenOppiaine extends PerusopetuksenOppiaine with PaikallinenKoulutusmoduuli with StorablePreference {
  @Tooltip("Paikallisen oppiaineen vapaamuotoinen kuvaus.")
  def kuvaus: LocalizedString
}

trait NuortenPerusopetuksenOppiaine extends PerusopetuksenOppiaine {
  @Tooltip("Oppiaineen laajuus vuosiviikkotunteina.")
  def laajuus: Option[LaajuusVuosiviikkotunneissa]
}

trait NuortenPerusopetuksenKoodistostaLöytyväOppiaine extends NuortenPerusopetuksenOppiaine with YleissivistavaOppiaine {
  def kuvaus: Option[LocalizedString]
}

case class NuortenPerusopetuksenPaikallinenOppiaine(
  tunniste: PaikallinenKoodi,
  laajuus: Option[LaajuusVuosiviikkotunneissa] = None,
  kuvaus: LocalizedString,
  perusteenDiaarinumero: Option[String] = None,
  @DefaultValue(false)
  pakollinen: Boolean = false
) extends NuortenPerusopetuksenOppiaine with PerusopetuksenPaikallinenOppiaine

case class MuuNuortenPerusopetuksenOppiaine(
  tunniste: Koodistokoodiviite,
  pakollinen: Boolean = true,
  perusteenDiaarinumero: Option[String] = None,
  override val laajuus: Option[LaajuusVuosiviikkotunneissa] = None,
  kuvaus: Option[LocalizedString] = None
) extends MuuPerusopetuksenOppiaine with NuortenPerusopetuksenKoodistostaLöytyväOppiaine

case class NuortenPerusopetuksenÄidinkieliJaKirjallisuus(
  tunniste: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "AI", koodistoUri = "koskioppiaineetyleissivistava"),
  kieli: Koodistokoodiviite,
  pakollinen: Boolean = true,
  perusteenDiaarinumero: Option[String] = None,
  override val laajuus: Option[LaajuusVuosiviikkotunneissa] = None,
  kuvaus: Option[LocalizedString] = None
) extends PerusopetuksenÄidinkieliJaKirjallisuus with NuortenPerusopetuksenKoodistostaLöytyväOppiaine

case class NuortenPerusopetuksenVierasTaiToinenKotimainenKieli(
  tunniste: Koodistokoodiviite,
  kieli: Koodistokoodiviite,
  pakollinen: Boolean = true,
  perusteenDiaarinumero: Option[String] = None,
  override val laajuus: Option[LaajuusVuosiviikkotunneissa] = None,
  kuvaus: Option[LocalizedString] = None
) extends PerusopetuksenVierasTaiToinenKotimainenKieli with NuortenPerusopetuksenKoodistostaLöytyväOppiaine

@Description("Ks. tarkemmin perusopetuksen opiskeluoikeuden tilat: [confluence](https://confluence.csc.fi/display/OPHPALV/KOSKI+opiskeluoikeuden+tilojen+selitteet+koulutusmuodoittain#KOSKIopiskeluoikeudentilojenselitteetkoulutusmuodoittain-Perusopetus)")
case class NuortenPerusopetuksenOpiskeluoikeudenTila(
  @MinItems(1)
  opiskeluoikeusjaksot: List[NuortenPerusopetuksenOpiskeluoikeusjakso]
) extends OpiskeluoikeudenTila

case class NuortenPerusopetuksenOpiskeluoikeusjakso(
  alku: LocalDate,
  tila: Koodistokoodiviite
) extends KoskiOpiskeluoikeusjakso


