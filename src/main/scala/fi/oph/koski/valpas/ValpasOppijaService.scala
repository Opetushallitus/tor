package fi.oph.koski.valpas

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.koodisto.KoodistoViite
import fi.oph.koski.log.{AuditLog, KoskiMessageField, Logging}
import fi.oph.koski.raportointikanta.RHenkilöRow
import fi.oph.koski.schema.Koodistokoodiviite
import fi.oph.koski.valpas.log.{ValpasAuditLogMessage, ValpasOperation}
import fi.oph.koski.valpas.repository.{ValpasDatabaseService, ValpasOppija}
import fi.oph.koski.valpas.valpasuser.ValpasSession

class ValpasOppijaService(application: KoskiApplication) extends Logging {
  private val dbService = new ValpasDatabaseService(application)
  private val koodisto = application.koodistoPalvelu

  def getOppijat(implicit session: ValpasSession): Option[Seq[RHenkilöRow]] = {
    // TODO
    Some(List())
  }

  def getOppija(oid: String)(implicit session: ValpasSession): Option[ValpasOppija] =
    dbService.getOppivelvollinenHenkilö(oid, ValpasAccessResolver.valpasOrganisaatioOids.toSeq)
      .map(enrichOppija)
      .map(oppija => {
        auditLogOppijaKatsominen(oppija)
        oppija
      })

  def enrichOppija(oppija: ValpasOppija): ValpasOppija =
    oppija.copy(
      opiskeluoikeudet = oppija.opiskeluoikeudet.map(opiskeluoikeus =>
        opiskeluoikeus.copy(tyyppi = enrichKoodistokoodiviite(opiskeluoikeus.tyyppi))))

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
}