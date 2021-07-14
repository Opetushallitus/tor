function OpinnotPage() {
  function resolveOpiskeluoikeus(indexOrName, omatTiedot) {
    omatTiedot = omatTiedot || false
    var all = !omatTiedot
      ? S('.opiskeluoikeuksientiedot > li > div.opiskeluoikeus')
      : S('.opiskeluoikeudet-list > li > div.opiskeluoikeus-container')

    if (typeof indexOrName === 'undefined') {
      // most common case, just a single opiskeluoikeus
      if (all.length !== 1) {
        throw new Error('opiskeluoikeus not found, expected one, got ' + all.length)
      }
      return all
    } else if (typeof indexOrName === 'string') {
      // find the right opiskeluoikeus by name (safe against changes in sorting)
      var matching = all.filter(function(i,v) { return $(v).find(!omatTiedot ? '.otsikkotiedot' : 'button.opiskeluoikeus-button .opiskeluoikeus-button-content .opiskeluoikeus-title').text().includes(indexOrName) })
      if (matching.length !== 1) {
        throw new Error('opiskeluoikeus not found, got ' + matching.length + ' matches for ' + indexOrName)
      }
      return matching
    } else if (typeof indexOrName === 'number') {
      // find the right opiskeluoikeus by position (will break when sorting is changed)
      if (indexOrName >= all.length) {
        throw new Error('opiskeluoikeus not found, got ' + all.length + ' items but wanted index ' + indexOrName)
      }
      return all.eq(indexOrName)
    } else {
      throw new Error('opiskeluoikeus not found, unknown type ' + typeof indexOrName)
    }
  }
  var api = {
    valitseOmatTiedotOpiskeluoikeus: function(indexOrName) {
      return function() {
        var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName, true)
        return click(findSingle('button.opiskeluoikeus-button', opiskeluoikeus))()
      }
    },
    valitseOmatTiedotOpiskeluoikeudenLukionSuoritus: function(indexOrName, suoritus) {
      return function() {
        var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName, true)
        return click(findSingle('table.omattiedot-suoritukset tr.oppiaine-header td.oppiaine:contains(' + suoritus + ')', opiskeluoikeus))()
      }
    },
    getOpiskeluoikeudenTila: function(indexOrName) {
      var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName)
      return findSingle('.opiskeluoikeuden-tiedot .property.tila .value', opiskeluoikeus)().text()
    },
    getTutkinto: function(indexOrName) {
      var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName)
      return opiskeluoikeus.find('.suoritus .property.koulutusmoduuli .koulutusmoduuli .tunniste').text()
    },
    getKoulutusModuuli: function(indexOrName) {
      var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName)
      return {
        nimi: opiskeluoikeus.find('.suoritus .property.koulutusmoduuli .koulutusmoduuli .tunniste .nimi .value').text(),
        koodi: opiskeluoikeus.find('.suoritus .property.koulutusmoduuli .koulutusmoduuli .tunniste .koodiarvo .value').text(),
        kuvaus: opiskeluoikeus.find('.suoritus .property.koulutusmoduuli .koulutusmoduuli .properties .kuvaus .value').text()
      }
    },
    getOppilaitos: function(indexOrName) {
      var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName)
      return opiskeluoikeus.find('h3 .otsikkotiedot .oppilaitos', opiskeluoikeus).text()
    },
    getSuorituskieli: function(indexOrName) {
      var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName)
      return opiskeluoikeus.find('.suoritus .property.suorituskieli .value').text()
    },
    valitseSuoritus: function(indexOrName, nimi, omatTiedot) {
      omatTiedot = omatTiedot || false
      return function() {
        var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName, omatTiedot)
        var tab = findFirst('.suoritus-tabs > ul > li:contains(' + nimi + ')', opiskeluoikeus)()
        if (!tab.hasClass('selected')) {
          return click(findSingle(omatTiedot ? 'button' : 'a', tab.eq(0)))()
        }
      }
    },
    suoritusOnValittu: function(indexOrName, nimi) {
      var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName)
      var tab = findSingle('.suoritus-tabs > ul > li:contains(' + nimi + ')', opiskeluoikeus)()
      return tab.hasClass('selected')
    },
    suoritusTabs: function(indexOrName, omatTiedot) {
      omatTiedot = omatTiedot || false
      var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName, omatTiedot)
      return textsOf(subElement(opiskeluoikeus, '.suoritus-tabs > ul > li'))
    },
    suoritusTabIndex: function(indexOrName){
      var opiskeluoikeus = resolveOpiskeluoikeus(indexOrName)
      var tabs = toArray(subElement(opiskeluoikeus, '.suoritus-tabs > ul > li'))
      for (var i in tabs) {
        if (S(tabs[i]).hasClass('selected')) return parseInt(i)
      }
      return -1
    },
    onTallennettavissa: function() {
      return S('#edit-bar button:not(:disabled)').is(':visible')
    },
    isEditing: function() {
      return api.isDirty() || S('.oppija-content.edit').is(':visible')
    },
    isDirty: function() {
      return S('.oppija-content.dirty').is(':visible')
    },
    avaaLisaysDialogi: function() {
      if (!S('.lisaa-opiskeluoikeusjakso-modal .modal-content').is(':visible')) {
        return click(S('.opiskeluoikeuden-tiedot .add-item a'))()
      }
    },
    suljeLisaysDialogi: function() {
      if (S('.lisaa-opiskeluoikeusjakso-modal .modal-content').is(':visible')) {
        return click(S('.opiskeluoikeuden-tiedot .modal-content .close-modal'))()
      }
    },
    avaaLisätiedot: function() {
      return click(S('.expandable-container .lisätiedot span'))()
    },
    opiskeluoikeudet: Opiskeluoikeudet(),
    opiskeluoikeusEditor: function(index, omatTiedot) {
      omatTiedot = omatTiedot || false
      var elem = findSingle('.opiskeluoikeus-content', function() { return resolveOpiskeluoikeus(index, omatTiedot) })
      return _.merge(
        Editor(elem),
        {
          päättymispäivä: function() { return elem().find('.päättymispäivä').text() }
        }
      )
    },
    lisääDIAValmistavaOppiaine: function() {
      return seq(
        click('div.uusi-oppiaine .dropdown:first div.input-container'),
        click('div.uusi-oppiaine .dropdown:first ul.options li:first')
      )()
    },
    lisääDIAValmistavaOppiaineOsasuoritus: function() {
      return seq(
        wait.untilVisible(function() { return S('.uusi-kurssi > a')} ),
        click('.uusi-kurssi > a'),
        wait.untilVisible(function() { return S('#modal-main-content > .kurssi')} ),
        click('.uusi-kurssi .dropdown input'),
        click('.uusi-kurssi .dropdown > .options > .option:first'),
        click('.kurssit .uusi-kurssi .modal-content .actions .vahvista span'),
        wait.until(function() { return !isElementVisible(S('#modal-main-content > .kurssi')) })
      )()
    },
    haeValmistavanOppiaineenOsasuoritustenVaihtoehtojenLukumäärä: function() {
      return new Promise(function(resolve) {
        seq(
          click('.uusi-kurssi > a'),
          wait.untilVisible(function() { return S('#modal-main-content > .kurssi')} ),
          click('.uusi-kurssi .dropdown input'),
          wait.untilVisible(function() { return S('.uusi-kurssi .modal-content')} ),
          function() { return resolve(S('.uusi-kurssi .dropdown > .options .option').length) },
        )()
      })
    },
    lisääSuoritusDialog: LisääSuoritusDialog(),
    tutkinto: function(selector) {
      return TutkintoSelector(findSingle(selector))
    },
    tilaJaVahvistus: TilaJaVahvistus(),
    versiohistoria: Versiohistoria(),
    oppiaineet: Oppiaineet(),
    tutkinnonOsat: TutkinnonOsat,
    vstSuoritukset: VSTSuoritukset(),
    ibYhteisetSuoritukset: IBSuoritukset(),
    avaaKaikki: click(findSingle('.expand-all')),
    suljeKaikki: click(findSingle('.expand-all.koski-button.expanded')),
    anythingEditable: function() {
      return Editor(findSingle('.content-area') ).isEditable()
    },
    expandAll: function() {
      var checkAndExpand = function() {
        if (expanders().is(':visible')) {
          return seq(click(expanders),
            wait.forMilliseconds(10),
            wait.forAjax,
            checkAndExpand)()
        }
      }
      return checkAndExpand()
      function expanders() {
        return S('.foldable.collapsed>.toggle-expand:not(.disabled), tbody:not(.expanded) > tr > td > .toggle-expand:not(.disabled), a.expandable:not(.open)')
      }
    },
    collapseAll: function() {
      var checkAndCollapse = function() {
        if (collapsers().is(':visible')) {
          return seq(click(collapsers),
            wait.forMilliseconds(10),
            wait.forAjax,
            checkAndCollapse)()
        }
      }
      return checkAndCollapse()
      function collapsers() {
        return S('.foldable:not(.collapsed)>.toggle-expand:not(.disabled), tbody.expanded .toggle-expand:not(.disabled), a.expandable.open')
      }
    },
    invalidateOpiskeluoikeusIsShown: function() {
      return isElementVisible(findSingle('.invalidate.invalidate-opiskeluoikeus'))
    },
    confirmInvalidateOpiskeluoikeusIsShown: function() {
      return isElementVisible(findSingle('.confirm-invalidate.invalidate-opiskeluoikeus__confirm'))
    },
    deletePäätasonSuoritusIsShown: function() {
      return isElementVisible(findSingle('.invalidate.delete-paatason-suoritus'))
    },
    confirmDeletePäätasonSuoritusIsShown: function() {
      return isElementVisible(findSingle('.confirm-invalidate.delete-paatason-suoritus__confirm'))
    },
    invalidateOpiskeluoikeus: click(findSingle('.invalidate.invalidate-opiskeluoikeus')),
    confirmInvalidateOpiskeluoikeus: click(findSingle('.confirm-invalidate.invalidate-opiskeluoikeus__confirm')),
    hideInvalidateMessage: click(findSingle('.hide-invalidation-notification')),
    deletePäätasonSuoritus: click(findSingle('.invalidate.delete-paatason-suoritus')),
    confirmDeletePäätasonSuoritus: click(findSingle('.confirm-invalidate.delete-paatason-suoritus__confirm')),
    backToList: click(findSingle('.back-link'))
  }

  return api
}

