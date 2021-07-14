package fi.oph.koski.schema

import fi.oph.scalaschema.annotation.{Description, Title}

import fi.oph.koski.schema.annotation.{KoodistoKoodiarvo, KoodistoUri}

case class VapaanSivistystyönLukutaitokoulutuksenSuoritus(
  toimipiste: OrganisaatioWithOid,
  @KoodistoKoodiarvo("vstlukutaitokoulutus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "vstlukutaitokoulutus", koodistoUri = "suorituksentyyppi"),
  koulutusmoduuli: VapaanSivistystyönLukutaitokoulutus,
  vahvistus: Option[HenkilövahvistusValinnaisellaPaikkakunnalla],
  @Description("Koulutuksen opetuskieli")
  @Title("Opetuskieli")
  suorituskieli: Koodistokoodiviite,
  @Title("Suorituskokonaisuudet")
  override val osasuoritukset: Option[List[VapaanSivistystyönLukutaitokoulutuksenKokonaisuudenSuoritus]],
  @Description("Todistuksella näytettävä lisätieto, vapaamuotoinen tekstikenttä")
  todistuksellaNäkyvätLisätiedot: Option[LocalizedString] = None
) extends VapaanSivistystyönPäätasonSuoritus with SuoritusVaatiiMahdollisestiMaksuttomuusTiedonOpiskeluoikeudelta

@Description("Vapaan sivistystyön lukutaitokoulutuksen tunnistetiedot")
case class VapaanSivistystyönLukutaitokoulutus(
 @KoodistoKoodiarvo("999911")
 tunniste: Koodistokoodiviite = Koodistokoodiviite("999911", koodistoUri = "koulutus"),
 perusteenDiaarinumero: Option[String] = None,
 koulutustyyppi: Option[Koodistokoodiviite] = None,
 laajuus: Option[LaajuusOpintopisteissä] = None
) extends DiaarinumerollinenKoulutus with Tutkinto

@Title("Lukutaitokoulutuksen kokonaisuuden suoritus")
case class VapaanSivistystyönLukutaitokoulutuksenKokonaisuudenSuoritus(
  @Title("Lukutaitokoulutuksen kokonaisuus")
  koulutusmoduuli: VapaanSivistystyönLukutaidonKokonaisuus,
  @KoodistoKoodiarvo("vstlukutaitokoulutuksenkokonaisuudensuoritus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "vstlukutaitokoulutuksenkokonaisuudensuoritus", koodistoUri = "suorituksentyyppi"),
  override val arviointi: Option[List[OppivelvollisilleSuunnatunVapaanSivistystyönOpintokokonaisuudenArviointi]] = None
) extends Suoritus with Vahvistukseton

trait VapaanSivistystyönLukutaitokoulutuksenOsasuoritustenKoulutusmoduuli extends KoulutusmoduuliValinnainenLaajuus with KoodistostaLöytyväKoulutusmoduuli {
  def laajuus: Option[LaajuusOpintopisteissä]
}

@Title("Lukutaitokoulutuksen kokonaisuus")
case class VapaanSivistystyönLukutaidonKokonaisuus(
  @KoodistoUri(koodistoUri = "vstlukutaitokoulutuksenkokonaisuus")
  tunniste: Koodistokoodiviite,
  laajuus: Option[LaajuusOpintopisteissä] = None
) extends VapaanSivistystyönLukutaitokoulutuksenOsasuoritustenKoulutusmoduuli with KoodistostaLöytyväKoulutusmoduuli with LaajuuttaEiValidoida
