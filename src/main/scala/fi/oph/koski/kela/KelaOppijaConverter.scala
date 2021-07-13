package fi.oph.koski.kela

import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.log.Logging
import fi.oph.koski.schema

object KelaOppijaConverter extends Logging {

  def convertOppijaToKelaOppija(oppija: schema.Oppija): Either[HttpStatus, KelaOppija] = {
    convertHenkilo(oppija.henkilö).flatMap(henkilo => {
      val opiskeluoikeudet = oppija.opiskeluoikeudet.flatMap(kelaaKiinnostavatOpinnot).map(convertOpiskeluoikeus).toList
      if (opiskeluoikeudet.isEmpty) {
        Left(KoskiErrorCategory.notFound())
      } else {
        Right(KelaOppija(henkilo, opiskeluoikeudet))
      }
    })
  }

  private def convertHenkilo(oppija: schema.Henkilö): Either[HttpStatus, Henkilo] = oppija match {
    case henkilotiedot: schema.Henkilötiedot=>
      Right(Henkilo(
        oid = henkilotiedot.oid,
        hetu = henkilotiedot.hetu,
        syntymäaika = henkilotiedot match {
          case t: schema.TäydellisetHenkilötiedot => t.syntymäaika
          case _ => None
        },
        etunimet = henkilotiedot.etunimet,
        sukunimi = henkilotiedot.sukunimi,
        kutsumanimi = henkilotiedot.kutsumanimi
      ))
    case x => {
      logger.error("KelaOppijaConverter: Unreachable match arm, expected Henkilötiedot, got " + x.toString)
      Left(KoskiErrorCategory.internalError())
    }
  }

  private def kelaaKiinnostavatOpinnot(opiskeluoikeus: schema.Opiskeluoikeus) = opiskeluoikeus match {
    case _: schema.AmmatillinenOpiskeluoikeus |
         _: schema.YlioppilastutkinnonOpiskeluoikeus |
         _: schema.LukionOpiskeluoikeus |
         _: schema.LukioonValmistavanKoulutuksenOpiskeluoikeus |
         _: schema.DIAOpiskeluoikeus |
         _: schema.IBOpiskeluoikeus |
         _: schema.InternationalSchoolOpiskeluoikeus |
         _: schema.PerusopetuksenOpiskeluoikeus |
         _: schema.PerusopetukseenValmistavanOpetuksenOpiskeluoikeus |
         _: schema.PerusopetuksenLisäopetuksenOpiskeluoikeus |
         _: schema.AikuistenPerusopetuksenOpiskeluoikeus => Some(opiskeluoikeus)
    case o: schema.VapaanSivistystyönOpiskeluoikeus => {
      val suoritukset = o.suoritukset.collect {
        case s: schema.OppivelvollisilleSuunnattuVapaanSivistystyönKoulutuksenSuoritus => s
        case s: schema.OppivelvollisilleSuunnattuMaahanmuuttajienKotoutumiskoulutuksenSuoritus => s
        case s: schema.VapaanSivistystyönLukutaitokoulutuksenSuoritus => s
      }
      if (suoritukset.isEmpty) None else Some(opiskeluoikeus.withSuoritukset(suoritukset))
    }
    case _ => None
  }

