package fi.oph.koski.kela

import java.time.{LocalDate, LocalDateTime}

import fi.oph.koski.koskiuser.Rooli
import fi.oph.koski.schema
import fi.oph.koski.schema.OikeuttaMaksuttomuuteenPidennetty
import fi.oph.koski.schema.annotation.SensitiveData
import fi.oph.scalaschema.{ClassSchema, SchemaToJson}
import org.json4s.JValue

object KelaSchema {
  lazy val schemaJson: JValue = SchemaToJson.toJsonSchema(schema.KoskiSchema.createSchema(classOf[KelaOppija]).asInstanceOf[ClassSchema])
}

case class KelaOppija(
  henkilö: Henkilo,
  opiskeluoikeudet: List[KelaOpiskeluoikeus]
)

case class Henkilo(
  oid: String,
  hetu: Option[String],
  syntymäaika: Option[LocalDate],
  etunimet: String,
  sukunimi: String,
  kutsumanimi: String
)

case class KelaOpiskeluoikeus(
  oid: Option[String],
  versionumero: Option[Int],
  aikaleima: Option[LocalDateTime],
  oppilaitos: Option[Oppilaitos],
  koulutustoimija: Option[Koulutustoimija],
  sisältyyOpiskeluoikeuteen: Option[Sisältäväopiskeluoikeus],
  arvioituPäättymispäivä: Option[LocalDate],
  ostettu: Option[Boolean],
  tila: OpiskeluoikeudenTila,
  suoritukset: List[Suoritus],
  lisätiedot: Option[OpiskeluoikeudenLisätiedot],
  tyyppi: Koodistokoodiviite,
  alkamispäivä: Option[LocalDate],
  päättymispäivä: Option[LocalDate],
  organisaatioHistoria: Option[List[OrganisaatioHistoria]]
)

case class Sisältäväopiskeluoikeus(
  oid: String,
  oppilaitos: Oppilaitos
)

case class OpiskeluoikeudenTila(
  opiskeluoikeusjaksot: List[Opiskeluoikeusjakso]
)

case class Opiskeluoikeusjakso(
  alku: LocalDate,
  tila: Koodistokoodiviite,
  opintojenRahoitus: Option[Koodistokoodiviite]
)

case class OrganisaatioHistoria(
  muutospäivä: LocalDate,
  oppilaitos: Option[Oppilaitos],
  koulutustoimija: Option[Koulutustoimija]
)

case class OpiskeluoikeudenLisätiedot(
  oikeusMaksuttomaanAsuntolapaikkaanPerusopetus: Option[schema.Aikajakso],
  oikeusMaksuttomaanAsuntolapaikkaan: Option[Boolean],
  majoitus: Option[List[schema.Aikajakso]],
  sisäoppilaitosmainenMajoitus: Option[List[schema.Aikajakso]],
  vaativanErityisenTuenYhteydessäJärjestettäväMajoitus: Option[List[schema.Aikajakso]],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  erityinenTuki: Option[List[schema.Aikajakso]],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  vaativanErityisenTuenErityinenTehtävä: Option[List[schema.Aikajakso]],
  ulkomaanjaksot: Option[List[Ulkomaanjakso]],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  hojks: Option[Hojks],
  osaAikaisuusjaksot: Option[List[schema.OsaAikaisuusJakso]],
  opiskeluvalmiuksiaTukevatOpinnot: Option[List[schema.OpiskeluvalmiuksiaTukevienOpintojenJakso]],
  vankilaopetuksessa: Option[List[schema.Aikajakso]],
  ulkomainenVaihtoopiskelija: Option[Boolean],
  yksityisopiskelija: Option[Boolean],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  koulukoti: Option[List[schema.Aikajakso]],
  majoitusetu: Option[schema.Aikajakso],
  ulkomailla: Option[schema.Aikajakso],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätös: Option[schema.TehostetunTuenPäätös],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätökset: Option[List[schema.TehostetunTuenPäätös]],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  joustavaPerusopetus: Option[schema.Aikajakso],
  maksuttomuus: Option[List[schema.Maksuttomuus]],
  oikeuttaMaksuttomuuteenPidennetty: Option[List[schema.OikeuttaMaksuttomuuteenPidennetty]]
)