function Oppiaineet() {
  var api = {
    isVisible: function() { return S('.oppiaineet h5').is(':visible') },

    merkitseOppiaineetValmiiksi: function(selectedArvosana) {
      selectedArvosana = selectedArvosana || '5'
      return function() {
        var editor = OpinnotPage().opiskeluoikeusEditor()
        var count = 20
        var promises = []
        for (var i = 0; i < count; i++) {
          var oppiaine = api.oppiaine(i)
          var arvosana = oppiaine.propertyBySelector('.arvosana')
          if (arvosana.isVisible()) {
            promises.push(arvosana.selectValue(selectedArvosana)())
          }
        }
        return Q.all(promises)
      }
    },

    uusiOppiaine: function(selector) {
      selector = selector || ""
      return OpinnotPage().opiskeluoikeusEditor().propertyBySelector(selector + ' .uusi-oppiaine')
    },

    oppiaine: function(indexOrClass) {
      var oppiaineElem = typeof indexOrClass == 'number'
        ? findSingle('.oppiaineet .oppiaine-rivi:eq(' + indexOrClass + ')')
        : findSingle('.oppiaineet .oppiaine-rivi.' + indexOrClass)
      return Oppiaine(oppiaineElem)
    }
  }
  return api
}

function Oppiaine(oppiaineElem) {
  var editorApi = Editor(oppiaineElem)
  var oppiaineApi = _.merge({
    text: function() { return extractAsText(oppiaineElem) },
    avaaLisääKurssiDialog: click(findSingle('.uusi-kurssi a', oppiaineElem)),
    avaaAlkuvaiheenLisääKurssiDialog: click(findSingle('.uusi-alkuvaiheen-kurssi a', oppiaineElem)),
    lisääKurssi: function(kurssi, tyyppi) { return seq(
      oppiaineApi.avaaLisääKurssiDialog,
      oppiaineApi.lisääKurssiDialog.valitseKurssi(kurssi || 'Kieli ja kulttuuri'),
      oppiaineApi.lisääKurssiDialog.valitseKurssinTyyppi(tyyppi || 'Pakollinen'),
      oppiaineApi.lisääKurssiDialog.lisääKurssi
    )},
    lisääLaajuudellinenOpintojakso: function(opintojakso, laajuus) { return seq(
      oppiaineApi.avaaLisääKurssiDialog,
      oppiaineApi.lisääKurssiDialog.valitseKurssi(opintojakso || 'Kieli ja kulttuuri'),
      oppiaineApi.lisääKurssiDialog.asetaLaajuus(laajuus || '2'),
      oppiaineApi.lisääKurssiDialog.lisääKurssi
      )},
    lisääPaikallinenKurssi: function(tunniste, nimi, kuvaus) { return seq(
      oppiaineApi.avaaLisääKurssiDialog,
      oppiaineApi.lisääKurssiDialog.valitseKurssi('paikallinen'),
      oppiaineApi.lisääKurssiDialog.asetaTunniste(tunniste),
      oppiaineApi.lisääKurssiDialog.asetaNimi(nimi),
      oppiaineApi.lisääKurssiDialog.asetaKuvaus(kuvaus),
      oppiaineApi.lisääKurssiDialog.valitseKurssinTyyppi('Soveltava'),
      oppiaineApi.lisääKurssiDialog.lisääKurssi
    )},
    poista: function() {
      return oppiaineApi.propertyBySelector('.remove-row').removeValue
    },
    lisääKurssiDialog: LisääKurssiDialog(),
    kurssi: function(identifier) {
      return Kurssi(subElement(oppiaineElem, ".kurssi:contains(" + identifier +")"))
    },
    nthOsasuoritus: function(n) {
      return Kurssi(subElement(oppiaineElem, ".kurssi:eq(" + n + ")"))
    },
    errorText: function() { return extractAsText(subElement(oppiaineElem, '> .error')) },
    arvosana: editorApi.propertyBySelector('tr td.arvosana'),
    laajuus: editorApi.propertyBySelector('span.laajuus'),
    suorituskieli: editorApi.propertyBySelector('tr.suorituskieli'),
    koetuloksenNelinkertainenPistemäärä: editorApi.propertyBySelector('tr.koetuloksenNelinkertainenPistemäärä'),
    expandable: function() {
      return typeof findFirstNotThrowing('.toggle-expand', oppiaineElem) !== 'undefined'
    },
    expand: click(findSingle('.toggle-expand', oppiaineElem))
  }, editorApi)
  return oppiaineApi

  function LisääKurssiDialog() {
    var modalElem = findSingle('.uusi-kurssi-modal', oppiaineElem)
    function kurssiDropdown() { return api.propertyBySelector('.kurssi') }
    function kurssinTyyppiDropdown() { return api.propertyBySelector('.kurssinTyyppi') }

    /* Paikallinen kurssi */
    function tunniste() { return api.propertyBySelector('.koodiarvo') }
    function nimi() { return api.propertyBySelector('.nimi') }
    function kuvaus() { return api.propertyBySelector('.kuvaus') }

    /* Laajuudellinen opintojakso */
    function laajuus() { return api.propertyBySelector('.laajuus .arvo') }

    var api = _.merge({
      valitseKurssi: function(kurssi) {
        return kurssiDropdown().setValue(kurssi)
      },
      valitseKurssinTyyppi: function(tyyppi) {
        return function() {
          try {
            return kurssinTyyppiDropdown().setValue(tyyppi)()
          } catch(e) {
            // valitaan tyyppi vain kun se on mahdollista
          }
        }
      },
      hasKurssinTyyppi: function() {
        return api.hasProperty('kurssinTyyppi')
      },
      asetaTunniste: function(arvo) {
        return tunniste().setValue(arvo || 'PA')
      },
      asetaNimi: function(arvo) {
        return nimi().setValue(arvo || 'Paikallinen kurssi')
      },
      asetaLaajuus: function(arvo) {
        return laajuus().setValue(arvo || '2')
      },
      asetaKuvaus: function(arvo) {
        return kuvaus().setValue(arvo || 'Paikallisen kurssin kuvaus')
      },
      lisääKurssi: click(subElement(modalElem, 'button.vahvista')),
      kurssit: function() {
        return kurssiDropdown().getOptions()
      },
      sulje: click('.uusi-kurssi-modal .peruuta')
    }, Editor(modalElem))
    return api
  }
}


