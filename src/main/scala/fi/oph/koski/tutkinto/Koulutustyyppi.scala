package fi.oph.koski.tutkinto

import fi.oph.koski.koodisto.MockKoodistoViitePalvelu
import fi.oph.koski.schema.SuorituksenTyyppi.SuorituksenTyyppi
import fi.oph.koski.schema.{Koodistokoodiviite, SuorituksenTyyppi}

object Koulutustyyppi {
  type Koulutustyyppi = Koodistokoodiviite

  // https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutustyyppi/2
  val ammatillinenPerustutkinto = apply(1)
  val lukiokoulutus = apply(2)
  val korkeakoulutus = apply(3)
  val ammatillinenPerustutkintoErityisopetuksena = apply(4)
  val telma = apply(5)
  val perusopetuksenLisäopetus = apply(6)
  val vieraskielistenLuva = apply(9)
  val ammattitutkinto = apply(11)
  val erikoisammattitutkinto = apply(12)
  val ammatillinenPerustutkintoNäyttötutkintona = apply(13)
  val aikuistenLukiokoulutus = apply(14)
  val esiopetus = apply(15)
  val perusopetus = apply(16)
  val aikuistenPerusopetus = apply(17)
  val valma = apply(18)
  val valmaErityisopetuksena = apply(19)
  val perusopetukseenValmistava = apply(22)
  val luva = apply(23)
  val vapaanSivistystyönKoulutus = apply(10)
  val vapaanSivistystyönMaahanmuuttajienKotoutumisKoulutus = apply(30)
  val vstlukutaitokoulutus = apply(35)

  def apply(numero: Int) = MockKoodistoViitePalvelu.validateRequired(Koodistokoodiviite(numero.toString, "koulutustyyppi"))
  def describe(koulutustyyppi: Koulutustyyppi) = koulutustyyppi.koodiarvo + koulutustyyppi.nimi.map(nimi => s"(${nimi.get("fi")})").getOrElse("")

  val ammatillisetKoulutustyypit = List(ammatillinenPerustutkinto, ammatillinenPerustutkintoErityisopetuksena, ammatillinenPerustutkintoNäyttötutkintona, ammattitutkinto, erikoisammattitutkinto)
  val ammatillisenPerustutkinnonTyypit = List(ammatillinenPerustutkinto, ammatillinenPerustutkintoErityisopetuksena, ammatillinenPerustutkintoNäyttötutkintona)
  val perusopetuksenKoulutustyypit = List(perusopetus, aikuistenPerusopetus)
  val lukionKoulutustyypit = List(lukiokoulutus, aikuistenLukiokoulutus)
  val luvaKoulutustyypit = List(vieraskielistenLuva, luva)
  val valmaKoulutustyypit = List(valma, valmaErityisopetuksena)

  def fromSuorituksenTyyppi(suorituksentyyppi: SuorituksenTyyppi) : Set[Koulutustyyppi] = {
    suorituksentyyppi match{
      case SuorituksenTyyppi.perusopetuksenoppimaara | SuorituksenTyyppi.perusopetuksenvuosiluokka | SuorituksenTyyppi.nuortenperusopetuksenoppiaineenoppimaara =>
        Set(perusopetus)
      case SuorituksenTyyppi.aikuistenperusopetuksenoppimaara | SuorituksenTyyppi.perusopetuksenoppiaineenoppimaara | SuorituksenTyyppi.aikuistenperusopetuksenoppimaaranalkuvaihe =>
        Set(aikuistenPerusopetus)
      case SuorituksenTyyppi.perusopetuksenlisaopetus =>
        Set(perusopetuksenLisäopetus)
      case SuorituksenTyyppi.perusopetukseenvalmistavaopetus =>
        Set(perusopetukseenValmistava)
      case SuorituksenTyyppi.esiopetuksensuoritus =>
        Set(esiopetus)
      case SuorituksenTyyppi.valma =>
        Set(valma)
      case SuorituksenTyyppi.telma =>
        Set(telma)
      case SuorituksenTyyppi.lukionoppimaara | SuorituksenTyyppi.lukionoppiaineenoppimaara | SuorituksenTyyppi.lukionaineopinnot =>
        lukionKoulutustyypit.toSet
      case SuorituksenTyyppi.vstoppivelvollisillesuunnattukoulutus =>
        Set(vapaanSivistystyönKoulutus)
      case SuorituksenTyyppi.vstmaahanmuuttajienkotoutumiskoulutus =>
        Set(vapaanSivistystyönMaahanmuuttajienKotoutumisKoulutus)
      case SuorituksenTyyppi.vstlukutaitokoulutus =>
        Set(vstlukutaitokoulutus)
      case _ => Set.empty[Koulutustyyppi]
    }
  }
}
