import React from 'react'
import {modelData, modelLookup} from '../editor/EditorModel'

import {PerusopetuksenOppiaineetEditor} from '../perusopetus/PerusopetuksenOppiaineetEditor'
import {PropertiesEditor} from '../editor/PropertiesEditor'
import {LukionOppiaineetEditor} from '../lukio/LukionOppiaineetEditor'
import {LuvaEditor} from '../lukio/LuvaEditor'
import {PropertyEditor} from '../editor/PropertyEditor'
import {Editor} from '../editor/Editor'
import {sortLanguages} from '../util/sorting'
import {ArvosanaEditor} from './ArvosanaEditor'
import {
  LukionOppiaineenOppimaaranSuoritus,
  OmatTiedotLukionOppiaineenOppimaaranSuoritus
} from '../lukio/LukionOppiaineenOppimaaranSuoritus'
import {CreativityActionService, ExtendedEssay, TheoryOfKnowledge} from '../ib/IBYhteinenSuoritus'
import RyhmiteltyOppiaineetEditor from './RyhmiteltyOppiaineetEditor'
import OmatTiedotSuoritustaulukko from './OmatTiedotSuoritustaulukko'
import OmatTiedotLukionOppiaineet from '../lukio/OmatTiedotLukionOppiaineet'
import OmatTiedotPerusopetuksenOppiaineet from '../perusopetus/OmatTiedotPerusopetuksenOppiaineet'
import OmatTiedotRyhmiteltyOppiaineet from './OmatTiedotRyhmiteltyOppiaineet'
import TäydentääTutkintoaEditor from '../ammatillinen/TaydentaaTutkintoaEditor'
import InternationalSchoolOppiaineetEditor from '../internationalschool/InternationalSchoolOppiaineetEditor'
import {AikuistenPerusopetuksenKurssitEditor} from '../aikuistenperusopetus/AikuistenPerusopetuksenKurssitEditor'
import {Suoritustaulukko} from './Suoritustaulukko'
import {VapaanSivistystyonSuoritustaulukko} from '../vapaasivistystyo/VapaanSivistystyonSuoritustaulukko'