  private def convertOpiskeluoikeus(opiskeluoikeus: schema.Opiskeluoikeus): KelaOpiskeluoikeus = {
    KelaOpiskeluoikeus(
      oid = opiskeluoikeus.oid,
      versionumero = opiskeluoikeus.versionumero,
      aikaleima = opiskeluoikeus match {
        case o: schema.KoskeenTallennettavaOpiskeluoikeus => o.aikaleima
        case _ => None
      },
      oppilaitos = opiskeluoikeus.oppilaitos.map(convertOppilaitos),
      koulutustoimija = opiskeluoikeus.koulutustoimija.map(convertKoulutustoimija),
      sisältyyOpiskeluoikeuteen = opiskeluoikeus.sisältyyOpiskeluoikeuteen.map(sisaltava =>
        Sisältäväopiskeluoikeus(
          oid = sisaltava.oid,
          oppilaitos = convertOppilaitos(sisaltava.oppilaitos)
        )
      ),
      arvioituPäättymispäivä = opiskeluoikeus.arvioituPäättymispäivä,
      ostettu = opiskeluoikeus match {
        case a: schema.AmmatillinenOpiskeluoikeus => Some(a.ostettu)
        case _ => None
      },
      tila = OpiskeluoikeudenTila(
        opiskeluoikeus.tila.opiskeluoikeusjaksot.map(jakso =>
          Opiskeluoikeusjakso(
            alku = jakso.alku,
            tila = convertKoodiviite(jakso.tila),
            opintojenRahoitus = jakso match {
              case a: schema.AmmatillinenOpiskeluoikeusjakso => a.opintojenRahoitus.map(convertKoodiviite)
              case _ => None
            }
          )
        ).sortWith((s, t) => s.alku.isBefore(t.alku))
      ),
      suoritukset = opiskeluoikeus.suoritukset.map(convertSuoritus),
      lisätiedot = opiskeluoikeus.lisätiedot.map(convertLisatiedot),
      tyyppi = convertKoodiviite(opiskeluoikeus.tyyppi),
      alkamispäivä = opiskeluoikeus.alkamispäivä,
      päättymispäivä = opiskeluoikeus.päättymispäivä,
      organisaatioHistoria = opiskeluoikeus match {
        case k: schema.KoskeenTallennettavaOpiskeluoikeus => k.organisaatiohistoria.map(_.map(historia =>
          OrganisaatioHistoria(
            muutospäivä = historia.muutospäivä,
            oppilaitos = historia.oppilaitos.map(convertOppilaitos),
            koulutustoimija = historia.koulutustoimija.map(convertKoulutustoimija)
          )
        ))
        case _ => None
      }
    )
  }