function Kurssi(elem) {
  var detailsElem = subElement(elem, '.details')
  var api = {
    detailsText: function() {
      return toElement(detailsElem).is(":visible") ? extractAsText(detailsElem) : ""
    },
    arvosana: Editor(elem).propertyBySelector('.arvosana'),
    toggleDetails: click(subElement(elem, '.tunniste')),
    showDetails: function() {
      if (api.detailsText() == '')
        return api.toggleDetails()
      return wait.forAjax()
    },
    details: function() {
      return Editor(detailsElem)
    },
    tunnustettu: Editor(elem).propertyBySelector('.tunnustettu'),
    laajuus: Editor(elem).propertyBySelector('tr.laajuus'),
    lasketaanKokonaispistemäärään: Editor(elem).propertyBySelector('tr.lasketaanKokonaispistemäärään'),
    lisääTunnustettu: click(subElement(detailsElem, '.tunnustettu .add-value')),
    poistaTunnustettu: click(subElement(detailsElem, '.tunnustettu .remove-value')),
    poistaKurssi: click(subElement(elem, '.remove-value'))
  }
  return api
}
Kurssi.findAll = function() {
  return toArray(S(".kurssi")).map(Kurssi)
}


function TutkinnonOsat(groupId, base) {
  function withSuffix(s) { return groupId ? s + '.' + groupId : s }
  var uusiTutkinnonOsaElement = findSingle(withSuffix('.uusi-tutkinnon-osa'), base)
  function osienElementit() {
    return subElement(base, withSuffix('.tutkinnon-osa'))
  }
  function lisääPaikallinenTutkinnonOsa(nimi, modalSelector, addLinkSelector) {
    return function() {
      var modalElement = subElement(uusiTutkinnonOsaElement, modalSelector)
      return click(subElement(uusiTutkinnonOsaElement, addLinkSelector))()
        .then(Page(modalElement).setInputValue('input', nimi))
        .then(click(subElement(modalElement, 'button.vahvista:not(:disabled)')))
    }
  }

  return {
    osienTekstit: function() {
      return extractAsText(osienElementit)
    },
    tyhjä: function() {
      return osienElementit().length === 0
    },
    isGroupHeaderVisible: function() {
      return S(withSuffix('.group-header')).is(':visible')
    },
    tutkinnonOsa: function(tutkinnonOsaIndex) {
      var tutkinnonOsaElement = findSingle(withSuffix('.tutkinnon-osa') + ':eq(' + tutkinnonOsaIndex + ')', base)

      var api = _.merge({
        tila: function() {
          return findSingle('.tila', tutkinnonOsaElement)().attr('title')
        },
        nimi: function() {
          return findSingle('.nimi', tutkinnonOsaElement)().text()
        },
        toggleExpand: click(findSingle('.suoritus .toggle-expand', tutkinnonOsaElement)),
        lisääOsaamisenTunnustaminen: click(findSingle('.tunnustettu .add-value', tutkinnonOsaElement)),
        poistaOsaamisenTunnustaminen: click(findSingle('.tunnustettu .remove-value', tutkinnonOsaElement)),
        poistaLisätieto: click(findSingle('.lisätiedot .remove-item', tutkinnonOsaElement)),
        poistaTutkinnonOsa: function() {
          var removeElement = findSingle('.remove-value', tutkinnonOsaElement)
          if (isElementVisible(removeElement)) {
            return click(removeElement)()
          }
        },
        näyttö: function() {
          return api.property('näyttö')
        },
        lisätiedot: function() {
          return api.property('lisätiedot')
        },
        arviointi: function() {
          return api.property('arviointi')
        },
        arviointiNth: function(nth) {
          return api.propertyBySelector('.arviointi > .value > ul > li:nth-child(' + (nth + 1) + ')')
        },
        liittyyTutkinnonOsaan: function() {
          return LiittyyTutkinnonOsaan(api.property('liittyyTutkinnonOsaan'))
        },
        osanOsat: function() {
          return TutkinnonOsat('999999', tutkinnonOsaElement)
        },
        sanallinenArviointi: function() {
          return api.propertyBySelector('.property.kuvaus:eq(1)')
        },
        lueNäyttöModal: function() {
          function extractDropdownArray(elem) {
            return elem.find('ul.array > li').map(function() {return Page(this).getInput('.dropdown').value()}).get().slice(0, -1)
          }
          return {
            arvosana: Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .arvosana .value .dropdown').value(),
            arviointipäivä: Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .päivä .value input').value(),
            kuvaus: Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .kuvaus .value textarea').value(),
            arvioinnistaPäättäneet: extractDropdownArray(S('.näyttö .modal-content .arvioinnistaPäättäneet .value', tutkinnonOsaElement)),
            arviointikeskusteluunOsallistuneet: extractDropdownArray(S('.näyttö .modal-content .arviointikeskusteluunOsallistuneet .value', tutkinnonOsaElement)),
            suorituspaikka: [
              Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .suorituspaikka .value .dropdown').value(),
              Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .suorituspaikka .value input:not(.select)').value()
            ],
            työssäoppimisenYhteydessä: Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .työssäoppimisenYhteydessä .value input').value()
          }
        },
        avaaNäyttöModal: function() {
          var valueExists = !!tutkinnonOsaElement().find('.näyttö .edit-value').length
          return Q()
            .then(click(findSingle('.näyttö .'+(valueExists?'edit':'add')+'-value', tutkinnonOsaElement)))
            .then(wait.untilVisible(findSingle('.lisää-näyttö-modal .modal-content')))
        },
        asetaNäytönTiedot: function(tiedot) {
          return function () {
            return wait.forAjax().then(function() {
              // Normalize state
              var addVButton = tutkinnonOsaElement().find('.näyttö .modal-content .arviointi .add-value')
              if (addVButton.length) {
                click(addVButton)()
              }
              else {
                tutkinnonOsaElement().find('.näyttö .modal-content .arvioinnistaPäättäneet .value li .remove-item:visible').each(function(i, e) {
                  click(e)()
                })
                tutkinnonOsaElement().find('.näyttö .modal-content .arviointikeskusteluunOsallistuneet .value li .remove-item:visible').each(function(i, e) {
                  click(e)()
                })
              }
            }).then(wait.forAjax).then(function () {
              Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .arvosana .value .dropdown').setValue(tiedot.arvosana, 1)
              Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .päivä .value input').setValue(tiedot.arviointipäivä)
              Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .kuvaus .value textarea').setValue(tiedot.kuvaus)
              tiedot.arvioinnistaPäättäneet.map(function (v, i) {
                Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .arvioinnistaPäättäneet .value li:'+(i===0?'first':'last')+'-child .dropdown').setValue(v)
              })
              tiedot.arviointikeskusteluunOsallistuneet.map(function (v, i) {
                Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .arviointikeskusteluunOsallistuneet .value li:'+(i===0?'first':'last')+'-child .dropdown').setValue(v)
              })
              Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .suorituspaikka .value .dropdown').setValue(tiedot.suorituspaikka[0])
              Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .suorituspaikka .value input:not(.select)').setValue(tiedot.suorituspaikka[1])
              Page(tutkinnonOsaElement).getInput('.näyttö .modal-content .työssäoppimisenYhteydessä .value input').setValue(tiedot.työssäoppimisenYhteydessä)

              if (findSingle('.näyttö .modal-content button.vahvista', tutkinnonOsaElement)().prop('disabled')) {
                throw new Error('Invalid model')
              }
            })
          }
        },
        painaOkNäyttöModal: click(findSingle('.näyttö .modal-content button.vahvista', tutkinnonOsaElement)),
        poistaNäyttö: click(findSingle('.näyttö .remove-value', tutkinnonOsaElement))
      }, {}, Editor(tutkinnonOsaElement))
      return api
    },
    lisääTutkinnonOsa: function(hakusana) {
      return function() {
        return Page(uusiTutkinnonOsaElement).setInputValue(".dropdown, .autocomplete", hakusana)()
          .then(wait.forAjax)
      }
    },
    lisääKorkeakouluopintoja: function() {
      return click(subElement(uusiTutkinnonOsaElement, ('.korkeakouluopinto a')))()
    },
    lisääYhteistenTutkinnonOsienOsaAlueitaLukioOpintojaTaiMuitaJatkoOpintovalmiuksiaTukeviaOpintoja: function() {
      return click(subElement(uusiTutkinnonOsaElement, ('.jatkoopintovalmiuksiatukevienopintojensuoritus a')))()
    },
    lisääLukioOpinto: function(nimi) {
      return lisääPaikallinenTutkinnonOsa(nimi, '.lisaa-lukio-opinto-modal', '.lukio-opinto a')
    },
    lisääMuuOpintovalmiuksiaTukevaOpinto : function(nimi) {
      return lisääPaikallinenTutkinnonOsa(nimi, '.lisaa-muu-opintovalmiuksia-tukeva-opinto-modal', '.muu-opintovalmiuksia-tukeva-opinto a')
    },
    lisääPaikallinenTutkinnonOsa: function(nimi, tarkentavaSelector) {
      return lisääPaikallinenTutkinnonOsa(nimi, '.lisaa-paikallinen-tutkinnon-osa-modal', '.paikallinen-tutkinnon-osa a ' + (tarkentavaSelector || ''))
    },
    lisääTutkinnonOsaaPienempiKokonaisuus: function(tutkinto, liittyyTutkinnonOsaan, nimi) {
      return function() {
        var modalElement = subElement(uusiTutkinnonOsaElement, '.lisaa-paikallinen-tutkinnon-osa-modal')
        return click(subElement(uusiTutkinnonOsaElement, ('.paikallinen-tutkinnon-osa a span:contains(Lisää tutkinnon osaa pienemmän kokonaisuuden suoritus)')))()
          .then(Page(modalElement).setInputValue('.tutkinto .autocomplete', tutkinto))
          .then(wait.until(Page(modalElement).button(findSingle('.vahvista')).isDisabled))
          .then(Page(modalElement).setInputValue('.tutkinnon-osat .dropdown', liittyyTutkinnonOsaan))
          .then(wait.until(Page(modalElement).button(findSingle('.vahvista')).isDisabled))
          .then(Page(modalElement).setInputValue('input.paikallinen-koulutusmoduuli-nimi', nimi))
          .then(click(subElement(modalElement, 'button.vahvista:not(:disabled)')))
      }
    },
    lisääTutkinnonOsaToisestaTutkinnosta: function(tutkinto, nimi) {
      return function() {
        var modalElement = subElement(uusiTutkinnonOsaElement, '.osa-toisesta-tutkinnosta .modal')
        return click(subElement(uusiTutkinnonOsaElement, ('.osa-toisesta-tutkinnosta a')))()
          .then(Page(modalElement).setInputValue('.tutkinto .autocomplete', tutkinto))
          .then(Page(modalElement).setInputValue('.tutkinnon-osat .dropdown', nimi))
          .then(click(subElement(modalElement, 'button.vahvista:not(:disabled)')))
      }
    },
    isLisääTutkinnonOsaToisestaTutkinnostaVisible: function() {
      return isElementVisible(subElement(uusiTutkinnonOsaElement, ('.osa-toisesta-tutkinnosta a')))
    },
    tutkinnonosavaihtoehdot: function() {
      return Page(uusiTutkinnonOsaElement).getInputOptions(".dropdown")
    },
    laajuudenOtsikko: function() {
      return S('.suoritus-taulukko:eq(0) th.laajuus:eq(0)').text()
    },
    laajuudetYhteensä: function() {
      return S('.suoritus-taulukko:eq(0) .laajuudet-yhteensä').text()
    },
    isSuoritusTaulukkoVisible: function() {
      return S('.suoritus-taulukko:eq(0)').is(':visible')
    },
    valitseSuoritustapaTeksti: function() {
      return S('.osasuoritukset').text()
    }
  }
}

