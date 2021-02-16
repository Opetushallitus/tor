package fi.oph.koski.koskiuser

import fi.oph.koski.koskiuser.Rooli._
import fi.oph.koski.schema.{Koulutustoimija, OpiskeluoikeudenTyyppi, OrganisaatioWithOid}

object Rooli {
  type Role = String
  val READ = "READ"
  val READ_UPDATE = "READ_UPDATE"
  val READ_UPDATE_ESIOPETUS = "READ_UPDATE_ESIOPETUS"
  val TIEDONSIIRRON_MITATOINTI = "TIEDONSIIRRON_MITATOINTI"
  val OPHKATSELIJA = "OPHKATSELIJA"
  val OPHPAAKAYTTAJA = "OPHPAAKAYTTAJA"
  val YLLAPITAJA = "YLLAPITAJA"
  val TIEDONSIIRTO = "TIEDONSIIRTO"
  val LUOTTAMUKSELLINEN_KAIKKI_TIEDOT = "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"
  val LUOTTAMUKSELLINEN_KELA_SUPPEA = "LUOTTAMUKSELLINEN_KELA_SUPPEA"
  val LUOTTAMUKSELLINEN_KELA_LAAJA = "LUOTTAMUKSELLINEN_KELA_LAAJA"
  val LUKU_ESIOPETUS = "LUKU_ESIOPETUS"
  val GLOBAALI_LUKU_PERUSOPETUS = "GLOBAALI_LUKU_PERUSOPETUS"
  val GLOBAALI_LUKU_TOINEN_ASTE = "GLOBAALI_LUKU_TOINEN_ASTE"
  val GLOBAALI_LUKU_KORKEAKOULU = "GLOBAALI_LUKU_KORKEAKOULU"
  val TIEDONSIIRTO_LUOVUTUSPALVELU = "TIEDONSIIRTO_LUOVUTUSPALVELU"
  val TILASTOKESKUS = "TILASTOKESKUS"
  val VALVIRA = "VALVIRA"
  val MITATOIDYT_OPISKELUOIKEUDET = "MITATOIDYT_OPISKELUOIKEUDET"

  val KAIKKI_OPISKELUOIKEUS_TYYPIT = "KAIKKI_OPISKELUOIKEUS_TYYPIT"
  val AIKUISTENPERUSOPETUS = "AIKUISTENPERUSOPETUS"
  val AMMATILLINENKOULUTUS = "AMMATILLINENKOULUTUS"
  val DIATUTKINTO = "DIATUTKINTO"
  val ESIOPETUS = "ESIOPETUS"
  val IBTUTKINTO = "IBTUTKINTO"
  val INTERNATIONALSCHOOL = "INTERNATIONALSCHOOL"
  val KORKEAKOULUTUS = "KORKEAKOULUTUS"
  val LUKIOKOULUTUS = "LUKIOKOULUTUS"
  val LUVA = "LUVA"
  val PERUSOPETUKSEENVALMISTAVAOPETUS = "PERUSOPETUKSEENVALMISTAVAOPETUS"
  val PERUSOPETUKSENLISAOPETUS = "PERUSOPETUKSENLISAOPETUS"
  val PERUSOPETUS = "PERUSOPETUS"
  val YLIOPPILASTUTKINTO = "YLIOPPILASTUTKINTO"
  val VAPAANSIVISTYSTYONKOULUTUS = "VAPAANSIVISTYSTYONKOULUTUS"

  def globaalitKoulutusmuotoRoolit = List(GLOBAALI_LUKU_PERUSOPETUS, GLOBAALI_LUKU_TOINEN_ASTE, GLOBAALI_LUKU_KORKEAKOULU)
}

object Käyttöoikeus {
  def parseAllowedOpiskeluoikeudenTyypit(roolit: List[Palvelurooli], accessTypes: List[AccessType.Value]): Set[String] = {
    val kaikkiOpiskeluoikeusTyypit = OpiskeluoikeudenTyyppi.kaikkiTyypit.map(_.koodiarvo)
    val kayttajanRoolit = unifyRoolit(roolit).filter(_.palveluName == "KOSKI").map(_.rooli.toLowerCase).toSet

    if (!accessTypes.contains(AccessType.read)) {
      Set.empty
    } else if (kayttajanRoolit.contains(KAIKKI_OPISKELUOIKEUS_TYYPIT.toLowerCase)) {
      kaikkiOpiskeluoikeusTyypit
    } else {
      kayttajanRoolit.intersect(kaikkiOpiskeluoikeusTyypit)
    }
  }