  private def convertLisatiedot(lisatiedot: schema.OpiskeluoikeudenLisätiedot): OpiskeluoikeudenLisätiedot = {
    OpiskeluoikeudenLisätiedot(
      majoitus = lisatiedot match {
        case x: schema.AmmatillisenOpiskeluoikeudenLisätiedot => x.majoitus
        case _ => None
      },
      sisäoppilaitosmainenMajoitus = lisatiedot match {
        case x: schema.SisäoppilaitosmainenMajoitus => x.sisäoppilaitosmainenMajoitus
        case _ => None
      },
      vaativanErityisenTuenYhteydessäJärjestettäväMajoitus = lisatiedot match {
        case x: schema.AmmatillisenOpiskeluoikeudenLisätiedot => x.vaativanErityisenTuenYhteydessäJärjestettäväMajoitus
        case _ => None
      },
      erityinenTuki = lisatiedot match {
        case x: schema.AmmatillisenOpiskeluoikeudenLisätiedot => x.erityinenTuki
        case _ => None
      },
      vaativanErityisenTuenErityinenTehtävä = lisatiedot match {
        case x: schema.AmmatillisenOpiskeluoikeudenLisätiedot => x.vaativanErityisenTuenErityinenTehtävä
        case _ => None
      },
      ulkomaanjaksot = lisatiedot match {
        case x: schema.Ulkomaajaksollinen => x.ulkomaanjaksot.map(_.map(convertUlkomaanjaksot))
        case n: schema.PerusopetuksenOpiskeluoikeudenLisätiedot => n.ulkomaanjaksot.map(_.map(convertPerusopetuksenUlkomaanjakso))
        case n: schema.PerusopetuksenLisäopetuksenOpiskeluoikeudenLisätiedot => n.ulkomaanjaksot.map(_.map(convertPerusopetuksenUlkomaanjakso))
        case a: schema.AikuistenPerusopetuksenOpiskeluoikeudenLisätiedot => a.ulkomaanjaksot.map(_.map(convertPerusopetuksenUlkomaanjakso))
        case _ => None
      },
      hojks = lisatiedot match {
        case x: schema.AmmatillisenOpiskeluoikeudenLisätiedot => x.hojks.map(h => Hojks(opetusryhmä = convertKoodiviite(h.opetusryhmä), alku = h.alku, loppu = h.loppu))
        case _ => None
      },
      osaAikaisuusjaksot = lisatiedot match {
        case x: schema.AmmatillisenOpiskeluoikeudenLisätiedot => x.osaAikaisuusjaksot
        case _ => None
      },
      opiskeluvalmiuksiaTukevatOpinnot = lisatiedot match {
        case x: schema.AmmatillisenOpiskeluoikeudenLisätiedot => x.opiskeluvalmiuksiaTukevatOpinnot
        case _ => None
      },
      vankilaopetuksessa = lisatiedot match {
        case x: schema.AmmatillisenOpiskeluoikeudenLisätiedot => x.vankilaopetuksessa
        case _ => None
      },
      ulkomainenVaihtoopiskelija = lisatiedot match {
        case x: schema.LukionOpiskeluoikeudenLisätiedot => Some(x.ulkomainenVaihtoopiskelija)
        case x: schema.LukioonValmistavanKoulutuksenOpiskeluoikeudenLisätiedot => Some(x.ulkomainenVaihtoopiskelija)
        case x: schema.DIAOpiskeluoikeudenLisätiedot => Some(x.ulkomainenVaihtoopiskelija)
        case _ => None
      },
      yksityisopiskelija = lisatiedot match {
        case x: schema.LukionOpiskeluoikeudenLisätiedot => Some(x.yksityisopiskelija)
        case _ => None
      },
      koulukoti = lisatiedot match {
        case x: schema.PerusopetuksenOpiskeluoikeudenLisätiedot => x.koulukoti
        case x: schema.PerusopetuksenLisäopetuksenOpiskeluoikeudenLisätiedot => x.koulukoti
        case _ => None
      },
      majoitusetu = lisatiedot match {
        case x: schema.PerusopetuksenOpiskeluoikeudenLisätiedot => x.majoitusetu
        case x: schema.PerusopetuksenLisäopetuksenOpiskeluoikeudenLisätiedot => x.majoitusetu
        case x: schema.AikuistenPerusopetuksenOpiskeluoikeudenLisätiedot => x.majoitusetu
        case _ => None
      },
      ulkomailla = lisatiedot match {
        case x: schema.PerusopetuksenOpiskeluoikeudenLisätiedot => x.ulkomailla
        case x: schema.PerusopetuksenLisäopetuksenOpiskeluoikeudenLisätiedot => x.ulkomailla
        case x: schema.AikuistenPerusopetuksenOpiskeluoikeudenLisätiedot => x.ulkomailla
        case _ => None
      },
      tehostetunTuenPäätös = lisatiedot match {
        case x: schema.PerusopetuksenOpiskeluoikeudenLisätiedot => x.tehostetunTuenPäätös
        case x: schema.PerusopetuksenLisäopetuksenOpiskeluoikeudenLisätiedot => x.tehostetunTuenPäätös
        case x: schema.AikuistenPerusopetuksenOpiskeluoikeudenLisätiedot => x.tehostetunTuenPäätös.map(jakso => schema.TehostetunTuenPäätös(alku = jakso.alku, loppu = jakso.loppu, tukimuodot = None))
        case _ => None
      },
      tehostetunTuenPäätökset = lisatiedot match {
        case x: schema.PerusopetuksenOpiskeluoikeudenLisätiedot => x.tehostetunTuenPäätökset
        case x: schema.PerusopetuksenLisäopetuksenOpiskeluoikeudenLisätiedot => x.tehostetunTuenPäätökset
        case x: schema.AikuistenPerusopetuksenOpiskeluoikeudenLisätiedot => x.tehostetunTuenPäätökset.map(_.map(jakso => schema.TehostetunTuenPäätös(alku = jakso.alku, loppu = jakso.loppu, tukimuodot = None)))
        case _ => None
      },
      joustavaPerusopetus = lisatiedot match {
        case x: schema.PerusopetuksenOpiskeluoikeudenLisätiedot => x.joustavaPerusopetus
        case x: schema.PerusopetuksenLisäopetuksenOpiskeluoikeudenLisätiedot => x.joustavaPerusopetus
        case _ => None
      },
      maksuttomuus = lisatiedot match {
        case x: schema.MaksuttomuusTieto => x.maksuttomuus
        case _ => None
      },
      oikeuttaMaksuttomuuteenPidennetty = lisatiedot match {
        case x: schema.MaksuttomuusTieto => x.oikeuttaMaksuttomuuteenPidennetty
        case _ => None
      }
    )
  }

