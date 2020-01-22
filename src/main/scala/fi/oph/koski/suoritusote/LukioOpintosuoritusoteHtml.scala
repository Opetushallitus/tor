package fi.oph.koski.suoritusote

import fi.oph.koski.koskiuser.KoskiSession
import fi.oph.koski.localization.LocalizationRepository
import fi.oph.koski.schema.Suoritus

class LukioOpintosuoritusoteHtml(implicit override val user: KoskiSession, override val localizationRepository: LocalizationRepository) extends OpintosuoritusoteHtml {
  override protected def laajuus(suoritus: Suoritus) = if (suoritus.osasuoritukset.isDefined) {
    decimalFormat.format(suoritus.osasuoritusLista.foldLeft(0d) { (laajuus: Double, suoritus: Suoritus) =>
      laajuus + suoritus.koulutusmoduuli.laajuus.map(_.arvo).getOrElse(1d)
    })
  } else {
    suoritus.koulutusmoduuli.laajuus.map(l => decimalFormat.format(l.arvo)).getOrElse("1")
  }
}
