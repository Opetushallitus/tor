import React from 'baret'
import {Editor} from '../editor/Editor'
import {wrapOptional, modelEmpty, modelProperty} from '../editor/EditorModel'
import * as L from 'partial.lenses'
import {lensedModel, modelData, modelLookup, modelSetValue, oneOfPrototypes, pushModel, contextualizeSubModel, modelItems, resetOptionalModel} from '../editor/EditorModel'
import {sortGrades} from '../util/sorting'
import {fetchAlternativesBasedOnPrototypes} from '../editor/EnumEditor'
import {fixArviointi} from './Suoritus'
import {DateEditor} from '../editor/DateEditor'
import {arviointiListaaKäyttäväKurssi} from '../kurssi/kurssi.js'

let global_run = 0

export const ArvosanaEditor = ({model, notFoundText}) => {
  if (!model.context.edit) {
    const arvosanaModel = resolveArvosanaModel(model)
    return arvosanaModel
      ? <Editor model={ arvosanaModel }/>
      : (notFoundText ? <span>{notFoundText}</span> : null)
  }

  if (!modelProperty(model, 'arviointi')) {
    return null
  }

  model = fixArviointi(model)

  const arvioinnitModel = wrapOptional(modelLookup(model, 'arviointi'))

  const arviointiItems = modelItems(arvioinnitModel)


  let addItem = () => {
    console.log("adding item")

    let contextualized = contextualizeSubModel(arvioinnitModel.arrayPrototype, arvioinnitModel, arviointiItems.length)
    console.log(contextualized)
    pushModel(contextualized)
  }

  const suorituksenTyyppi = modelData(model, 'tyyppi').koodiarvo
  const näytetäänArviointiListana = model.context.edit && arviointiListaaKäyttäväKurssi(suorituksenTyyppi)

  if (näytetäänArviointiListana) {
   return (<div>
       {
         arviointiItems.map((m, id) => {
           return (<div key={id}>
             {YksittainenArvosana(m)}
             {YksittainenArviointipaiva(m)}
             <br/>
             <span>
               <a className="remove-value" style={{position:"static"}} onClick={() => {resetOptionalModel(m, undefined); console.log("poistetaan arviointi");}}>Poista arviointi</a>
             </span>
           </div>)
         })
      }
     <a className="add-value" onClick={() => addItem()}>Lisää arviointi</a>
   </div>)
  } else {
    return VanhaArvosanaKoodi(model)
  }
}

export const VanhaArvosanaKoodi = model => {
    const alternativesP = fetchAlternativesBasedOnPrototypes(oneOfPrototypes(wrapOptional(modelLookup(model, 'arviointi.-1'))), 'arvosana').startWith([])
    const arvosanatP = alternativesP.map(alternatives => alternatives.map(m => modelLookup(m, 'arvosana').value))
    return (<span>{
      alternativesP.map(alternatives => {
        const arvosanaLens = L.lens(
          (m) => {
            return modelLookup(m, '-1.arvosana')
          },
          (v, m) => {
            const valittu = modelData(v)
            if (valittu) {
              // Arvosana valittu -> valitaan vastaava prototyyppi (eri prototyypit eri arvosanoille)
              const found = alternatives.find(alt => {
                const altData = modelData(alt, 'arvosana')
                return altData.koodiarvo === valittu.koodiarvo && altData.koodistoUri === valittu.koodistoUri
              })
              return modelSetValue(m, found.value, '-1')
            } else {
              // Ei arvosanaa -> poistetaan arviointi kokonaan
              return modelSetValue(m, undefined)
            }
          }
        )
        const arviointiModel = modelLookup(model, 'arviointi')
        const arvosanaModel = lensedModel(arviointiModel, arvosanaLens)
        // Use key to ensure re-render when alternatives are supplied
        return <Editor key={alternatives.length} model={ arvosanaModel } sortBy={sortGrades} fetchAlternatives={() => arvosanatP} showEmptyOption="true"/>
      })
    }</span>)
}

export const YksittainenArvosana = model => {
  const alternativesP = fetchAlternativesBasedOnPrototypes(oneOfPrototypes(wrapOptional(model)), 'arvosana').startWith([])
  const arvosanatP = alternativesP.map(alternatives => alternatives.map(m => modelLookup(m, 'arvosana').value))

  return (<span>{
    alternativesP.map(alternatives => {
      const arvosanaLens = L.lens(
        (m) => {
          return modelLookup(m, 'arvosana')
        },
        (v, m) => {
          const valittu = modelData(v)
          if (valittu) {
            // Arvosana valittu -> valitaan vastaava prototyyppi (eri prototyypit eri arvosanoille)
            const found = alternatives.find(alt => {
              const altData = modelData(alt, 'arvosana')
              return altData.koodiarvo === valittu.koodiarvo && altData.koodistoUri === valittu.koodistoUri
            })
            return modelSetValue(m, found.value)
          } else {
            // Ei arvosanaa -> poistetaan arviointi kokonaan
            return modelSetValue(m, undefined)
          }
        }
      )
      const arviointiModel = model
      const arvosanaModel = lensedModel(arviointiModel, arvosanaLens)
      // Use key to ensure re-render when alternatives are supplied
      return <Editor key={alternatives.length} model={ arvosanaModel } sortBy={sortGrades} fetchAlternatives={() => arvosanatP} showEmptyOption="true"/>
    })
  }</span>)
}

export const YksittainenArviointipaiva = model => {
  const päiväModel = modelLookup(model, 'päivä')

  return (<span>
    <DateEditor model={päiväModel} />
  </span>)
}

export const resolveArvosanaModel = model => {
  let arviointi = modelLookup(model, 'arviointi.-1')
  let arvosana = arviointi ? modelLookup(model, 'arviointi.-1.arvosana') : null

  modelItems(model, 'arviointi').map(item => {
    const nthArvosana = modelLookup(item, 'arvosana')
    // Tää pitää korjata jonneki muualle jotenkin hienommaksi.
    // Idea siis se, että näytetään paras arvosana.
    // Arvosana ei ole kuitenkaan välttämättä numero..
    if (nthArvosana.value.data !== undefined && (nthArvosana.value.data.koodiarvo === 'S' || parseInt(nthArvosana.value.data.koodiarvo) > parseInt(arvosana.value.data.koodiarvo))) {
      arviointi = item
      arvosana = nthArvosana
    }
  })

  const isPaikallinenArviointi = arviointi && !modelEmpty(arviointi) && arviointi.value.classes.includes('paikallinenarviointi')

  return isPaikallinenArviointi ? modelLookup(arvosana, 'nimi') : arvosana
}
