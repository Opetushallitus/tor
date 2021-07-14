function AddOppijaPage() {
  function form() { return S('form.uusi-oppija') }
  function button() { return form().find('button') }
  function modalButton() { return form().find('button.vahvista') }
  function selectedOppilaitos() { return form().find('.oppilaitos .selected') }
  function selectedTutkinto() { return form().find('.tutkinto .selected') }
  var pageApi = Page(form)
  var api = {
    addNewOppija: function(username, hetu, oppijaData) {
      return function() {
        return prepareForNewOppija(username, hetu)()
          .then(api.enterValidDataAmmatillinen(oppijaData))
          .then(api.submitAndExpectSuccess(hetu, (oppijaData || {}).tutkinto))
        }
    },
    isVisible: function() {
      return isElementVisible(form()) && isNotLoading()
    },
    isEnabled: function() {
      return button().is(":visible") && !button().is(':disabled')
    },
    isModalButtonEnabled: function () {
      return modalButton().is(":visible") && !modalButton().is(':disabled')
    },
    tutkintoIsEnabled: function() {
      return S('.tutkinto input').is(':visible') && !S('.tutkinto input').is(':disabled')
    },
    rahoitusIsVisible: function() {
      return isElementVisible(S('.opintojenrahoitus'))
    },
    enterValidDataInternationalSchool: function(params) {
      params = _.merge({  oppilaitos: 'International School of Helsinki', grade: 'Grade explorer', opintojenRahoitus: 'Valtionosuusrahoitteinen koulutus', alkamispäivä: '1.1.2018'}, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('International school'))
          .then(api.selectFromDropdown('.international-school-grade .dropdown', params.grade))
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(api.selectOpintojenRahoitus(params.opintojenRahoitus))
          .then(wait.forAjax)
      }
    },
    enterValidDataPerusopetus: function(params) {
      params = _.merge({  oppilaitos: 'Jyväskylän normaalikoulu', alkamispäivä: '1.1.2018'}, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(wait.forAjax)
      }
    },
    enterValidDataEsiopetus: function(params) {
      params = _.merge({  oppilaitos: 'Jyväskylän normaalikoulu' }, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('Esiopetus'))
      }
    },
    enterValidDataPäiväkodinEsiopetus: function(params) {
      params = _.merge({  oppilaitos: 'PK Vironniemi' }, {}, params)
      return function() {
        return api.enterData(params)()
      }
    },
    enterHenkilötiedot: function(params) {
      params = _.merge({ etunimet: 'Tero', kutsumanimi: 'Tero', sukunimi: 'Tyhjä'}, {}, params)
      return function() {
        return pageApi.setInputValue('.etunimet input', params.etunimet)()
          .then(pageApi.setInputValue('.kutsumanimi input[disabled], .kutsumanimi .dropdown', params.kutsumanimi))
          .then(pageApi.setInputValue('.sukunimi input', params.sukunimi))
      }
    },
    enterValidDataVSTKOPS: function(params) {
      params = _.merge({
        oppilaitos: 'Varsinais-Suomen kansanopisto',
        oppimäärä: 'Oppivelvollisille suunnattu vapaan sivistystyön koulutus',
        suorituskieli: 'suomi',
        alkamispäivä: '1.8.2021'
      })
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('Vapaan sivistystyön koulutus'))
          .then(api.selectOppimäärä(params.oppimäärä))
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(api.selectMaksuttomuus(0))
      }
    },
    enterValidDataVSTLukutaito: function(params) {
      params = _.merge({
        oppilaitos: 'Varsinais-Suomen kansanopisto',
        oppimäärä: 'Vapaan sivistystyön lukutaitokoulutus',
        suorituskieli: 'suomi',
        alkamispäivä: '1.8.2021'
      })
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('Vapaan sivistystyön koulutus'))
          .then(api.selectOppimäärä(params.oppimäärä))
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(api.selectMaksuttomuus(0))
      }
    },
    enterValidDataAmmatillinen: function(params) {
      params = _.merge({  oppilaitos: 'Stadin', tutkinto: 'Autoalan perust', suoritustapa: 'Ammatillinen perustutkinto', opintojenRahoitus: 'Valtionosuusrahoitteinen koulutus', alkamispäivä: '1.1.2018' }, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('Ammatillinen koulutus'))
          .then(api.selectTutkinto(params.tutkinto))
          .then(api.selectSuoritustapa(params.suoritustapa))
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(api.selectOpintojenRahoitus(params.opintojenRahoitus))
      }
    },
    enterValidDataMuuAmmatillinen: function(params) {
      params = _.merge({  oppilaitos: 'Stadin', oppimäärä: 'Muun ammatillisen koulutuksen suoritus', opintojenRahoitus: 'Valtionosuusrahoitteinen koulutus'}, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('Ammatillinen koulutus'))
          .then(api.selectOppimäärä(params.oppimäärä))
          .then(function() {
            if (params.koulutusmoduuli) {
              return api.selectKoulutusmoduuli(params.koulutusmoduuli)()
            }
          })
          .then(api.selectOpintojenRahoitus(params.opintojenRahoitus))
      }
    },
    enterValidDataLukio: function(params) {
      params = _.merge({ oppilaitos: 'Ressun', oppimäärä: 'Lukion oppimäärä', peruste: '60/011/2015', opintojenRahoitus: 'Valtionosuusrahoitteinen koulutus', alkamispäivä: '1.1.2018' }, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('Lukiokoulutus'))
          .then(api.selectOppimäärä(params.oppimäärä))
          .then(api.selectPeruste(params.peruste))
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(api.selectOpintojenRahoitus(params.opintojenRahoitus))
      }
    },
    enterValidDataIB: function(params) {
      params = _.merge({ oppilaitos: 'Ressun', oppimäärä: 'IB-tutkinto', opintojenRahoitus: 'Valtionosuusrahoitteinen koulutus', alkamispäivä: '1.1.2018' }, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('IB-tutkinto'))
          .then(api.selectOppimäärä(params.oppimäärä))
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(api.selectOpintojenRahoitus(params.opintojenRahoitus))
      }
    },
    enterValidDataPreIB: function(params) {
      params = _.merge({ oppilaitos: 'Ressun', oppimäärä: 'Pre-IB', opintojenRahoitus: 'Valtionosuusrahoitteinen koulutus', alkamispäivä: '1.1.2018' }, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('IB-tutkinto'))
          .then(api.selectOppimäärä(params.oppimäärä))
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(api.selectOpintojenRahoitus(params.opintojenRahoitus))
      }
    },
    enterValidDataDIA: function(params) {
      params = _.merge({ oppilaitos: 'Helsingin', oppimäärä: 'DIA-tutkinto', alkamispäivä: '1.1.2018' }, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('DIA-tutkinto'))
          .then(api.selectOppimäärä(params.oppimäärä))
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(api.selectOpintojenRahoitus(params.opintojenRahoitus))
      }
    },
    enterValidDataDIAValmistavaVaihe: function(params) {
      params = _.merge({ oppilaitos: 'Helsingin', oppimäärä: 'Valmistava DIA-vaihe', alkamispäivä: '1.1.2018' }, {}, params)
      return function() {
        return api.enterData(params)()
          .then(api.selectOpiskeluoikeudenTyyppi('DIA-tutkinto'))
          .then(api.selectOppimäärä(params.oppimäärä))
          .then(api.selectAloituspäivä(params.alkamispäivä))
          .then(api.selectOpintojenRahoitus(params.opintojenRahoitus))
      }
    },
    enterPaikallinenKoulutusmoduuliData: function(params) {
      params = _.merge({
        nimi: 'Varaston täyttäminen',
        koodi: 'vrs-t-2019-k',
        kuvaus: 'Opiskelija osaa tehdä tilauksen vakiotoimittajilta sekä menettelytavat palavien ja vaarallisten aineiden varastoinnissa'
      }, {}, params)

      return function() {
        return pageApi.setInputValue('.koulutusmoduuli .nimi input', params.nimi)()
          .then(pageApi.setInputValue('.koulutusmoduuli .koodiarvo input', params.koodi))
          .then(pageApi.setInputValue('.koulutusmoduuli .kuvaus textarea', params.kuvaus))
      }
    },
    enterAmmatilliseenTehtäväänvalmistava: function(ammatilliseentehtäväänvalmistavakoulutus) {
      return selectFromDropdown('.ammatilliseentehtäväänvalmistavakoulutus .dropdown', ammatilliseentehtäväänvalmistavakoulutus)
    },
    enterData: function(params) {
      return function() {
        return api.enterHenkilötiedot(params)()
          .then(api.selectOppilaitos(params.oppilaitos))
          .then(function() {
            if (params.suorituskieli) {
              return api.selectSuorituskieli(params.suorituskieli)()
            }
          })
          .then(function () {
            if (params.opintojenRahoitus) {
              return api.selectOpintojenRahoitus(params.opintojenRahoitus)
            }
          })
      }
    },
    enterTutkinto: function(name) {
      return function() {
        return pageApi.setInputValue('.tutkinto input', name)()
      }
    },
    enterOppilaitos: function(name) {
      return OrganisaatioHaku(form).enter(name)
    },
    selectOppilaitos: function(name) {
      return OrganisaatioHaku(form).select(name)
    },
    oppilaitokset: OrganisaatioHaku(form).oppilaitokset,
    toimipisteet: OrganisaatioHaku(form).toimipisteet,
    oppilaitos: OrganisaatioHaku(form).oppilaitos,
    selectTutkinto: function(name) {
      if (!name) { return wait.forAjax }
      return function() {
        return wait.until(pageApi.getInput('.tutkinto input').isVisible)()
            .then(pageApi.setInputValue('.tutkinto input', name))
            .then(wait.until(function() { return isElementVisible(selectedTutkinto()) }))
            .then(click(selectedTutkinto))
      }
    },
    selectSuoritustapa: function(suoritustapa) {
      if (suoritustapa)
        return selectFromDropdown('.suoritustapa .dropdown', suoritustapa)
      else
        return function() {}
    },
    selectAloituspäivä: function(date) {
      return pageApi.setInputValue('.aloituspaiva input', date)
    },
    selectMaksuttomuus: function (index) {
      return function () {
        return click(S('.radio-option-container')[index])()
      }
    },
    henkilötiedot: function() {
      return ['.etunimet input', '.kutsumanimi input[disabled], .kutsumanimi .dropdown', '.sukunimi input'].map(function(selector) {
        return pageApi.getInputValue(selector)
      })
    },
    hetu: function() {
      return extractAsText(S('.hetu .value'))
    },
    submit: function() {
      if (!api.isEnabled()) {
        throw new Error('Button not enabled')
      }
      return click(button)()
    },
    submitModal: function() {
      if (!api.isModalButtonEnabled()) {
        throw new Error('Button not enabled')
      }
      return click(modalButton)()
    },
    submitAndExpectSuccess: function(oppija, tutkinto) {
      tutkinto = tutkinto || "Autoalan perustutkinto"
      return function() {
        return wait.until(api.isEnabled)()
          .then(api.submit)
          .then(wait.until(function() {
            return KoskiPage().getSelectedOppija().indexOf(oppija) >= 0 &&
              OpinnotPage().suoritusOnValittu(0, tutkinto)
          }))
      }
    },
    submitAndExpectSuccessModal: function(oppija, tutkinto) {
      tutkinto = tutkinto || "Autoalan perustutkinto"
      return function() {
        return wait.until(api.isModalButtonEnabled)()
          .then(api.submitModal)
          .then(wait.until(function() {
            return KoskiPage().getSelectedOppija().indexOf(oppija) >= 0 &&
              OpinnotPage().suoritusOnValittu(0, tutkinto)
          }))
      }
    },
    isErrorShown: function(field) {
      return function() {
        return isElementVisible(form().find('.error-messages .' + field))
      }
    },
    opiskeluoikeudenTyypit: function() {
      return pageApi.getInputOptions('.opiskeluoikeudentyyppi .dropdown')
    },
    opiskeluoikeudenTyyppi: function() {
      return pageApi.getInputValue('.opiskeluoikeudentyyppi input')
    },
    selectOpiskeluoikeudenTyyppi: function(tyyppi) {
      return pageApi.setInputValue('.opiskeluoikeudentyyppi .dropdown', tyyppi)
    },
    oppimäärät: function() {
      return pageApi.getInputOptions('.oppimaara .dropdown')
    },
    oppiaineet : function() {
      return pageApi.getInputOptions('.oppiaine .dropdown')
    },
    selectOppimäärä: function(oppimäärä) {
      return selectFromDropdown('.oppimaara .dropdown', oppimäärä)
    },
    selectOppiaine: function(oppiaine) {
      return selectFromDropdown('.oppiaine .dropdown', oppiaine)
    },
    selectSuorituskieli: function(kieli) {
      return selectFromDropdown('.suorituskieli .dropdown', kieli)
    },
    opintojenRahoitukset: function() {
      return pageApi.getInputOptions('.opintojenrahoitus .dropdown')
    },
    selectOpintojenRahoitus: function(rahoitus) {
      return rahoitus
        ? selectFromDropdown('.opintojenrahoitus .dropdown', rahoitus)
        : function() {}
    },
    opiskeluoikeudenTilat: function() {
      return pageApi.getInputOptions('.opiskeluoikeudentila .dropdown')
    },
    selectPeruste: function(peruste) {
      return selectFromDropdown('.peruste .dropdown', peruste)
    },
    selectKieli: function(kieli) {
      return selectFromDropdown('.kieli .dropdown', kieli)
    },
    selectKoulutusmoduuli: function(koulutusmoduuli) {
      return selectFromDropdown('.koulutusmoduuli .dropdown', koulutusmoduuli)
    },
    goBack: click(findSingle('h1 a')),
    selectFromDropdown: selectFromDropdown,
    selectVarhaiskasvatusOrganisaationUlkopuolelta: function(checked) {
      return pageApi.setInputValue('#varhaiskasvatus-checkbox', checked)
    },
    selectJärjestämismuoto: function(järjestämismuoto) {
      return selectFromDropdown('#varhaiskasvatus-jarjestamismuoto .dropdown', järjestämismuoto)
    }
  }
  function selectFromDropdown(selector, value, exact) {
    return function () {
      return wait.until(pageApi.getInput(selector).isVisible)().then(wait.forAjax).then(
        pageApi.setInputValue(selector, value)
      )
    }
  }
  return api
}