case class Suoritus(
  koulutusmoduuli: SuorituksenKoulutusmoduuli,
  suoritustapa: Option[Koodistokoodiviite],
  toimipiste: Option[Toimipiste],
  oppimäärä: Option[Koodistokoodiviite],
  vahvistus: Option[Vahvistus],
  osasuoritukset: Option[List[Osasuoritus]],
  tyyppi: Koodistokoodiviite,
  tila: Option[Koodistokoodiviite],
  osaamisala: Option[List[schema.Osaamisalajakso]],
  toinenOsaamisala: Option[Boolean],
  alkamispäivä: Option[LocalDate],
  järjestämismuodot: Option[List[Järjestämismuotojakso]],
  osaamisenHankkimistavat: Option[List[OsaamisenHankkimistapajakso]],
  työssäoppimisjaksot: Option[List[Työssäoppimisjakso]],
  koulutussopimukset: Option[List[Koulutussopimusjakso]],
  tutkintonimike: Option[List[Koodistokoodiviite]],
  toinenTutkintonimike: Option[Boolean],
  theoryOfKnowledge: Option[IBTheoryOfKnowledgeSuoritus],
  extendedEssay: Option[IBExtendedEssaySuoritus],
  creativityActionService: Option[IBCASSuoritus],
  jääLuokalle: Option[Boolean],
  pakollisetKokeetSuoritettu: Option[Boolean],
  kokonaislaajuus: Option[schema.Laajuus]
)

case class Osasuoritus(
  koulutusmoduuli: OsasuorituksenKoulutusmoduuli,
  liittyyTutkinnonOsaan: Option[Koodistokoodiviite],
  arviointi: Option[List[Arviointi]],
  toimipiste: Option[Toimipiste],
  vahvistus: Option[Vahvistus],
  osasuoritukset: Option[List[Osasuoritus]],
  tyyppi: Koodistokoodiviite,
  tila: Option[Koodistokoodiviite],
  tutkinto: Option[Tutkinto],
  tutkinnonOsanRyhmä: Option[Koodistokoodiviite],
  osaamisala: Option[List[schema.Osaamisalajakso]],
  alkamispäivä: Option[LocalDate],
  tunnustettu: Option[OsaamisenTunnustaminen],
  toinenOsaamisala: Option[Boolean],
  toinenTutkintonimike: Option[Boolean],
  näyttö: Option[Näyttö],
  vastaavuusTodistuksenTiedot: Option[VastaavuusTodistuksenTiedot],
  suoritettuLukiodiplomina: Option[Boolean],
  suoritettuSuullisenaKielikokeena: Option[Boolean],
  luokkaAste: Option[Koodistokoodiviite],
  tutkintokerta: Option[YlioppilastutkinnonTutkintokerta],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  yksilöllistettyOppimäärä: Option[Boolean],
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  lisätiedot: Option[List[AmmatillisenTutkinnonOsanLisätieto]]
)

case class SuorituksenKoulutusmoduuli(
  tunniste: Koodistokoodiviite,
  laajuus: Option[schema.Laajuus],
  perusteenDiaarinumero: Option[String],
  perusteenNimi: Option[schema.LocalizedString],
  koulutustyyppi: Option[Koodistokoodiviite],
  pakollinen: Option[Boolean],
  kuvaus: Option[schema.LocalizedString],
  kieli: Option[Koodistokoodiviite],
  diplomaType: Option[Koodistokoodiviite],
  oppimäärä: Option[Koodistokoodiviite]
)

case class OsasuorituksenKoulutusmoduuli(
  tunniste: Koodistokoodiviite,
  laajuus: Option[schema.Laajuus],
  perusteenNimi: Option[schema.LocalizedString],
  pakollinen: Option[Boolean],
  kuvaus: Option[schema.LocalizedString],
  kieli: Option[Koodistokoodiviite],
  osaAlue: Option[Koodistokoodiviite],
  taso: Option[Koodistokoodiviite],
  ryhmä: Option[Koodistokoodiviite],
  kurssinTyyppi: Option[Koodistokoodiviite],
  oppimäärä: Option[Koodistokoodiviite]
)

case class Koodistokoodiviite(
  koodiarvo: String,
  nimi: Option[schema.LocalizedString],
  lyhytNimi: Option[schema.LocalizedString],
  koodistoUri: Option[String],
  koodistoVersio: Option[Int]
)

case class Ulkomaanjakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  maa: Option[Koodistokoodiviite],
  kuvaus: Option[schema.LocalizedString]
)

case class Koulutussopimusjakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  työssäoppimispaikka: Option[schema.LocalizedString],
  paikkakunta: Koodistokoodiviite,
  maa: Koodistokoodiviite
)

