package fi.oph.koski.eperusteet

import fi.oph.koski.json.{JsonFiles, JsonResources, JsonSerializer}
import fi.oph.koski.tutkinto.Koulutustyyppi.Koulutustyyppi

object MockEPerusteetRepository extends EPerusteetRepository {
  lazy val rakenteet: List[EPerusteRakenne] = List(
    "rakenne-autoalan-perustutkinto",
    "rakenne-autoalan-perustutkinto2017",
    "rakenne-luonto-ja-ymparistoala",
    "rakenne-autoalan-tyonjohto",
    "rakenne-perusopetus",
    "rakenne-aikuisten-perusopetus2017",
    "rakenne-lukio",
    "rakenne-lukio-2019",
    "rakenne-aikuisten-lukio-2019",
    "rakenne-hiusalan-perustutkinto",
    "rakenne-puutarhatalouden-perustutkinto",
    "rakenne-automekaanikon-erikoisammattitutkinto",
    "rakenne-liiketalouden-perustutkinto",
    "rakenne-puuteollisuuden-perustutkinto",
    "rakenne-virheellinen-puuteollisuuden-perustutkinto",
    "rakenne-valma",
    "rakenne-sosiaali-ja-terveysala",
    "rakenne-vst-oppivelvollisille-suunnattu",
    "rakenne-vst-maahanmuuttajien-kotoutumiskoulutus",
    "rakenne-vst-lukutaitokoulutus"
  ).map { id =>
    JsonSerializer.extract[EPerusteRakenne](JsonResources.readResourceIfExists("/mockdata/eperusteet/" + id + ".json").get, ignoreExtras = true)
  }

  def findPerusteet(query: String): List[EPeruste] = {
    // Hakee aina samoilla kriteereillä "auto"
    JsonSerializer.extract[EPerusteet](JsonFiles.readFile("src/main/resources/mockdata/eperusteet/hakutulokset-auto.json"), ignoreExtras = true).data.filter(_.nimi("fi").toLowerCase.contains(query.toLowerCase))
  }

  def findPerusteetByDiaarinumero(diaarinumero: String): List[EPeruste] = {
    rakenteet.filter(_.diaarinumero == diaarinumero).map(_.toEPeruste)
  }

  def findPerusteetByKoulutustyyppi(koulutustyypit: Set[Koulutustyyppi]): List[EPeruste] = {
    rakenteet.filter(r => koulutustyypit.map(k => s"${k.koodistoUri}_${k.koodiarvo}").contains(r.koulutustyyppi)).map(_.toEPeruste)
  }

  def findRakenne(diaariNumero: String): Option[EPerusteRakenne] = {
    if (diaariNumero == "mock-empty-koulutukset") {
      rakenteet.find(_.diaarinumero == "39/011/2014").map(_.copy(koulutukset = Nil))
    } else {
      rakenteet.find(_.diaarinumero == diaariNumero)
    }
  }

  def findPerusteenYksilöintitiedot(diaariNumero: String): Option[EPerusteTunniste] = {
    rakenteet.find(_.diaarinumero == diaariNumero).map(r => EPerusteTunniste(r.id, r.nimi))
  }

  override protected def webBaseUrl = "https://eperusteet.opintopolku.fi"
}
