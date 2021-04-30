describe('Lukioon valmistava koulutus', function() {

  var page = KoskiPage()
  var opinnot = OpinnotPage()
  var editor = opinnot.opiskeluoikeusEditor()

  before(Authentication().login(), resetFixtures, page.openPage, page.oppijaHaku.searchAndSelect('211007-442N'))

  describe('Oppijan suorituksissa', function() {
    it('näytetään', function() {
      expect(opinnot.getTutkinto()).to.equal("Lukiokoulutukseen valmistava koulutus")
      expect(opinnot.getOppilaitos()).to.equal("Jyväskylän normaalikoulu")
    })
  })

  describe('Kaikki tiedot näkyvissä', function() {
    before(opinnot.expandAll)

    it('näyttää opiskeluoikeuden tiedot', function() {
      expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal(
        'Opiskeluoikeuden voimassaoloaika : 15.8.2008 — 4.6.2016\n' +
        'Tila 4.6.2016 Valmistunut (valtionosuusrahoitteinen koulutus)\n' +
        '15.8.2008 Läsnä (valtionosuusrahoitteinen koulutus)\n' +
        'Lisätiedot\n' +
        'Pidennetty päättymispäivä kyllä\n' +
        'Ulkomaanjaksot 1.9.2012 — 1.9.2013 Maa Ruotsi Kuvaus Harjoittelua ulkomailla\n' +
        'Oikeus maksuttomaan asuntolapaikkaan kyllä\n' +
        'Sisäoppilaitosmainen majoitus 1.9.2013 — 12.12.2013')
    })

    it('näyttää suorituksen tiedot', function() {
      expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
        'Koulutus Lukiokoulutukseen valmistava koulutus 999906 56/011/2015\n' +
        'Laajuus 2 kurssia\n' +
        'Opetussuunnitelma Lukio suoritetaan nuorten opetussuunnitelman mukaan\n' +
        'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
        'Suorituskieli suomi\n' +
        'Suoritus valmis Vahvistus : 4.6.2016 Jyväskylä Reijo Reksi , rehtori')
    })

    it('näyttää oppiaineiden ja kurssien arvosanat', function() {
      expect(extractAsText(S('.osasuoritukset'))).to.equal(
        'Lukioon valmistavat opinnot\n' +
        'Oppiaine Laajuus (kurssia) Arvosana (keskiarvo)\n' +
        'Äidinkieli ja kirjallisuus, Suomi toisena kielenä ja kirjallisuus\n' +
        'LVS1\n' +
        'S 2 S\n' +
        'Perusopetuksessa alkanut kieli, ruotsi\n' +
        'LVKA1\n' +
        'S 1 S\n' +
        'Matemaattiset ja luonnontieteelliset opinnot\n' +
        'LVLUMA1\n' +
        'S 1 S\n' +
        'Yhteiskuntatietous ja kulttuurintuntemus\n' +
        'LVHY1\n' +
        'S 1 S\n' +
        'Opinto-ohjaus\n' +
        'LVOP1\n' +
        'S 1 S\n' +
        'Tietojenkäsittely *\n' +
        'ATK1 *\n' +
        'S 1 S\n' +
        'Suoritettujen kurssien laajuus yhteensä: 7\n' +
        '* = paikallinen kurssi tai oppiaine\n' +
        'Valinnaisena suoritetut lukiokurssit\n' +
        'Oppiaine Laajuus (kurssia) Arvosana (keskiarvo)\n' +
        'A1-kieli, englanti\n' +
        'ENA1\n' +
        '8 1 S\n' +
        '(8,0)\n' +
        'Suoritettujen kurssien laajuus yhteensä: 1'
      )
    })
  })

  describe('Kurssin tiedot', function() {
    var kurssi = opinnot.oppiaineet.oppiaine('LVMALUO').kurssi('LVLUMA1')
    describe('Kun klikataan', function() {
      before(kurssi.toggleDetails)
      it('näyttää kurssin tiedot', function() {
        expect(kurssi.detailsText()).to.equal(
          'Nimi Matematiikkaa, fysiikkaa ja kemiaa\n' +
          'Laajuus 1 kurssia\n' +
          'Arvosana S\n' +
          'Arviointipäivä 4.6.2016'
        )
      })
    })
  })

  describe('Tietojen muuttaminen', function() {
    before(page.openPage, page.oppijaHaku.searchAndSelect('211007-442N'))

    describe('Suoritusten tiedot', function () {
      describe('Kun päätason suoritus on merkitty valmiiksi', function () {
        var aine = opinnot.oppiaineet.oppiaine('oppiaine.A1')
        var aineenArvosana = aine.propertyBySelector('td.arvosana')

        before(
          editor.edit,
          aineenArvosana.selectValue('Ei valintaa')
        )

        it('arvosana vaaditaan kun päätason suoritus on merkitty valmiiksi', function () {
          expect(editor.canSave()).to.equal(false)
          expect(extractAsText(S('.oppiaineet'))).to.contain('Arvosana vaaditaan, koska päätason suoritus on merkitty valmiiksi.')
        })

        after(
          aineenArvosana.selectValue('S'),
          editor.saveChanges,
          wait.until(page.isSavedLabelShown)
        )
      })

      describe('Oppiaine', function () {
        before(
          editor.edit,
          editor.property('tila').removeItem(0),
          opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi
        )

        var valmistavaAi = opinnot.oppiaineet.oppiaine('oppiaine.LVAIK')
        var valmistavaAiArvosana = valmistavaAi.propertyBySelector('td.arvosana')
        var valmistavaAiKieli = valmistavaAi.propertyBySelector('.title .properties')

        var valinnainenEn = opinnot.oppiaineet.oppiaine('oppiaine.A1')
        var valinnainenEnArvosana = valinnainenEn.propertyBySelector('td.arvosana')
        var valinnainenEnKieli = valinnainenEn.propertyBySelector('.title .properties')

        describe('Alkutila', function () {
          it('on oikein', function () {
            expect(valmistavaAiArvosana.getValue()).to.equal('S')
            expect(valinnainenEnArvosana.getValue()).to.equal('S')
          })
        })

        describe('Kieliaineen kielen muuttaminen', function() {
          describe('valmistavalle äidinkielelle', function () {
            before(valmistavaAiKieli.selectValue('Ruotsi toisena kielenä ja kirjallisuus'))
            it('toimii', function() {
              expect(valmistavaAiKieli.getValue()).to.equal('Ruotsi toisena kielenä ja kirjallisuus')
              expect(editor.canSave()).to.equal(true)
            })
          })

          describe('valinnaiselle vieraalle kielelle', function () {
            before(valinnainenEnKieli.selectValue('portugali'))
            it('toimii', function() {
              expect(valinnainenEnKieli.getValue()).to.equal('portugali')
              expect(editor.canSave()).to.equal(true)
            })
          })

          describe('tallennus', function () {
            before(editor.saveChanges, wait.until(page.isSavedLabelShown))
            it('toimii', function () {
              expect(findSingle('.oppiaine.LVAIK .title .nimi')().text()).to.equal('Äidinkieli ja kirjallisuus, Ruotsi toisena kielenä ja kirjallisuus')
              expect(findSingle('.oppiaine.A1 .title .nimi')().text()).to.equal('A1-kieli, portugali')
            })
          })
        })

        describe('Arvosanan muuttaminen', function () {
          describe('valmistavalle aineelle', function () {
            before(
              editor.edit,
              valmistavaAiArvosana.selectValue(8),
              editor.saveChanges,
              wait.until(page.isSavedLabelShown)
            )

            it('toimii', function() {
              expect(findSingle('.oppiaine.LVAIK .arvosana .annettuArvosana')().text()).to.equal('8')
            })
          })

          describe('valinnaiselle aineelle', function () {
            before(
              editor.edit,
              valinnainenEnArvosana.selectValue(9),
              editor.saveChanges,
              wait.until(page.isSavedLabelShown)
            )

            it('onnistuu', function() {
              expect(findSingle('.oppiaine.A1 .arvosana .annettuArvosana')().text()).to.equal('9')
            })
          })

        })

        describe('Valmistava oppiaine', function () {
          var valmistavatSelector = '.lukioon-valmistavat-opinnot'
          var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine(valmistavatSelector)
          var mat = editor.subEditor(valmistavatSelector + ' .oppiaine.LVMALUO:eq(0)')

          describe('Valtakunnallisen valmistavan oppiaineen', function () {
            describe('poistaminen', function () {
              before(
                editor.edit,
                mat.propertyBySelector('.remove-row').removeValue,
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )

              it('toimii', function () {
                expect(extractAsText(S(valmistavatSelector))).to.not.contain('Matemaattiset ja luonnontieteelliset opinnot')
              })
            })

            describe('lisääminen', function () {
              before(
                editor.edit,
                uusiOppiaine.selectValue('Matemaattiset ja luonnontieteelliset opinnot')
              )

              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet'))).to.contain('Matemaattiset ja luonnontieteelliset opinnot')
              })
            })

            describe('paikallisen kurssin', function () {
              describe('lisääminen', function () {
                before(
                  editor.edit,
                  opinnot.oppiaineet.oppiaine('LVAIK').lisääPaikallinenKurssi(),
                  opinnot.oppiaineet.oppiaine('LVAIK').kurssi('PA').arvosana.selectValue('9'),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('toimii', function () {
                  expect(extractAsText(S('.oppiaineet .LVAIK'))).to.contain('PA')
                })
              })

              describe('arvosanan muuttaminen', function () {
                var kurssi = opinnot.oppiaineet.oppiaine('LVAIK').kurssi('PA')

                before(
                  editor.edit,
                  kurssi.arvosana.selectValue('5'),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('toimii', function () {
                  expect(kurssi.arvosana.getText()).to.equal('5')
                })
              })

              describe('poistaminen', function () {
                var pa = valmistavaAi.kurssi('PA')

                before(
                  editor.edit,
                  pa.poistaKurssi,
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('toimii', function () {
                  expect(extractAsText(S('.oppiaineet .LVAIK'))).to.not.contain('PA')
                })
              })
            })

            describe('valtakunnallisen kurssin', function() {
              describe('kurssivaihtoehdot', function() {
                var lvyhku = opinnot.oppiaineet.oppiaine('LVYHKU')
                before(
                  editor.edit,
                  lvyhku.avaaLisääKurssiDialog
                )
                it('näytetään vain oikean oppiaineen kurssit', function () {
                  expect(lvyhku.lisääKurssiDialog.kurssit()).to.deep.equal(['LVHY2 Suomen yhteiskunta ja kulttuurit',
                    'Lisää paikallinen kurssi...'])
                })
                after(
                  lvyhku.lisääKurssiDialog.sulje,
                  editor.cancelChanges
                )
              })

              describe('lisääminen', function () {
                var lvopo = opinnot.oppiaineet.oppiaine('LVOPO')
                before(
                  editor.edit,
                  lvopo.avaaLisääKurssiDialog,
                  lvopo.lisääKurssiDialog.valitseKurssi('LVOP2'),
                  lvopo.lisääKurssiDialog.lisääKurssi,
                  lvopo.kurssi('LVOP2').arvosana.setValue('S'),
                  editor.saveChanges
                )
                it('Kurssin tiedot näytetään oikein', function() {
                  expect(lvopo.text()).to.equal('Opinto-ohjaus\nLVOP1\nS LVOP2\nS 2 S')
                })
              })
            })
          })
        })

        describe('Valinnainen oppiaine', function () {
          describe('Valtakunnallisen valinnaisen oppiaineen', function () {
            var valinnaisetSelector = '.valinnaisena-suoritetut-lukiokurssit'
            var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine(valinnaisetSelector)
            var kotitalous = editor.subEditor(valinnaisetSelector + ' .oppiaine.KO:eq(0)')

            describe('lisääminen', function () {
              before(
                editor.edit,
                uusiOppiaine.selectValue('Kotitalous'),
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )

              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet'))).to.contain('Kotitalous')
              })
            })

            describe('poistaminen', function () {
              before(
                editor.edit,
                kotitalous.propertyBySelector('.remove-row').removeValue,
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )

              it('toimii', function () {
                expect(extractAsText(S(valinnaisetSelector))).to.not.contain('Kotitalous')
              })
            })

            describe('valtakunnallisen kurssin', function () {
              describe('arvosanan muuttaminen', function () {
                var kurssi = opinnot.oppiaineet.oppiaine('A1').kurssi('ENA1')

                before(
                  editor.edit,
                  kurssi.arvosana.selectValue('6'),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('toimii', function () {
                  expect(kurssi.arvosana.getText()).to.equal('6')
                })
              })

              describe('lisääminen', function () {
                before(
                  editor.edit,
                  opinnot.oppiaineet.oppiaine('A1').lisääKurssi('ENA2'),
                  opinnot.oppiaineet.oppiaine('A1').kurssi('ENA2').arviointi.click('.add-item a'),
                  opinnot.oppiaineet.oppiaine('A1').kurssi('ENA2').arvosana.selectValue('9'),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('toimii', function () {
                  expect(extractAsText(S('.oppiaineet .A1'))).to.contain('ENA2')
                })
              })

              describe('poistaminen', function () {
                var ena1 = valinnainenEn.kurssi('ENA2')

                before(
                  editor.edit,
                  ena1.poistaKurssi,
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('toimii', function () {
                  expect(extractAsText(S('.oppiaineet .A1'))).to.not.contain('ENA2')
                })
              })
            })

            describe('paikallisen kurssin', function () {
              describe('arvosanan muuttaminen', function () {
                var kurssi = opinnot.oppiaineet.oppiaine('A1').kurssi('ENA1')

                before(
                  editor.edit,
                  kurssi.arvosana.selectValue('5'),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('toimii', function () {
                  expect(kurssi.arvosana.getText()).to.equal('5')
                })
              })

              describe('lisääminen', function () {
                before(
                  editor.edit,
                  opinnot.oppiaineet.oppiaine('A1').lisääPaikallinenKurssi(),
                  opinnot.oppiaineet.oppiaine('A1').kurssi('PA').arviointi.click('.add-item a'),
                  opinnot.oppiaineet.oppiaine('A1').kurssi('PA').arvosana.selectValue('9'),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('toimii', function () {
                  expect(extractAsText(S('.oppiaineet .A1'))).to.contain('PA')
                })
              })

              describe('poistaminen', function () {
                var pa = opinnot.oppiaineet.oppiaine('A1').kurssi('PA')

                before(
                  editor.edit,
                  pa.poistaKurssi,
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('toimii', function () {
                  expect(extractAsText(S('.oppiaineet .A1'))).to.not.contain('PA')
                })
              })
            })
          })
        })
      })
    })
  })

})
