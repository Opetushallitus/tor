package fi.oph.koski.schema

import java.time.{LocalDate, LocalDateTime}

import fi.oph.koski.schema.annotation.{KoodistoKoodiarvo, KoodistoUri, OksaUri, SensitiveData}
import fi.oph.scalaschema.annotation._

@Description("DIA-tutkinnon opiskeluoikeus")
@Title("DIA-tutkinnon opiskeluoikeus")
case class DIAOpiskeluoikeus(
  oid: Option[String] = None,
  versionumero: Option[Int] = None,
  aikaleima: Option[LocalDateTime] = None,
  lähdejärjestelmänId: Option[LähdejärjestelmäId] = None,
  oppilaitos: Option[Oppilaitos],
  koulutustoimija: Option[Koulutustoimija] = None,
  sisältyyOpiskeluoikeuteen: Option[SisältäväOpiskeluoikeus] = None,
  arvioituPäättymispäivä: Option[LocalDate] = None,
  päättymispäivä: Option[LocalDate] = None,
  tila: LukionOpiskeluoikeudenTila,
  @MaxItems(2)
  suoritukset: List[DIAPäätasonSuoritus],
  @KoodistoKoodiarvo(OpiskeluoikeudenTyyppi.diatutkinto.koodiarvo)
  tyyppi: Koodistokoodiviite = OpiskeluoikeudenTyyppi.diatutkinto,
  override val lisätiedot: Option[DIAOpiskeluoikeudenLisätiedot] = None
) extends KoskeenTallennettavaOpiskeluoikeus {
  override def withOppilaitos(oppilaitos: Oppilaitos) = this.copy(oppilaitos = Some(oppilaitos))
  override def withKoulutustoimija(koulutustoimija: Koulutustoimija) = this.copy(koulutustoimija = Some(koulutustoimija))
}

@Description("DIA-opiskeluoikeuden lisätiedot")
case class DIAOpiskeluoikeudenLisätiedot(
  @Description("Opiskeluajan pidennetty päättymispäivä (true/false).")
  @SensitiveData
  @DefaultValue(false)
  pidennettyPäättymispäivä: Boolean = false,
  @Description("Opiskelija on ulkomainen vaihto-opiskelija Suomessa (true/false).")
  @Title("Ulkomainen vaihto-opiskelija.")
  @DefaultValue(false)
  ulkomainenVaihtoopiskelija: Boolean = false,
  @SensitiveData
  @DefaultValue(false)
  yksityisopiskelija: Boolean = false,
  erityisenKoulutustehtävänJaksot: Option[List[ErityisenKoulutustehtävänJakso]] = None,
  ulkomaanjaksot: Option[List[Ulkomaanjakso]] = None
) extends OpiskeluoikeudenLisätiedot

trait DIAPäätasonSuoritus extends KoskeenTallennettavaPäätasonSuoritus with Toimipisteellinen with Arvioinniton with Suorituskielellinen

@Title("DIA-tutkinnon suoritus")
case class DIATutkinnonSuoritus(
  @Title("Koulutus")
  koulutusmoduuli: DIATutkinto = DIATutkinto(),
  toimipiste: OrganisaatioWithOid,
  @MinValue(0)
  @MaxValue(900)
  kokonaispistemäärä: Option[Int] = None,
  vahvistus: Option[HenkilövahvistusPaikkakunnalla] = None,
  suorituskieli: Koodistokoodiviite,
  @Description("Oppiaineiden suoritukset")
  @Title("Oppiaineet")
  override val osasuoritukset: Option[List[DIAOppiaineenTutkintovaiheenSuoritus]],
  todistuksellaNäkyvätLisätiedot: Option[LocalizedString] = None,
  @KoodistoKoodiarvo("diatutkintovaihe")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("diatutkintovaihe", koodistoUri = "suorituksentyyppi")
) extends DIAPäätasonSuoritus with Todistus

@Title("Valmistavan DIA-vaiheen suoritus")
case class DIAValmistavanVaiheenSuoritus(
  @Title("Koulutus")
  koulutusmoduuli: DIAValmistavaVaihe = DIAValmistavaVaihe(),
  toimipiste: OrganisaatioWithOid,
  vahvistus: Option[HenkilövahvistusPaikkakunnalla] = None,
  suorituskieli: Koodistokoodiviite,
  @Title("Oppiaineet")
  override val osasuoritukset: Option[List[DIAOppiaineenValmistavanVaiheenSuoritus]],
  todistuksellaNäkyvätLisätiedot: Option[LocalizedString] = None,
  @KoodistoKoodiarvo("diavalmistavavaihe")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("diavalmistavavaihe", koodistoUri = "suorituksentyyppi")
) extends DIAPäätasonSuoritus

