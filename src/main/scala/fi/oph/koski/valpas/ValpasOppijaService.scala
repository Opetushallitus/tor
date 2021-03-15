package fi.oph.koski.valpas

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.koodisto.KoodistoViite
import fi.oph.koski.log.{AuditLog, KoskiMessageField, Logging}
import fi.oph.koski.schema.Koodistokoodiviite
import fi.oph.koski.valpas.hakukooste.ValpasHakukoosteService
import fi.oph.koski.valpas.log.{ValpasAuditLogMessage, ValpasOperation}
import fi.oph.koski.valpas.repository._
import fi.oph.koski.valpas.valpasuser.ValpasSession

class ValpasOppijaService(
  application: KoskiApplication,
  hakukoosteService: ValpasHakukoosteService,
) extends Logging {
  private val dbService = new ValpasDatabaseService(application)
  private val koodisto = application.koodistoPalvelu
  private val accessResolver = new ValpasAccessResolver(application)

  // TODO: Tästä puuttuu oppijan tietoihin käsiksi pääsy seuraavilta käyttäjäryhmiltä:
  // (1) muut kuin peruskoulun hakeutumisen valvojat (esim. nivelvaihe ja aikuisten perusopetus)
  // (4) OPPILAITOS_SUORITTAMINEN-, OPPILAITOS_MAKSUTTOMUUS- ja KUNTA -käyttäjät.
  def getOppijat(oppilaitosOids: Set[String], rajapäivät: Rajapäivät)(implicit session: ValpasSession): Option[Seq[ValpasOppija]] =
    accessResolver.organisaatiohierarkiaOids(oppilaitosOids)
      .map(oids => dbService.getPeruskoulunValvojalleNäkyvätOppijat(Some(oids.toSeq), rajapäivät).map(enrichOppija))
      .map(fetchHaut)
      .map(oppijat => {
        oppilaitosOids.foreach(auditLogOppilaitoksenOppijatKatsominen)
        oppijat
      })

  // TODO: Tästä puuttuu oppijan tietoihin käsiksi pääsy seuraavilta käyttäjäryhmiltä:
  // (1) muut kuin peruskoulun hakeutumisen valvojat (esim. nivelvaihe ja aikuisten perusopetus)
  // (4) OPPILAITOS_SUORITTAMINEN-, OPPILAITOS_MAKSUTTOMUUS- ja KUNTA -käyttäjät.
  def getOppija(oppijaOid: String, rajapäivät: Rajapäivät)(implicit session: ValpasSession): Option[ValpasOppija] =
    Some(oppijaOid)
      .flatMap(oid => dbService.getPeruskoulunValvojalleNäkyväOppija(oid, rajapäivät))
      .filter(oppija => accessResolver.accessToSomeOrgs(oppija.oikeutetutOppilaitokset))
      .map(enrichOppija)
      .map(fetchHaku)
      .map(oppija => {
        auditLogOppijaKatsominen(oppija)
        oppija
      })

  def fetchHaku(oppija: ValpasOppijaLisätiedoilla): ValpasOppijaLisätiedoilla = {
    oppija.copy(
      haut = hakukoosteService
        .getHakukoosteet(Set(oppija.henkilö.oid))
        .map(ValpasHakutilanne.apply)
        .toOption // TODO: Virheen voisi käsitellä, eikä tipauttaa pois
    )
  }

  def fetchHaut(oppijat: Seq[ValpasOppijaLisätiedoilla]): Seq[ValpasOppijaLisätiedoilla] = {
    val haut = hakukoosteService.getHakukoosteet(oppijat.map(_.henkilö.oid).toSet)
    oppijat.map(oppija => oppija.copy(
      haut = haut
        .map(h => h.filter(_.oppijaOid == oppija.henkilö.oid))
        .map(ValpasHakutilanne.apply)
        .toOption // TODO: Virheen voisi käsitellä, eikä tipauttaa pois
    ))
  }

  def enrichOppija(oppija: ValpasOppijaResult): ValpasOppijaLisätiedoilla =
    ValpasOppijaLisätiedoilla(
      oppija.copy(
        opiskeluoikeudet = oppija.opiskeluoikeudet.map(opiskeluoikeus =>
          opiskeluoikeus.copy(
            tyyppi = enrichKoodistokoodiviite(opiskeluoikeus.tyyppi),
            viimeisinTila = enrichKoodistokoodiviite(opiskeluoikeus.viimeisinTila)
          )
        )
      )
    )

  def enrichKoodistokoodiviite(koodiviite: Koodistokoodiviite): Koodistokoodiviite =
    if (koodiviite.nimi.isDefined) {
      koodiviite
    } else {
      koodisto
        .getKoodistoKoodit(KoodistoViite(koodiviite.koodistoUri, koodiviite.koodistoVersio.getOrElse(1)))
        .find(_.koodiArvo == koodiviite.koodiarvo)
        .map(k => Koodistokoodiviite(
          koodiarvo = k.koodiArvo,
          nimi = k.nimi,
          lyhytNimi = k.lyhytNimi,
          koodistoUri = k.koodistoUri,
          koodistoVersio = Some(k.versio)
        ))
        .getOrElse(koodiviite)
    }

  def auditLogOppijaKatsominen(oppija: ValpasOppija)(implicit session: ValpasSession) =
    AuditLog.log(ValpasAuditLogMessage(
      ValpasOperation.VALPAS_OPPIJA_KATSOMINEN,
      Map(KoskiMessageField.oppijaHenkiloOid -> oppija.henkilö.oid)
    ))

  def auditLogOppilaitoksenOppijatKatsominen(oppilaitosOid: String)(implicit session: ValpasSession) =
    AuditLog.log(ValpasAuditLogMessage(
      ValpasOperation.VALPAS_OPPILAITOKSET_OPPIJAT_KATSOMINEN,
      Map(KoskiMessageField.juuriOrganisaatio -> oppilaitosOid)
    ))
}