  private def convertSuoritus(suoritus: schema.Suoritus): Suoritus = {
    Suoritus(
      koulutusmoduuli = convertSuorituksenKoulutusmoduuli(suoritus.koulutusmoduuli),
      suoritustapa = suoritus match {
        case _: schema.NuortenPerusopetuksenOppiaineenOppimääränSuoritus => None
        case _: schema.AikuistenPerusopetuksenOppiaineenOppimääränSuoritus => None
        case x: schema.AmmatillisenTutkinnonOsittainenTaiKokoSuoritus => Some(convertKoodiviite(x.suoritustapa))
        case x: schema.SuoritustavallinenPerusopetuksenSuoritus => Some(convertKoodiviite(x.suoritustapa))
        case x: schema.SuoritustapanaMahdollisestiErityinenTutkinto => x.suoritustapa.map(convertKoodiviite)
        case _ => None
      },
      toimipiste = suoritus match {
        case x: schema.Toimipisteellinen => Some(convertToimipiste(x.toimipiste))
        case x: schema.MahdollisestiToimipisteellinen => x.toimipiste.map(convertToimipiste)
        case _ => None
      },
      oppimäärä = suoritus match {
        case x: schema.Oppimäärällinen => Some(convertKoodiviite(x.oppimäärä))
        case _ => None
      },
      vahvistus = suoritus.vahvistus.map(v => Vahvistus(v.päivä)),
      osasuoritukset = suoritus.osasuoritukset.map(_.map(convertOsasuoritus)),
      tyyppi = convertKoodiviite(suoritus.tyyppi),
      tila = suoritus.tila.map(convertKoodiviite),
      osaamisala = suoritus match {
        case a: schema.Osaamisalallinen => a.osaamisala
        case _ => None
      },
      toinenOsaamisala = suoritus match {
        case a: schema.AmmatillisenTutkinnonOsittainenSuoritus => Some(a.toinenOsaamisala)
        case _ => None
      },
      alkamispäivä = suoritus.alkamispäivä,
      järjestämismuodot = suoritus match {
        case j: schema.Järjestämismuodollinen => j.järjestämismuodot.map(_.map(j =>
          Järjestämismuotojakso(
            alku = j.alku,
            loppu = j.loppu,
            järjestämismuoto = Järjestämismuoto(tunniste = convertKoodiviite(j.järjestämismuoto.tunniste))
          )
        ))
        case _ => None
      },
      osaamisenHankkimistavat = suoritus match {
        case o: schema.OsaamisenHankkimistavallinen => o.osaamisenHankkimistavat.map(_.map(jakso =>
          OsaamisenHankkimistapajakso(
            alku = jakso.alku,
            loppu = jakso.loppu,
            osaamisenHankkimistapa = convertOsaamisenHankkimisTapa(jakso.osaamisenHankkimistapa)
          )
        ))
        case _ => None
      },
      työssäoppimisjaksot = suoritus match {
        case t: schema.Työssäoppimisjaksollinen => convertTyössäoppimisjaksot(t.työssäoppimisjaksot)
        case _ => None
      },
      koulutussopimukset = suoritus match {
        case k: schema.Koulutussopimuksellinen => convertKoulutussopimusjaksot(k.koulutussopimukset)
        case _ => None
      },
      tutkintonimike = suoritus match {
        case t: schema.Tutkintonimikkeellinen => t.tutkintonimike.map(_.map(convertKoodiviite))
        case _ => None
      },
      toinenTutkintonimike = suoritus match {
        case a: schema.AmmatillisenTutkinnonOsittainenSuoritus => Some(a.toinenTutkintonimike)
        case _ => None
      },
      theoryOfKnowledge = suoritus match {
        case ib: schema.IBTutkinnonSuoritus => ib.theoryOfKnowledge.map(convertTheoryOfKowledge)
        case _ => None
      },
      extendedEssay = suoritus match {
        case ib: schema.IBTutkinnonSuoritus => ib.extendedEssay.map(convertExtendedEssay)
        case _ => None
      },
      creativityActionService = suoritus match {
        case ib: schema.IBTutkinnonSuoritus => ib.creativityActionService.map(convertCreativityActionService)
        case _ => None
      },
      jääLuokalle = suoritus match {
        case p: schema.PerusopetuksenVuosiluokanSuoritus => Some(p.jääLuokalle)
        case _ => None
      },
      pakollisetKokeetSuoritettu = suoritus match {
        case y: schema.YlioppilastutkinnonSuoritus => Some(y.pakollisetKokeetSuoritettu)
        case _ => None
      },
      kokonaislaajuus = suoritus match {
        case p: schema.PerusopetukseenValmistavanOpetuksenSuoritus => p.kokonaislaajuus
        case _ => None
      }
    )
  }

