package fi.oph.koski.schema

import java.time.LocalDate
import fi.oph.koski.koodisto.{KoodistoViite, MockKoodistoViitePalvelu}
import fi.oph.koski.schema.LocalizedString.unlocalized
import fi.oph.koski.schema.annotation._
import fi.oph.scalaschema.annotation._
import mojave.{Traversal, traversal}

import java.sql.Date

trait Suoritus {
  @Description("Suorituksen tyyppi, jolla erotellaan eri koulutusmuotoihin (perusopetus, lukio, ammatillinen...) ja eri tasoihin (tutkinto, tutkinnon osa, kurssi, oppiaine...) liittyvät suoritukset")
  @KoodistoUri("suorituksentyyppi")
  @Hidden
  @Discriminator
  def tyyppi: Koodistokoodiviite
  @Representative
  def koulutusmoduuli: Koulutusmoduuli
  @Description("Suorituksen alkamispäivä. Muoto YYYY-MM-DD")
  @Tooltip("Suorituksen alkamispäivä.")
  def alkamispäivä: Option[LocalDate] = None
  @Description("Suorituksen tila (KESKEN, VALMIS, KESKEYTYNYT)")
  @KoodistoUri("suorituksentila")
  @SyntheticProperty
  @ReadOnly("Suorituksen tila päätellään automaattisesti. Päättelylogiikka kuvattu alla. Koski ei enää käsittele tila-kentän arvoa. Kenttä poistetaan tulevaisuudessa tarpeettomana.\nPäättelylogiikka: Suoritus on valmis, kun sillä on vahvistus. Siihen asti suoritus on kesken. Suoritus on keskeytynyt, jos vahvistusta ei ole ja opiskeluoikeuden tila on ERONNUT tai KATSOTAAN ERONNEEKSI.")
  @Hidden
  def tila: Option[Koodistokoodiviite] = None
  @Description("Arviointi. Jos listalla useampi arviointi, tulkitaan myöhemmät arvioinnit arvosanan korotuksiksi edellisiin samalla listalla oleviin arviointeihin. Jos aiempaa, esimerkiksi väärin kirjattua, arviota korjataan, ei listalle tule uutta arviota")
  def arviointi: Option[List[Arviointi]]
  @Description("Suorituksen virallinen vahvistus (päivämäärä, henkilöt).")
  def vahvistus: Option[Vahvistus]
  def osasuoritukset: Option[List[Suoritus]] = None

  def osasuoritusLista: List[Suoritus] = osasuoritukset.toList.flatten
  def rekursiivisetOsasuoritukset: List[Suoritus] = {
    osasuoritusLista ++ osasuoritusLista.flatMap(_.rekursiivisetOsasuoritukset)
  }
  def viimeisinArviointi: Option[Arviointi] = arviointi.toList.flatten.lastOption
  def parasArviointi: Option[Arviointi] = arviointi.toList.flatten.reduceOption((a, b) => {
    Arviointi.korkeampiArviointi(a, b)
  })
  def parasArviointiPäivä: Option[LocalDate] = parasArviointi.flatMap(_.arviointipäivä)
  def viimeisinArvosana: Option[String] = viimeisinArviointi.map(_.arvosana.koodiarvo)
  def ensimmäinenArviointi: Option[Arviointi] = {
    arviointi.toList.flatten.reduceOption((a, b) => {
      (a.arviointipäivä, b.arviointipäivä) match {
        case (Some(aPäivä), Some(bPäivä)) => {
          if (aPäivä.isBefore(bPäivä)) {
            a
          } else {
            b
          }
        }
        case _ => a
      }
    })
  }
  def ensimmäinenArviointiPäivä: Option[LocalDate] = ensimmäinenArviointi match {
    case Some(arviointi) => arviointi.arviointipäivä
    case _ => None
  }
  def arvosanaKirjaimin: LocalizedString = viimeisinArviointi.map(_.arvosanaKirjaimin).getOrElse(unlocalized(""))
  def arvosanaNumeroin: Option[LocalizedString] = viimeisinArviointi.flatMap(_.arvosanaNumeroin)
  def sanallinenArviointi: Option[LocalizedString] = viimeisinArviointi.flatMap {
    case a: SanallinenArviointi => a.kuvaus
    case _ => None
  }
  def tarvitseeVahvistuksen: Boolean = false
  /** Onko suoritus valmis tai merkitty valmistuvaksi tulevaisuuden päivämäärällä. Valmius määritellään päätason suorituksissa vahvistuksen olemassaololla ja muissa suorituksissa arvioinnin olemassaololla. */
  def valmis = !vahvistusPuuttuu && !arviointiPuuttuu
  def arvioitu: Boolean = arviointi.exists(_.nonEmpty)
  def arviointiPuuttuu: Boolean = !arvioitu
  def vahvistusPuuttuu = tarvitseeVahvistuksen && !vahvistus.isDefined
  def vahvistettu = vahvistus.isDefined
  def kesken = !valmis
  def ryhmittelytekijä: Option[String] = None
  def salliDuplikaatit = false