case class Työssäoppimisjakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  työssäoppimispaikka: Option[schema.LocalizedString],
  paikkakunta: Koodistokoodiviite,
  maa: Koodistokoodiviite,
  laajuus: schema.Laajuus
)

case class Järjestämismuoto (
  tunniste: Koodistokoodiviite
)

case class Järjestämismuotojakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  järjestämismuoto: Järjestämismuoto
)

case class Oppisopimus(
  työnantaja: Yritys
)

case class Yritys(
  nimi: schema.LocalizedString,
  yTunnus: String
)

trait OsaamisenHankkimistapa {
  def tunniste: Koodistokoodiviite
}

case class OsaamisenHankkimistapaIlmanLisätietoja (
  tunniste: Koodistokoodiviite
) extends OsaamisenHankkimistapa

case class OppisopimuksellinenOsaamisenHankkimistapa (
  tunniste: Koodistokoodiviite,
  oppisopimus: Oppisopimus
) extends OsaamisenHankkimistapa

case class OsaamisenHankkimistapajakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  osaamisenHankkimistapa: OsaamisenHankkimistapa
)

case class OsaamisenTunnustaminen(
  osaaminen: Option[Osasuoritus],
  selite: schema.LocalizedString,
  rahoituksenPiirissä: Boolean
)

case class Vahvistus(
  päivä: LocalDate
)

case class Arviointi(
  hyväksytty: Boolean,
  päivä: Option[LocalDate]
)

case class Oppilaitos(
  oid: String,
  oppilaitosnumero: Option[Koodistokoodiviite],
  nimi: Option[schema.LocalizedString],
  kotipaikka: Option[Koodistokoodiviite]
)

case class Koulutustoimija(
  oid: String,
  nimi: Option[schema.LocalizedString],
  yTunnus: Option[String],
  kotipaikka: Option[Koodistokoodiviite]
)

case class Toimipiste(
  oid: String,
  nimi: Option[schema.LocalizedString] = None,
  kotipaikka: Option[Koodistokoodiviite] = None
)

case class Hojks(
  opetusryhmä: Koodistokoodiviite,
  alku: Option[LocalDate],
  loppu: Option[LocalDate]
)

case class Näyttö(
  suorituspaikka: Option[NäytönSuorituspaikka],
  suoritusaika: Option[schema.NäytönSuoritusaika],
  työssäoppimisenYhteydessä: Boolean,
  arviointi: Option[NäytönArviointi],
)

case class NäytönSuorituspaikka(
  tunniste: Koodistokoodiviite,
  kuvaus: schema.LocalizedString
)

case class NäytönArviointi(
  hyväksytty: Boolean
)

case class Tutkinto(
  tunniste: Koodistokoodiviite,
  perusteenDiaarinumero: Option[String],
  perusteenNimi: Option[schema.LocalizedString],
  koulutustyyppi: Option[Koodistokoodiviite]
)

case class IBTheoryOfKnowledgeSuoritus(
  koulutusmoduuli: IBTheoryOfKnowledgeSuoritusKoulutusmoduuli,
  tila: Option[Koodistokoodiviite],
  arviointi: Option[List[Arviointi]] = None,
  osasuoritukset: Option[List[Osasuoritus]],
  tyyppi: Koodistokoodiviite
)

case class IBTheoryOfKnowledgeSuoritusKoulutusmoduuli(
  tunniste: Koodistokoodiviite,
  pakollinen: Boolean
)

case class IBExtendedEssaySuoritus(
  koulutusmoduuli: IBExtendedEssaySuoritusKoulutusmoduuli,
  tila: Option[Koodistokoodiviite],
  arviointi: Option[List[Arviointi]] = None,
  tyyppi: Koodistokoodiviite
)

case class IBExtendedEssaySuoritusKoulutusmoduuli(
  tunniste: Koodistokoodiviite,
  pakollinen: Boolean
)

case class IBCASSuoritus(
  koulutusmoduuli: SuorituksenKoulutusmoduuli,
  arviointi: Option[List[Arviointi]] = None,
  tyyppi: Koodistokoodiviite,
  tila: Option[Koodistokoodiviite]
)

case class YlioppilastutkinnonTutkintokerta(
  koodiarvo: String,
  vuosi: Int,
  vuodenaika: schema.LocalizedString
)

case class VastaavuusTodistuksenTiedot(
  lukioOpintojenLaajuus: schema.Laajuus
)

case class AmmatillisenTutkinnonOsanLisätieto(
  tunniste: Koodistokoodiviite,
  kuvaus: schema.LocalizedString
)