@Title("Valmistava DIA-vaihe")
@Description("Valmistavan DIA-vaiheen tunnistetiedot")
case class DIAValmistavaVaihe(
  @Description("Valmistavan DIA-vaiheen tunniste")
  @KoodistoUri("suorituksentyyppi")
  @KoodistoKoodiarvo("diavalmistavavaihe")
  tunniste: Koodistokoodiviite = Koodistokoodiviite("diavalmistavavaihe", koodistoUri = "suorituksentyyppi")
) extends KoodistostaLöytyväKoulutusmoduuli with Laajuudeton

@Title("DIA-tutkintovaihe")
@Description("DIA-tutkintovaiheen tunnistetiedot")
case class DIATutkinto(
  @KoodistoKoodiarvo("301103")
  tunniste: Koodistokoodiviite = Koodistokoodiviite("301103", koodistoUri = "koulutus"),
  koulutustyyppi: Option[Koodistokoodiviite] = None
) extends Koulutus with Laajuudeton with Tutkinto


trait DIASuoritus extends Vahvistukseton

@Title("DIA-oppiaineen valmistavan vaiheen suoritus")
case class DIAOppiaineenValmistavanVaiheenSuoritus(
  @Description("DIA-oppiaineen tunnistetiedot")
  @Title("Oppiaine")
  koulutusmoduuli: DIAOsaAlueOppiaine,
  suorituskieli: Option[Koodistokoodiviite] = None,
  @Description("Oppiaineeseen kuuluvien lukukausien suoritukset")
  @Title("Lukukaudet")
  @MaxItems(2)
  override val osasuoritukset: Option[List[DIAOppiaineenValmistavanVaiheenLukukaudenSuoritus]],
  @KoodistoKoodiarvo("diaoppiaine")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "diaoppiaine", koodistoUri = "suorituksentyyppi")
) extends DIASuoritus with Arvioinniton with MahdollisestiSuorituskielellinen

@Title("DIA-oppiaineen tutkintovaiheen suoritus")
case class DIAOppiaineenTutkintovaiheenSuoritus(
  @Description("DIA-oppiaineen tunnistetiedot")
  @Title("Oppiaine")
  koulutusmoduuli: DIAOsaAlueOppiaine,
  suorituskieli: Option[Koodistokoodiviite] = None,
  @Description("Oppiaineeseen kuuluvien lukukausien suoritukset")
  @Title("Lukukaudet")
  @MaxItems(4)
  override val osasuoritukset: Option[List[DIAOppiaineenTutkintovaiheenOsasuoritus]],
  @KoodistoKoodiarvo("diaoppiaine")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "diaoppiaine", koodistoUri = "suorituksentyyppi")
) extends DIASuoritus with Arvioinniton with MahdollisestiSuorituskielellinen

