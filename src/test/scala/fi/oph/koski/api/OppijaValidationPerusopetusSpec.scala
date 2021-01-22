package fi.oph.koski.api

import java.time.LocalDate

import fi.oph.koski.documentation.OsaAikainenErityisopetusExampleData._
import fi.oph.koski.documentation.PerusopetusExampleData.{suoritus, _}
import fi.oph.koski.henkilo.MockOppijat
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.schema._
import mojave._

// Perusopetuksen validointi perustuu tässä testattua diaarinumeroa lukuunottamatta domain-luokista generoituun JSON-schemaan.
// Schemavalidoinnille on tehty kattavat testit ammatillisten opiskeluoikeuksien osalle. Yleissivistävän koulutuksen validoinnissa luotamme
// toistaiseksi siihen, että schema itsessään on katselmoitu, ja että geneerinen mekanismi toimii.

class OppijaValidationPerusopetusSpec extends TutkinnonPerusteetTest[PerusopetuksenOpiskeluoikeus] with LocalJettyHttpSpecification with OpiskeluoikeusTestMethodsPerusopetus {
  def opiskeluoikeusWithPerusteenDiaarinumero(diaari: Option[String]) = defaultOpiskeluoikeus.copy(suoritukset = List(
    päättötodistusSuoritus.copy(koulutusmoduuli = päättötodistusSuoritus.koulutusmoduuli.copy(perusteenDiaarinumero = diaari))
  ))
  def eperusteistaLöytymätönValidiDiaarinumero: String = "1/011/2004"

