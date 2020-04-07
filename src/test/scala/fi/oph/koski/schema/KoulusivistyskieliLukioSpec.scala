package fi.oph.koski.schema

import fi.oph.koski.api.{LocalJettyHttpSpecification, OpiskeluoikeusTestMethodsLukio}
import fi.oph.koski.documentation.{ExampleData, YleissivistavakoulutusExampleData}
import fi.oph.koski.documentation.LukioExampleData._
import org.scalatest.FreeSpec

class KoulusivistyskieliLukioSpec extends FreeSpec with LocalJettyHttpSpecification with OpiskeluoikeusTestMethodsLukio {

  "Koulusivistyskieli - Lukion oppimäärän suoritus" - {

    "Koulusivistyskieliä voi olla kaksi jos äidinkieli ja kirjallisuus on suoritettu suomen ja ruotsin kielellä" in {
      val opiskeluoikeus = oppimääränOpiskeluoikeusOppiaineilla(
        äidinkieli("AI1", "8"),
        äidinkieli("AI2", "9")
      )
      verifyKoulusivistyskieli(opiskeluoikeus, Some(List(
        Koodistokoodiviite("FI", "kieli"),
        Koodistokoodiviite("SV", "kieli")
      )))
    }

    "Hyväksytty suoritus valitaan koulusivistyskieleksi" in {
      val opiskeluoikeus = oppimääränOpiskeluoikeusOppiaineilla(äidinkieli("AI1", "8"))

      verifyKoulusivistyskieli(opiskeluoikeus,
        Some(List(Koodistokoodiviite("FI", "kieli")))
      )
    }

    "Hylättyä suoritusta ei valita koulusivistyskieleksi" in {
      val opiskeluoikeus = oppimääränOpiskeluoikeusOppiaineilla(äidinkieli("AI1", "H"))
      verifyKoulusivistyskieli(opiskeluoikeus, None)
    }

    "Valinnaista suoritusta ei valita koulusivistyskieleksi" in {
      val opiskeluoikeus = oppimääränOpiskeluoikeusOppiaineilla(
        äidinkieli("AI1", "6", pakollinen = false)
      )
      verifyKoulusivistyskieli(opiskeluoikeus, None)
    }
  }


  def verifyKoulusivistyskieli(opiskeluoikeus: LukionOpiskeluoikeus, expected: Option[List[Koodistokoodiviite]]) = {
    putOpiskeluoikeus(opiskeluoikeus) {
      verifyResponseStatusOk()
      val koulusivistyskielet = lastOpiskeluoikeusByHetu(defaultHenkilö).suoritukset.collect {
        case x: LukionOppimääränSuoritus => x.koulusivistyskieli
      }
      koulusivistyskielet.length should equal(1)
      koulusivistyskielet.head should equal(expected)
    }
  }

  def äidinkieli(kieli: String, arvosana: String, pakollinen: Boolean = true) = suoritus(lukionÄidinkieli(kieli).copy(pakollinen = pakollinen)).copy(arviointi = arviointi(arvosana))

  def oppimääränOpiskeluoikeusOppiaineilla(oppiaineet: LukionOppimääränOsasuoritus*) = lukionOpiskeluoikeus().copy(
    suoritukset = List(LukionOppimääränSuoritus(
      koulutusmoduuli = lukionOppimäärä,
      oppimäärä = nuortenOpetussuunnitelma,
      suorituskieli = ExampleData.suomenKieli,
      toimipiste = YleissivistavakoulutusExampleData.jyväskylänNormaalikoulu,
      osasuoritukset = Some(oppiaineet.toList),
      vahvistus = ExampleData.vahvistusPaikkakunnalla()
    ))
  )
}