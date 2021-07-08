describe('VST', function () {
  var opinnot = OpinnotPage()
  var vst = VSTSuoritukset()
  var editor = opinnot.opiskeluoikeusEditor()
  var addOppija = AddOppijaPage()
  var page = KoskiPage()

  describe('Opiskeluoikeuden lisääminen oppivelollisten suorituksella', function () {
    before(
      prepareForNewOppija('kalle', '230872-7258'),
      addOppija.enterValidDataVSTKOPS(),
      addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille')
    )

    it('toimii', function () {
      expect(opinnot.getTutkinto()).to.equal('Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille')
      expect(opinnot.getOppilaitos()).to.equal('Varsinais-Suomen kansanopisto')
      expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('OPH-58-2021')
      expect(extractAsText(S('.tunniste-koodiarvo'))).to.equal('999909')
    })

    describe('Osasuoritusten lisäys', function () {
      describe('Osaamiskokonaisuus', function () {
        before(
          editor.edit,
          vst.lisääOsaamiskokonaisuus('Aktiivinen kansalaisuus'),
          editor.saveChanges
        )

        it('voidaan lisätä', function () {
          expect(extractAsText(S('.suoritus-taulukko'))).to.include('Aktiivinen kansalaisuus')
        })

        describe('Opintokokonaisuus osasuorituksena', function () {
          before(
            editor.edit,
            opinnot.avaaKaikki,
            function () {
              return vst.selectOsasuoritus('Aktiivinen kansalaisuus')().lisääPaikallinen('Katutaide')()
            },
            function () {
              return vst.selectOsasuoritus('Katutaide')().property('laajuus').setValue(20)()
            },
            editor.saveChanges,
            opinnot.avaaKaikki
          )

          it('voidaan lisätä', function () {
            expect(extractAsText(S('.suoritus-taulukko'))).to.include('Katutaide 20 op')
          })
        })
      })
      describe('Suuntautumisopinto', function () {
        before(
          editor.edit,
          vst.lisääSuuntautumisopinto('Valinnaiset suuntautumisopinnot'),
          editor.saveChanges
        )

        it('voidaan lisätä', function () {
          expect(extractAsText(S('.suoritus-taulukko'))).to.include('Valinnaiset suuntautumisopinnot')
        })

        describe('Muualla suoritettu opinto osasuorituksena', function () {
          before(
            editor.edit,
            opinnot.avaaKaikki,
            function () {
              return vst.selectOsasuoritus('Valinnaiset suuntautumisopinnot')().lisääMuuallaSuoritettuOpinto('Lukio-opinnot')()
            },
            function () {
              return vst.selectOsasuoritus('Lukio-opinnot')().property('laajuus').setValue(30)()
            },
            function () {
              return vst.selectOsasuoritus('Lukio-opinnot')().property('kuvaus').setValue('Lukio kuvaus')()
            },
            editor.saveChanges,
            opinnot.avaaKaikki
          )

          it('voidaan lisätä', function () {
            expect(extractAsText(S('.suoritus-taulukko'))).to.include('Lukio-opinnot 30 op')
          })
        })
        describe('Opintokokonaisuus osasuorituksena', function () {
          before(
            editor.edit,
            opinnot.avaaKaikki,
            function () {
              return vst.selectOsasuoritus('Valinnaiset suuntautumisopinnot')().lisääPaikallinen('Valinnainen matematiikka')()
            },
            function () {
              return vst.selectOsasuoritus('Valinnainen matematiikka')().property('laajuus').setValue(5)()
            },
            editor.saveChanges,
            opinnot.avaaKaikki
          )

          it('voidaan lisätä', function () {
            expect(extractAsText(S('.suoritus-taulukko'))).to.include('Valinnainen matematiikka 5 op')
          })
        })
      })

      describe('Suoritusten yhteislaajuus', function () {
        it('lasketaan oikein', function () {
          expect(extractAsText(S('.yhteislaajuus'))).to.equal('Yhteensä 55 op')
        })
      })
    })
  })

  describe('Kotoutuskoulutus', function () {
    before(
      Authentication().login(),
      page.openPage,
      page.oppijaHaku.searchAndSelect('260769-598H'),
      opinnot.avaaKaikki
    )

    it('kielisuorituksen arvioinnin taitotasot näkyvät', function () {
      expect(extractAsText(S('.kuullunYmmärtämisenTaitotaso'))).to.equal('Kuullun ymmärtämisen taitotaso Taso C2.2')
      expect(extractAsText(S('.puhumisenTaitotaso'))).to.equal('Puhumisen taitotaso Taso C2.2')
      expect(extractAsText(S('.luetunYmmärtämisenTaitotaso'))).to.equal('Luetun ymmärtämisen taitotaso Taso C2.2')
      expect(extractAsText(S('.kirjoittamisenTaitotaso'))).to.equal('Kirjoittamisen taitotaso Taso C2.2')
    })
  })

  describe('Opiskeluoikeuden lisääminen lukutaito koulutuksella', function () {
    before(
      prepareForNewOppija('kalle', '230872-7258'),
      addOppija.enterValidDataVSTLukutaito(),
      addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Kansanopistojen vapaan sivistystyön lukutaitokoulutus')
    )

    it('toimii', function () {
      expect(opinnot.getTutkinto()).to.equal('Kansanopistojen vapaan sivistystyön lukutaitokoulutus')
      expect(opinnot.getOppilaitos()).to.equal('Varsinais-Suomen kansanopisto')
      expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('OPH-2984-2017')
      expect(extractAsText(S('.tunniste-koodiarvo'))).to.equal('999911')
    })

    describe('Osasuoritusten lisäys', function () {
      before(
        editor.edit,
        vst.lisääLukutaitokoulutuksenKokonaisuus('Vapaan sivistystyön lukutaitokoulutuksen numeeristen taitojen suoritus'),
        function () {
          return vst.selectOsasuoritus('Vapaan sivistystyön lukutaitokoulutuksen numeeristen taitojen suoritus')().property('laajuus').setValue(5)()
        },
        function () {
          return vst.selectOsasuoritus('Vapaan sivistystyön lukutaitokoulutuksen numeeristen taitojen suoritus')().propertyBySelector('.arvosana').selectValue('Hyväksytty')()
        }
      )

      it('toimii', function () {
        expect(extractAsText(S('.vst-osasuoritus'))).to.include('Vapaan sivistystyön lukutaitokoulutuksen numeeristen taitojen suoritus 5 Hyväksytty')
      })
    })
  })
})