  private def convertOsaamisenHankkimisTapa(osaamisenHankkimistapa: schema.OsaamisenHankkimistapa) = {
    osaamisenHankkimistapa match {
      case schema.OsaamisenHankkimistapaIlmanLisätietoja(tunniste) =>
        OsaamisenHankkimistapaIlmanLisätietoja(tunniste = convertKoodiviite(tunniste))
      case schema.OppisopimuksellinenOsaamisenHankkimistapa(tunniste, oppisopimus) =>
        OppisopimuksellinenOsaamisenHankkimistapa(
          tunniste = convertKoodiviite(tunniste),
          oppisopimus = Oppisopimus(
            työnantaja = Yritys(nimi = oppisopimus.työnantaja.nimi, yTunnus = oppisopimus.työnantaja.yTunnus)
          )
        )
    }
  }

  private def convertSuorituksenKoulutusmoduuli(koulutusmoduuli: schema.Koulutusmoduuli) = {
    SuorituksenKoulutusmoduuli(
      tunniste = convertKoodiviite(koulutusmoduuli.tunniste),
      laajuus = koulutusmoduuli.getLaajuus,
      perusteenDiaarinumero = koulutusmoduuli match {
        case d: schema.Diaarinumerollinen => d.perusteenDiaarinumero
        case _ => None
      },
      perusteenNimi = koulutusmoduuli match {
        case a: schema.AmmatillinenTutkintoKoulutus => a.perusteenNimi
        case _ => None
      },
      koulutustyyppi = koulutusmoduuli match {
        case k: schema.Koulutus => k.koulutustyyppi.map(convertKoodiviite)
        case _ => None
      },
      pakollinen = koulutusmoduuli match {
        case x: schema.Valinnaisuus => Some(x.pakollinen)
        case _ => None
      },
      kuvaus = koulutusmoduuli match {
        case x: schema.Kuvaus => Some(x.kuvaus)
        case _ => None
      },
      kieli = koulutusmoduuli match {
        case k: schema.Kieliaine => Some(convertKoodiviite(k.kieli))
        case _ => None
      },
      diplomaType = koulutusmoduuli match {
        case d: schema.DiplomaLuokkaAste => Some(convertKoodiviite(d.diplomaType))
        case _ => None
      },
      oppimäärä = koulutusmoduuli match {
        case o: schema.Oppimäärä => Some(o.oppimäärä).map(convertKoodiviite)
        case _ => None
      }
    )
  }

