package fi.oph.koski.raportit

import java.sql.Date
import java.time.{LocalDate, LocalDateTime}
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.raportointikanta._
import fi.oph.koski.schema.Organisaatio.Oid
import fi.oph.koski.schema.{Koodistokoodiviite, _}
import org.json4s.JValue
import fi.oph.koski.log.Logging

object PerusopetuksenVuosiluokkaRaportti extends VuosiluokkaRaporttiPaivalta with Logging {

  def buildRaportti(repository: PerusopetuksenRaportitRepository, oppilaitosOids: Seq[Oid], paiva: LocalDate, vuosiluokka: String): Seq[PerusopetusRow] = {
    val rows = if (vuosiluokka == "9") {
      repository.peruskoulunPaattavatJaLuokalleJääneet(oppilaitosOids, paiva, vuosiluokka)
    } else {
      repository.perusopetuksenvuosiluokka(oppilaitosOids, paiva, vuosiluokka)
    }
    rows.map(buildRow(_, paiva))
  }

  private def buildRow(row: PerusopetuksenRaporttiRows, hakupaiva: LocalDate) = {
    val opiskeluoikeudenLisätiedot = JsonSerializer.extract[Option[PerusopetuksenOpiskeluoikeudenLisätiedot]](row.opiskeluoikeus.data \ "lisätiedot")
    val lähdejärjestelmänId = JsonSerializer.extract[Option[LähdejärjestelmäId]](row.opiskeluoikeus.data \ "lähdejärjestelmänId")
    val (toimintaalueOsasuoritukset, muutOsasuoritukset) = row.osasuoritukset.partition(_.suorituksenTyyppi == "perusopetuksentoimintaalue")
    val (valtakunnalliset, paikalliset) = muutOsasuoritukset.partition(isValtakunnallinenOppiaine)
    val (pakollisetValtakunnalliset, valinnaisetValtakunnalliset) = valtakunnalliset.partition(isPakollinen)
    val (pakollisetPaikalliset, valinnaisetPaikalliset) = paikalliset.partition(isPakollinen)
    val kaikkiValinnaiset = valinnaisetPaikalliset.union(valinnaisetValtakunnalliset)
    val voimassaOlevatErityisenTuenPäätökset = opiskeluoikeudenLisätiedot.map(lt => combineErityisenTuenPäätökset(lt.erityisenTuenPäätös, lt.erityisenTuenPäätökset).filter(erityisentuenPäätösvoimassaPaivalla(_, hakupaiva))).getOrElse(List.empty)
    val päätasonVahvistusPäivä = row.päätasonSuoritus.vahvistusPäivä

    PerusopetusRow(
      opiskeluoikeusOid = row.opiskeluoikeus.opiskeluoikeusOid,
      oppilaitoksenNimi = row.opiskeluoikeus.oppilaitosNimi,
      oppilaitosRaportointipäivänä = oppilaitosRaportointipäivänä(row),
      lähdejärjestelmä = lähdejärjestelmänId.map(_.lähdejärjestelmä.koodiarvo),
      lähdejärjestelmänId = lähdejärjestelmänId.flatMap(_.id),
      yksiloity = row.henkilo.yksiloity,
      oppijaOid = row.opiskeluoikeus.oppijaOid,
      hetu = row.henkilo.hetu,
      sukunimi = row.henkilo.sukunimi,
      etunimet = row.henkilo.etunimet,
      sukupuoli = row.henkilo.sukupuoli,
      kotikunta = row.henkilo.kotikuntaNimiFi,
      opiskeluoikeudenAlkamispäivä = row.opiskeluoikeus.alkamispäivä.map(_.toLocalDate),
      viimeisinTila = row.opiskeluoikeus.viimeisinTila.getOrElse(""),
      tilaHakupaivalla = row.aikajaksot.last.tila,
      suorituksenTila = if (row.päätasonSuoritus.vahvistusPäivä.isDefined) "valmis" else "kesken",
      suorituksenAlkamispaiva = JsonSerializer.extract[Option[LocalDate]](row.päätasonSuoritus.data \ "alkamispäivä").getOrElse("").toString,
      suorituksenVahvistuspaiva = row.päätasonSuoritus.vahvistusPäivä.getOrElse("").toString,
      jaaLuokalle = JsonSerializer.extract[Option[Boolean]](row.päätasonSuoritus.data \ "jääLuokalle").getOrElse(false),
      luokka = row.luokka,
      voimassaolevatVuosiluokat = row.voimassaolevatVuosiluokat.mkString(","),
      aidinkieli = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "AI")(pakollisetValtakunnalliset),
      pakollisenAidinkielenOppimaara = getOppiaineenOppimäärä("AI")(pakollisetValtakunnalliset),
      kieliA1 = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "A1")(pakollisetValtakunnalliset),
      kieliA1Oppimaara = getOppiaineenOppimäärä("A1")(pakollisetValtakunnalliset),
      kieliA2 = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "A2")(pakollisetValtakunnalliset),
      kieliA2Oppimaara = getOppiaineenOppimäärä("A2")(pakollisetValtakunnalliset),
      kieliB = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "B1")(pakollisetValtakunnalliset),
      kieliBOppimaara = getOppiaineenOppimäärä("B1")(pakollisetValtakunnalliset),
      uskonto = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "KT", "ET")(pakollisetValtakunnalliset),
      uskonnonOppimaara = uskonnonOppimääräIfNotElämänkatsomustieto(pakollisetValtakunnalliset),
      historia = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "HI")(pakollisetValtakunnalliset),
      yhteiskuntaoppi = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "YH")(pakollisetValtakunnalliset),
      matematiikka = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "MA")(pakollisetValtakunnalliset),
      kemia = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "KE")(pakollisetValtakunnalliset),
      fysiikka = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "FY")(pakollisetValtakunnalliset),
      biologia = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "BI")(pakollisetValtakunnalliset),
      maantieto = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "GE")(pakollisetValtakunnalliset),
      musiikki = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "MU")(pakollisetValtakunnalliset),
      kuvataide = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "KU")(pakollisetValtakunnalliset),
      kotitalous = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "KO")(pakollisetValtakunnalliset),
      terveystieto = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "TE")(pakollisetValtakunnalliset),
      kasityo = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "KS")(pakollisetValtakunnalliset),
      liikunta = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "LI")(pakollisetValtakunnalliset),
      ymparistooppi = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "YL")(pakollisetValtakunnalliset),
      opintoohjaus = oppiaineenArvosanaTiedot(päätasonVahvistusPäivä, "OP")(pakollisetValtakunnalliset),
      kayttaymisenArvio = JsonSerializer.extract[Option[PerusopetuksenKäyttäytymisenArviointi]](row.päätasonSuoritus.data \ "käyttäytymisenArvio").map(_.arvosana.koodiarvo).getOrElse(""),
      paikallistenOppiaineidenKoodit = paikalliset.map(_.koulutusmoduuliKoodiarvo).mkString(","),
      pakollisetPaikalliset = pakollisetPaikalliset.map(nimiJaKoodi).mkString(","),
      valinnaisetPaikalliset = valinnaisetPaikalliset.map(nimiJaKoodiJaArvosana).mkString(","),
      valinnaisetValtakunnalliset = valinnaisetValtakunnalliset.map(nimiJaKoodiJaArvosana).mkString(","),
      valinnaisetLaajuus_SuurempiKuin_2Vuosiviikkotuntia = kaikkiValinnaiset.filter(vuosiviikkotunteja(_, _ >= _, 2)).map(nimiJaKoodiJaLaajuusJaArvosana).mkString(","),
      valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia = kaikkiValinnaiset.filter(vuosiviikkotunteja(_, _ < _, 2)).map(nimiJaKoodiJaLaajuusJaArvosana).mkString(","),
      numeroarviolliset_valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia = kaikkiValinnaiset.filter(os => vuosiviikkotunteja(os, _ < _, 2) && isNumeroarviollinen(os)).map(nimiJaKoodiJaLaajuusJaArvosana).mkString(","),
      valinnaisetEiLaajuutta = kaikkiValinnaiset.filter(_.koulutusmoduuliLaajuusArvo.isEmpty).map(nimiJaKoodi).mkString(","),
      vahvistetutToimintaAlueidenSuoritukset = toimintaalueOsasuoritukset.filter(_.arviointiHyväksytty.getOrElse(false)).sortBy(_.koulutusmoduuliKoodiarvo).map(nimiJaKoodi).mkString(","),
      majoitusetu = opiskeluoikeudenLisätiedot.exists(_.majoitusetu.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva))),
      kuljetusetu = opiskeluoikeudenLisätiedot.exists(_.kuljetusetu.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva))),
      kotiopetus = opiskeluoikeudenLisätiedot.exists(oo => oneOfAikajaksoistaVoimassaHakuPaivalla(oo.kotiopetus, oo.kotiopetusjaksot, hakupaiva)),
      ulkomailla = opiskeluoikeudenLisätiedot.exists(oo => oneOfAikajaksoistaVoimassaHakuPaivalla(oo.ulkomailla, oo.ulkomaanjaksot, hakupaiva)),
      perusopetuksenAloittamistaLykatty = opiskeluoikeudenLisätiedot.exists(_.perusopetuksenAloittamistaLykätty),
      aloittanutEnnenOppivelvollisuutta = opiskeluoikeudenLisätiedot.exists(_.aloittanutEnnenOppivelvollisuutta),
      pidennettyOppivelvollisuus = opiskeluoikeudenLisätiedot.exists(_.pidennettyOppivelvollisuus.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva))),
      tehostetunTuenPaatos = opiskeluoikeudenLisätiedot.exists(oo => oneOfAikajaksoistaVoimassaHakuPaivalla(oo.tehostetunTuenPäätös, oo.tehostetunTuenPäätökset, hakupaiva)),
      joustavaPerusopetus = opiskeluoikeudenLisätiedot.exists(_.joustavaPerusopetus.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva))),
      vuosiluokkiinSitoutumatonOpetus = opiskeluoikeudenLisätiedot.exists(_.vuosiluokkiinSitoutumatonOpetus),
      vammainen = opiskeluoikeudenLisätiedot.exists(_.vammainen.exists(_.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva)))),
      vaikeastiVammainen = opiskeluoikeudenLisätiedot.exists(_.vaikeastiVammainen.exists(_.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva)))),
      oikeusMaksuttomaanAsuntolapaikkaan = opiskeluoikeudenLisätiedot.flatMap(_.oikeusMaksuttomaanAsuntolapaikkaan.map(aikajaksoVoimassaHakuPaivalla(_, hakupaiva))).getOrElse(false),
      sisäoppilaitosmainenMajoitus = opiskeluoikeudenLisätiedot.exists(_.sisäoppilaitosmainenMajoitus.exists(_.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva)))),
      koulukoti = opiskeluoikeudenLisätiedot.exists(_.koulukoti.exists(_.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva)))),
      erityisenTuenPaatosVoimassa = voimassaOlevatErityisenTuenPäätökset.size > 0,
      erityisenTuenPaatosToimialueittain = voimassaOlevatErityisenTuenPäätökset.exists(_.opiskeleeToimintaAlueittain),
      erityisenTuenPaatosToteutuspaikat = voimassaOlevatErityisenTuenPäätökset.flatMap(_.toteutuspaikka.map(_.koodiarvo)).sorted.map(eritysopetuksentoteutuspaikkaKoodisto.getOrElse(_, "")).mkString(","),
      tukimuodot = tukimuodot(opiskeluoikeudenLisätiedot)
    )
  }

  private def oppilaitosRaportointipäivänä(data: PerusopetuksenRaporttiRows): Option[String] = {
    val organisaatiohistoriaRaportointipäivältä = data.organisaatiohistoriaResult.head
    val oppilaitosOidNyt = data.opiskeluoikeus.oppilaitosOid
    if (organisaatiohistoriaRaportointipäivältä.oppilaitosOid == oppilaitosOidNyt) {
      None
    } else {
      Some(organisaatiohistoriaRaportointipäivältä.toString)
    }
  }

  private val yleissivistäväkoodisto = Seq(
    "A1", "A2", "AI", "B1", "B2", "B3", "BI", "ET", "FI", "FY", "GE", "HI", "KE", "KO", "KS", "KT", "KU", "LI", "MA", "MU", "OP", "OPA", "PS", "TE", "YH", "YL"
  )

  private def isValtakunnallinenOppiaine(osasuoritus: ROsasuoritusRow) = {
    yleissivistäväkoodisto.contains(osasuoritus.koulutusmoduuliKoodiarvo) &&
      osasuoritus.koulutusmoduuliKoodisto.contains("koskioppiaineetyleissivistava")
  }

  private def isPakollinen(osasuoritus: ROsasuoritusRow) = {
    JsonSerializer.extract[Option[Boolean]](osasuoritus.data \ "koulutusmoduuli" \ "pakollinen").getOrElse(false)
  }

  private def oppiaineenArvosanaTiedot(päätasonVahvistusPäivä: Option[Date], koodistoKoodit: String*)(oppiaineidenSuoritukset: Seq[ROsasuoritusRow]) = {
    oppiaineidenSuoritukset.filter(s => koodistoKoodit.contains(s.koulutusmoduuliKoodiarvo)) match {
      case Nil => "Oppiaine puuttuu"
      case suoritukset@_ => suoritukset.map(oppiaineenArvosanaJaYksilöllistettyTieto(_, päätasonVahvistusPäivä)).mkString(",")
    }
  }

  private def oppiaineenArvosanaJaYksilöllistettyTieto(osasuoritus: ROsasuoritusRow, päätasonVahvistusPäivä: Option[Date]) = {
    val arvosana = osasuoritus.arviointiArvosanaKoodiarvo.getOrElse("Arvosana puuttuu")
    val viimeinenPäiväIlmanLaajuuksia = Date.valueOf(LocalDate.of(2020,7,31))
    if (päätasonVahvistusPäivä.exists(_.after(viimeinenPäiväIlmanLaajuuksia)) && osasuoritus.koulutusmoduuliPakollinen.getOrElse(false)) {
      val laajuus = osasuoritus.koulutusmoduuliLaajuusArvo.getOrElse("Ei laajuutta")
      s"$arvosana${täppäIfYksilöllistetty(osasuoritus)} laajuus: $laajuus"
    } else {
      s"$arvosana${täppäIfYksilöllistetty(osasuoritus)}"
    }
  }

  private def täppäIfYksilöllistetty(osasuoritus: ROsasuoritusRow) = {
    val isYksilöllistetty = JsonSerializer.extract[Option[Boolean]](osasuoritus.data \ "yksilöllistettyOppimäärä").getOrElse(false)
    if (isYksilöllistetty) "*" else ""
  }

  private def uskonnonOppimääräIfNotElämänkatsomustieto(osasuoritukset: Seq[ROsasuoritusRow]) = {
    val hasElämänkatsomustieto = osasuoritukset.exists(_.koulutusmoduuliKoodiarvo == "ET")
    if (hasElämänkatsomustieto) "" else uskonnonOppimäärä(osasuoritukset)
  }

  private def uskonnonOppimäärä(osasuoritukset: Seq[ROsasuoritusRow]) = {
    osasuoritukset
      .find(_.koulutusmoduuliKoodiarvo == "KT")
      .map { uskonto =>
        JsonSerializer.extract[Option[Koodistokoodiviite]](uskonto.data \ "koulutusmoduuli" \ "uskonnonOppimäärä")
          .flatMap(_.nimi.map(_.get("fi")))
          .getOrElse("Oppimäärä puuttuu")
      }
      .getOrElse("Oppiaine puuttuu")
  }

  private def getOppiaineenOppimäärä(koodistoKoodi: String)(osasuoritukset: Seq[ROsasuoritusRow]) = {
    osasuoritukset.filter(_.koulutusmoduuliKoodiarvo == koodistoKoodi) match {
      case Nil => "Oppiaine puuttuu"
      case found@_ => found.map(getOppiaineenNimi).mkString(",")
    }
  }

  private def getOppiaineenNimi(osasuoritus: ROsasuoritusRow) = {
    val muuAine = getFinnishNimi(osasuoritus.data \ "koulutusmoduuli" \ "tunniste" \ "nimi")
    val kieliAine = getFinnishNimi(osasuoritus.data \ "koulutusmoduuli" \ "kieli" \ "nimi")
    val result = (kieliAine, muuAine) match {
      case (Some(_), _)  => kieliAine
      case (_, Some(_)) => muuAine
      case _ => None
    }
    result.getOrElse("Oppiaine puuttuu")
  }

  private def nimiJaKoodiJaLaajuusJaArvosana(osasuoritus: ROsasuoritusRow) = {
    val arvosana = osasuoritus.arviointiArvosanaKoodiarvo match {
      case Some(a) => s"Arvosana $a"
      case _ => "Ei arvosanaa"
    }

    nimiJaKoodi(osasuoritus) + " " +
      osasuoritus.koulutusmoduuliLaajuusArvo.getOrElse("Ei laajuutta") + " " +
      osasuoritus.arviointiArvosanaKoodiarvo.getOrElse("Ei arvosanaa")
  }

  private def nimiJaKoodi(osasuoritus: ROsasuoritusRow) = {
    s"${getOppiaineenNimi(osasuoritus)} (${osasuoritus.koulutusmoduuliKoodiarvo})"
  }

  private def nimiJaKoodiJaArvosana(osasuoritus: ROsasuoritusRow) = {
    s"${getOppiaineenNimi(osasuoritus)} (${osasuoritus.koulutusmoduuliKoodiarvo}) ${osasuoritus.arviointiArvosanaKoodiarvo.getOrElse("Ei arvosanaa")}"
  }

  private def getFinnishNimi(j: JValue) = {
    JsonSerializer.extract[Option[LocalizedString]](j).map(_.get("fi"))
  }

  private val vuosiviikkotunnitKoodistoarvo = "3"

  private def vuosiviikkotunteja(osasuoritus: ROsasuoritusRow, op: (Double, Double) => Boolean, threshold: Double) = {
    osasuoritus.koulutusmoduuliLaajuusYksikkö.contains(vuosiviikkotunnitKoodistoarvo) &&
      op(osasuoritus.koulutusmoduuliLaajuusArvo.getOrElse(-1d), threshold)
  }

  private def isNumeroarviollinen(osasuoritus: ROsasuoritusRow) = {
    osasuoritus.arviointiArvosanaKoodiarvo.exists(_.matches("\\d+"))
  }

  private def oneOfAikajaksoistaVoimassaHakuPaivalla(aikajakso: Option[Jakso], aikajaksot: Option[List[Jakso]], hakupaiva: LocalDate) = {
   aikajakso.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva)) ||
     aikajaksot.exists(_.exists(aikajaksoVoimassaHakuPaivalla(_, hakupaiva)))
  }

  private def aikajaksoVoimassaHakuPaivalla(aikajakso: Jakso, paiva: LocalDate) = {
    (aikajakso.alku, aikajakso.loppu) match {
      case (alku, Some(loppu)) => !alku.isAfter(paiva) && !loppu.isBefore(paiva)
      case (alku, _) => !alku.isAfter(paiva)
    }
  }

  val perusopetuksenToiminaAlueKoodisto = Map(
    "1" -> "motoriset taidot",
    "2" -> "kieli ja kommunikaatio",
    "3"	-> "sosiaaliset taidot",
    "4"	-> "päivittäisten toimintojen taidot",
    "5"	->"kognitiiviset taidot"
  )

  val eritysopetuksentoteutuspaikkaKoodisto = Map(
    "1" -> "Opetus on kokonaan erityisryhmissä tai -luokassa",
    "2" -> "Opetuksesta 1-19 % on yleisopetuksen ryhmissä",
    "3"	-> "Opetuksesta 20-49 % on yleisopetuksen ryhmissä",
    "4"	-> "Opetuksesta 50-79 % on yleisopetuksen ryhmissä",
    "5"	-> "Opetuksesta 80-100 % on yleisopetuksen ryhmissä"
  )

 private def combineErityisenTuenPäätökset(erityisenTuenPäätös: Option[ErityisenTuenPäätös], erityisenTuenPäätökset: Option[List[ErityisenTuenPäätös]]) = {
   erityisenTuenPäätös.toList ++ erityisenTuenPäätökset.toList.flatten
 }

  private def erityisentuenPäätösvoimassaPaivalla(päätös: ErityisenTuenPäätös, paiva: LocalDate) = {
    (päätös.alku, päätös.loppu) match {
      case (Some(alku), Some(loppu)) => !alku.isAfter(paiva) && !loppu.isBefore(paiva)
      case (Some(alku), _) => !alku.isAfter(paiva)
      case _ => false
    }
  }

  private def tukimuodot(lisätiedot: Option[PerusopetuksenOpiskeluoikeudenLisätiedot]) = {
    lisätiedot
      .flatMap(_.tukimuodot)
      .map(_.flatMap(_.nimi.map(_.get("fi"))))
      .map(_.mkString(","))
      .getOrElse("")
  }

  def title(oppilaitosOid: String, paiva: LocalDate, vuosiluokka: String): String = "TITLE TODO"

  def documentation(oppilaitosOid: String, paiva: LocalDate, vuosiluokka: String, loadStarted: LocalDateTime): String =
    s"""
      |Tarkempi kuvaus joistakin sarakkeista:
      |
      |- Sukupuoli: 1 = mies, 2 = nainen
      |- Viimeisin opiskeluoikeuden tila: Se opiskeluoikeuden tila, joka opiskeluoikeudella on nyt.
      |- Opiskeluoikeuden tila tulostuspäivänä: Opiskeluoikeuden tila, joka opiskeluoikeudella oli sinä päivämääränä, joka on syötetty tulostusparametreissa ”Päivä”-kenttään.
      |- Suorituksen vahvistuspäivä: Sen päätason suorituksen (vuosiluokka tai perusopetuksen oppimäärä), jolta raportti on tulostettu, vahvistuspäivä.
      |- Vuosiluokkien suoritukset, joilta puuttuu vahvistus: Lista niistä vuosiluokista, joilta puuttuu vahvistus. Jos tässä sarakkeessa on useampia vuosiluokkia, se on osoitus siitä, että tiedoissa on virheitä.
      |- Pakollisten oppiaineiden arvosana- ja oppimääräsarakkeet (sarakkeet S-AQ): Valtakunnalliset oppiainesuoritukset (https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/koskioppiaineetyleissivistava/latest), jotka siirretty pakollisena.
      |- Paikallisten oppiaineiden koodit: Vuosiluokkasuorituksella olevien paikallisten oppiaineiden koodit. Jos tästä löytyy jokin valtakunnallinen oppiaine (https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/koskioppiaineetyleissivistava/latest), tiedonsiirroissa on siirretty virheellisesti valtakunnallinen oppiaine paikallisena.
      |- Pakolliset paikalliset oppiaineet: Pakollisissa oppiaineissa olevat paikalliset oppiaineet. Pääsääntöisesti, jos tässä sarakkeessa on mitään arvoja, oppiaineiden siirrossa on tapahtunut virhe (eli joko pakollinen valtakunnallinen oppiaine on siirretty pakollisena paikallisena oppiaineena tai valinnainen paikallinen oppiaine on siirretty pakollisena paikallisena oppiaineena). Vain tietyillä erityiskouluilla (esim. steinerkoulut) on pakollisia paikallisia oppiaineita.
      |- Valinnaiset oppiaineet joilla on numeroarviointi ja joiden laajuus on pienempi kuin 2 vuosiviikkotuntia: Jos tässä sarakkeessa on muita kuin tyhjiä kenttiä, kyseisten oppiaineiden siirrossa on jokin virhe (eli joko oppiaineen laajuus on oikeasti 2 vuosiviikkotuntia tai enemmän tai sitten alle 2 vuosiviikkotunnin laajuiselle valinnaiselle oppiaineelle on virheellisesti siirretty numeroarvosana). Alle 2 vuosiviikkotunnin laajuisella oppiainesuorituksella ei pitäisi olla numeroarvosanaa.
      |- Vahvistetut toiminta-alueiden suoritukset: Sarake listaa S-merkinnällä vahvistetut toiminta-alueen suoritukset niiltä oppilailta, jotka opiskelevat toiminta-alueittain. Jos sarakkeesta löytyy kenttiä, joissa on vähemmän kuin viisi toiminta-alueittain opiskeltavan perusopetuksen toiminta-aluetta (https://virkailija.opintopolku.fi/koski/dokumentaatio/koodisto/perusopetuksentoimintaalue/latest), suoritettujen toiminta-alueiden siirroissa on todennäköisesti virhe.
      |- Opiskeluoikeuden lisätiedoissa ilmoitettavat etu- ja tukimuodot (sarakkeet BB-BR: Sarakkeessa oleva arvo kertoo, onko siirretyn KOSKI-datan mukaan kyseinen etu- tai tukimuoto ollut voimassa raportin tulostusparametrien ”Päivä”-kenttään syötettynä päivämääränä. Esimerkki: Jos raportti on tulostettu päivälle 1.6.2019 ja oppilaalla on opiskeluoikeuden lisätiedoissa majoitusjakso välillä 1.1.-31.12.2019, ”Majoitusetu”-sarakkeeseen tulostuu oppilaan rivillä ”Kyllä”.
    """.stripMargin

  def filename(oppilaitosOid: String, paiva: LocalDate, vuosiluokka: String): String = {
    s"Perusopetuksen_vuosiluokka_${oppilaitosOid}_${vuosiluokka}_${paiva}.xlsx"
  }

  private def compactLisätiedotColumn(title: String) = CompactColumn(title, comment = Some("Sarakkeessa oleva arvo kertoo, onko siirretyn KOSKI-datan mukaan kyseinen etu- tai tukimuoto ollut voimassa raportin tulostusparametrien ”Päivä”-kenttään syötettynä päivämääränä."))

  val columnSettings: Seq[(String, Column)] = Seq(
    "opiskeluoikeusOid" -> Column("Opiskeluoikeuden oid"),
    "oppilaitoksenNimi" -> Column("Oppilaitoksen nimi", comment = Some("Oppilaitos, joka opiskeluoikeudella on nyt. Mahdollisesti eri kuin todellinen oppilaitos raportin tulostusparametrin \"Päivä\"- kenttään syötettynä päivämääränä.")),
    "oppilaitosRaportointipäivänä" -> Column("Oppilaitos raportointipäivänä (jos eri)", comment = Some("Oppilaitos ja koulutustoimija, joihin opiskeluoikeus kuului raportin tulostusparametrin \"Päivä\"- kenttään syötettynä päivämääränä, jos eri kuin viereisen sarakkeen oppilaitos.")),
    "lähdejärjestelmä" -> Column("Lähdejärjestelmä"),
    "lähdejärjestelmänId" -> CompactColumn("Opiskeluoikeuden tunniste lähdejärjestelmässä"),
    "yksiloity" -> Column("Yksilöity", comment = Some("Jos tässä on arvo 'ei', tulee oppija yksilöidä oppijanumerorekisterissä")),
    "oppijaOid" -> Column("Oppijan oid"),
    "hetu" -> Column("hetu"),
    "sukunimi" -> Column("Sukunimi"),
    "etunimet" -> Column("Etunimet"),
    "sukupuoli" -> Column("Sukupuoli", comment = Some("1 = mies, 2 = nainen")),
    "kotikunta" -> Column("Kotikunta", comment = Some("Tieto 'kotikunta' haetaan opintopolun oppijanumerorekisteristä, koulutuksenjärjestäjä ei tallenna kotikunta-tietoa KOSKI-palveluun. Oppijanumerorekisterin kotikunta-tieto ei sisällä takautuvia tietoja oppijan kotikuntahistoriasta, joten raportilla näkyvä kotikunta on raportin tulostamispäivän mukainen kotikunta, eli ei välttämättä sama, kuin mikä väestötietojärjestelmässä oppijan tosiasiallinen kotikunta on ollut raportille valittuna päivänä.")),
    "opiskeluoikeudenAlkamispäivä" -> Column("Opiskeluoikeuden alkamispäivä"),
    "viimeisinTila" -> CompactColumn("Viimeisin opiskeluoikeuden tila", comment = Some("Se opiskeluoikeuden tila, joka opiskeluoikeudella on nyt.")),
    "tilaHakupaivalla" -> CompactColumn("Opiskeluoikeuden tila tulostuspäivänä", comment = Some("Se opiskeluoikeuden tila, joka opiskeluoikeudella oli raportin tulostusparametrin \"Päivä\"- kenttään syötettynä päivämääränä")),
    "suorituksenTila" -> CompactColumn("Suorituksen tila"),
    "suorituksenAlkamispaiva" -> CompactColumn("Suoritukselle merkattu alkamispäivä"),
    "suorituksenVahvistuspaiva" -> CompactColumn("Suorituksen vahvistuspäivä"),
    "jaaLuokalle" -> CompactColumn("Jää luokalle"),
    "luokka" -> CompactColumn("Luokan tunniste"),
    "voimassaolevatVuosiluokat" -> CompactColumn("Vuosiluokkien suoritukset joilta puuttuu vahvistus", comment = Some("Jos tässä sarakkeessa on useampia vuosiluokkia, se on osoitus siitä, että tiedoissa on virheitä.")),
    "aidinkieli" -> CompactColumn("Äidinkieli"),
    "pakollisenAidinkielenOppimaara" -> CompactColumn("Äidinkielen oppimäärä"),
    "kieliA1" -> CompactColumn("A1-kieli"),
    "kieliA1Oppimaara" -> CompactColumn("A1-kielen oppimäärä"),
    "kieliA2" -> CompactColumn("A2-kieli"),
    "kieliA2Oppimaara" -> CompactColumn("A2-kielen oppimäärä"),
    "kieliB" -> CompactColumn("B-kieli"),
    "kieliBOppimaara" -> CompactColumn("B-kielen oppimäärä"),
    "uskonto" -> CompactColumn("Uskonto/Elämänkatsomustieto"),
    "uskonnonOppimaara" -> CompactColumn("Uskonnon oppimäärä"),
    "historia" -> CompactColumn("Historia"),
    "yhteiskuntaoppi" -> CompactColumn("Yhteiskuntaoppi"),
    "matematiikka" -> CompactColumn("Matematiikka"),
    "kemia" -> CompactColumn("Kemia"),
    "fysiikka" -> CompactColumn("Fysiikka"),
    "biologia" -> CompactColumn("Biologia"),
    "maantieto" -> CompactColumn("Maantieto"),
    "musiikki" -> CompactColumn("Musiikki"),
    "kuvataide" -> CompactColumn("Kuvataide"),
    "kotitalous" -> CompactColumn("Kotitalous"),
    "terveystieto" -> CompactColumn("Terveystieto"),
    "kasityo" -> CompactColumn("Käsityö"),
    "liikunta" -> CompactColumn("Liikunta"),
    "ymparistooppi" -> CompactColumn("Ympäristöoppi"),
    "opintoohjaus" -> CompactColumn("Opinto-ohjaus"),
    "kayttaymisenArvio" -> CompactColumn("Käyttäytymisen arviointi"),
    "paikallistenOppiaineidenKoodit" -> CompactColumn("Paikallisten oppiaineiden koodit"),
    "pakollisetPaikalliset" -> CompactColumn("Pakolliset paikalliset oppiaineet", comment = Some("Pääsääntöisesti, jos tässä sarakkeessa on mitään arvoja, oppiaineiden siirrossa on tapahtunut virhe (eli joko pakollinen valtakunnallinen oppiaine on siirretty pakollisena paikallisena oppiaineena tai valinnainen paikallinen oppiaine on siirretty pakollisena paikallisena oppiaineena). Vain tietyillä erityiskouluilla (esim. steinerkoulut) on pakollisia paikallisia oppiaineita.")),
    "valinnaisetPaikalliset" -> CompactColumn("Valinnaiset paikalliset oppiaineet"),
    "valinnaisetValtakunnalliset" -> CompactColumn("Valinnaiset valtakunnalliset oppiaineet"),
    "valinnaisetLaajuus_SuurempiKuin_2Vuosiviikkotuntia" -> CompactColumn("Valinnaiset oppiaineet, joiden laajuus on suurempi tai yhtäsuuri kuin 2 vuosiviikkotuntia"),
    "valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia" -> CompactColumn("Valinnaiset oppiaineet, joiden laajuus on pienempi kuin 2 vuosiviikko tuntia"),
    "numeroarviolliset_valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia" -> CompactColumn("Valinnaiset oppiaineet, joilla on numeroarviointi ja joiden laajuus on pienempi kuin 2 vuosiviikkotuntia", comment = Some("Jos tässä sarakkeessa on muita kuin tyhjiä kenttiä, kyseisten oppiaineiden siirrossa on jokin virhe (eli joko oppiaineen laajuus on oikeasti 2 vuosiviikkotuntia tai enemmän tai sitten alle 2 vuosiviikkotunnin laajuiselle valinnaiselle oppiaineelle on virheellisesti siirretty numeroarvosana). Alle 2 vuosiviikkotunnin laajuisella oppiainesuorituksella ei pitäisi olla numeroarvosanaa.")),
    "valinnaisetEiLaajuutta" -> CompactColumn("Valinnaiset oppiaineet, joilla ei ole laajuutta"),
    "vahvistetutToimintaAlueidenSuoritukset" -> CompactColumn("Vahvistetut toiminta-alueiden suoritukset", comment = Some("Sarake listaa S-merkinnällä vahvistetut toiminta-alueen suoritukset niiltä oppilailta, jotka opiskelevat toiminta-alueittain. Jos sarakkeesta löytyy kenttiä, joissa on vähemmän kuin viisi toiminta-alueittain opiskeltavan perusopetuksen toiminta-aluetta (https://virkailija.opintopolku.fi/koski/dokumentaatio/koodisto/perusopetuksentoimintaalue/latest), suoritettujen toiminta-alueiden siirroissa on todennäköisesti virhe.")),
    "majoitusetu" -> compactLisätiedotColumn("Majoitusetu"),
    "kuljetusetu" -> compactLisätiedotColumn("Kuljetusetu"),
    "kotiopetus" -> compactLisätiedotColumn("Kotiopetus"),
    "ulkomailla" -> compactLisätiedotColumn("Ulkomailla"),
    "perusopetuksenAloittamistaLykatty" -> compactLisätiedotColumn("Perusopetuksen aloittamista lykätty"),
    "aloittanutEnnenOppivelvollisuutta" -> compactLisätiedotColumn("Aloittanut ennen oppivelvollisuutta"),
    "pidennettyOppivelvollisuus" -> compactLisätiedotColumn("Pidennetty oppivelvollisuus"),
    "tehostetunTuenPaatos" -> CompactColumn("Tehostetun tuen jakso voimassa", comment = Some("Sarakkeessa arvo \"kyllä\" jos opiskeluoikeudella on voimassa oleva tehostetun tuen jakso raportin tulostukseen valittuna päivämääränä.")),
    "joustavaPerusopetus" -> compactLisätiedotColumn("Joustava perusopetus"),
    "vuosiluokkiinSitoutumatonOpetus" -> compactLisätiedotColumn("Vuosiluokkiin sitomaton opetus"),
    "vammainen" -> compactLisätiedotColumn("Vammainen"),
    "vaikeastiVammainen" -> compactLisätiedotColumn("Vaikeimmin kehitysvammainen"),
    "oikeusMaksuttomaanAsuntolapaikkaan" -> compactLisätiedotColumn("Oikeus maksuttomaan asuntolapaikkaan"),
    "sisäoppilaitosmainenMajoitus" -> compactLisätiedotColumn("Sisäoppilaitosmainen majoitus"),
    "koulukoti" -> compactLisätiedotColumn("Koulukoti"),
    "erityisenTuenPaatosVoimassa" -> CompactColumn("Erityisen tuen jakso voimassa", comment = Some("Sarakkeessa arvo \"kyllä\" jos opiskeluoikeudella on voimassa oleva erityisen tuen jakso raportin tulostukseen valittuna päivämääränä.")),
    "erityisenTuenPaatosToimialueittain" -> compactLisätiedotColumn("Opiskelee toimialueittain"),
    "erityisenTuenPaatosToteutuspaikat" -> compactLisätiedotColumn("Erityisen tuen päätöksen toteutuspaikka"),
    "tukimuodot" -> compactLisätiedotColumn("Tukimuodot")
  )
}