  private def unifyRoolit(roolit: List[Palvelurooli]) = roolit flatMap {
    case Palvelurooli("KOSKI", GLOBAALI_LUKU_PERUSOPETUS) => List(
      ESIOPETUS,
      PERUSOPETUS,
      AIKUISTENPERUSOPETUS,
      PERUSOPETUKSENLISAOPETUS,
      PERUSOPETUKSEENVALMISTAVAOPETUS,
      INTERNATIONALSCHOOL
    ).map(Palvelurooli(_))
    case Palvelurooli("KOSKI", GLOBAALI_LUKU_TOINEN_ASTE) => List(
      AMMATILLINENKOULUTUS,
      IBTUTKINTO,
      DIATUTKINTO,
      LUKIOKOULUTUS,
      LUVA,
      YLIOPPILASTUTKINTO,
      INTERNATIONALSCHOOL,
      VAPAANSIVISTYSTYONKOULUTUS
    ).map(Palvelurooli(_))
    case Palvelurooli("KOSKI", READ_UPDATE_ESIOPETUS) => List(Palvelurooli(ESIOPETUS))
    case Palvelurooli("KOSKI", LUKU_ESIOPETUS) => List(Palvelurooli(ESIOPETUS))
    case Palvelurooli("KOSKI", GLOBAALI_LUKU_KORKEAKOULU) => List(Palvelurooli(KORKEAKOULUTUS))
    case rooli => List(rooli)
  }
}

// this trait is intentionally left mostly blank to make it harder to accidentally mix global and organization-specific rights
trait Käyttöoikeus {
  def allowedOpiskeluoikeusTyypit: Set[String] = Set.empty
}

case class KäyttöoikeusGlobal(globalPalveluroolit: List[Palvelurooli]) extends Käyttöoikeus {
  def globalAccessType: List[AccessType.Value] = globalPalveluroolit flatMap {
    case Palvelurooli("KOSKI", "OPHKATSELIJA") => List(AccessType.read)
    case Palvelurooli("KOSKI", "OPHPAAKAYTTAJA") => List(AccessType.read, AccessType.write, AccessType.tiedonsiirronMitätöinti)
    case _ => Nil
  }

  override lazy val allowedOpiskeluoikeusTyypit: Set[String] = Käyttöoikeus.parseAllowedOpiskeluoikeudenTyypit(globalPalveluroolit, globalAccessType)
}

trait OrgKäyttöoikeus extends Käyttöoikeus {
  def organisaatiokohtaisetPalveluroolit: List[Palvelurooli]
  def organisaatioAccessType: List[AccessType.Value] = organisaatiokohtaisetPalveluroolit flatMap {
    case Palvelurooli("KOSKI", "READ") => List(AccessType.read)
    case Palvelurooli("KOSKI", "READ_UPDATE") => List(AccessType.read, AccessType.write)
    case Palvelurooli("KOSKI", "TIEDONSIIRRON_MITATOINTI") => List(AccessType.tiedonsiirronMitätöinti)
    case Palvelurooli("KOSKI", "READ_UPDATE_ESIOPETUS") => List(AccessType.read, AccessType.write)
    case Palvelurooli("KOSKI", "LUKU_ESIOPETUS") => List(AccessType.read)
    case _ => Nil
  }

  override lazy val allowedOpiskeluoikeusTyypit: Set[String] = Käyttöoikeus.parseAllowedOpiskeluoikeudenTyypit(organisaatiokohtaisetPalveluroolit, organisaatioAccessType)

  def globalAccessType: List[AccessType.Value] = Nil
  def globalPalveluroolit: List[Palvelurooli] = Nil
}

case class KäyttöoikeusVarhaiskasvatusToimipiste(koulutustoimija: Koulutustoimija, ulkopuolinenOrganisaatio: OrganisaatioWithOid, organisaatiokohtaisetPalveluroolit: List[Palvelurooli]) extends OrgKäyttöoikeus

case class KäyttöoikeusOrg(organisaatio: OrganisaatioWithOid, organisaatiokohtaisetPalveluroolit: List[Palvelurooli], juuri: Boolean, oppilaitostyyppi: Option[String]) extends OrgKäyttöoikeus

case class KäyttöoikeusViranomainen(globalPalveluroolit: List[Palvelurooli]) extends Käyttöoikeus {
  def globalAccessType: List[AccessType.Value] = if (globalPalveluroolit.exists(r => r.palveluName == "KOSKI" && Rooli.globaalitKoulutusmuotoRoolit.contains(r.rooli))) {
    List(AccessType.read)
  } else {
    Nil
  }

  override lazy val allowedOpiskeluoikeusTyypit: Set[String] = Käyttöoikeus.parseAllowedOpiskeluoikeudenTyypit(globalPalveluroolit, globalAccessType)

  def isLuovutusPalveluAllowed: Boolean = globalPalveluroolit.contains(Palvelurooli("KOSKI", TIEDONSIIRTO_LUOVUTUSPALVELU))
}
