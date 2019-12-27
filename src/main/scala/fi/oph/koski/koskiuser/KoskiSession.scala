package fi.oph.koski.koskiuser

import java.net.InetAddress

import fi.oph.koski.henkilo.OppijaHenkilö
import fi.oph.koski.json.SensitiveDataAllowed
import fi.oph.koski.koskiuser.Rooli._
import fi.oph.koski.log.{LogUserContext, Loggable, Logging}
import fi.oph.koski.schema._
import org.scalatra.servlet.RichRequest

import scala.concurrent.{ExecutionContext, Future}

class KoskiSession(val user: AuthenticationUser, val lang: String, val clientIp: InetAddress, val userAgent: String, käyttöoikeudet: => Set[Käyttöoikeus]) extends LogUserContext with UserWithUsername with UserWithOid with SensitiveDataAllowed  with Loggable with Logging {
  def oid = user.oid
  def username = user.username
  def userOption = Some(user)
  def logString = "käyttäjä " + username + " / " + user.oid

  lazy val orgKäyttöoikeudet: Set[KäyttöoikeusOrg] = käyttöoikeudet.collect { case k : KäyttöoikeusOrg => k}
  lazy val globalKäyttöoikeudet: Set[KäyttöoikeusGlobal] = käyttöoikeudet.collect { case k: KäyttöoikeusGlobal => k}
  lazy val globalViranomaisKäyttöoikeudet: Set[KäyttöoikeusViranomainen] = käyttöoikeudet.collect { case k: KäyttöoikeusViranomainen => k}
  lazy val allowedOpiskeluoikeusTyypit: Set[String] = käyttöoikeudet.flatMap(_.allowedOpiskeluoikeusTyypit)
  lazy val hasKoulutusmuotoRestrictions: Boolean = allowedOpiskeluoikeusTyypit != OpiskeluoikeudenTyyppi.kaikkiTyypit.map(_.koodiarvo)
  lazy val kaikkiKäyttöoikeudet: Set[Käyttöoikeus] = käyttöoikeudet

  def orgKäyttöoikeudetByAccessType(accessType: AccessType.Value): Set[KäyttöoikeusOrg] = orgKäyttöoikeudet.filter(_.organisaatioAccessType.contains(accessType))
  lazy val globalAccess = globalKäyttöoikeudet.flatMap { _.globalAccessType }
  def isRoot = globalAccess.contains(AccessType.write)
  def isPalvelukäyttäjä = orgKäyttöoikeudet.flatMap(_.organisaatiokohtaisetPalveluroolit).contains(Palvelurooli(TIEDONSIIRTO))
  def hasReadAccess(organisaatio: OrganisaatioPath): Boolean = hasAccess(organisaatio, AccessType.read)
  def hasCreateAndUpdateAccess(organisaatio: OrganisaatioPath): Boolean = hasAccess(organisaatio, AccessType.write)
  def hasCreateAccess(organisaatio: Organisaatio.Oid): Boolean = {
    orgKäyttöoikeudetByAccessType(AccessType.write).exists(_.organisaatioOid == organisaatio)
  }
  def hasTiedonsiirronMitätöintiAccess(organisaatio: OrganisaatioPath): Boolean = hasAccess(organisaatio, AccessType.tiedonsiirronMitätöinti)
  def hasLuovutuspalveluAccess: Boolean = globalViranomaisKäyttöoikeudet.exists(_.isLuovutusPalveluAllowed)
  def hasTilastokeskusAccess: Boolean = globalViranomaisKäyttöoikeudet.flatMap(_.globalPalveluroolit).contains(Palvelurooli("KOSKI", TILASTOKESKUS))
  def hasValviraAccess: Boolean = globalViranomaisKäyttöoikeudet.flatMap(_.globalPalveluroolit).contains(Palvelurooli("KOSKI", VALVIRA))

  def hasAccess(orgPath: OrganisaatioPath, accessType: AccessType.Value): Boolean = {
    def access = globalAccess.contains(accessType) || hasOrganisationAccess(orgPath, accessType)
    if (accessType == AccessType.write) {
      access && hasRole(LUOTTAMUKSELLINEN_KAIKKI_TIEDOT)
    } else {
      access
    }
  }

  private def hasOrganisationAccess(orgPath: OrganisaatioPath, accessType: AccessType.Value) = {
    orgKäyttöoikeudetByAccessType(accessType).exists { orgAccess =>
      orgAccess.tuple == (orgPath.root, orgPath.leaf)       ||
      orgAccess.tuple == (orgPath.oppilaitos, orgPath.leaf) ||
      orgAccess.tuple == (orgPath.leaf, orgPath.leaf)
    }
  }

  def hasGlobalKoulutusmuotoReadAccess: Boolean = globalViranomaisKäyttöoikeudet.flatMap(_.globalAccessType).contains(AccessType.read)