function LiittyyTutkinnonOsaan(property) {
  var tutkintoProperty = property.subProperty('.tutkinto')
  var tutkinnonOsaProperty = property.subProperty('.tutkinnon-osat')

  var api = {
    valitseTutkinto: function(tutkinto) {
      return tutkintoProperty.setValue(tutkinto)
    },
    valitseTutkinnonOsa: function(tutkinnonOsa) {
      return tutkinnonOsaProperty.setValue(tutkinnonOsa)
    }
  }
  return api
}

function VSTSuoritukset(prev) {
  var selectedOsasuoritus = prev
  var api = {
    lisääOsaamiskokonaisuus: function(hakusana) {
      return function () {
        return Page(findSingle('.lisaa-uusi-suoritus.vst-osaamiskokonaisuus', selectedOsasuoritus))
          .setInputValue(".dropdown, .autocomplete", hakusana)()
          .then(wait.forAjax)
      }
    },
    lisääSuuntautumisopinto: function(hakusana) {
      return function () {
        return Page(findSingle('.lisaa-uusi-suoritus.vst-suuntautumisopinnot', selectedOsasuoritus))
          .setInputValue(".dropdown, .autocomplete", hakusana)()
          .then(wait.forAjax)
      }
    },
    lisääMuuallaSuoritettuOpinto: function(hakusana) {
      return function () {
        return Page(findSingle('.lisaa-uusi-suoritus.vst-muutopinnot', selectedOsasuoritus))
          .setInputValue(".dropdown, .autocomplete", hakusana)()
          .then(wait.forAjax)
      }
    },
    lisääPaikallinen: function(nimi) {
      return function () {
        var modalElement = findSingle('.lisaa-paikallinen-vst-suoritus-modal', selectedOsasuoritus)
        return click(findSingle('.lisaa-paikallinen-suoritus a', selectedOsasuoritus))()
          .then(Page(modalElement).setInputValue('input', nimi))
          .then(click(subElement(modalElement, 'button.vahvista:not(:disabled)')))
      }
    },
    lisääLukutaitokoulutuksenKokonaisuus: function(hakusana) {
      return function () {
        return Page(findSingle('.lisaa-uusi-suoritus.vst-lukutaitokoulutuksenkokonaisuudensuoritus', selectedOsasuoritus))
          .setInputValue('.dropdown, .autocomplete', hakusana)()
          .then(wait.forAjax)
      }
    },
    selectOsasuoritus: function (nimi) {
      return function () {
        var osasuoritukset = selectedOsasuoritus ? selectedOsasuoritus.find('.vst-osasuoritus') : S('.vst-osasuoritus')

        var found

        osasuoritukset.each(function (i, e) {
          if (extractAsText(S(e).find('tr > td.suoritus > button.nimi')).includes(nimi)) {
            found = S(e)
          }
        })

        if (!found) {
          throw Error('Osasuoritus ' + nimi + ' not found')
        }
        return VSTSuoritukset(found)
      }
    }
  }
  return _.merge(api, Editor(S(selectedOsasuoritus)), Property(function () { return S(selectedOsasuoritus) }))
}

