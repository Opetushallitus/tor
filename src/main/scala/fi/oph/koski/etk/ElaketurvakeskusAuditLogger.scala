package fi.oph.koski.etk

import fi.oph.koski.config.KoskiApplication
import fi.oph.common.koskiuser.KoskiSession
import fi.oph.common.log.{AuditLog, AuditLogMessage, KoskiMessageField, KoskiOperation}
import fi.oph.koski.schema.Henkilö
import fi.oph.koski.schema.Henkilö.Hetu
import fi.oph.koski.schema.Henkilö.Oid

object ElaketurvakeskusAuditLogger {

  def auditLog(response: EtkResponse, application: KoskiApplication)(implicit koskiSession: KoskiSession) = {
    val oids = getOids(response, application)
    auditLogOpiskeluoikeusKatsominen(oids)
  }

  private def auditLogOpiskeluoikeusKatsominen(oids: List[Oid])(implicit koskiSession: KoskiSession): Unit = oids
    .map(oid => AuditLogMessage(KoskiOperation.OPISKELUOIKEUS_KATSOMINEN, koskiSession, Map(KoskiMessageField.oppijaHenkiloOid -> oid)))
    .foreach(AuditLog.log)

  private def getOids(response: EtkResponse, application: KoskiApplication) = {
    val (hetut, oidit) = validateIdentificationInfo(response)
    val haetutOidit = hetut.grouped(1000).flatMap(application.opintopolkuHenkilöFacade.findOppijatByHetusNoSlaveOids(_).map(_.oid))
    oidit ++ haetutOidit
  }

  private def validateIdentificationInfo(response: EtkResponse): (List[Hetu], List[Oid]) = {
    response.tutkinnot.map(identificationInfo).foldLeft[(List[Hetu], List[Oid])]((Nil, Nil)) {
      case ((hetut, oidit), hetuOrOid) => hetuOrOid match {
        case Right(oid) => (hetut, oid :: oidit)
        case Left(Some(hetu)) => (hetu :: hetut, oidit)
        case _ => (hetut, oidit)
      }
    }
  }

  private def identificationInfo(tutkintotieto: EtkTutkintotieto): Either[Option[Hetu], Oid] =
    validateOid(tutkintotieto.oid).toRight(tutkintotieto.henkilö.hetu)

  private def validateOid(oid: Option[Oid]): Option[Oid] = if (oid.exists(Henkilö.isValidHenkilöOid)) oid else None
}