  final def withKoulutusmoduuli(km: Koulutusmoduuli): Suoritus = {
    import mojave._
    shapeless.lens[Suoritus].field[Koulutusmoduuli]("koulutusmoduuli").set(this)(km)
  }
}

object Suoritus {
  def toimipisteetTraversal: Traversal[Suoritus, OrganisaatioWithOid] = new Traversal[Suoritus, OrganisaatioWithOid] {
    def modify(suoritus: Suoritus)(f: OrganisaatioWithOid => OrganisaatioWithOid) = {
      val toimipisteTraversal = traversal[Suoritus].ifInstanceOf[Toimipisteellinen].field[OrganisaatioWithOid]("toimipiste")++
        traversal[Suoritus].ifInstanceOf[MahdollisestiToimipisteellinen].filter(!_.isInstanceOf[Toimipisteetön]).field[Option[OrganisaatioWithOid]]("toimipiste").items
      val withModifiedToimipiste = toimipisteTraversal.modify(suoritus)(f)
      if (suoritus.osasuoritusLista.nonEmpty) {
        val osasuorituksetTraversal = traversal[Suoritus].field[Option[List[Suoritus]]]("osasuoritukset").items.items
        toimipisteetTraversal.compose(osasuorituksetTraversal).modify(withModifiedToimipiste)(f)
      } else {
        withModifiedToimipiste
      }
    }
  }
}

object SuorituksenTyyppi {
  type SuorituksenTyyppi = Koodistokoodiviite

  val lukionaineopinnot = apply("lukionaineopinnot")
  val perusopetuksenoppimaara = apply("perusopetuksenoppimaara")
  val perusopetuksenvuosiluokka = apply("perusopetuksenvuosiluokka")
  val nuortenperusopetuksenoppiaineenoppimaara = apply("nuortenperusopetuksenoppiaineenoppimaara")
  val aikuistenperusopetuksenoppimaara = apply("aikuistenperusopetuksenoppimaara")
  val perusopetuksenoppiaineenoppimaara = apply("perusopetuksenoppiaineenoppimaara")
  val aikuistenperusopetuksenoppimaaranalkuvaihe = apply("aikuistenperusopetuksenoppimaaranalkuvaihe")
  val perusopetuksenlisaopetus = apply("perusopetuksenlisaopetus")
  val perusopetukseenvalmistavaopetus = apply("perusopetukseenvalmistavaopetus")
  val esiopetuksensuoritus = apply("esiopetuksensuoritus")
  val valma = apply("valma")
  val telma = apply("telma")
  val lukionoppimaara = apply("lukionoppimaara")
  val lukionoppiaineenoppimaara = apply("lukionoppiaineenoppimaara")
  val vstoppivelvollisillesuunnattukoulutus = apply("vstoppivelvollisillesuunnattukoulutus")
  val vstmaahanmuuttajienkotoutumiskoulutus = apply("vstmaahanmuuttajienkotoutumiskoulutus")
  val vstlukutaitokoulutus = apply("vstlukutaitokoulutus")

  private def apply(koodiarvo: String): SuorituksenTyyppi =
    kaikkiTyypit.find(_.koodiarvo == koodiarvo)
      .getOrElse(throw new IllegalArgumentException("Väärä suorituksen tyyppi " + koodiarvo))

  lazy val kaikkiTyypit: List[SuorituksenTyyppi] = {
    val suorituksenTyyppiKoodisto: KoodistoViite = MockKoodistoViitePalvelu.getLatestVersionRequired("suorituksentyyppi")
    MockKoodistoViitePalvelu.getKoodistoKoodiViitteet(suorituksenTyyppiKoodisto)
  }
}

trait MahdollisestiToimipisteellinen extends Suoritus {
  @Description("Oppilaitoksen toimipiste, jossa opinnot on suoritettu. Jos oppilaitoksella ei ole toimipisteitä, syötetään tähän oppilaitoksen tiedot")
  @Tooltip("Oppilaitoksen toimipiste, jossa opinnot on suoritettu. Jos oppilaitoksella ei ole toimipisteitä, syötetään tähän oppilaitoksen tiedot")
  @OksaUri("tmpOKSAID148", "koulutusorganisaation toimipiste")
  @Title("Oppilaitos / toimipiste")
  def toimipiste: Option[OrganisaatioWithOid]
}