function IBSuoritukset() {
  var elem = findSingle('.ibtutkinnonsuoritus')

  var api = {
    suoritus: function(suoritusClass) {
      switch (suoritusClass) {
        case 'theoryOfKnowledge':
          return function() {
            var ee = Editor(elem)
            return {
              arvosana: ee.property('theoryOfKnowledge .arviointi'),
              predicted: ee.property('theoryOfKnowledge .predicted'),
              asOppiaine: Oppiaine('.theoryOfKnowledge')
            }
          }()
        case 'creativityActionService':
          return Editor(elem).property('creativityActionService')
        case 'extendedEssay':
          return function() {
            var ee = Editor(elem)
            return {
              arvosana: ee.property('extendedEssay .arviointi'),
              tunniste: ee.property('extendedEssay .tunniste'),
              taso: ee.property('extendedEssay .taso'),
              ryhmä: ee.property('extendedEssay .ryhmä'),
              aihe: ee.property('extendedEssay .aihe')
            }
          }()
        default:
          throw new Error('No such IB-suoritus: ' + suoritusClass)
      }
    }
  }
  return api
}

function Opiskeluoikeudet() {
  return {
    oppilaitokset: function() {
      return textsOf(S('.oppilaitos-list .oppilaitos-container h2.oppilaitos-title'))
    },
    opiskeluoikeustyypit: function() {
      return textsOf(S('.opiskeluoikeustyypit-nav .opiskeluoikeustyyppi'))
    },
    opiskeluoikeuksienMäärä: function() {
      return S('.opiskeluoikeuksientiedot .opiskeluoikeus').length
    },
    omatTiedotOpiskeluoikeuksienMäärä: function() {
      return S('.oppilaitos-list .oppilaitos-container .opiskeluoikeudet-list button.opiskeluoikeus-button').length
    },
    opiskeluoikeuksienOtsikot: function() {
      return textsOf(S('.opiskeluoikeuksientiedot .opiskeluoikeus h3 .otsikkotiedot'))
    },
    omatTiedotOpiskeluoikeuksienOtsikot: function() {
      return textsOf(S('.oppilaitos-list .oppilaitos-container .opiskeluoikeudet-list button.opiskeluoikeus-button h3'))
    },
    valitunVälilehdenAlaotsikot: function() {
      return textsOf(S('.opiskeluoikeustyypit-nav .selected .opiskeluoikeudet .opiskeluoikeus'))
    },
    valitseOpiskeluoikeudenTyyppi: function(tyyppi) {
      return function() {
        var tab = findSingle('.opiskeluoikeustyypit-nav .' + tyyppi)()
        if (!tab.hasClass('selected')) {
          return click(findSingle('a', tab))()
        }
      }
    },
    lisääOpiskeluoikeus: click(findSingle('.add-opiskeluoikeus a')),
    lisääOpiskeluoikeusEnabled: function() {
      return S('.add-opiskeluoikeus').is(':visible')
    }
  }
}