  "Suoritusten tila" - {
    "Vahvistettu päättötodistus -> HTTP 200" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus) {
        verifyResponseStatusOk()
      }
    }

    "Vahvistettu päättötodistus ilman yhtään oppiainetta -> HTTP 400" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(Nil))))) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.tila.oppiaineetPuuttuvat("Suorituksella ei ole osasuorituksena yhtään oppiainetta, vaikka sillä on vahvistus"))
      }
    }

    "Vahvistamaton päättötodistus ilman yhtään oppiainetta -> HTTP 200" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(vahvistus = None, osasuoritukset = Some(Nil))))) {
        verifyResponseStatusOk()
      }
    }

    "Vahvistettu päättötodistus keskeneräisellä oppiaineella -> HTTP 400" in {
      val oppiaineidenArvioinnit = traversal[PerusopetuksenOpiskeluoikeus]
        .field[List[PerusopetuksenPäätasonSuoritus]]("suoritukset")
        .items
        .field[Option[List[Suoritus]]]("osasuoritukset")
        .items.items
        .field[Option[List[Arviointi]]]("arviointi")

      putOpiskeluoikeus(oppiaineidenArvioinnit.set(defaultOpiskeluoikeus)(None)) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.tila.keskeneräinenOsasuoritus("Valmiiksi merkityllä suorituksella koulutus/201101 on keskeneräinen osasuoritus koskioppiaineetyleissivistava/AI"))
      }
    }

    "Kaksi samaa oppiainetta" - {
      "Identtisillä tiedoilla -> HTTP 400" in {
        putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(
          suoritus(äidinkieli("AI1")).copy(arviointi = arviointi(9)),
          suoritus(äidinkieli("AI1")).copy(arviointi = arviointi(9))
        )))))) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.duplikaattiOsasuoritus("Osasuoritus (koskioppiaineetyleissivistava/AI,oppiaineaidinkielijakirjallisuus/AI1) esiintyy useammin kuin kerran ryhmässä pakolliset"))
        }
      }
      "Eri kielivalinnalla -> HTTP 200" in {
        putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(
          suoritus(äidinkieli("AI1")).copy(arviointi = arviointi(9)),
          suoritus(äidinkieli("AI2")).copy(arviointi = arviointi(9))
        )))))) {
          verifyResponseStatusOk()
        }
      }
      "Valinnaisissa oppiaineissa -> HTTP 200" in {
        putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(
          suoritus(äidinkieli("AI1").copy(pakollinen = false)).copy(arviointi = arviointi(9)),
          suoritus(äidinkieli("AI1").copy(pakollinen = false)).copy(arviointi = arviointi(9))
        )))))) {
          verifyResponseStatusOk()
        }
      }
    }

    "Opiskeluoikeudella ei saa olla sama alkamispäivä kahdella vuosiluokalla" - {
      "Samassa päivityksessä" in {
        putOpiskeluoikeus(defaultOpiskeluoikeus.copy(
          suoritukset = List(
            yhdeksännenLuokanSuoritus.copy(alkamispäivä = Some(LocalDate.of(2006, 1, 1))),
            kahdeksannenLuokanSuoritus.copy(alkamispäivä = Some(LocalDate.of(2006, 1, 1))),
          )
        ), MockOppijat.eero) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.epäsopiviaSuorituksia("Vuosiluokilla (perusopetuksenluokkaaste/9, perusopetuksenluokkaaste/8) on sama alkamispäivä"))
        }
      }
      "Peräkkäisissä päivityksissä" in {
        putOpiskeluoikeus(defaultOpiskeluoikeus.copy(
          suoritukset = List(
            yhdeksännenLuokanSuoritus.copy(alkamispäivä = Some(LocalDate.of(2006, 1, 1))),
          )
        ), MockOppijat.eero) {
          verifyResponseStatusOk()
        }

        val edellinenOpiskeluoikeus = lastOpiskeluoikeus(MockOppijat.eero.oid)
        putOpiskeluoikeus(defaultOpiskeluoikeus.copy(
          suoritukset = List(
            kahdeksannenLuokanSuoritus.copy(alkamispäivä = Some(LocalDate.of(2006, 1, 1))),
          )
        ).withOidAndVersion(edellinenOpiskeluoikeus.oid, edellinenOpiskeluoikeus.versionumero), MockOppijat.eero) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.epäsopiviaSuorituksia("Vuosiluokilla (perusopetuksenluokkaaste/9, perusopetuksenluokkaaste/8) on sama alkamispäivä"))
        }
      }
    }
  }

  "Arvosanat" - {

    "Päättötodistuksella ei ole vahvistusta" - {
      val vahvistamatonPäättötodistus = päättötodistusSuoritus.copy(vahvistus = None)

      "S" - {
        val valinnainenS = suoritus(kieli("B1", "SV").copy(pakollinen = false, laajuus = vuosiviikkotuntia(2))).copy(arviointi = hyväksytty)
        val pakollinenS = äidinkielenSuoritus.copy(arviointi = hyväksytty)

        "Sallitaan pakollisten oppiaineiden suorituksilta" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(vahvistamatonPäättötodistus.copy(osasuoritukset = Some(List(pakollinenS)))))) {
            verifyResponseStatusOk()
          }
        }

        "Sallitaan valinnaisten oppiaineiden suorituksilta joiden laajuus on 2 vuosiviikkotuntia tai yli" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(vahvistamatonPäättötodistus.copy(osasuoritukset = Some(List(valinnainenS)))))) {
            verifyResponseStatusOk()
          }
        }
      }

      "O" - {
        val valinnainenO = suoritus(kieli("B1", "SV").copy(pakollinen = false, laajuus = vuosiviikkotuntia(2))).copy(arviointi = osallistunut)
        val pakollinenO = äidinkielenSuoritus.copy(arviointi = osallistunut)

        "Sallittu pakollisten oppiaineiden suorituksilta" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(vahvistamatonPäättötodistus.copy(osasuoritukset = Some(List(pakollinenO)))))) {
            verifyResponseStatusOk()
          }
        }

        "Sallittu valinnaisten oppiaineiden suorituksilta joiden laajuus on 2 vuosiviikkotuntia tai yli" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(vahvistamatonPäättötodistus.copy(osasuoritukset = Some(List(valinnainenO)))))) {
            verifyResponseStatusOk()
          }
        }
      }
    }

    "Päättötodistuksella on vahvistus" - {
      "S" - {
        val valinnainenS = suoritus(äidinkieli("AI1").copy(pakollinen = false, laajuus = vuosiviikkotuntia(2))).copy(arviointi = hyväksytty)
        val pakollinenS = äidinkielenSuoritus.copy(arviointi = hyväksytty)

        "Kielletty pakollisten oppiaineiden suorituksilta" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(pakollinenS)))))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.arviointi.sallittuVainValinnaiselle("Arviointi S on sallittu vain jos oppimäärä on yksilöllistetty tai valinnaisille oppiaineille joiden laajuus on alle kaksi vuosiviikkotuntia"))
          }
        }

        "Sallittu yksilöllistetyille pakollisille" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(pakollinenS.copy(yksilöllistettyOppimäärä = true))))))) {
            verifyResponseStatusOk()
          }
        }

        "Sallittu väliaikaisesti valinnaisten oppiaineiden suorituksille joiden laajuus on 2 vuosiviikkotuntia tai yli" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(valinnainenS)))))) {
            verifyResponseStatusOk()
          }
        }

        "Sallittu valinnaisten kielioppiaineiden suorituksilta joiden laajuus on 2 vuosiviikkotuntia tai yli" in {
          val valinnainenKieliS = suoritus(kieli("B1", "SV").copy(pakollinen = false, laajuus = vuosiviikkotuntia(2))).copy(arviointi = hyväksytty)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(valinnainenKieliS)))))) {
            verifyResponseStatusOk()
          }
        }

        "Sallittu valinnaisille oppiaineiden suorituksille joiden laajuus on alle 2" in {
          val valinnainenLaajuusAlle2 = suoritus(oppiaine("HI").copy(pakollinen = false, laajuus = vuosiviikkotuntia(1.9))).copy(arviointi = hyväksytty)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(valinnainenLaajuusAlle2)))))) {
            verifyResponseStatusOk()
          }
        }

        "Sallittu paikallisille oppiaineille joiden laajuus 2 vuosiviikkotuntia tai yli" in {
          val paikallinenLaajuus2 = suoritus(paikallinenOppiaine("HI", "Historia", "Opiskellaan historiaa", vuosiviikkotuntia(2))).copy(arviointi = hyväksytty)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(paikallinenLaajuus2)))))) {
            verifyResponseStatusOk()
          }
        }

        "Sallittu paikallisille oppiaineille joiden laajuus on alle 2 vuosiviikkotuntia" in {
          val paikallinenLaajuus2 = suoritus(paikallinenOppiaine("HI", "Historia", "Opiskellaan historiaa", vuosiviikkotuntia(1.9))).copy(arviointi = hyväksytty)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(paikallinenLaajuus2)))))) {
            verifyResponseStatusOk()
          }
        }
      }

      "O" - {
        val valinnainenO = suoritus(äidinkieli("AI1").copy(pakollinen = false, laajuus = vuosiviikkotuntia(2))).copy(arviointi = osallistunut)
        val pakollinenO = äidinkielenSuoritus.copy(arviointi = osallistunut)

        "Kielletty pakollisten oppiaineiden suorituksilta" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(pakollinenO)))))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.arviointi.sallittuVainValinnaiselle("Arviointi O on sallittu vain jos oppimäärä on yksilöllistetty tai valinnaisille oppiaineille joiden laajuus on alle kaksi vuosiviikkotuntia"))
          }
        }

        "Sallittu yksilöllistetyille pakollisille" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(pakollinenO.copy(yksilöllistettyOppimäärä = true))))))) {
            verifyResponseStatusOk()
          }
        }

        "Kielletty valinnaisten oppiaineiden suorituksilta joiden laajuus on 2 vuosiviikkotuntia tai yli" in {
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(valinnainenO)))))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.arviointi.sallittuVainValinnaiselle("Arviointi O on sallittu vain jos oppimäärä on yksilöllistetty tai valinnaisille oppiaineille joiden laajuus on alle kaksi vuosiviikkotuntia"))
          }
        }

        "Kielletty valinnaisten kielioppiaineiden suorituksilta joiden laajuus on 2 vuosiviikkotuntia tai yli" in {
          val valinnainenKieliO = suoritus(kieli("B1", "SV").copy(pakollinen = false, laajuus = vuosiviikkotuntia(2))).copy(arviointi = osallistunut)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(valinnainenKieliO)))))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.arviointi.sallittuVainValinnaiselle("Arviointi O on sallittu vain jos oppimäärä on yksilöllistetty tai valinnaisille oppiaineille joiden laajuus on alle kaksi vuosiviikkotuntia"))
          }
        }

        "Sallittu valinnaisille oppiaineiden suorituksille joiden laajuus on alle 2" in {
          val valinnainenLaajuusAlle2 = suoritus(äidinkieli("AI1").copy(pakollinen = false, laajuus = vuosiviikkotuntia(1.9))).copy(arviointi = osallistunut)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(valinnainenLaajuusAlle2)))))) {
            verifyResponseStatusOk()
          }
        }

        "Sallittu paikallisille oppiaineille joiden laajuus on alle 2 vuosiviikkotuntia" in {
          val paikallinenLaajuusAlle2 = suoritus(paikallinenOppiaine("HI", "Historia", "Opiskellaan historiaa", vuosiviikkotuntia(1.9))).copy(arviointi = osallistunut)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(paikallinenLaajuusAlle2)))))) {
            verifyResponseStatusOk()
          }
        }

        "Kielletty paikallisten oppiaineden suorituksilta joiden laajuus on 2 vuosiviikkotuntia tai yli" in {
          val paikallinenLaajuus2 = suoritus(paikallinenOppiaine("HI", "Historia", "Opiskellaan historiaa", vuosiviikkotuntia(2))).copy(arviointi = osallistunut)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(paikallinenLaajuus2)))))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.arviointi.sallittuVainValinnaiselle("Arviointi O on sallittu vain jos oppimäärä on yksilöllistetty tai valinnaisille oppiaineille joiden laajuus on alle kaksi vuosiviikkotuntia"))
          }
        }
      }

      "4-10" - {
        "Kielletty valinnaiselle valtakunnalliselle oppiaineelle, jonka laajuus on alle kaksi vuosiviikkotuntia" in {
          val valinnainenLaajuusAlle2 = suoritus(
            oppiaine("BI").copy(pakollinen = false, laajuus = vuosiviikkotuntia(1.9))
          ).copy(arviointi = arviointi(9))

          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(valinnainenLaajuusAlle2)))))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.arviointi.eiSallittuSuppealleValinnaiselle(

            ))
          }
        }

        "Sallittu valinnaiselle valtakunnalliselle oppiaineelle, jonka laajuus on kaksi vuosiviikkotuntia" in {
          val valinnainenLaajuus2 = suoritus(
            oppiaine("BI").copy(pakollinen = false, laajuus = vuosiviikkotuntia(2))
          ).copy(arviointi = arviointi(9))

          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(valinnainenLaajuus2)))))) {
            verifyResponseStatusOk()
          }
        }
      }

      "Opinto-ohjaus (OP) oppiaineena" - {
        "Sallitaan aina arvosana S" in {
          val opinto_ohjaus_S = suoritus(oppiaine("OP")).copy(arviointi = hyväksytty)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(opinto_ohjaus_S)))))) {
            verifyResponseStatusOk()
          }
        }
        "Sallitaan aina arvosana O" in {
          val opinto_ohjaus_O = suoritus(oppiaine("OP")).copy(arviointi = osallistunut)
          putOpiskeluoikeus(defaultOpiskeluoikeus.copy(suoritukset = List(päättötodistusSuoritus.copy(osasuoritukset = Some(List(opinto_ohjaus_O)))))) {
            verifyResponseStatusOk()
          }
        }
      }
    }
  }

  "Osa-aikainen erityisopetus" - {
    "Opiskeluoikeudella on erityisen tuen päätös muusta kuin osa-aikaisesta erityisopetuksesta, muttei tietoa suorituksessa -> HTTP 200" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(
        lisätiedot = perusopetuksenOpiskeluoikeudenLisätiedotJoissaErityisenTuenPäätösIlmanOsaAikaistaErityisopetusta,
        suoritukset = List(vuosiluokkasuoritus.copy(osaAikainenErityisopetus = false))
      )) {
        verifyResponseStatusOk()
      }
    }

    "Opiskeluoikeudella on tehostetun tuen päätös muusta kuin osa-aikaisesta erityisopetuksesta, muttei tietoa suorituksessa -> HTTP 200" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(
        lisätiedot = perusopetuksenOpiskeluoikeudenLisätiedotJoissaTehostetunTuenPäätösIlmanOsaAikaistaErityisopetusta,
        suoritukset = List(vuosiluokkasuoritus.copy(osaAikainenErityisopetus = false))
      )) {
        verifyResponseStatusOk()
      }
    }

    "Opiskeluoikeudella on erityisen tuen päätös osa-aikaisesta erityisopetuksesta ja tieto suorituksessa -> HTTP 200" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(
        lisätiedot = perusopetuksenOpiskeluoikeudenLisätiedotJoissaOsaAikainenErityisopetusErityisenTuenPäätöksessä,
        suoritukset = List(
          yhdeksännenLuokanSuoritus.copy(osaAikainenErityisopetus = false),
          kahdeksannenLuokanSuoritus.copy(osaAikainenErityisopetus = false),
          seitsemännenLuokanSuoritus.copy(osaAikainenErityisopetus = true)
        )
      )) {
        verifyResponseStatusOk()
      }
    }

    "Opiskeluoikeudella on erityisen tuen päätös osa-aikaisesta erityisopetuksesta, mutta suorituksissa ei yhtään vuosiluokan suoritusta -> HTTP 400" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(
        lisätiedot = perusopetuksenOpiskeluoikeudenLisätiedotJoissaOsaAikainenErityisopetusErityisenTuenPäätöksessä,
        suoritukset = List(päättötodistusSuoritus)
      )) {
        verifyResponseStatus(400,
          KoskiErrorCategory.badRequest.validation.osaAikainenErityisopetus.kirjausPuuttuuSuorituksesta(
            "Jos osa-aikaisesta erityisopetuksesta on päätös opiskeluoikeuden lisätiedoissa, se pitää kirjata myös vuosiluokan suoritukseen")
        )
      }
    }

    "Opiskeluoikeudella on erityisen tuen päätös osa-aikaisesta erityisopetuksesta, muttei tietoa suorituksessa -> HTTP 400" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(
        lisätiedot = perusopetuksenOpiskeluoikeudenLisätiedotJoissaOsaAikainenErityisopetusErityisenTuenPäätöksessä,
        suoritukset = List(yhdeksännenLuokanSuoritus.copy(osaAikainenErityisopetus = false), kahdeksannenLuokanSuoritus.copy(osaAikainenErityisopetus = false))
      )) {
        verifyResponseStatus(400,
          KoskiErrorCategory.badRequest.validation.osaAikainenErityisopetus.kirjausPuuttuuSuorituksesta(
            "Jos osa-aikaisesta erityisopetuksesta on päätös opiskeluoikeuden lisätiedoissa, se pitää kirjata myös vuosiluokan suoritukseen")
        )
      }
    }

    "Opiskeluoikeudella on tehostetun tuen päätös osa-aikaisesta erityisopetuksesta, muttei tietoa suorituksessa -> HTTP 400" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(lisätiedot = perusopetuksenOpiskeluoikeudenLisätiedotJoissaOsaAikainenErityisopetusTehostetunTuenPäätöksessä,
        suoritukset = List(yhdeksännenLuokanSuoritus.copy(osaAikainenErityisopetus = false), kahdeksannenLuokanSuoritus.copy(osaAikainenErityisopetus = false))
      )) {
        verifyResponseStatus(400,
          KoskiErrorCategory.badRequest.validation.osaAikainenErityisopetus.kirjausPuuttuuSuorituksesta(
            "Jos osa-aikaisesta erityisopetuksesta on päätös opiskeluoikeuden lisätiedoissa, se pitää kirjata myös vuosiluokan suoritukseen")
        )
      }
    }
  }
}
