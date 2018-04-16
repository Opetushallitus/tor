import React from 'react'
import Bacon from 'baconjs'
import {CopyableText} from '../../components/CopyableText'
import Text from '../../i18n/Text'
import DateInput from '../../date/DateInput'
import {formatISODate, parseISODate} from '../../date/date'
import Http from '../../util/http'

const ApiBaseUrl = '/koski/api/suoritusjako'

const doDelete = secret => Http.post(`${ApiBaseUrl}/delete`, {secret})
const doUpdate = (secret, expirationDate) => Http.post(`${ApiBaseUrl}/update`, {secret, expirationDate})

export const SuoritusjakoLinkPlaceholder = () => <div className='suoritusjako-link--placeholder'/>

export class SuoritusjakoLink extends React.Component {
  constructor(props) {
    super(props)

    this.state = {
      isDeletePending: false,
      isDateUpdatePending: false
    }

    this.dateChangeBus = new Bacon.Bus()
  }

  componentDidMount() {
    const debounced = this.dateChangeBus
      .toProperty()
      .changes()
      .debounce(500)

    debounced.onValue(() => this.setState({isDateUpdatePending: true}))

    const update = debounced
      .flatMapLatest(date => date && doUpdate(this.props.suoritusjako.secret, formatISODate(date)))

    update.onValue(() => this.setState({isDateUpdatePending: false}))
  }

  static isDateInFuture(date) {
    const today = new Date()
    const tomorrow = new Date()
    tomorrow.setDate(today.getDate() + 1)
    tomorrow.setHours(0,0,0,0)

    return date.getTime() >= tomorrow.getTime()
  }

  removeHandler() {
    const {isDateUpdatePending} = this.state
    const {suoritusjako, onRemove} = this.props
    const {secret} = suoritusjako

    if (isDateUpdatePending) return

    this.setState({isDeletePending: true},
      () => {
        const res = doDelete(secret)
        res.onValue(() => onRemove(suoritusjako))
        res.onError(() => this.setState({isDeletePending: false}))
      }
    )
  }

  render() {
    const {isDeletePending, isDateUpdatePending} = this.state
    const {suoritusjako} = this.props
    const {secret, expirationDate} = suoritusjako
    const url = `${window.location.origin}/koski/opinnot/${secret}`

    return isDeletePending ? <SuoritusjakoLinkPlaceholder/>
      : (
        <div className='suoritusjako-link'>
          <div className='suoritusjako-link__top-container'>
            <CopyableText className='suoritusjako-link__url' message={url} multiline={false}/>
            <div className='suoritusjako-link__expiration'>
              <label>
                <Text name='Linkin voimassaoloaika'/>
                <Text name='Päättyy'/>
                <div style={{display: 'inline'}} className={isDateUpdatePending ? 'ajax-indicator-right' : ''}>
                  <DateInput
                    value={parseISODate(expirationDate)}
                    valueCallback={date => this.dateChangeBus.push(date)}
                    validityCallback={isValid => !isValid && this.dateChangeBus.push(null)}
                    isAllowedDate={SuoritusjakoLink.isDateInFuture}
                    isPending={isDateUpdatePending}
                  />
                </div>
              </label>
            </div>
          </div>
          <div className='suoritusjako-link__bottom-container'>
            <div className='suoritusjako-link__preview'>
              <a target='_blank' href={url}><Text name='Esikatsele'/></a>
            </div>
            <div className='suoritusjako-link__remove'>
              <a className={`text-button${(isDateUpdatePending ? '--disabled' : '')}`} onClick={this.removeHandler.bind(this)}>
                <Text name='Poista linkki käytöstä'/>
              </a>
            </div>
          </div>
        </div>
      )
  }
}