function Versiohistoria() {
  function elem() { return S('.versiohistoria') }
  function versiot() {
    return elem().find('.versionumero')
  }

  var api = {
    avaa: function () {
      if (!S('.versiohistoria > .modal').is(':visible')) {
        click(findSingle('> a', elem()))()
      }
      return wait.untilVisible(subElement(elem, 'li.selected'))().then(wait.forAjax)
    },
    sulje: function () {
      if (S('.versiohistoria > .modal').is(':visible')) {
        return click(findSingle('> a', elem()))()
      }
    },
    listaa: function() {
      return textsOf(versiot())
    },
    valittuVersio: function() {
      return elem().find('.selected').find('.versionumero').text()
    },
    valitse: function(versio) {
      return function() {
        click(findSingle('.versionumero:contains('+ versio +')', elem())().parent())()
        return wait.until(function() { return api.valittuVersio() == versio })()
      }
    }
  }
  return api
}

function TilaJaVahvistusIndeksillä(index = 0) {
  return TilaJaVahvistus(findSingle('.tila-vahvistus:eq(' +index +')'))
}

function TilaJaVahvistus(elem = findSingle('.tila-vahvistus')) {
  function merkitseValmiiksiButton() { return elem().find('button.merkitse-valmiiksi') }
  function merkitseKeskeneräiseksiButton() { return elem().find('button.merkitse-kesken') }

  var api = {
    merkitseValmiiksiEnabled: function() {
      return merkitseValmiiksiButton().is(':visible') && !merkitseValmiiksiButton().is(':disabled')
    },
    merkitseValmiiksi: seq(
      click(merkitseValmiiksiButton),
      wait.until(function() { return isElementVisible(S('.merkitse-valmiiksi-modal')) })
    ),
    merkitseKeskeneräiseksi: click(merkitseKeskeneräiseksiButton),
    text: function( ){
      return extractAsText(findSingle('.tiedot', elem()))
    },
    tila: function( ) {
      return extractAsText(findSingle('.tila', elem()))
    },
    merkitseValmiiksiDialog: MerkitseValmiiksiDialog(),
    lisääVahvistus: function(pvm) {
      var dialog = MerkitseValmiiksiDialog()
      var dialogEditor = dialog.editor
      return seq(dialogEditor.property('päivä').setValue(pvm),
        dialog.myöntäjät.itemEditor(0).setValue('Lisää henkilö'),
        dialog.myöntäjät.itemEditor(0).propertyBySelector('.nimi').setValue('Reijo Reksi'),
        dialog.myöntäjät.itemEditor(0).propertyBySelector('.titteli').setValue('rehtori'),
        wait.until(dialog.merkitseValmiiksiEnabled),
        dialog.merkitseValmiiksi)
    },
    isVisible: function() {
      return isElementVisible(elem)
    }
  }
  return api
}