private[raportit] case class PerusopetusRow(
  opiskeluoikeusOid: String,
  oppilaitoksenNimi: String,
  oppilaitosRaportointipäivänä: Option[String],
  lähdejärjestelmä: Option[String],
  lähdejärjestelmänId: Option[String],
  yksiloity: Boolean,
  oppijaOid: String,
  hetu: Option[String],
  sukunimi: String,
  etunimet: String,
  sukupuoli: Option[String],
  kotikunta: Option[String],
  opiskeluoikeudenAlkamispäivä: Option[LocalDate],
  viimeisinTila: String,
  tilaHakupaivalla: String,
  suorituksenTila: String,
  suorituksenAlkamispaiva: String,
  suorituksenVahvistuspaiva: String,
  jaaLuokalle: Boolean,
  luokka: Option[String],
  voimassaolevatVuosiluokat: String,
  aidinkieli: String,
  pakollisenAidinkielenOppimaara: String,
  kieliA1: String,
  kieliA1Oppimaara: String,
  kieliA2: String,
  kieliA2Oppimaara: String,
  kieliB: String,
  kieliBOppimaara: String,
  uskonto: String,
  uskonnonOppimaara: String,
  historia: String,
  yhteiskuntaoppi: String,
  matematiikka: String,
  kemia: String,
  fysiikka: String,
  biologia: String,
  maantieto: String,
  musiikki: String,
  kuvataide: String,
  kotitalous: String,
  terveystieto: String,
  kasityo: String,
  liikunta: String,
  ymparistooppi: String,
  opintoohjaus: String,
  kayttaymisenArvio: String,
  paikallistenOppiaineidenKoodit: String,
  pakollisetPaikalliset: String,
  valinnaisetPaikalliset: String,
  valinnaisetValtakunnalliset: String,
  valinnaisetLaajuus_SuurempiKuin_2Vuosiviikkotuntia: String,
  valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia: String,
  numeroarviolliset_valinnaisetLaajuus_PienempiKuin_2Vuosiviikkotuntia: String,
  valinnaisetEiLaajuutta: String,
  vahvistetutToimintaAlueidenSuoritukset: String,
  majoitusetu: Boolean,
  kuljetusetu: Boolean,
  kotiopetus: Boolean,
  ulkomailla: Boolean,
  perusopetuksenAloittamistaLykatty: Boolean,
  aloittanutEnnenOppivelvollisuutta: Boolean,
  pidennettyOppivelvollisuus: Boolean,
  tehostetunTuenPaatos: Boolean,
  joustavaPerusopetus: Boolean,
  vuosiluokkiinSitoutumatonOpetus: Boolean,
  vammainen: Boolean,
  vaikeastiVammainen: Boolean,
  oikeusMaksuttomaanAsuntolapaikkaan: Boolean,
  sisäoppilaitosmainenMajoitus: Boolean,
  koulukoti: Boolean,
  erityisenTuenPaatosVoimassa: Boolean,
  erityisenTuenPaatosToimialueittain: Boolean,
  erityisenTuenPaatosToteutuspaikat: String,
  tukimuodot: String
)
