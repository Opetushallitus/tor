function AddOppijaPage() {
  function form() { return S('form.oppija') }
  function button() { return form().find('button') }
  function selectedOppilaitos() { return form().find('.oppilaitos .selected') }
  function selectedTutkinto() { return form().find('.tutkinto .selected') }
  var pageApi = Page(form)
  var api = {
    isVisible: function() {
      return form().is(':visible') && !TorPage().isLoading()
    },
    isEnabled: function() {
      return !button().is(':disabled')
    },
    tutkintoIsEnabled: function() {
      return !S('.tutkinto input').is(':disabled')
    },
    enterValidData: function(params) {
      var self = this
      params = _.merge({ etunimet: 'Ossi Olavi', kutsumanimi: 'Ossi', sukunimi: 'Oppija', hetu: '300994-9694', oppilaitos: 'Helsingin', tutkinto: 'auto'}, {}, params)
      return function() {
        return pageApi.setInputValue('.etunimet input', params.etunimet)()
          .then(pageApi.setInputValue('.kutsumanimi input', params.kutsumanimi))
          .then(pageApi.setInputValue('.sukunimi input', params.sukunimi))
          .then(pageApi.setInputValue('.hetu input', params.hetu))
          .then(self.selectOppilaitos(params.oppilaitos))
          .then(self.selectTutkinto(params.tutkinto))
      }
    },
    enterTutkinto: function(name) {
      return function() {
        return pageApi.setInputValue('.tutkinto input', name)()
      }
    },
    enterOppilaitos: function(name) {
      return function() {
        return pageApi.setInputValue('.oppilaitos input', name)().then(wait.forAjax())
      }
    },
    selectOppilaitos: function(name) {
      return function() {
        return pageApi.setInputValue('.oppilaitos input', name)()
          .then(wait.until(function() { return selectedOppilaitos().is(':visible') }))
          .then(function() {triggerEvent(selectedOppilaitos(), 'click')})
      }
    },
    oppilaitokset: function() {
      return textsOf(form().find('.oppilaitos .results li'))
    },
    selectTutkinto: function(name) {
      return function() {
        return pageApi.setInputValue('.tutkinto input', name)()
          .then(wait.until(function() { return selectedTutkinto().is(':visible') }))
          .then(function() {triggerEvent(selectedTutkinto(), 'click')})
      }
    },
    submit: function() {
      triggerEvent(button(), 'click')
    },
    isErrorShown: function(field) {
      return function() {
        return form().find('.error-messages .' + field).is(':visible')
      }
    },
    postOpintoOikeusAjax: function(opintoOikeus) {
      return function() {
        opintoOikeus = _.merge(defaultOpintoOikeus(), opintoOikeus)
        data = makeOppija({}, [opintoOikeus])
        return api.postOppijaAjax(data)()
      }
    },
    postOppijaAjax: function(oppija) {
      return function() {
        var defaults = makeOppija(defaultHenkilo(), [defaultOpintoOikeus()])
        oppija = _.merge(defaults, {}, oppija)
        return postJson(
          'http://localhost:7021/tor/api/oppija', oppija
        )
      }
    }
  }
  function defaultOpintoOikeus() { return {
    oppilaitosOrganisaatio: { oid: '1' },
    tutkinto: {ePerusteetDiaarinumero:'39/011/2014', tutkintoKoodi: '351301'}
  }}
  function defaultHenkilo() {return {
    'etunimet':'Testi',
    'sukunimi':'Toivola',
    'kutsumanimi':'Testi',
    'hetu':'010101-123N'
  }}
  function makeOppija(henkilo, opintoOikeudet) { return _.cloneDeep({
    henkilo: henkilo || defaultHenkilo(),
    opintoOikeudet: opintoOikeudet || [defaultOpintoOikeus()]
  })}
  return api
}