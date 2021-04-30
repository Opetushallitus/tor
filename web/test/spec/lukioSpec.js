describe('Lukiokoulutus', function( ){

  var page = KoskiPage()
  var opinnot = OpinnotPage()
  var editor = opinnot.opiskeluoikeusEditor()
  var addOppija = AddOppijaPage()
  before(Authentication().login(), resetFixtures)

  describe('Lukion päättötodistus', function() {
    before(page.openPage, page.oppijaHaku.searchAndSelect('020655-2479'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('lukiokoulutus'))
    describe('Oppijan suorituksissa', function() {
      it('näytetään', function() {
        expect(opinnot.getTutkinto()).to.equal("Lukion oppimäärä")
        expect(opinnot.getOppilaitos()).to.equal("Jyväskylän normaalikoulu")
      })
    })
    describe('Kaikki tiedot näkyvissä', function() {
      before(opinnot.expandAll)
      it('näyttää opiskeluoikeuden tiedot', function() {
        expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal('Opiskeluoikeuden voimassaoloaika : 1.9.2012 — 8.6.2016\n' +
          'Tila 8.6.2016 Valmistunut (valtionosuusrahoitteinen koulutus)\n' +
          '1.9.2012 Läsnä (valtionosuusrahoitteinen koulutus)\n' +
          'Lisätiedot\n' +
          'Syy alle 18-vuotiaana aloitettuun opiskeluun aikuisten lukiokoulutuksessa Pikkuvanha yksilö\n' +
          'Erityisen koulutustehtävän jaksot 1.9.2012 — 1.9.2013 Tehtävä Kieliin painottuva koulutus\n' +
          'Ulkomaanjaksot 1.9.2012 — 1.9.2013 Maa Ruotsi Kuvaus Harjoittelua ulkomailla\n' +
          'Oikeus maksuttomaan asuntolapaikkaan kyllä\n' +
          'Sisäoppilaitosmainen majoitus 1.9.2012 — 1.9.2013')
      })

      it('näyttää suorituksen tiedot', function() {
        expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
          'Koulutus Lukion oppimäärä 309902 60/011/2015\n' +
          'Opetussuunnitelma Lukio suoritetaan nuorten opetussuunnitelman mukaan\n' +
          'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
          'Suorituskieli suomi\n' +
          'Todistuksella näkyvät lisätiedot Ruotsin opinnoista osa hyväksiluettu Ruotsissa suoritettujen lukio-opintojen perusteella\n' +
          'Ryhmä 12A\n' +
          'Koulusivistyskieli suomi\n' +
          'Suoritus valmis Vahvistus : 8.6.2016 Jyväskylä Reijo Reksi , rehtori')
      })

      it('näyttää oppiaineiden ja kurssien arvosanat', function() {
        expect(extractAsText(S('.osasuoritukset'))).to.equal(
          'Oppiaine Laajuus (kurssia) Arvosana (keskiarvo)\n' +
          'Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus\n' +
          'ÄI1\n8 ÄI2\n8 ÄI3\n8 ÄI4\n8 ÄI5\n9 ÄI6\n9 ÄI8\n9 ÄI9\n9 8 9\n(8,5)\n' +
          'A1-kieli, englanti\nENA1\n10 ENA2\n10 ENA3\n9 ENA4\n9 ENA5\n9 ENA6\n8 ENA7\n8 ENA8\n9 ENA 10 *\nS 9 9\n(9,0)\n' +
          'B1-kieli, ruotsi\nRUB11\n9 RUB12\n8 RUB13\n7 RUB14\n7 RUB15\n6 5 7\n(7,4)\n' +
          'B3-kieli, latina\nLAB31\n9 LAB32\n8 2 9\n(8,5)\n' +
          'Matematiikka, pitkä oppimäärä\nMAA1 *\n9 MAA2\n10 MAA3\n8 MAA4\n10 MAA5\n7 MAA6\n9 MAA7\n8 MAA8\n7 MAA9\n9 MAA10\n8 MAA11\n8 MAA12\n10 MAA13\nH MAA14 *\n9 MAA16 *\n9 14 9\n(8,6)\n' +
          'Biologia\nBI1\n8 BI2\n9 BI3\n8 BI4\n9 BI5\n10 BI6 *\nS BI7 *\nS BI8 *\nS 7,5 9\n(8,8)\n' +
          'Maantieto\nGE1\n9 GE2\n7 2 8\n(8,0)\nFysiikka\nFY1\n8 FY2\n9 FY3\n9 FY4\n7 FY5\n8 FY6\n7 FY7\n8 FY8 *\n7 FY9 *\n7 FY10 *\nS FY11 *\nS FY12 *\nS FY13 *\nS 13 8\n(7,8)\n' +
          'Kemia\nKE1\n8 KE2\n9 KE3\n9 KE4\n5 KE5\n7 KE6 *\n5 KE7 *\nS KE8 *\nS 8 8\n(7,2)\n' +
          'Uskonto/Elämänkatsomustieto\nUE1\n8 UE2\n7 UE3\n8 3 8\n(7,7)\nFilosofia\nFI1\n8 1 8\n(8,0)\n' +
          'Psykologia\nPS1\n9 1 9\n(9,0)\n' +
          'Historia\nHI1\n7 HI2\n8 HI3\n7 HI4\n6 4 7\n(7,0)\n' +
          'Yhteiskuntaoppi\nYH1\n8 YH2\n8 2 8\n(8,0)\n' +
          'Liikunta\nLI1\n8 LI2\n9 LI12 *\nS 3 9\n(8,5)\n' +
          'Musiikki\nMU1\n8 1 8\n(8,0)\n' +
          'Kuvataide\nKU1\n8 KU2 *\n9 2 9\n(8,5)\n' +
          'Terveystieto\nTE1\n8 1 9\n(8,0)\n' +
          'Tanssi ja liike *\nITT1 *\n10 1 10\n(10,0)\n' +
          'Teemaopinnot\nMTA *\nS 1 S\n' +
          'Oman äidinkielen opinnot\nOA1 *\nS 1 S\n' +
          'Suoritettujen kurssien laajuus yhteensä: 89,5\n' +
          '* = paikallinen kurssi tai oppiaine')
      })
    })

    describe('Kurssin tiedot', function() {
      var kurssi = opinnot.oppiaineet.oppiaine('MA').kurssi('MAA16')
      before(page.openPage, page.oppijaHaku.searchAndSelect('020655-2479'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('lukiokoulutus'))
      describe('Kun klikataan', function() {
        before(kurssi.toggleDetails)
        it('näyttää kurssin tiedot', function() {
          expect(kurssi.detailsText()).to.equal(
            'Tunniste MAA16\n' +
            'Nimi Analyyttisten menetelmien lisäkurssi, ksy, vuositaso 2\n' +
            'Laajuus 1 kurssia\n' +
            'Kuvaus Kurssilla syvennetään kurssien MAA4, MAA5 ja MAA7 sisältöjä.\n' +
            'Kurssin tyyppi Syventävä\n' +
            'Arvosana 9\n' +
            'Arviointipäivä 4.6.2016')
        })
      })
      describe('Kun klikataan uudestaan', function() {
        before(kurssi.toggleDetails)
        it('piilottaa kurssin tiedot', function() {
          expect(kurssi.detailsText()).to.equal('')
        })
      })
      describe('Kaikkien kurssien tiedot', function() {
        it('voidaan avata yksitellen virheettömästi', function() {
          Kurssi.findAll().forEach(function(kurssi) {
            expect(kurssi.detailsText()).to.equal('')
            kurssi.toggleDetails()
            expect(kurssi.detailsText().length > 10).to.equal(true)
          })
        })
      })
    })

    describe('Tietojen muuttaminen', function() {
      describe('Suoritusten tiedot', function() {
        describe('Oppiaine', function() {
          before(editor.edit)

          var ai = opinnot.oppiaineet.oppiaine('oppiaine.AI')
          var kieli = ai.propertyBySelector('.title .properties')
          var arvosana = ai.propertyBySelector('td.arvosana')

          describe('Alkutila', function () {
            it('on oikein', function() {
              expect(editor.canSave()).to.equal(false)
              expect(kieli.getValue()).to.equal('Suomen kieli ja kirjallisuus')
              expect(arvosana.getValue()).to.equal('9')
            })
          })

          describe('Kieliaineen kielen muuttaminen', function() {
            before(kieli.selectValue('Ruotsin kieli ja kirjallisuus'))

            it('onnistuu', function() {
              expect(kieli.getValue()).to.equal('Ruotsin kieli ja kirjallisuus')
            })

            it('tallennus on mahdollista', function() {
              expect(editor.canSave()).to.equal(true)
            })
          })

          describe('Arvosanan muuttaminen', function () {
            before(arvosana.selectValue(8), editor.saveChanges, wait.until(page.isSavedLabelShown))

            it('onnistuu', function () {
              expect(findSingle('.oppiaine.AI .arvosana .annettuArvosana')().text()).to.equal('8')
            })
          })

          describe('Valtakunnallinen oppiaine', function() {
            var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine()
            var kotitalous = editor.subEditor('.oppiaine.KO:eq(0)')

            describe('Lisääminen', function () {
              before(
                editor.edit,
                uusiOppiaine.selectValue('Kotitalous')
              )

              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet'))).to.contain('Kotitalous')
              })

              it('arvosana vaaditaan kun päätason suoritus on merkitty valmiiksi', function () {
                expect(editor.canSave()).to.equal(false)
                expect(extractAsText(S('.oppiaineet'))).to.contain('Arvosana vaaditaan, koska päätason suoritus on merkitty valmiiksi.')
              })

              describe('Arvosanan kanssa', function () {
                before(
                  kotitalous.propertyBySelector('.arvosana').selectValue('9'),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('tallennus toimii', function () {
                  expect(extractAsText(S('.oppiaineet'))).to.contain('Kotitalous')
                })
              })
            })

            describe('Poistaminen', function () {
              before(
                editor.edit,
                kotitalous.propertyBySelector('.remove-row').removeValue,
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )

              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet'))).to.not.contain('Kotitalous')
              })
            })
          })

          describe('Paikallinen oppiaine', function() {
            before(editor.edit)

            var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine()
            var paikallinen = editor.subEditor('.oppiaine.oppiaine-rivi:last')

            it('alkutila', function() {
              expect(editor.canSave()).to.equal(false)
              expect(editor.getEditBarMessage()).to.equal('Ei tallentamattomia muutoksia')
              expect(S('.oppiaineet .oppiaine-rivi').length).to.equal(21)
            })

            describe('Lisääminen', function () {
              before(
                editor.edit,
                uusiOppiaine.selectValue('Lisää')
              )

              it('lisää oppiaineen', function () {
                expect(S('.oppiaineet .oppiaine-rivi').length).to.equal(22)
              })

              it('estää tallennuksen kunnes pakolliset tiedot on täytetty', function () {
                expect(editor.canSave()).to.equal(false)
                expect(editor.getEditBarMessage()).to.equal('Korjaa virheelliset tiedot.')
              })

              describe('Tiedot täytettynä', function () {
                before(
                  paikallinen.propertyBySelector('.koodi').setValue('PAI'),
                  paikallinen.propertyBySelector('.nimi').setValue('Paikallinen oppiaine'),
                  paikallinen.propertyBySelector('.kuvaus').setValue('Pakollinen kuvaus'),
                  paikallinen.propertyBySelector('.arvosana').selectValue(9),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown)
                )

                it('tallennus toimii', function () {
                  expect(extractAsText(S('.oppiaineet'))).to.contain('Paikallinen oppiaine')
                })
              })
            })

            describe('Poistaminen', function () {
              before(
                editor.edit,
                paikallinen.propertyBySelector('.remove-row').removeValue,
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )

              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet'))).to.not.contain('Paikallinen oppiaine')
              })
            })

            describe('Lisätty paikallinen oppiaine', function() {
              before(editor.edit)

              it('tallettuu organisaation preferenceihin', function() {
                expect(uusiOppiaine.getOptions()).to.contain('Paikallinen oppiaine')
              })

              after(editor.cancelChanges)
            })

            describe('Organisaation preferenceistä löytyvä aine', function() {
              describe('Lisääminen', function () {
                before(
                  editor.edit,
                  uusiOppiaine.selectValue('Paikallinen oppiaine'),
                  paikallinen.propertyBySelector('.arvosana').selectValue(9),
                  editor.saveChanges
                )

                it('toimii', function () {
                  expect(extractAsText(S('.oppiaineet'))).to.contain('Paikallinen oppiaine')
                })
              })
            })
          })

          describe('Oppiaineen kurssi', function() {
            before(
              editor.edit,
              editor.property('tila').removeItem(0),
              opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi
            )

            describe('Arvosanan muuttaminen', function() {
              var kurssi = opinnot.oppiaineet.oppiaine('MA').kurssi('MAA16')

              before(kurssi.arvosana.selectValue('6'), editor.saveChanges, wait.until(page.isSavedLabelShown))

              it('Toimii', function() {
                expect(kurssi.arvosana.getText()).to.equal('6')
              })
            })

            describe('Osaamisen tunnustaminen', function() {
              var kurssi = opinnot.oppiaineet.oppiaine('MA').kurssi('MAA16')

              before(
                editor.edit,
                kurssi.toggleDetails
              )

              describe('Alussa', function() {
                it('ei osaamisen tunnustamistietoa, näytetään lisäysmahdollisuus', function() {
                  expect(kurssi.tunnustettu.getValue()).to.equal('Lisää osaamisen tunnustaminen')
                })
              })

              describe('Lisääminen', function () {
                before(
                  kurssi.lisääTunnustettu,
                  kurssi.tunnustettu.propertyBySelector('.selite').setValue('Tunnustamisen esimerkkiselite'),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown),
                  kurssi.showDetails
                )

                it('toimii', function() {
                  expect(kurssi.tunnustettu.getText()).to.equal(
                    'Tunnustettu\nSelite Tunnustamisen esimerkkiselite\nRahoituksen piirissä ei'
                  )
                })
              })

              describe('Rahoituksen piirissä -tiedon lisääminen', function () {
                before(
                  editor.edit,
                  kurssi.toggleDetails,
                  kurssi.tunnustettu.property('rahoituksenPiirissä').setValue(true),
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown),
                  kurssi.showDetails
                )

                it('toimii', function() {
                  expect(kurssi.tunnustettu.getText()).to.equal(
                    'Tunnustettu\nSelite Tunnustamisen esimerkkiselite\nRahoituksen piirissä kyllä'
                  )
                })
              })

              describe('Poistaminen', function () {
                before(
                  editor.edit,
                  kurssi.toggleDetails,
                  kurssi.poistaTunnustettu,
                  editor.saveChanges,
                  wait.until(page.isSavedLabelShown),
                  editor.edit,
                  kurssi.toggleDetails
                )

                it('toimii', function() {
                  expect(kurssi.tunnustettu.getValue()).to.equal('Lisää osaamisen tunnustaminen')
                })

                after(editor.cancelChanges)
              })
            })

            describe('Lisääminen', function () {
              before(
                editor.edit,
                opinnot.oppiaineet.oppiaine('FI').lisääKurssi('FI2'),
                opinnot.oppiaineet.oppiaine('FI').kurssi('FI2').arviointi.click('.add-item a'),
                opinnot.oppiaineet.oppiaine('FI').kurssi('FI2').arvosana.selectValue('9'),
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )

              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet .FI'))).to.contain('FI2')
              })
            })

            describe('Poistaminen', function () {
              var ai1 = ai.kurssi('ÄI1')

              before(
                editor.edit,
                ai1.poistaKurssi,
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )

              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet .AI'))).to.not.contain('ÄI1')
              })
            })
          })
        })
      })

      describe('Päätason suorituksen poistaminen', function() {
        before(editor.edit)

        describe('Mitätöintilinkki', function() {
          it('Ei näytetä', function() {
            expect(opinnot.deletePäätasonSuoritusIsShown()).to.equal(false)
          })
        })
      })
    })
  })

  describe('Lukion oppiaineen oppimäärän suoritus', function() {
    before(page.openPage, page.oppijaHaku.searchAndSelect('210163-2367'))

    describe('Oppijan suorituksissa', function() {
      it('näytetään', function() {
        expect(opinnot.getTutkinto()).to.equal("Historia")
        expect(opinnot.getOppilaitos()).to.equal("Jyväskylän normaalikoulu")
      })
    })

    describe('Kaikki tiedot näkyvissä', function () {
      it('näyttää opiskeluoikeuden tiedot', function() {
        expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal(
          'Opiskeluoikeuden voimassaoloaika : 1.9.2015 — 10.1.2016\n' +
          'Tila 10.1.2016 Valmistunut (valtionosuusrahoitteinen koulutus)\n' +
          '1.9.2015 Läsnä (valtionosuusrahoitteinen koulutus)')
      })

      it('näyttää suorituksen tyypin opiskeluoikeuden otsikossa', function () {
        expect(S('.opiskeluoikeus h3 .koulutus').text()).to.equal('Lukion oppiaineen oppimäärä')
      })

      it('näyttää suorituksen tiedot', function() {
        expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
          'Oppiaine Historia HI 60/011/2015\n' +
          'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
          'Arviointi 9\n' +
          'Suorituskieli suomi\n' +
          'Suoritus valmis Vahvistus : 10.1.2016 Jyväskylä Reijo Reksi , rehtori')
      })

      it('näyttää oppiaineiden ja kurssien arvosanat', function() {
        expect(extractAsText(S('.osasuoritukset'))).to.equal(
          'Oppiaine Laajuus (kurssia) Arvosana (keskiarvo)\n' +
          'Historia\n' +
          'HI1\n7 HI2\n8 HI3\n7 HI4\n6 4 9\n(7,0)')
      })
    })

    describe('Tietojen muuttaminen', function() {
      describe('Suoritusten tiedot', function () {
        describe('Oppiaine', function () {
          before(editor.edit)

          var hi = opinnot.oppiaineet.oppiaine('oppiaine.HI')
          var arvosana = hi.propertyBySelector('td.arvosana')

          describe('Alkutila', function () {
            it('on oikein', function () {
              expect(editor.canSave()).to.equal(false)
              expect(arvosana.getValue()).to.equal('9')
            })
          })

          describe('Arvosanan muuttaminen', function () {
            before(arvosana.selectValue(8), editor.saveChanges, wait.until(page.isSavedLabelShown))

            it('onnistuu', function () {
              expect(findSingle('.oppiaine.HI .arvosana .annettuArvosana')().text()).to.equal('8')
            })
          })

          describe('Oppiaineen', function () {
            before(editor.edit)

            it('lisääminen ei ole mahdollista', function () {
              expect(findSingle('.uusi-oppiaine')).to.throw(Error, /not found/)
            })

            it('poistaminen ei ole mahdollista', function () {
              expect(findSingle('.oppiaine-rivi > .remove-row')).to.throw(Error, /not found/)
            })
          })

          describe('Oppiaineen kurssi', function () {
            before(
              editor.edit,
              editor.property('tila').removeItem(0),
              opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi
            )

            describe('Arvosanan muuttaminen', function () {
              var kurssi = opinnot.oppiaineet.oppiaine('HI').kurssi('HI4')

              before(kurssi.arvosana.selectValue('10'), editor.saveChanges, wait.until(page.isSavedLabelShown))

              it('Toimii', function () {
                expect(kurssi.arvosana.getText()).to.equal('10')
              })
            })

            describe('Lisääminen', function () {
              before(
                editor.edit,
                opinnot.oppiaineet.oppiaine('HI').lisääKurssi('HI5'),
                opinnot.oppiaineet.oppiaine('HI').kurssi('HI5').arviointi.click('.add-item a'),
                opinnot.oppiaineet.oppiaine('HI').kurssi('HI5').arvosana.selectValue('9'),
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )

              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet .HI'))).to.contain('HI5')
              })
            })

            describe('Poistaminen', function () {
              var hi1 = hi.kurssi('HI1')

              before(
                editor.edit,
                hi1.poistaKurssi,
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )

              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet .HI'))).to.not.contain('HI1')
              })
            })
          })
        })
      })
    })
  })

  describe('Lukion useamman oppiaineen aineopiskelija', function() {
    describe('Opiskeluoikeuden tilaa', function () {
      before(
        page.openPage,
        page.oppijaHaku.searchAndSelect('200300-624E'),
        editor.edit,
        opinnot.valitseSuoritus(undefined, 'Kemia'),
        opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi,
        opinnot.valitseSuoritus(undefined, 'Historia'),
        opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi,
        opinnot.avaaLisaysDialogi
      )

      it('ei voida merkitä valmiiksi', function () {
        expect(OpiskeluoikeusDialog().radioEnabled('valmistunut')).to.equal(false)
      })

      describe('Kun yksikin suoritus merkitään valmiiksi', function () {
        before(
          opinnot.tilaJaVahvistus.merkitseValmiiksi,
          opinnot.tilaJaVahvistus.lisääVahvistus('01.01.2000'),
          opinnot.avaaLisaysDialogi,
          OpiskeluoikeusDialog().tila().aseta('valmistunut'),
          OpiskeluoikeusDialog().opintojenRahoitus().aseta('1'),
          OpiskeluoikeusDialog().tallenna,
          editor.saveChanges,
        )

        it('myös opiskeluoikeuden tila voidaan merkitä valmiiksi', function () {
          expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.contain('Valmistunut')
        })
      })
      after(editor.cancelChanges)
    })

    describe('Jos opiskelijalla on "ei tiedossa"-oppiaineita', function () {
      before(
        page.openPage,
        page.oppijaHaku.searchAndSelect('151132-746V'),
        editor.edit,
        opinnot.opiskeluoikeusEditor().edit,
        opinnot.avaaLisaysDialogi
      )

      it('Opiskeluoikeuden tilaa ei voi merkitä valmiiksi', function () {
        expect(OpiskeluoikeusDialog().radioEnabled('valmistunut')).to.equal(false)
      })
      after(editor.cancelChanges)
    })
  })

  describe('Opiskeluoikeuden lisääminen', function() {
    describe('Lukion oppimäärä', function() {
      describe('Nuorten 2015 oppimäärä', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOppimäärä('Lukion oppimäärä'),
          addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Lukion oppimäärä')
        )

        describe('Lisäyksen jälkeen', function () {
          describe('Opiskeluoikeuden tiedot', function() {
            it('näytetään oikein', function () {
              expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Lukion oppimäärä')
              expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('60/011/2015')
              expect(editor.propertyBySelector('.oppimäärä').getValue()).to.equal('Lukio suoritetaan nuorten opetussuunnitelman mukaan')
              expect(editor.propertyBySelector('.toimipiste').getValue()).to.equal('Ressun lukio')
              expect(opinnot.getSuorituskieli()).to.equal('suomi')
            })
          })

          describe('Oppiaineita', function () {
            before(editor.edit)
            it('ei esitäytetä', function () {
              expect(S('.oppiaineet').text()).to.equal('')
            })
            after(editor.cancelChanges)
          })
        })
      })

      describe('Aikuisten 2015 oppimäärä', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOppimäärä('Lukion oppimäärä'),
          addOppija.selectPeruste('70/011/2015'),
          addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Lukion oppimäärä')
        )

        describe('Lisäyksen jälkeen', function () {
          describe('Opiskeluoikeuden tiedot', function() {
            it('näytetään oikein', function () {
              expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Lukion oppimäärä')
              expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('70/011/2015')
              expect(editor.propertyBySelector('.oppimäärä').getValue()).to.equal('Lukio suoritetaan aikuisten opetussuunnitelman mukaan')
              expect(editor.propertyBySelector('.toimipiste').getValue()).to.equal('Ressun lukio')
              expect(opinnot.getSuorituskieli()).to.equal('suomi')
            })
          })
        })
      })

      describe('Nuorten 2003 oppimäärä', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOppimäärä('Lukion oppimäärä'),
          addOppija.selectPeruste('33/011/2003'),
          addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Lukion oppimäärä')
        )

        describe('Lisäyksen jälkeen', function () {
          describe('Opiskeluoikeuden tiedot', function() {
            it('näytetään oikein', function () {
              expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Lukion oppimäärä')
              expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('33/011/2003')
              expect(editor.propertyBySelector('.oppimäärä').getValue()).to.equal('Lukio suoritetaan nuorten opetussuunnitelman mukaan')
              expect(editor.propertyBySelector('.toimipiste').getValue()).to.equal('Ressun lukio')
              expect(opinnot.getSuorituskieli()).to.equal('suomi')
            })
          })
        })
      })

      describe('Aikuisten 2004 oppimäärä', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOppimäärä('Lukion oppimäärä'),
          addOppija.selectPeruste('4/011/2004'),
          addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Lukion oppimäärä')
        )

        describe('Lisäyksen jälkeen', function () {
          describe('Opiskeluoikeuden tiedot', function() {
            it('näytetään oikein', function () {
              expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Lukion oppimäärä')
              expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('4/011/2004')
              expect(editor.propertyBySelector('.oppimäärä').getValue()).to.equal('Lukio suoritetaan aikuisten opetussuunnitelman mukaan')
              expect(editor.propertyBySelector('.toimipiste').getValue()).to.equal('Ressun lukio')
              expect(opinnot.getSuorituskieli()).to.equal('suomi')
            })
          })
        })
      })
    })

    describe('Lukion oppiaineen oppimäärä', function() {
      describe('Nuorten 2015 oppimäärä', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOppimäärä('Lukion oppiaineen oppimäärä')
        )

        it('Ei-tiedossa oppiaine on valittavissa', function() {
          expect(addOppija.oppiaineet()).to.contain('Ei tiedossa')
        })

        describe('Lisäyksen jälkeen', function () {
          before(
            addOppija.selectOppiaine('Biologia'),
            addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Biologia')
          )

          describe('Opiskeluoikeuden tiedot', function() {
            it('näytetään oikein', function () {
              expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Biologia')
              expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('60/011/2015')
              expect(editor.propertyBySelector('.toimipiste').getValue()).to.equal('Ressun lukio')
              expect(opinnot.getSuorituskieli()).to.equal('suomi')
            })
          })

          describe('Oppiaine', function () {
            it('näytetään oikein', function () {
              expect(extractAsText(S('.oppiaineet'))).to.equal('' +
                'Oppiaine Laajuus (kurssia) Arvosana (keskiarvo)\n' +
                'Biologia 0 -'
              )
            })
          })
        })
      })

      describe('Aikuisten 2015 oppimäärä', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOppimäärä('Lukion oppiaineen oppimäärä'),
          addOppija.selectPeruste('70/011/2015')
        )

        it('Ei-tiedossa oppiaine on valittavissa', function() {
          expect(addOppija.oppiaineet()[0]).to.equal('Ei tiedossa')
        })

        describe('Lisäyksen jälkeen', function () {
          before(
            addOppija.selectOppiaine('Biologia'),
            addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Biologia')
          )
          describe('Opiskeluoikeuden tiedot', function() {
            it('näytetään oikein', function () {
              expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Biologia')
              expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('70/011/2015')
              expect(editor.propertyBySelector('.toimipiste').getValue()).to.equal('Ressun lukio')
              expect(opinnot.getSuorituskieli()).to.equal('suomi')
            })
          })
        })
      })

      describe('Nuorten 2003 oppimäärä', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOppimäärä('Lukion oppiaineen oppimäärä'),
          addOppija.selectPeruste('33/011/2003'),
          addOppija.selectOppiaine('Biologia'),
          addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Biologia')
        )

        describe('Lisäyksen jälkeen', function () {
          describe('Opiskeluoikeuden tiedot', function() {
            it('näytetään oikein', function () {
              expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Biologia')
              expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('33/011/2003')
              expect(editor.propertyBySelector('.toimipiste').getValue()).to.equal('Ressun lukio')
              expect(opinnot.getSuorituskieli()).to.equal('suomi')
            })
          })
        })
      })

      describe('Aikuisten 2004 oppimäärä', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOppimäärä('Lukion oppiaineen oppimäärä'),
          addOppija.selectPeruste('4/011/2004'),
          addOppija.selectOppiaine('Biologia'),
          addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Biologia')
        )

        describe('Lisäyksen jälkeen', function () {
          describe('Opiskeluoikeuden tiedot', function() {
            it('näytetään oikein', function () {
              expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Biologia')
              expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('4/011/2004')
              expect(editor.propertyBySelector('.toimipiste').getValue()).to.equal('Ressun lukio')
              expect(opinnot.getSuorituskieli()).to.equal('suomi')
            })
          })
        })
      })

      describe('Päätason suorituksen poistaminen', function () {
        before(
          Authentication().logout,
          Authentication().login(),
          page.openPage,
          page.oppijaHaku.searchAndSelect("200300-624E"),
          editor.edit
        )

        describe('Mitätöintilinkki', function() {
          it('Näytetään', function() {
            expect(opinnot.deletePäätasonSuoritusIsShown()).to.equal(true)
          })

          describe('Painettaessa', function() {
            before(opinnot.deletePäätasonSuoritus)
            it('Pyydetään vahvistus', function() {
              expect(opinnot.confirmDeletePäätasonSuoritusIsShown()).to.equal(true)
            })

            describe('Painettaessa uudestaan', function() {
              before(opinnot.confirmDeletePäätasonSuoritus, wait.until(page.isPäätasonSuoritusDeletedMessageShown))
              it('Päätason suoritus poistetaan', function() {
                expect(page.isPäätasonSuoritusDeletedMessageShown()).to.equal(true)
              })

              describe('Poistettua päätason suoritusta', function() {
                before(wait.until(page.isReady), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('lukiokoulutus'))

                it('Ei näytetä', function () {
                  expect(opinnot.suoritusTabs()).to.deep.equal(['Kemia', 'Filosofia'])
                })
              })
            })
          })
        })
      })
    })

    describe('Opintojen rahoitus', function() {
      before(
        prepareForNewOppija('kalle', '230872-7258'),
        addOppija.enterValidDataLukio()
      )

      it('kenttä näytetään', function() {
        expect(addOppija.rahoitusIsVisible()).to.equal(true)
      })

      it('vaihtoehtoina on ainoastaan "valtionosuusrahoitteinen koulutus" ja "muuta kautta rahoitettu"', function() {
        expect(addOppija.opintojenRahoitukset()).to.deep.equal(['Valtionosuusrahoitteinen koulutus', 'Muuta kautta rahoitettu'])
      })

      describe('Valtionosuusrahoitteinen koulutus', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOpintojenRahoitus('Valtionosuusrahoitteinen koulutus'),
          addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Lukion oppimäärä')
        )

        it('on mahdollista lisätä', function() {
          expect(opinnot.getOpiskeluoikeudenTila()).to.match(/Läsnä \(valtionosuusrahoitteinen koulutus\)$/)
        })
      })

      describe('Muuta kautta rahoitettu', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataLukio(),
          addOppija.selectOpintojenRahoitus('Muuta kautta rahoitettu'),
          addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Lukion oppimäärä')
        )

        it('on mahdollista lisätä', function() {
          expect(opinnot.getOpiskeluoikeudenTila()).to.match(/Läsnä \(muuta kautta rahoitettu\)$/)
        })
      })
    })
  })
})