function MerkitseValmiiksiDialog() {
  var elem = findSingle('.merkitse-valmiiksi-modal')
  var buttonElem = findSingle('button.vahvista', elem)
  var api = {
    merkitseValmiiksi: function( ) {
      if (buttonElem().is(':disabled')) throw new Error('disabled button')
      return click(buttonElem())()
    },
    merkitseValmiiksiEnabled: function() {
      return !buttonElem().is(':disabled')
    },
    peruuta: click(findSingle('.peruuta', elem)),
    organisaatio: OrganisaatioHaku(findSingle('.myöntäjäOrganisaatio', elem) ),
    editor: Editor(elem),
    myöntäjät: Editor(elem).property('myöntäjäHenkilöt'),
    lisääMyöntäjä: function(nimi, titteli) {
      return function() {
        return api.myöntäjät.itemEditor(0).setValue('Lisää henkilö')()
          .then(api.myöntäjät.itemEditor(0).propertyBySelector('.nimi').setValue(nimi))
          .then(api.myöntäjät.itemEditor(0).propertyBySelector('.titteli').setValue(titteli))
      }
    }
  }
  return api
}

function LisääSuoritusDialog() {
  var elem = findSingle('.lisaa-suoritus-modal')
  var buttonElem = findSingle('button.vahvista', elem)
  function link(text) { return findSingle(".add-suoritus a:contains(" + (text || '') + ")") }
  var api = _.merge({
    isLinkVisible: function(text) {
      return isElementVisible(link(text))
    },
    clickLink: function(text) {
      return click(link(text))
    },

    open: function(text) {
      return function() {
        if (!api.isVisible()) {
          return seq(api.clickLink(text), wait.until(api.isVisible))()
        }
      }
    },
    isVisible: function() {
      return isElementVisible(elem)
    },
    isEnabled: function() {
      return !buttonElem().is(':disabled')
    },
    lisääSuoritus: function() {
      if (!api.isEnabled()) throw new Error('button not enabled')
      function count() { return OpinnotPage().suoritusTabs().length }
      var prevCount = count()
      return Q()
        .then(click(buttonElem))
        .then(wait.until(function() { return count() == prevCount + 1 }))
    },
    selectSuoritustapa: function(suoritustapa) {
      return Page(elem).setInputValue('.suoritustapa .dropdown', suoritustapa)
    },
    selectTutkinto: function(name, selector) {
      selector = selector || '.koulutusmoduuli'
      return TutkintoSelector(function() { return elem().find(selector) }).select(name)
    },
    tutkinto: function() {
      return Page(elem).getInputValue('.koulutusmoduuli input, .tutkinto input')
    },
    toimipiste: OrganisaatioHaku(elem)
  }, {}, Editor(elem))
  return api
}

function TutkintoSelector(elem) {
  function selectedTutkinto() {
    return elem().find('.selected')
  }

  var api = {
    select: function (name) {
      return function () {
        return wait.until(Page(elem).getInput('input').isVisible)()
          .then(Page(elem).setInputValue('input', name))
          .then(wait.until(function () {
            return isElementVisible(selectedTutkinto())
          }))
          .then(click(selectedTutkinto))
      }
    }
  }
  return api
}

function Päivämääräväli(elem) {
  var api = {
    setAlku: function(value) {
      return function() {
        return Page(elem).setInputValue('.alku input', value)()
      }
    },
    getAlku: function() {
      return elem().find('.alku span.inline.date').text()
    },
    setLoppu: function(value) {
      return function() {
        return Page(elem).setInputValue('.loppu input', value)()
      }
    },
    isValid: function() {
      return !elem().find('.date-range').hasClass('error')
    }
  }
  return api
}