trait Toimipisteetön extends MahdollisestiToimipisteellinen {
  override def toimipiste: Option[OrganisaatioWithOid] = None
}

trait Suorituskielellinen {
  @Description("Opintojen suorituskieli")
  @Tooltip("Opintojen suorituskieli")
  @KoodistoUri("kieli")
  @OksaUri("tmpOKSAID309", "opintosuorituksen kieli")
  def suorituskieli: Koodistokoodiviite
}

trait MahdollisestiSuorituskielellinen {
  @Description("Opintojen suorituskieli")
  @Tooltip("Opintojen suorituskieli")
  @KoodistoUri("kieli")
  @OksaUri("tmpOKSAID309", "opintosuorituksen kieli")
  def suorituskieli: Option[Koodistokoodiviite]
}

trait Oppimäärällinen extends Suoritus {
  def oppimäärä: Koodistokoodiviite
}

trait Arvioinniton extends Suoritus {
  def arviointi = None
  override def arviointiPuuttuu = false
}

trait Välisuoritus extends Arvioinniton

trait MahdollisestiArvioinniton extends Suoritus {
  override def arviointiPuuttuu = false
}

trait DuplikaatitSallittu extends Suoritus {
  override def salliDuplikaatit = true
}

trait Toimipisteellinen extends Suoritus with OrganisaatioonLiittyvä {
  @Description("Oppilaitoksen toimipiste, jossa opinnot on suoritettu. Jos oppilaitoksella ei ole toimipisteitä, syötetään tähän oppilaitoksen tiedot")
  @Tooltip("Oppilaitoksen toimipiste, jossa opinnot on suoritettu. Jos oppilaitoksella ei ole toimipisteitä, syötetään tähän oppilaitoksen tiedot")
  @OksaUri("tmpOKSAID148", "koulutusorganisaation toimipiste")
  @Title("Oppilaitos / toimipiste")
  def toimipiste: OrganisaatioWithOid
  def omistajaOrganisaatio = Some(toimipiste)
}

trait Ryhmällinen {
  @Description("Ryhmän tunniste")
  def ryhmä: Option[String]
}

trait PäätasonSuoritus extends Suoritus {
  override def tarvitseeVahvistuksen = true
  def mutuallyExclusivePäätasoVahvistukseton = {}

  final def withOsasuoritukset(oss: Option[List[Suoritus]]): PäätasonSuoritus = {
    import mojave._
    shapeless.lens[PäätasonSuoritus].field[Option[List[Suoritus]]]("osasuoritukset").set(this)(oss)
  }
}

trait KoskeenTallennettavaPäätasonSuoritus extends PäätasonSuoritus with Toimipisteellinen

trait OpintopistelaajuuksienYhteislaskennallinenPäätasonSuoritus extends PäätasonSuoritus {
  override def osasuoritukset: Option[List[OpintopistelaajuuksienYhteislaskennallinenSuoritus]] = None
}

trait OpintopistelaajuuksienYhteislaskennallinenSuoritus extends Suoritus {
  def koulutusmoduuli: OpintopistelaajuuksienYhteenlaskennallinenKoulutusmoduuli
}

trait Todistus extends PäätasonSuoritus with Suorituskielellinen {
  @MultiLineString(3)
  def todistuksellaNäkyvätLisätiedot: Option[LocalizedString]
}

trait Vahvistukseton extends Suoritus {
  override def vahvistus: Option[Vahvistus] = None
  def mutuallyExclusivePäätasoVahvistukseton = {}
}

trait MonikielinenSuoritus {
  @Description("Opintojen muut suorituskielet. Ne muut (kuin koulun opetuskielet) kielet joilla on opetettu vähintään 25% oppilaan oppitunneista lukuvuoden aikana")
  @KoodistoUri("kieli")
  @OksaUri("tmpOKSAID308", "koulutusorganisaation opetuskieli")
  def muutSuorituskielet: Option[List[Koodistokoodiviite]]
}

trait PakollisenTaiValinnaisenSuoritus extends Suoritus {
  def koulutusmoduuli: KoulutusmoduuliValinnainenLaajuus with Valinnaisuus
  override def ryhmittelytekijä = Some(if (koulutusmoduuli.pakollinen) "pakolliset" else "valinnaiset")
}

trait Laajuudellinen extends Suoritus {
  @Description("Tässä suorituksessa koulutusmoduulin laajuus on pakollinen")
  def koulutusmoduuli: Koulutusmoduuli
}

trait ErityisopetuksellinenPäätasonSuoritus extends PäätasonSuoritus {
  def sisältääOsaAikaisenErityisopetuksen: Boolean
}
