import {
  contextualizeSubModel,
  ensureArrayKey,
  modelData,
  modelItems,
  modelSet,
  modelSetTitle, oneOfPrototypes,
  pushModel,
  wrapOptional
} from '../editor/EditorModel'
import {t} from '../i18n/i18n'

export const findKoodistoByDiaarinumero = (kurssiKoodistot, oppimaaranDiaarinumero) => {
  if (!kurssiKoodistot) return null

  return kurssiKoodistot.length > 1
    ? kurssiKoodistot.find(k => {
      const diaarinumeroaVastaavaKurssikoodisto = () => {
        switch (oppimaaranDiaarinumero) {
          case '60/011/2015':
          case '70/011/2015':
          case '56/011/2015': // Lukiokoulutukseen valmistava koulutus (valinnaisina suoritetut lukiokurssit)
            return 'lukionkurssit'
          case '33/011/2003':
            return 'lukionkurssitops2003nuoret'
          case '4/011/2004':
            return 'lukionkurssitops2004aikuiset'
        }
      }

      return k === diaarinumeroaVastaavaKurssikoodisto()
    })
    : kurssiKoodistot[0]
}

export const findDefaultKoodisto = kurssiKoodistot =>
  kurssiKoodistot.includes('lukionkurssit') ? 'lukionkurssit' : undefined

export const isIBKurssi = kurssi => kurssi.value.classes.includes('ibkurssinsuoritus')

export const lisääKurssi = (kurssi, model, showUusiKurssiAtom, kurssinSuoritusProto) => {
  if (kurssi) {
    const nimi = t(modelData(kurssi, 'tunniste.nimi'))
    const kurssiWithTitle = nimi ? modelSetTitle(kurssi, nimi) : kurssi
    const suoritusUudellaKurssilla = modelSet(kurssinSuoritusProto, kurssiWithTitle, 'koulutusmoduuli')
    ensureArrayKey(suoritusUudellaKurssilla)
    pushModel(suoritusUudellaKurssilla, model.context.changeBus)
  }
  showUusiKurssiAtom.set(false)
}

export const createKurssinSuoritusProto = (osasuoritukset, modelClass) => {
  osasuoritukset = wrapOptional(osasuoritukset)
  const newItemIndex = modelItems(osasuoritukset).length
  const oppiaineenSuoritusProto = contextualizeSubModel(osasuoritukset.arrayPrototype, osasuoritukset, newItemIndex)
  const options = oneOfPrototypes(oppiaineenSuoritusProto)
  const proto = modelClass && options.find(p => p.value.classes.includes(modelClass)) || options[0]
  return contextualizeSubModel(proto, osasuoritukset, newItemIndex)
}

export const osasuoritusCanBeAdded = (osasuoritukset) => {
  if (!osasuoritukset.value || !Array.isArray(osasuoritukset.value)) return true // Empty: can't be more than maxItems
  if (!osasuoritukset.maxItems || typeof osasuoritukset.maxItems !== 'number') return true // maxItems not specified
  return osasuoritukset.value.length < osasuoritukset.maxItems
}
