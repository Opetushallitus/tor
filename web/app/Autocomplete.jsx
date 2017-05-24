import React from 'react'
import Bacon from 'baconjs'
import BaconComponent from './BaconComponent'
import delays from './delays'
import {t} from './i18n'

export default BaconComponent({
  render() {
    let {disabled, selected} = this.props
    let {items, query, selectionIndex} = this.state

    let itemElems = items ? items.map((item, i) => {
        return (
          <li key={i} className={i === selectionIndex ? 'selected' : null} onClick={this.handleSelect.bind(this, item)}>{item.nimi.fi}</li>
        )}
    ) : []

    let results = items.length ? <ul className='results'>{itemElems}</ul> : null

    return (
      <div ref='autocomplete' className='autocomplete'>
        <input type="text" className='autocomplete-input' onKeyDown={this.onKeyDown} onChange={this.handleInput} value={(query ? query : (selected ? t(selected.nimi) : '')) || ''} disabled={disabled}></input>
        {results}
      </div>
    )
  },

  handleInput(e) {
    let query = e.target.value
    this.setValue(undefined)
    this.state.inputBus.push(query)
    this.setState({query: query})
  },

  handleSelect(selected) {
    this.setState({query: undefined, items: []})
    this.setValue(selected)
  },

  setValue(value) {
    if (this.props.resultBus) {
      this.props.resultBus.push(value)
    } else if (this.props.resultAtom) {
      this.props.resultAtom.set(value)
    } else {
      throw 'resultBus, resultAtom missing'
    }
  },

  onKeyDown(e) {
    let handler = this.keyHandlers[e.key]
    if(handler) {
      handler.call(this, e)
    }
  },

  componentDidMount() {
    this.state.inputBus
      .throttle(delays().delay(200))
      .flatMapLatest(query => this.props.fetchItems(query).mapError([]))
      .takeUntil(this.unmountE)
      .onValue((items) => this.setState({ items: items, selectionIndex: 0 }))
  },

  getInitialState() {
    return {query: undefined, items: [], selectionIndex: 0, inputBus: Bacon.Bus()}
  },

  keyHandlers: {
    ArrowUp() {
      let {selectionIndex} = this.state
      selectionIndex = selectionIndex === 0 ? 0 : selectionIndex - 1
      this.setState({selectionIndex: selectionIndex})
    },
    ArrowDown() {
      let {selectionIndex, items} = this.state
      selectionIndex = selectionIndex === items.length - 1 ? selectionIndex : selectionIndex + 1
      this.setState({selectionIndex: selectionIndex})
    },
    Enter(e) {
      e.preventDefault()
      let {selectionIndex, items} = this.state
      this.handleSelect(items[selectionIndex])
    },
    Escape() {
      this.setState({query: undefined, items: []})
    }
  }
})