  def hasGlobalReadAccess = globalAccess.contains(AccessType.read)
  def hasAnyWriteAccess = (globalAccess.contains(AccessType.write) || orgKäyttöoikeudetByAccessType(AccessType.write).nonEmpty) && hasRole(LUOTTAMUKSELLINEN_KAIKKI_TIEDOT)
  def hasLocalizationWriteAccess = globalKäyttöoikeudet.find(_.globalPalveluroolit.contains(Palvelurooli("LOKALISOINTI", "CRUD"))).isDefined
  def hasAnyReadAccess = hasGlobalReadAccess || orgKäyttöoikeudet.exists(_.organisaatioAccessType.contains(AccessType.read)) || hasGlobalKoulutusmuotoReadAccess
  def hasAnyTiedonsiirronMitätöintiAccess = globalAccess.contains(AccessType.tiedonsiirronMitätöinti) || orgKäyttöoikeudetByAccessType(AccessType.tiedonsiirronMitätöinti).nonEmpty
  def hasRaportitAccess = hasAnyReadAccess && hasRole(LUOTTAMUKSELLINEN_KAIKKI_TIEDOT) && !hasGlobalKoulutusmuotoReadAccess
  def sensitiveDataAllowed(requiredRoles: Set[Role]) = requiredRoles.exists(hasRole)

  // Note: keep in sync with PermissionCheckServlet's hasSufficientRoles function. See PermissionCheckServlet for more comments.
  private val OppijanumerorekisteriRekisterinpitäjä = Palvelurooli("OPPIJANUMEROREKISTERI", "REKISTERINPITAJA")
  private val OppijanumerorekisteriReadUpdate = Palvelurooli("OPPIJANUMEROREKISTERI", "HENKILON_RU")
  def hasHenkiloUiWriteAccess = globalKäyttöoikeudet.exists(ko => ko.globalPalveluroolit.contains(OppijanumerorekisteriRekisterinpitäjä) || ko.globalPalveluroolit.contains(OppijanumerorekisteriReadUpdate)) ||
    orgKäyttöoikeudet.exists(ko => ko.organisaatiokohtaisetPalveluroolit.contains(OppijanumerorekisteriRekisterinpitäjä) || ko.organisaatiokohtaisetPalveluroolit.contains(OppijanumerorekisteriReadUpdate))

  def hasRole(role: String): Boolean = {
    val palveluRooli = Palvelurooli("KOSKI", role)
    globalKäyttöoikeudet.exists(_.globalPalveluroolit.contains(palveluRooli)) ||
    globalViranomaisKäyttöoikeudet.exists(_.globalPalveluroolit.contains(palveluRooli)) ||
    orgKäyttöoikeudet.exists(_.organisaatiokohtaisetPalveluroolit.contains(palveluRooli))
  }

  def juuriOrganisaatiot: List[String] = orgKäyttöoikeudet.collect { case r: KäyttöoikeusOrg if r.juuri => r.organisaatioOid }.toList

  Future(käyttöoikeudet)(ExecutionContext.global) // haetaan käyttöoikeudet toisessa säikeessä rinnakkain
}

case class OrganisaatioPath(root: Organisaatio.Oid, oppilaitos: Organisaatio.Oid, leaf: Organisaatio.Oid)

object KoskiSession {
  def apply(user: AuthenticationUser, request: RichRequest, käyttöoikeudet: KäyttöoikeusRepository): KoskiSession = {
    new KoskiSession(user, KoskiUserLanguage.getLanguageFromCookie(request), LogUserContext.clientIpFromRequest(request), LogUserContext.userAgent(request), käyttöoikeudet.käyttäjänKäyttöoikeudet(user))
  }

  def huollettavaSession(huoltajaSession: KoskiSession, huollettava: OppijaHenkilö): KoskiSession = {
    val user = huoltajaSession.user.copy(oid = huollettava.oid, username = huollettava.oid, name = s"${huollettava.etunimet} ${huollettava.sukunimi}", huollettava = true)
    new KoskiSession(user, huoltajaSession.lang, huoltajaSession.clientIp, huoltajaSession.userAgent, huoltajaSession.kaikkiKäyttöoikeudet)
  }

  private val systemKäyttöoikeudet: Set[Käyttöoikeus] = Set(KäyttöoikeusGlobal(List(Palvelurooli(OPHPAAKAYTTAJA), Palvelurooli(LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))))
  private val KOSKI_SYSTEM_USER: String = "Koski system user"
  private val UNTRUSTED_SYSTEM_USER = "Koski untrusted system user"
  val SUORITUSJAKO_KATSOMINEN_USER = "Koski suoritusjako katsominen"
  // Internal user with root access
  val systemUser = new KoskiSession(AuthenticationUser(KOSKI_SYSTEM_USER, KOSKI_SYSTEM_USER, KOSKI_SYSTEM_USER, None), "fi", InetAddress.getLoopbackAddress, "", systemKäyttöoikeudet)

  val untrustedUser = new KoskiSession(AuthenticationUser(UNTRUSTED_SYSTEM_USER, UNTRUSTED_SYSTEM_USER, UNTRUSTED_SYSTEM_USER, None), "fi", InetAddress.getLoopbackAddress, "", Set())

  def suoritusjakoKatsominenUser(request: RichRequest) = new KoskiSession(AuthenticationUser(SUORITUSJAKO_KATSOMINEN_USER, SUORITUSJAKO_KATSOMINEN_USER, SUORITUSJAKO_KATSOMINEN_USER, None), KoskiUserLanguage.getLanguageFromCookie(request), LogUserContext.clientIpFromRequest(request), LogUserContext.userAgent(request), Set(KäyttöoikeusGlobal(List(Palvelurooli(OPHKATSELIJA)))))
}

