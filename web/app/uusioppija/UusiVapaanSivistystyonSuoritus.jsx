import React from 'baret'
import Bacon from 'baconjs'
import Atom from 'bacon.atom'
import Peruste from './Peruste'
import Suoritustyyppi from './Suoritustyyppi'
import {koodistoValues} from './koodisto'
import {ift} from '../util/util'

export default ({suoritusAtom, oppilaitosAtom, suorituskieliAtom}) => {
  const suoritustyyppiAtom = Atom()
  const perusteAtom = Atom()

  const suoritustyypitP =  koodistoValues('suorituksentyyppi/vstoppivelvollisillesuunnattukoulutus,vstmaahanmuuttajienkotoutumiskoulutus,vstlukutaitokoulutus')

  Bacon.combineWith(oppilaitosAtom, suoritustyyppiAtom, perusteAtom, suorituskieliAtom, makeSuoritus)
    .onValue(suoritus => suoritusAtom.set(suoritus))

  return (
    <div>
      <Suoritustyyppi suoritustyyppiAtom={suoritustyyppiAtom} suoritustyypitP={suoritustyypitP} title="Suoritustyyppi"/>
      {ift(suoritustyyppiAtom,
        <Peruste {...{suoritusTyyppiP: suoritustyyppiAtom, perusteAtom}} />
      )}
    </div>
  )
}

const makeSuoritus = (oppilaitos, suoritustyyppi, peruste, suorituskieli) => {
  switch (suoritustyyppi?.koodiarvo) {
    case 'vstoppivelvollisillesuunnattukoulutus':
      return (
        {
          suorituskieli : suorituskieli,
          koulutusmoduuli: {
            tunniste: {
              koodiarvo: '999909',
              koodistoUri: 'koulutus'
            },
            perusteenDiaarinumero: peruste
          },
          toimipiste: oppilaitos,
          tyyppi: suoritustyyppi
        }
      )
    case 'vstmaahanmuuttajienkotoutumiskoulutus':
      return (
        {
          suorituskieli : suorituskieli,
          koulutusmoduuli: {
            tunniste: {
              koodiarvo: '999910',
              koodistoUri: 'koulutus'
            },
            perusteenDiaarinumero: peruste
          },
          toimipiste: oppilaitos,
          tyyppi: suoritustyyppi
        }
      )
    case 'vstlukutaitokoulutus':
      return (
        {
          suorituskieli : suorituskieli,
          koulutusmoduuli: {
            tunniste: {
              koodiarvo: '999911',
              koodistoUri: 'koulutus'
            },
            perusteenDiaarinumero: peruste
          },
          toimipiste: oppilaitos,
          tyyppi: suoritustyyppi
        }
      )
    default:
      return undefined
  }
}