  private def convertOsasuoritus(suoritus: schema.Suoritus): Osasuoritus = {
    Osasuoritus(
      koulutusmoduuli = convertOsasuorituksenKoulutusmoduuli(suoritus.koulutusmoduuli),
      liittyyTutkinnonOsaan = suoritus match {
        case t: schema.TutkinnonOsaaPienemmänKokonaisuudenSuoritus => Some(t.liittyyTutkinnonOsaan).map(convertKoodiviite)
        case _ => None
      },
      arviointi = suoritus.arviointi.map(_.map(a => a match {
        case vst: schema.VapaanSivistystyönMaahanmuuttajienKotoutumiskoulutuksenKieliopintojenArviointi =>
         OsasuorituksenArviointi(
           a.hyväksytty,
           a.arviointipäivä,
           kuullunYmmärtämisenTaitotaso = vst.kuullunYmmärtämisenTaitotaso.map(x => VSTKielenTaitotasonArviointi(convertKoodiviite(x.taso))),
           puhumisenTaitotaso = vst.puhumisenTaitotaso.map(x => VSTKielenTaitotasonArviointi(convertKoodiviite(x.taso))),
           luetunYmmärtämisenTaitotaso = vst.luetunYmmärtämisenTaitotaso.map(x => VSTKielenTaitotasonArviointi(convertKoodiviite(x.taso))),
           kirjoittamisenTaitotaso = vst.kirjoittamisenTaitotaso.map(x => VSTKielenTaitotasonArviointi(convertKoodiviite(x.taso)))
         )
        case _ =>
          OsasuorituksenArviointi(a.hyväksytty, a.arviointipäivä, None, None, None, None)
      })),
      toimipiste = suoritus match {
        case x: schema.Toimipisteellinen => Some(convertToimipiste(x.toimipiste))
        case x: schema.MahdollisestiToimipisteellinen => x.toimipiste.map(convertToimipiste)
        case _ => None
      },
      vahvistus = suoritus.vahvistus.map(v => Vahvistus(v.päivä)),
      osasuoritukset = suoritus.osasuoritukset.map(_.map(convertOsasuoritus)),
      tyyppi = convertKoodiviite(suoritus.tyyppi),
      tila = suoritus.tila.map(convertKoodiviite),
      tutkinto = suoritus match {
        case s: schema.TutkinnonOsanSuoritus => s.tutkinto.map(t =>
          Tutkinto(
            tunniste = convertKoodiviite(t.tunniste),
            perusteenDiaarinumero = t.perusteenDiaarinumero,
            perusteenNimi = t.perusteenNimi,
            koulutustyyppi = t.koulutustyyppi.map(convertKoodiviite)
          )
        )
        case _ => None
      },
      tutkinnonOsanRyhmä = suoritus match {
        case s: schema.TutkinnonOsanSuoritus => s.tutkinnonOsanRyhmä.map(convertKoodiviite)
        case _ => None
      },
      osaamisala = suoritus match {
        case a: schema.Osaamisalallinen => a.osaamisala
        case _ => None
      },
      alkamispäivä = suoritus.alkamispäivä,
      tunnustettu = suoritus match {
        case x: schema.MahdollisestiTunnustettu => x.tunnustettu.map(osaamisenTunnustaminen => OsaamisenTunnustaminen(
          osaaminen = osaamisenTunnustaminen.osaaminen.map(convertOsasuoritus),
          selite = osaamisenTunnustaminen.selite,
          rahoituksenPiirissä = osaamisenTunnustaminen.rahoituksenPiirissä
        ))
        case _ => None
      },
      toinenOsaamisala = suoritus match {
        case a: schema.AmmatillisenTutkinnonOsittainenSuoritus => Some(a.toinenOsaamisala)
        case _ => None
      },
      toinenTutkintonimike = suoritus match {
        case a: schema.AmmatillisenTutkinnonOsittainenSuoritus => Some(a.toinenTutkintonimike)
        case _ => None
      },
      näyttö = suoritus match {
        case s: schema.TutkinnonOsanSuoritus => s.näyttö.map(convertNäyttö)
        case _ => None
      },
      vastaavuusTodistuksenTiedot = suoritus match {
        case dia: schema.DIAOppiaineenTutkintovaiheenSuoritus => dia.vastaavuustodistuksenTiedot.map(v => VastaavuusTodistuksenTiedot(v.lukioOpintojenLaajuus))
        case _ => None
      },
      suoritettuLukiodiplomina = suoritus match {
        case l: schema.LukionKurssinSuoritus2015 => l.suoritettuLukiodiplomina
        case _ => None
      },
      suoritettuSuullisenaKielikokeena = suoritus match {
        case l: schema.LukionKurssinSuoritus2015 => l.suoritettuSuullisenaKielikokeena
        case _ => None
      },
      luokkaAste = suoritus  match {
        case n: schema.NuortenPerusopetuksenOppiaineenSuoritusValmistavassaOpetuksessa => n.luokkaAste.map(convertKoodiviite)
        case _ => None
      },
      tutkintokerta = suoritus match {
        case s: schema.YlioppilastutkinnonKokeenSuoritus => Some(
          YlioppilastutkinnonTutkintokerta(
            koodiarvo = s.tutkintokerta.koodiarvo,
            vuosi = s.tutkintokerta.vuosi,
            vuodenaika = s.tutkintokerta.vuodenaika
          ))
        case _ => None
      },
      yksilöllistettyOppimäärä = suoritus match {
        case s: schema.Yksilöllistettävä => Some(s.yksilöllistettyOppimäärä)
        case _ => None
      },
      lisätiedot = suoritus match {
        case s: schema.TutkinnonOsanSuoritus => {
          s.lisätiedot.flatMap(lisätiedot => {
            val mukautetut = lisätiedot.filter(_.tunniste.koodiarvo == "mukautettu")
            if (mukautetut.isEmpty) {
              None
            } else {
              Some(mukautetut.map(x => AmmatillisenTutkinnonOsanLisätieto(convertKoodiviite(x.tunniste), x.kuvaus)))
            }
          })
        }
        case _ => None
      }
    )
  }