@Title("DIA-oppiaineen valmistavan vaiheen lukukauden suoritus")
case class DIAOppiaineenValmistavanVaiheenLukukaudenSuoritus(
  koulutusmoduuli: DIAOppiaineenValmistavanVaiheenLukukausi,
  arviointi: Option[List[DIAOppiaineenValmistavanVaiheenLukukaudenArviointi]] = None,
  @KoodistoKoodiarvo("diaoppiaineenvalmistavanvaiheenlukukaudensuoritus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "diaoppiaineenvalmistavanvaiheenlukukaudensuoritus", koodistoUri = "suorituksentyyppi")
) extends DIASuoritus with KurssinSuoritus

trait DIAOppiaineenTutkintovaiheenOsasuoritus extends DIASuoritus

@Title("DIA-oppiaineen tutkintovaiheen lukukauden suoritus")
case class DIAOppiaineenTutkintovaiheenLukukaudenSuoritus(
  koulutusmoduuli: DIAOppiaineenTutkintovaiheenLukukausi,
  arviointi: Option[List[DIAOppiaineenTutkintovaiheenLukukaudenArviointi]] = None,
  @KoodistoKoodiarvo("diaoppiaineentutkintovaiheenlukukaudensuoritus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "diaoppiaineentutkintovaiheenlukukaudensuoritus", koodistoUri = "suorituksentyyppi")
) extends DIAOppiaineenTutkintovaiheenOsasuoritus with KurssinSuoritus

@Title("DIA-oppiaineen päättökokeen suoritus")
case class DIAOppiaineenPäättökokeenSuoritus(
  koulutusmoduuli: DIAPäättökoe,
  arviointi: Option[List[DIAPäättökokeenArviointi]] = None,
  @KoodistoKoodiarvo("diapaattokokeensuoritus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "diapaattokokeensuoritus", koodistoUri = "suorituksentyyppi")
) extends DIAOppiaineenTutkintovaiheenOsasuoritus

@Title("DIA-tutkinnon erityisosaamisen näyttötutkinnon suoritus")
case class DIAErityisosaamisenNäyttötutkinto(
  koulutusmoduuli: DIANäyttötutkinto,
  arviointi: Option[List[DIAPäättökokeenArviointi]] = None,
  @KoodistoKoodiarvo("diapaattokokeensuoritus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "diapaattokokeensuoritus", koodistoUri = "suorituksentyyppi")
) extends DIAOppiaineenTutkintovaiheenOsasuoritus

trait DIAOppiaineenLukukausi extends KoodistostaLöytyväKoulutusmoduuli with Laajuudeton {
  @KoodistoUri("dialukukausi")
  def tunniste: Koodistokoodiviite
}

@Title("DIA-oppiaineen valmistavan vaiheen lukukausi")
@Description("DIA-oppiaineen valmistavan vaiheen lukukauden tunnistetiedot")
case class DIAOppiaineenValmistavanVaiheenLukukausi(
  @KoodistoKoodiarvo("10/I")
  @KoodistoKoodiarvo("10/II")
  tunniste: Koodistokoodiviite
) extends DIAOppiaineenLukukausi

@Title("DIA-oppiaineen tutkintovaiheen lukukausi")
@Description("DIA-oppiaineen tutkintovaiheen lukukauden tunnistetiedot")
case class DIAOppiaineenTutkintovaiheenLukukausi(
  @KoodistoKoodiarvo("11/I")
  @KoodistoKoodiarvo("11/II")
  @KoodistoKoodiarvo("12/I")
  @KoodistoKoodiarvo("12/II")
  tunniste: Koodistokoodiviite,
) extends DIAOppiaineenLukukausi

@Title("DIA-tutkinnon päättökoe")
@Description("DIA-tutkinnon päättökokeen tunnistetiedot")
case class DIAPäättökoe (
  @KoodistoUri("diapaattokoe")
  @KoodistoKoodiarvo("kirjallinenkoe")
  @KoodistoKoodiarvo("suullinenkoe")
  tunniste: Koodistokoodiviite
) extends KoodistostaLöytyväKoulutusmoduuli with Laajuudeton

@Title("DIA-tutkinnon erityisosaamisen näyttötutkinto")
@Description("DIA-tutkinnon erityisosaamisen näyttötutkinnon tunnistetiedot")
case class DIANäyttötutkinto (
  @KoodistoUri("diapaattokoe")
  @KoodistoKoodiarvo("nayttotutkinto")
  tunniste: Koodistokoodiviite
) extends KoodistostaLöytyväKoulutusmoduuli with Laajuudeton


trait DIAArviointi extends KoodistostaLöytyväArviointi {
  def arvosana: Koodistokoodiviite
  def arvioitsijat: Option[List[Arvioitsija]] = None
}

@Title("DIA-oppiaineen valmistavan vaiheen lukukauden arviointi")
case class DIAOppiaineenValmistavanVaiheenLukukaudenArviointi(
  @KoodistoUri("arviointiasteikkodiavalmistava")
  arvosana: Koodistokoodiviite,
  päivä: Option[LocalDate]
) extends DIAArviointi {
  override def arviointipäivä: Option[LocalDate] = päivä
  override def hyväksytty: Boolean = arvosana.koodiarvo match {
    case "6" => false
    case _ => true
  }
}

trait DIATutkintovaiheenArviointi extends DIAArviointi {
  @KoodistoUri("arviointiasteikkodiatutkinto")
  def arvosana: Koodistokoodiviite
  def päivä: Option[LocalDate]
  override def arviointipäivä: Option[LocalDate] = päivä
  override def hyväksytty: Boolean = arvosana.koodiarvo match {
    case "0" => false
    case _ => true
  }
}

@Title("DIA-oppiaineen tutkintovaiheen lukukauden arviointi")
case class DIAOppiaineenTutkintovaiheenLukukaudenArviointi(
  arvosana: Koodistokoodiviite,
  päivä: Option[LocalDate],
  lasketaanKokonaispistemäärään: Boolean = true
) extends DIATutkintovaiheenArviointi

@Title("DIA-oppiaineen päättökokeen arviointi")
case class DIAPäättökokeenArviointi(
  arvosana: Koodistokoodiviite,
  päivä: Option[LocalDate],
) extends DIATutkintovaiheenArviointi

@Description("DIA-oppiaineen tunnistetiedot")
trait DIAOppiaine extends KoodistostaLöytyväKoulutusmoduuli {
  @KoodistoUri("oppiaineetdia")
  @OksaUri("tmpOKSAID256", "oppiaine")
  def tunniste: Koodistokoodiviite
}

trait DIAOsaAlueOppiaine extends DIAOppiaine {
  @KoodistoUri("diaosaalue")
  def osaAlue: Koodistokoodiviite
}

@Title("Muu DIA-oppiaine")
case class DIAOppiaineMuu(
  @Description("DIA-lukion oppiaineen tunnistetiedot")
  @KoodistoKoodiarvo("A")
  @KoodistoKoodiarvo("KU")
  @KoodistoKoodiarvo("MU")
  @KoodistoKoodiarvo("MA")
  @KoodistoKoodiarvo("FY")
  @KoodistoKoodiarvo("BI")
  @KoodistoKoodiarvo("KE")
  @KoodistoKoodiarvo("TI")
  @KoodistoKoodiarvo("TK")
  @KoodistoKoodiarvo("HI")
  @KoodistoKoodiarvo("MAA")
  @KoodistoKoodiarvo("TA")
  @KoodistoKoodiarvo("US")
  @KoodistoKoodiarvo("FI")
  @KoodistoKoodiarvo("ET")
  tunniste: Koodistokoodiviite,
  laajuus: Option[LaajuusVuosiviikkotunneissa],
  @Description("Oppiaineen osa-alue (1-3)")
  osaAlue: Koodistokoodiviite,
  pakollinen: Boolean = true
) extends DIAOsaAlueOppiaine

@Title("DIA-kielioppiaine")
case class DIAOppiaineKieli(
  @Description("DIA-lukion kielioppiaineen tunnistetiedot")
  @KoodistoKoodiarvo("A")
  @KoodistoKoodiarvo("B1")
  @KoodistoKoodiarvo("B3")
  tunniste: Koodistokoodiviite,
  laajuus: Option[LaajuusVuosiviikkotunneissa],
  @KoodistoUri("kielivalikoima")
  @KoodistoKoodiarvo("FR")
  @KoodistoKoodiarvo("SV")
  @KoodistoKoodiarvo("RU")
  @Discriminator
  @Description("Mikä kieli on kyseessä")
  kieli: Koodistokoodiviite,
  @Description("Oppiaineen osa-alue (1)")
  @KoodistoKoodiarvo("1")
  @DefaultValue("1")
  osaAlue: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "1", koodistoUri = "diaosaalue"),
  pakollinen: Boolean = true
) extends DIAOsaAlueOppiaine with Kieliaine {
  override def description = kieliaineDescription
}

@Title("DIA-äidinkieli")
case class DIAOppiaineÄidinkieli(
  @KoodistoKoodiarvo("AI")
  tunniste: Koodistokoodiviite,
  laajuus: Option[LaajuusVuosiviikkotunneissa],
  @KoodistoUri("oppiainediaaidinkieli")
  @KoodistoKoodiarvo("FI")
  @KoodistoKoodiarvo("S2")
  @KoodistoKoodiarvo("DE")
  kieli: Koodistokoodiviite,
  @Description("Oppiaineen osa-alue (1)")
  @KoodistoKoodiarvo("1")
  @DefaultValue("1")
  osaAlue: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "1", koodistoUri = "diaosaalue"),
) extends DIAOsaAlueOppiaine with Äidinkieli {
  override def description = kieliaineDescription
}