export const resolveOsasuorituksetEditor = (mdl) => {
  const oneOf = (...classes) => classes.some(c => mdl.value.classes.includes(c))
  const firstClassOneOf = (...classes) => classes.includes(mdl.value.classes[0])
  const {kansalainen} = mdl.context
  const LukionOppiaineetComponent = kansalainen ? OmatTiedotLukionOppiaineet : LukionOppiaineetEditor

  if (firstClassOneOf(
      'perusopetuksenvuosiluokansuoritus',
      'nuortenperusopetuksenoppimaaransuoritus',
      'aikuistenperusopetuksenoppimaaransuoritus',
      'aikuistenperusopetuksenalkuvaiheensuoritus',
      'perusopetuksenlisaopetuksensuoritus',
      'perusopetukseenvalmistavanopetuksensuoritus')) {
    const PerusopetuksenOppiaineetComponent = kansalainen ? OmatTiedotPerusopetuksenOppiaineet : PerusopetuksenOppiaineetEditor
    return <PerusopetuksenOppiaineetComponent model={mdl}/>
  }
  if (firstClassOneOf('aikuistenperusopetuksenoppiaineenoppimaaransuoritus')) {
    return <AikuistenPerusopetuksenKurssitEditor model={mdl}/>
  }
  if (firstClassOneOf('esiopetuksensuoritus')) {
    return <PropertiesEditor model={modelLookup(mdl, 'koulutusmoduuli')} propertyFilter={p => p.key === 'kuvaus'} />
  }
  if (oneOf(
    'ammatillinenpaatasonsuoritus',
    'ylioppilastutkinnonsuoritus',
    'korkeakoulusuoritus'
    )) {
    const SuoritustaulukkoComponent = kansalainen ? OmatTiedotSuoritustaulukko : Suoritustaulukko
    return <SuoritustaulukkoComponent suorituksetModel={modelLookup(mdl, 'osasuoritukset')} />
  }
  if (oneOf(
    'oppivelvollisillesuunnattuvapaansivistystyonkoulutuksensuoritus',
    'oppivelvollisillesuunnattumaahanmuuttajienkotoutumiskoulutuksensuoritus',
    'vapaansivistystyonlukutaitokoulutuksensuoritus'
  )) {
    return <VapaanSivistystyonSuoritustaulukko parentSuoritus={mdl} suorituksetModel={modelLookup(mdl, 'osasuoritukset')}/>
  }
  if (oneOf('lukionoppimaaransuoritus2015')) {
    return (
      <LukionOppiaineetComponent
        suorituksetModel={modelLookup(mdl, 'osasuoritukset')}
        classesForUusiOppiaineenSuoritus={['lukionoppiaineensuoritus2015', 'muidenlukioopintojensuoritus2015']}
      />
    )
  }
  if (oneOf('lukionoppimaaransuoritus2019', 'lukionoppiaineidenoppimaariensuoritus2019')) {
    return (
      <LukionOppiaineetComponent
        suorituksetModel={modelLookup(mdl, 'osasuoritukset')}
        classesForUusiOppiaineenSuoritus={['lukionoppiaineensuoritus2019', 'muidenlukioopintojensuoritus2019']}
        useOppiaineLaajuus={true}
        showKeskiarvo={false}
        additionalOnlyEditableProperties={['suorituskieli', 'suoritettuErityisenäTutkintona']}
        additionalEditableKoulutusmoduuliProperties={['pakollinen']}
        laajuusHeaderText={'Arvioitu'}
        showHyväksytystiArvioitujenLaajuus={true}
        useHylkäämättömätLaajuus={false}
      />
    )
  }
  if (oneOf('preibsuoritus2015')) {
    return (
      <LukionOppiaineetComponent
        suorituksetModel={modelLookup(mdl, 'osasuoritukset')}
        classesForUusiOppiaineenSuoritus={['preiboppiaineensuoritus2015', 'muidenlukioopintojensuoritus']}
        additionalEditableKoulutusmoduuliProperties={['ryhmä']}
      />
    )
  }
  if (oneOf('preibsuoritus2019')) {
    return (
      <LukionOppiaineetComponent
        suorituksetModel={modelLookup(mdl, 'osasuoritukset')}
        classesForUusiOppiaineenSuoritus={['iboppiaineenpreibsuoritus2019', 'lukionoppiaineenpreibsuoritus2019', 'muidenlukioopintojenpreibsuoritus2019']}
        useOppiaineLaajuus={true}
        showKeskiarvo={false}
        additionalOnlyEditableProperties={['suorituskieli', 'suoritettuErityisenäTutkintona']}
        additionalEditableKoulutusmoduuliProperties={['pakollinen', 'ryhmä']}
        laajuusHeaderText={'Arvioitu'}
        showHyväksytystiArvioitujenLaajuus={true}
        useHylkäämättömätLaajuus={false}
      />
    )
  }
  if (oneOf('lukionoppiaineenoppimaaransuoritus2015')) {
    const LukionOppiaineenOppimaaranSuoritusComponent = kansalainen
      ? OmatTiedotLukionOppiaineenOppimaaranSuoritus
      : LukionOppiaineenOppimaaranSuoritus
    return <LukionOppiaineenOppimaaranSuoritusComponent model={mdl} />
  }
  if (oneOf('lukioonvalmistavankoulutuksensuoritus')) {
    return <LuvaEditor suorituksetModel={modelLookup(mdl, 'osasuoritukset')}/>
  }
  if (oneOf('ibtutkinnonsuoritus')) {
    const TutkinnonOppiaineetComponent = kansalainen ? OmatTiedotRyhmiteltyOppiaineet : RyhmiteltyOppiaineetEditor
    return (
      <TutkinnonOppiaineetComponent
        suorituksetModel={modelLookup(mdl, 'osasuoritukset')}
        päätasonSuorituksenTyyppi={modelData(mdl, 'tyyppi').koodiarvo}
        additionalEditableKoulutusmoduuliProperties={['taso']}
      />
    )
  }
  if (oneOf('diplomavuosiluokansuoritus', 'mypvuosiluokansuoritus', 'pypvuosiluokansuoritus')) {
    return <InternationalSchoolOppiaineetEditor suorituksetModel={modelLookup(mdl, 'osasuoritukset')} />
  }
  if (oneOf('diavalmistavanvaiheensuoritus', 'diatutkinnonsuoritus')) {
    const TutkinnonOppiaineetComponent = kansalainen ? OmatTiedotRyhmiteltyOppiaineet : RyhmiteltyOppiaineetEditor
    return (
      <TutkinnonOppiaineetComponent
        suorituksetModel={modelLookup(mdl, 'osasuoritukset')}
        päätasonSuorituksenTyyppi={modelData(mdl, 'tyyppi').koodiarvo}
        additionalEditableKoulutusmoduuliProperties={['laajuus']}
      />
    )
  }
  return <PropertyEditor model={mdl} propertyName="osasuoritukset"/>
}

export const resolvePropertyEditor = (property, model) => {
  switch (property.key) {
    case 'suorituskieli': return <Editor model={modelLookup(model, 'suorituskieli')} sortBy={sortLanguages}/>
    case 'arviointi': return <ArvosanaEditor model={model}/>

    // IB
    case 'theoryOfKnowledge': return <TheoryOfKnowledge model={property.model}/>
    case 'creativityActionService': return <CreativityActionService model={property.model}/>
    case 'extendedEssay': return <ExtendedEssay model={property.model}/>
    case 'täydentääTutkintoa': return <TäydentääTutkintoaEditor model={property.model} />

    default: return null
  }
}