  private def convertOsasuorituksenKoulutusmoduuli(koulutusmoduuli: schema.Koulutusmoduuli) = {
    OsasuorituksenKoulutusmoduuli(
      tunniste = convertKoodiviite(koulutusmoduuli.tunniste),
      laajuus = koulutusmoduuli.getLaajuus,
      perusteenNimi = koulutusmoduuli match {
        case a: schema.AmmatillinenTutkintoKoulutus => a.perusteenNimi
        case _ => None
      },
      pakollinen = koulutusmoduuli match {
        case x: schema.Valinnaisuus => Some(x.pakollinen)
        case _ => None
      },
      kuvaus = koulutusmoduuli match {
        case x: schema.Kuvaus => Some(x.kuvaus)
        case _ => None
      },
      kieli = koulutusmoduuli match {
        case k: schema.Kieliaine => Some(convertKoodiviite(k.kieli))
        case _ => None
      },
      osaAlue = koulutusmoduuli match {
        case d: schema.DIAOsaAlueOppiaine => Some(convertKoodiviite(d.osaAlue))
        case _ => None
      },
      taso = koulutusmoduuli match {
        case t: schema.IBTaso => t.taso.map(convertKoodiviite)
        case _ => None
      },
      ryhmä = koulutusmoduuli match {
        case ib: schema.IBAineRyhmäOppiaine => Some(convertKoodiviite(ib.ryhmä))
        case _ => None
      },
      kurssinTyyppi = koulutusmoduuli match {
        case l: schema.LukionKurssi2015 => Some(convertKoodiviite(l.kurssinTyyppi))
        case _ => None
      },
      oppimäärä = koulutusmoduuli match {
        case o: schema.Oppimäärä => Some(convertKoodiviite(o.oppimäärä))
        case _ => None
      }
    )
  }

  private def convertKoodiviite(koodiviite :schema.KoodiViite): Koodistokoodiviite = {
    koodiviite match {
      case k: schema.Koodistokoodiviite => Koodistokoodiviite(
        koodiarvo = k.koodiarvo,
        nimi = k.nimi,
        lyhytNimi = k.lyhytNimi,
        koodistoUri = Some(k.koodistoUri),
        koodistoVersio = k.koodistoVersio
      )
      case p: schema.PaikallinenKoodiviite => Koodistokoodiviite(
        koodiarvo = p.koodiarvo,
        nimi = p.getNimi,
        lyhytNimi = None,
        koodistoUri = None,
        koodistoVersio = None
      )
    }
  }

  private def convertOppilaitos(oppilaitos: schema.Oppilaitos): Oppilaitos = {
    Oppilaitos(
      oid = oppilaitos.oid,
      oppilaitosnumero = oppilaitos.oppilaitosnumero.map(convertKoodiviite),
      nimi = oppilaitos.nimi,
      kotipaikka = oppilaitos.kotipaikka.map(convertKoodiviite)
    )
  }

  private def convertKoulutustoimija(koulutustoimija: schema.Koulutustoimija): Koulutustoimija = {
    Koulutustoimija(
      oid = koulutustoimija.oid,
      nimi = koulutustoimija.nimi,
      yTunnus = koulutustoimija.yTunnus,
      kotipaikka = koulutustoimija.kotipaikka.map(convertKoodiviite)
    )
  }

  private def convertToimipiste(organisaatio: schema.OrganisaatioWithOid) = {
    Toimipiste(
      oid = organisaatio.oid,
      nimi = organisaatio.nimi,
      kotipaikka = organisaatio.kotipaikka.map(convertKoodiviite)
    )
  }