function OpiskeluoikeusDialog() {
  var elem = findSingle('.lisaa-opiskeluoikeusjakso-modal')
  var button = findSingle('button.vahvista', elem)
  return {
    tila: function() {
      var p = Property(function() {return findSingle('.lisaa-opiskeluoikeusjakso-modal')})
      p.aseta = function(tila) {
        return p.click('input[value="koskiopiskeluoikeudentila_' + tila + '"]')
      }
      return p
    },
    alkuPaiva: function() {
      return Property(findSingle('.property.alku', elem))
    },
    tallenna: click(button),
    peruuta: click(findSingle('.peruuta', elem)),
    isEnabled: function() {
      return !button().is(':disabled')
    },
    radioEnabled: function(value) {
      return !findSingle('input[value="koskiopiskeluoikeudentila_' + value + '"]', elem())().is(':disabled')
    },
    tilat: function() {
      return toArray(elem().find('.tila input[type="radio"]')).map(function(i) { return i.value })
    },
    opintojenRahoitus: function () {
      var p = Property(function() {return findSingle('.lisaa-opiskeluoikeusjakso-modal')})
      p.aseta = function(rahoituskoodi) {
        return p.click('input[value="opintojenrahoitus_' + rahoituskoodi + '"]')
      }
      return p
    }
  }
}

function Editor(elem) {
  var editButton = findSingle('.toggle-edit', elem)
  var enabledSaveButton = findSingle('#edit-bar button:not(:disabled)')
  var api = {
    isVisible: function() {
      return isElementVisible(elem)
    },
    edit: function() {
      return wait.until(api.isVisible)().then(function() {
        if (isElementVisible(editButton)) {
          return click(editButton)()
        }
      }).then(KoskiPage().verifyNoError)
    },
    canSave: function() {
      return isElementVisible(enabledSaveButton)
    },
    getEditBarMessage: function() {
      return findSingle('#edit-bar .state-indicator')().text()
    },
    saveChanges: seq(click(enabledSaveButton), KoskiPage().verifyNoError),
    saveChangesAndWaitForSuccess: seq(click(enabledSaveButton), KoskiPage().verifyNoError, wait.until(KoskiPage().isSavedLabelShown)),
    saveChangesAndExpectError: seq(click(enabledSaveButton), wait.until(KoskiPage().isErrorShown)),
    cancelChanges: seq(click(findSingle('#edit-bar .cancel')), KoskiPage().verifyNoError),
    isEditable: function() {
      return elem().find('.toggle-edit').is(':visible')
    },
    hasProperty: function(key) {
      return isElementVisible('.property.'+key+':eq(0)', elem)
    },
    property: function(key) {
      return Property(findSingle('.property.'+key+':eq(0)', elem))
    },
    propertyBySelector: function(selector) {
      return Property(findSingle(selector, elem))
    },
    subEditor: function(selector) {
      return Editor(findSingle(selector, elem))
    },
    isEditBarVisible: function() {
      return S("#edit-bar").hasClass("visible")
    },
    elem: elem
  }
  return api
}

function Property(elem) {
  if (typeof elem != 'function') throw new Error('elem has to be function')
  return _.merge({
    addValue: seq(click(findSingle('.add-value', elem)), KoskiPage().verifyNoError),
    isRemoveValueVisible: function() {
      return elem().find('.remove-value').is(':visible')
    },
    addItem: seq(click(findSingle('.add-item a', elem)), KoskiPage().verifyNoError),
    removeValue: seq(click(findSingle('.remove-value', elem)), KoskiPage().verifyNoError),
    removeFromDropdown: function(value) {
      var dropdownElem = findSingle('.dropdown', elem)
      return seq(
        click(findSingle('.select', dropdownElem)),
        wait.until(Page(dropdownElem).getInput('li:contains('+ value +')').isVisible),
        triggerEvent(findSingle('a.remove-value', dropdownElem), 'mousedown'),
        wait.forAjax
      )
    },
    removeItem: function(index) {
      return seq(click(findSingle('li:eq(' + index + ') .remove-item', elem)), KoskiPage().verifyNoError)
    },
    waitUntilLoaded: wait.until(function(){
      return elem().is(':visible') && !elem().find('.loading').is(':visible')
    }),
    selectValue: function(value) {
      return function() {
        return Page(elem).setInputValue('.dropdown', value.toString())().then(wait.forAjax)
      }
    },
    setValue: function(value, index) {
      return function() {
        return Page(elem).setInputValue('.dropdown, .editor-input, .autocomplete', value, index)()
      }
    },
    getLanguage: function() {
      return elem().find('.localized-string').attr('class').split(/\s+/).filter(function(c) { return ['fi', 'sv', 'en'].includes(c) }).join(' ')
    },
    toPäivämääräväli: function() {
      return Päivämääräväli(elem)
    },
    click: function(selector) {
      return seq(click(findSingle(selector, elem)), KoskiPage().verifyNoError)
    },
    getValue: function() {
      return elem().find('input').val() || elem().find('.value').text()
    },
    getText: function() {
      return extractAsText(elem())
    },
    itemEditor: function(index) {
      return this.propertyBySelector('.array > li:nth-child(' + (index + 1) +')')
    },
    getItems: function() {
      return toArray(elem().find('.value .array > li:not(.add-item)')).map(function(elem) { return Property(function() { return S(elem) })})
    },
    isVisible: function() {
      return isElementVisible(findSingle('.value', elem))
        || isElementVisible(findSingle('.dropdown', elem))
        || isElementVisible(findSingle('input', elem))
    },
    isValid: function() {
      return !elem().find('.error').is(':visible')
    },
    organisaatioValitsin: function() {
      return OrganisaatioHaku(elem)
    },
    getOptions: function() {
      return Page(elem).getInputOptions('.dropdown')
    },
    subProperty: function(selector) {
      return Property(findSingle(selector, elem))
    }
  }, Editor(elem))
}
