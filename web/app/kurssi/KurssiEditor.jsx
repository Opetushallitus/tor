import React from 'react'
import {modelData, modelLookup, modelTitle} from '../editor/EditorModel.js'
import {ArvosanaEditor} from '../suoritus/ArvosanaEditor'
import {pushRemoval} from '../editor/EditorModel'
import {buildClassNames} from '../components/classnames'
import {KurssiPopup} from './KurssiPopup'
import {isLukio2019ModuuliTaiOpintojakso, isLukionKurssimainen, isPaikallinen} from '../suoritus/Koulutusmoduuli'
import {FootnoteHint} from '../components/footnote'
import {eiLasketaKokonaispistemäärään} from '../dia/DIA'
import {ArrayEditor} from '../editor/ArrayEditor'
import {arviointiListaaKäyttäväKurssi} from './kurssi.js'

export class KurssiEditor extends React.Component {
  constructor(props) {
    super(props)
    this.state = { open: false }
    this.handleClickOutside = this.handleClickOutside.bind(this)
    this.handleEsc = this.handleEsc.bind(this)
  }

  render() {
    let {kurssi} = this.props
    let {open} = this.state
    let koulutusmoduuli = modelData(kurssi, 'koulutusmoduuli')
    let koulutusmoduuliModel = modelLookup(kurssi, 'koulutusmoduuli')
    let showDetails = () => {
      if (!open) {
        document.addEventListener('click', this.handleClickOutside, false)
        document.addEventListener('keyup', this.handleEsc)
        this.setState({ open: true })
      }
    }
    let hideDetails = () => {
      this.setState({open: false})
    }
    let kurssinTyyppi = koulutusmoduuli.kurssinTyyppi ? koulutusmoduuli.kurssinTyyppi.koodiarvo : ''
    let edit = kurssi.context.edit
    const paikallinenLukionKurssimainen = isLukionKurssimainen(koulutusmoduuliModel) && isPaikallinen(koulutusmoduuliModel)
    const paikallinenLukionOpintojakso = isLukio2019ModuuliTaiOpintojakso(koulutusmoduuliModel) && isPaikallinen(koulutusmoduuliModel)
    let suorituksenTyyppi = modelData(kurssi, 'tyyppi').koodiarvo
    const näytetäänArviointiListana = edit && arviointiListaaKäyttäväKurssi(suorituksenTyyppi)
    let className = buildClassNames([
      'tunniste',
      kurssinTyyppi,
      !edit && 'hoverable',
      eiLasketaKokonaispistemäärään(kurssi) && 'ei-lasketa-kokonaispistemäärään',
      isPaikallinen(koulutusmoduuliModel) && !paikallinenLukionKurssimainen && 'paikallinen'
    ])
    const title = kurssi.value.classes.includes('diasuoritus') ? modelTitle(kurssi, 'koulutusmoduuli') : koulutusmoduuli.tunniste.koodiarvo
    return (
      <li className="kurssi" ref={e => this.kurssiElement = e}>
        <button onClick={showDetails} onMouseEnter={!edit ? showDetails : undefined} onMouseLeave={!edit ? hideDetails : undefined} className={`text-button-small ${className}`} title={modelTitle(kurssi, 'koulutusmoduuli')}>{title}</button>
        {
          edit && <a className="remove-value" onClick={() => pushRemoval(kurssi)}/>
        }
        {
          paikallinenLukionKurssimainen && <FootnoteHint title={'Paikallinen kurssi'} />
        }
        {
          paikallinenLukionOpintojakso && <FootnoteHint title={'Paikallinen opintojakso'} />
        }
        {
          eiLasketaKokonaispistemäärään(kurssi) &&
          <FootnoteHint title={'Ei lasketa kokonaispistemäärään'}/>
        }
        {
          <div className="arviointi">
            <ArvosanaEditor model={kurssi}/>
          </div>
        }
        {
          open && <KurssiPopup kurssi={kurssi} parentElemPosition={this.kurssiElement.getBoundingClientRect()}/>
        }
      </li>
    )
  }

/*
          näytetäänArviointiListana ?
          <div className="arviointi">
            <ArrayEditor model={modelLookup(kurssi, 'arviointi')} lisääTeksti="Lisää arviointi"/>
            <hr/>
          </div> :
          <div className="arvosana">
            <ArvosanaEditor model={kurssi}/>
          </div>
          */

  componentDidMount() {
    this.setState({open: false})
  }

  componentWillUnmount() {
    this.removeListeners()
  }

  removeListeners() {
    document.removeEventListener('click', this.handleClickOutside, false)
    document.removeEventListener('keyup', this.handleEsc)
  }

  handleClickOutside(e) {
    let detailsElem = this.kurssiElement.querySelector('.details')
    if (detailsElem && !detailsElem.contains(e.target)) {
      this.removeListeners()
      this.setState({open: false})
    }
  }

  handleEsc(e) {
    e.keyCode == 27 && this.setState({open: false})
  }
}
