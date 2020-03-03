package fi.oph.koski.organisaatio

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.koskiuser.{KoskiSession, KäyttöoikeusOrg}
import fi.oph.koski.perustiedot.VarhaiskasvatusToimipistePerustiedot

class OrganisaatioService(application: KoskiApplication) {
  private val organisaatioRepository = application.organisaatioRepository
  private val perustiedot = VarhaiskasvatusToimipistePerustiedot(application.perustiedotIndexer)
  private val localizationRepository = application.localizationRepository

  def searchInAllOrganizations(query: Option[String])(implicit u: KoskiSession): List[OrganisaatioHierarkia] = {
    query match {
      case Some(qry) if qry.length >= 3 =>
        organisaatioRepository.findHierarkia(qry).sortBy(organisaatioNimi)
      case _ => Nil
    }
  }

  def searchInEntitledOrganizations(query: Option[String], orgTypes: OrgTypesToShow)(implicit u: KoskiSession): Iterable[OrganisaatioHierarkia] = {
    val orgs = getOrganisaatiot(orgTypes)

    query match {
      case Some(qry) => OrganisaatioHierarkiaFilter(qry, u.lang).filter(orgs)
      case None => orgs
    }
  }

  private def getOrganisaatiot(orgTypes: OrgTypesToShow)(implicit u: KoskiSession) = orgTypes match {
    case OmatOrganisaatiot => omatOrganisaatioHierarkiat
    case Kaikki => omatOrganisaatioHierarkiat ++ omatOstopalveluOrganisaatioHierarkiat
    case VarhaiskasvatusToimipisteet => kaikkiOstopalveluHierarkiat
  }

  def omatOrganisaatioHierarkiat(implicit u: KoskiSession): List[OrganisaatioHierarkia] =
    u.orgKäyttöoikeudet.filter(_.juuri).toList.flatMap { ko: KäyttöoikeusOrg =>
      organisaatioRepository.getOrganisaatioHierarkia(ko.organisaatio.oid)
    }.sortBy(organisaatioNimi)

  private def omatOstopalveluOrganisaatioHierarkiat(implicit u: KoskiSession) = List(OrganisaatioHierarkia(
    oid = "ostopalvelu/palveluseteli",
    nimi = localizationRepository.get("Ostopalvelu/palveluseteli"),
    children = omatOstopalveluOrganisaatiot
  ))

  private def omatOstopalveluOrganisaatiot(implicit u: KoskiSession) = {
    val päiväkoditJoihinTallennettuOpiskeluoikeuksia: Set[String] = perustiedot.haeVarhaiskasvatustoimipisteet(u.varhaiskasvatusKoulutustoimijat)
    OrganisaatioHierarkia.flatten(kaikkiOstopalveluHierarkiat).filter(o => päiväkoditJoihinTallennettuOpiskeluoikeuksia.contains(o.oid))
  }

  private def kaikkiOstopalveluHierarkiat(implicit u: KoskiSession) = if (u.hasKoulutustoimijaVarhaiskasvatuksenJärjestäjäAccess) {
    organisaatioRepository.findVarhaiskasvatusHierarkiat
      .filterNot(h => u.varhaiskasvatusKoulutustoimijat.contains(h.oid)) // karsi oman organisaation päiväkodit pois
      .sortBy(organisaatioNimi)
  } else {
    Nil
  }

  private def organisaatioNimi(implicit u: KoskiSession): OrganisaatioHierarkia => String = _.nimi.get(u.lang)
}