  private def convertUlkomaanjaksot(jakso: schema.Ulkomaanjakso) = {
    Ulkomaanjakso(
      alku = jakso.alku,
      loppu = jakso.loppu,
      maa = Some(jakso.maa).map(convertKoodiviite),
      kuvaus = Some(jakso.kuvaus)
    )
  }

  private def convertPerusopetuksenUlkomaanjakso(jakso: schema.Aikajakso) = {
    Ulkomaanjakso(
      alku = jakso.alku,
      loppu = jakso.loppu,
      maa = None,
      kuvaus = None
    )
  }

  private def convertTyössäoppimisjaksot(jaksot: Option[List[schema.Työssäoppimisjakso]]) = jaksot.map(_.map(jakso =>
    Työssäoppimisjakso(
      alku = jakso.alku,
      loppu = jakso.loppu,
      työssäoppimispaikka = jakso.työssäoppimispaikka,
      paikkakunta = convertKoodiviite(jakso.paikkakunta),
      maa = convertKoodiviite(jakso.maa),
      laajuus = jakso.laajuus
    )
  ))

  private def convertKoulutussopimusjaksot(jaksot: Option[List[schema.Koulutussopimusjakso]]) = jaksot.map(_.map(jakso =>
    Koulutussopimusjakso(
      alku = jakso.alku,
      loppu = jakso.loppu,
      työssäoppimispaikka = jakso.työssäoppimispaikka,
      paikkakunta = convertKoodiviite(jakso.paikkakunta),
      maa = convertKoodiviite(jakso.maa)
    )
  ))

  private def convertTheoryOfKowledge(suoritus: schema.IBTheoryOfKnowledgeSuoritus) = {
    IBTheoryOfKnowledgeSuoritus(
      koulutusmoduuli = IBTheoryOfKnowledgeSuoritusKoulutusmoduuli(
        tunniste = convertKoodiviite(suoritus.koulutusmoduuli.tunniste),
        pakollinen = suoritus.koulutusmoduuli.pakollinen
      ),
      tila = suoritus.tila.map(convertKoodiviite),
      arviointi = suoritus.arviointi.map(_.map(a => Arviointi(hyväksytty = a.hyväksytty, päivä = a.päivä))),
      osasuoritukset = suoritus.osasuoritukset.map(_.map(convertOsasuoritus)),
      tyyppi = convertKoodiviite(suoritus.tyyppi)
    )
  }

  private def convertExtendedEssay(suoritus: schema.IBExtendedEssaySuoritus) = {
    IBExtendedEssaySuoritus(
      koulutusmoduuli = IBExtendedEssaySuoritusKoulutusmoduuli(
        tunniste = convertKoodiviite(suoritus.koulutusmoduuli.tunniste),
        pakollinen = suoritus.koulutusmoduuli.pakollinen
      ),
      tila = suoritus.tila.map(convertKoodiviite),
      arviointi = suoritus.arviointi.map(_.map(a => Arviointi(hyväksytty = a.hyväksytty, päivä = a.päivä))),
      tyyppi = convertKoodiviite(suoritus.tyyppi)
    )
  }

  private def convertCreativityActionService(suoritus: schema.IBCASSuoritus) = {
    IBCASSuoritus(
      koulutusmoduuli = convertSuorituksenKoulutusmoduuli(suoritus.koulutusmoduuli),
      arviointi = suoritus.arviointi.map(_.map(a => Arviointi(a.hyväksytty, a.päivä))),
      tyyppi = convertKoodiviite(suoritus.tyyppi),
      tila = suoritus.tila.map(convertKoodiviite)
    )
  }

  private def convertNäyttö(n: schema.Näyttö) = {
    Näyttö(
      suorituspaikka = n.suorituspaikka.map(suorituspaikka => NäytönSuorituspaikka(convertKoodiviite(suorituspaikka.tunniste), suorituspaikka.kuvaus)),
      suoritusaika = n.suoritusaika,
      työssäoppimisenYhteydessä =  n.työssäoppimisenYhteydessä,
      arviointi = n.arviointi.map(a => NäytönArviointi(a.hyväksytty))
    )
  }
}
