describe('Perusopetus', function() {
  var page = KoskiPage()
  var login = LoginPage()
  var opinnot = OpinnotPage()
  var tilaJaVahvistus = opinnot.tilaJaVahvistus
  var addOppija = AddOppijaPage()
  var opiskeluoikeus = OpiskeluoikeusDialog()
  var editor = opinnot.opiskeluoikeusEditor()
  var currentDate = new Date()
  function currentDatePlusYears(years) {return currentDate.getDate() + '.' + (1 + currentDate.getMonth()) + '.' + (currentDate.getFullYear() + years)}
  var currentDateStr = currentDatePlusYears(0)
  var date2017Str = '1.1.2017'
  var date2018Str = '1.1.2018'
  var date2019Str = '1.1.2019'

  before(Authentication().login(), resetFixtures)

  describe('Perusopetuksen lukuvuositodistukset ja päättötodistus', function() {
    before(page.openPage, page.oppijaHaku.searchAndSelect('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'))

    it('näyttää opiskeluoikeuden tiedot', function() {
      expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal('Opiskeluoikeuden voimassaoloaika : 15.8.2008 — 4.6.2016\n' +
        'Tila 4.6.2016 Valmistunut\n' +
        '15.8.2008 Läsnä'
      )

      expect(opinnot.opiskeluoikeudet.valitunVälilehdenAlaotsikot()).to.deep.equal([
        'Perusopetuksen oppimäärä 2008—2016, Valmistunut'
      ])
    })

    describe('Perusopetuksen oppimäärä', function() {
      describe('Kaikki tiedot näkyvissä BrowserStack', function() {
        before(opinnot.expandAll)
        it('näyttää suorituksen tiedot', function() {
          expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal('Koulutus Perusopetus 201101 104/011/2014\n' +
            'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
            'Suoritustapa Koulutus\n' +
            'Suorituskieli suomi\n' +
            'Koulusivistyskieli suomi\n' +
            'Suoritus valmis Vahvistus : 4.6.2016 Jyväskylä Reijo Reksi , rehtori')
        })
        it('näyttää oppiaineiden arvosanat', function() {
          expect(extractAsText(S('.oppiaineet'))).to.equal(
            'Oppiaineiden arvosanat\n' +
            'Arvostelu 4-10, S (suoritettu) tai H (hylätty)\n' +
            'Pakolliset oppiaineet\n' +
            'Oppiaine Arvosana\n' +
            'Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9\n' +
            'Suoritustapa Erityinen tutkinto\n' +
            'B1-kieli, ruotsi 8\n' +
            'A1-kieli, englanti 8\n' +
            'Uskonto/Elämänkatsomustieto 10\n' +
            'Uskonnon oppimäärä Ortodoksinen uskonto\n' +
            'Historia 8\n' +
            'Yhteiskuntaoppi 10\n' +
            'Matematiikka 9\n' +
            'Kemia 7\n' +
            'Fysiikka 9\n' +
            'Biologia 9 *\n' +
            'Maantieto 9\n' +
            'Musiikki 7\n' +
            'Kuvataide 8\n' +
            'Kotitalous 8\n' +
            'Terveystieto 8\n' +
            'Käsityö 9\n' +
            'Liikunta 9 **\n' +
            'Valinnaiset oppiaineet\n' +
            'Oppiaine Arvosana Laajuus\n' +
            'B1-kieli, ruotsi S 1 vuosiviikkotuntia\n' +
            'Kotitalous S 1 vuosiviikkotuntia\n' +
            'Liikunta S 0,5 vuosiviikkotuntia\n' +
            'B2-kieli, saksa 9 4 vuosiviikkotuntia\n' +
            'Tietokoneen hyötykäyttö 9\n' +
            'Kuvaus Kurssilla tarjotaan yksityiskohtaisempaa tietokoneen, oheislaitteiden sekä käyttöjärjestelmän ja ohjelmien tuntemusta.\n' +
            '* = yksilöllistetty oppimäärä, ** = painotettu opetus'
          )
        })

        describe('Kun suoritus on kesken', function() {
          before(editor.edit, editor.property('tila').removeItem(0), opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi, editor.saveChanges)

          it('arvosanoja ei näytetä', function () {
            expect(isElementVisible(S('.oppiaineet .arvosana'))).to.equal(false)
          })

          describe('Kun vahvistus on tulevaisuudessa', function () {
            var future = new Date().getDate() + '.' + (1 + new Date().getMonth()) + '.' + (new Date().getFullYear() + 1)
            before(editor.edit, tilaJaVahvistus.merkitseValmiiksi, opinnot.tilaJaVahvistus.lisääVahvistus(future), editor.saveChanges)

            it('arvosanat näytetään', function () {
              expect(isElementVisible(S('.oppiaineet .arvosana'))).to.equal(true)
            })
          })
          after(resetFixtures, page.openPage, page.oppijaHaku.searchAndSelect('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'))
        })
      })
    })

    describe('Lukuvuosisuoritus 8. luokka', function() {
      before(wait.until(page.isOppijaSelected('Kaisa')), opinnot.valitseSuoritus(undefined, '8. vuosiluokka'))
      describe('Kaikki tiedot näkyvissä', function() {
        before(opinnot.expandAll)
        it('näyttää suorituksen tiedot', function() {
          expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
            'Luokka-aste 8. vuosiluokka 8 104/011/2014\n' +
            'Luokka 8C\n' +
            'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
            'Alkamispäivä 15.8.2014\n' +
            'Suoritustapa Koulutus\n' +
            'Suorituskieli suomi\n' +
            'Muut suorituskielet sloveeni\n' +
            'Kielikylpykieli ruotsi\n' +
            'Suoritus valmis Vahvistus : 30.5.2015 Jyväskylä Reijo Reksi , rehtori\n' +
            'Siirretään seuraavalle luokalle')
        })
        it('näyttää oppiaineiden arvosanat', function() {
          expect(extractAsText(S('.oppiaineet'))).to.equal(
            'Oppiaineiden arvosanat\n' +
            'Arvostelu 4-10, S (suoritettu) tai H (hylätty)\n' +
            'Pakolliset oppiaineet\n' +
            'Oppiaine Arvosana\n' +
            'Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9\n' +
            'Suoritustapa Erityinen tutkinto\n' +
            'B1-kieli, ruotsi 8\n' +
            'A1-kieli, englanti 8\n' +
            'Uskonto/Elämänkatsomustieto 10\n' +
            'Uskonnon oppimäärä Ortodoksinen uskonto\n' +
            'Historia 8\n' +
            'Yhteiskuntaoppi 10\n' +
            'Matematiikka 9\n' +
            'Kemia 7\n' +
            'Fysiikka 9\n' +
            'Biologia 9 *\n' +
            'Maantieto 9\n' +
            'Musiikki 7\n' +
            'Kuvataide 8\n' +
            'Kotitalous 8\n' +
            'Terveystieto 8\n' +
            'Käsityö 9\n' +
            'Liikunta 9 **\n' +
            'Valinnaiset oppiaineet\n' +
            'Oppiaine Arvosana Laajuus\n' +
            'B1-kieli, ruotsi S 1 vuosiviikkotuntia\n' +
            'Kotitalous S 1 vuosiviikkotuntia\n' +
            'Liikunta S 0,5 vuosiviikkotuntia\n' +
            'B2-kieli, saksa 9 4 vuosiviikkotuntia\n' +
            'Tietokoneen hyötykäyttö 9\n' +
            'Kuvaus Kurssilla tarjotaan yksityiskohtaisempaa tietokoneen, oheislaitteiden sekä käyttöjärjestelmän ja ohjelmien tuntemusta.\n' +
            'Käyttäytymisen arviointi\n' +
            'Arvosana S\n' +
            'Sanallinen arviointi Esimerkillistä käyttäytymistä koko vuoden ajan\n' +
            '* = yksilöllistetty oppimäärä, ** = painotettu opetus'
          )
        })
      })
    })

    describe('Lukuvuosisuoritus 9. luokka', function() {
      before(wait.until(page.isOppijaSelected('Kaisa')), opinnot.valitseSuoritus(undefined, '9. vuosiluokka'))
      describe('Kaikki tiedot näkyvissä', function() {
        before(opinnot.expandAll)
        it('näyttää suorituksen tiedot', function() {
          expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
            'Luokka-aste 9. vuosiluokka 9 104/011/2014\n' +
            'Luokka 9C\n' +
            'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
            'Alkamispäivä 15.8.2015\n' +
            'Suorituskieli suomi\n' +
            'Suoritus valmis Vahvistus : 30.5.2016 Jyväskylä Reijo Reksi , rehtori')
        })
      })
      describe('Lukuvuositodistus', function() {
        it('ei näytetä', function() {
          expect(S('a.todistus').is(':visible')).to.equal(false)
        })
      })
    })

    describe('Luokalle jäänyt 7-luokkalainen', function() {
      before(wait.until(page.isOppijaSelected('Kaisa')), opinnot.valitseSuoritus(undefined, '7. vuosiluokka'))
      describe('Kaikki tiedot näkyvissä', function() {
        before(opinnot.expandAll)
        it('näyttää suorituksen tiedot', function() {
          expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
            'Luokka-aste 7. vuosiluokka 7 104/011/2014\n' +
            'Luokka 7C\n' +
            'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
            'Alkamispäivä 16.8.2013\n' +
            'Suorituskieli suomi\n' +
            'Suoritus valmis Vahvistus : 30.5.2014 Jyväskylä Reijo Reksi , rehtori\n' +
            'Ei siirretä seuraavalle luokalle')
        })
      })
    })

    describe('Päättötodistus toiminta-alueittain', function() {
      before(resetFixtures, Authentication().login(), page.openPage, page.oppijaHaku.searchAndSelect('031112-020J'))
      describe('Kaikki tiedot näkyvissä', function() {
        before(opinnot.expandAll)

        it('näyttää opiskeluoikeuden tiedot', function() {
          expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal('Opiskeluoikeuden voimassaoloaika : 15.8.2008 — 4.6.2016\n' +
            'Tila 4.6.2016 Valmistunut\n' +
            '15.8.2008 Läsnä\n' +
            'Lisätiedot\n' +
            'Perusopetuksen aloittamista lykätty kyllä\n' +
            'Pidennetty oppivelvollisuus 15.8.2008 — 4.6.2016\n' +
            'Tukimuodot Osa-aikainen erityisopetus\n' +
            'Erityisen tuen päätös 15.8.2008 — 4.6.2016\n' +
            'Opiskelee toiminta-alueittain kyllä\n' +
            'Opiskelee erityisryhmässä kyllä\n' +
            'Erityisen tuen päätökset 15.8.2008 — 4.6.2016\n' +
            'Opiskelee toiminta-alueittain kyllä\n' +
            'Opiskelee erityisryhmässä kyllä\n' +
            'Tehostetun tuen päätökset 15.8.2008 — 4.6.2016\n' +
            'Tukimuodot Tukiopetus\n' +
            'Joustava perusopetus 15.8.2008 — 4.6.2016\n' +
            'Kotiopetusjaksot 15.8.2008 — 4.6.2016\n' +
            '14.7.2017 — 18.10.2017\n' +
            'Ulkomaanjaksot 15.8.2008 — 4.6.2016\n' +
            '16.9.2018 — 2.10.2019\n' +
            'Vuosiluokkiin sitomaton opetus kyllä\n' +
            'Muu kuin vaikeimmin kehitysvammainen 14.8.2010 —\n' +
            'Vaikeimmin kehitysvammainen 14.8.2010 —\n' +
            'Majoitusetu 15.8.2008 — 4.6.2016\n' +
            'Kuljetusetu 15.8.2008 — 4.6.2016\n' +
            'Oikeus maksuttomaan asuntolapaikkaan 15.8.2008 — 4.6.2016\n' +
            'Sisäoppilaitosmainen majoitus 1.9.2012 — 1.9.2013\n' +
            'Koulukoti 1.9.2013 — 1.9.2014')
        })

        it('näyttää suorituksen tiedot', function() {
          expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
            'Koulutus Perusopetus 201101 104/011/2014\n' +
            'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
            'Suoritustapa Erityinen tutkinto\n' +
            'Suorituskieli suomi\n' +
            'Suoritus valmis Vahvistus : 4.6.2016 Jyväskylä Reijo Reksi , rehtori')
        })
        it('näyttää oppiaineiden arvosanat', function() {
          expect(extractAsText(S('.oppiaineet'))).to.equal('Toiminta-alueiden arvosanat\n' +
            'Arvostelu 4-10, S (suoritettu) tai H (hylätty)\n' +
            'Toiminta-alue Arvosana\n' +
            'motoriset taidot S\n' +
            'Sanallinen arviointi Motoriset taidot kehittyneet hyvin perusopetuksen aikana\n' +
            'kieli ja kommunikaatio S\n' +
            'sosiaaliset taidot S\n' +
            'päivittäisten toimintojen taidot S\n' +
            'kognitiiviset taidot S')
        })
      })

      describe('Tietojen muuttaminen', function() {
        describe('Oppiaineet', function() {
          var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine()
          var sosiaalisetTaidot = editor.subEditor('.3')

          describe('Poistaminen', function () {
            before(
              editor.edit,
              opinnot.expandAll,
              editor.property('erityisenTuenPäätös').removeValue,
              sosiaalisetTaidot.propertyBySelector('>tr:first-child').removeValue,
              editor.saveChanges,
              wait.until(page.isSavedLabelShown)
            )

            it('toimii', function () {
              expect(extractAsText(S('.oppiaineet'))).to.not.contain('Filosofia 8')
            })

            describe('Lisääminen', function() {
              before(editor.edit, uusiOppiaine.selectValue('sosiaaliset taidot'), sosiaalisetTaidot.propertyBySelector('.arvosana').selectValue('8'), editor.saveChanges, wait.until(page.isSavedLabelShown))
              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet'))).to.contain('sosiaaliset taidot 8')
              })
            })
          })
        })

        describe('Vuosiluokan suorituksen lisäys', function() {
          var lisääSuoritus = opinnot.lisääSuoritusDialog
          before(
            editor.edit,
            editor.property('tila').removeItem(0),
            lisääSuoritus.open('lisää vuosiluokan suoritus'),
            lisääSuoritus.property('luokka').setValue('1a'),
            lisääSuoritus.toimipiste.select('Jyväskylän normaalikoulu, alakoulu'),
            lisääSuoritus.property('alkamispäivä').setValue(currentDateStr),
            lisääSuoritus.lisääSuoritus
          )
          it('Esitäyttää toiminta-alueet', function() {
            expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal(['motoriset taidot',
              'kieli ja kommunikaatio',
              'sosiaaliset taidot',
              'päivittäisten toimintojen taidot',
              'kognitiiviset taidot'])
          })

        })
      })

      /*
      describe('Deprekoidut kentät', function() {
        before(
          resetFixtures,
          page.openPage,
          page.oppijaHaku.searchAndSelect('031112-020J'),
          opinnot.expandAll, editor.edit, editor.property('opiskeleeToimintaAlueittain').setValue(false)
        )

        it('muokkaus estetty', function() {
          expect(extractAsText(findSingle('.lisätiedot .erityisenTuenPäätös .propertyError'))).to.equal('Käytä korvaavaa kenttää Erityisen tuen päätökset')
          expect(editor.canSave()).to.equal(false)
        })

        describe('Jos ei sisällä dataa', function() {
          before(
            page.openPage,
            page.oppijaHaku.searchAndSelect('220109-784L'),
            editor.edit,
            wait.untilVisible(findSingle('.lisätiedot .expandable')),
            opinnot.expandAll
          )

          it('piilotetaan', function() {
            var lisätiedot = textsOf(S('.lisätiedot .property .label'))
            expect(lisätiedot.includes('Erityisen tuen päätös')).to.equal(false)
            expect(lisätiedot.includes('Erityisen tuen päätökset')).to.equal(true)
            expect(lisätiedot.includes('Tehostetun tuen päätös')).to.equal(false)
            expect(lisätiedot.includes('Tehostetun tuen päätökset')).to.equal(true)
          })
        })
      })
      */

      describe('Ilman luottamuksellinen-roolia', function() {
        before(Authentication().logout, Authentication().login('epäluotettava-tallentaja'))
        before(page.openPage, page.oppijaHaku.searchAndSelect('031112-020J'))
        it('näyttää suorituksen tiedot', function() {
          expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
            'Koulutus Perusopetus 201101 104/011/2014\n' +
            'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
            'Suoritustapa Erityinen tutkinto\n' +
            'Suorituskieli suomi\n' +
            'Suoritus valmis Vahvistus : 4.6.2016 Jyväskylä Reijo Reksi , rehtori')
        })
      })
    })

    describe('Virhetilanteet', function() {
      describe('Todistuksen avaaminen, kun käyttäjä ei ole kirjautunut', function() {
        before(Authentication().logout, reloadTestFrame, wait.until(login.isVisible))
        it('Näytetään login-sivu', function() {
          expect(login.isVisible()).to.equal(true)
        })
      })

      describe('Todistuksen avaaminen, kun todistusta ei löydy', function() {
        before(Authentication().login(), page.openPage, openPage('/koski/1010101010', page.is404))
        it('Näytetään 404-sivu', function() {} )
      })
    })
  })

  describe('Aikuisten perusopetus', function() {
    before(page.openPage, page.oppijaHaku.searchAndSelect('280598-2415'))

    it('näyttää opiskeluoikeuden tiedot', function() {
      expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal('Opiskeluoikeuden voimassaoloaika : 15.8.2008 — 4.6.2016\n' +
        'Tila 4.6.2016 Valmistunut (valtionosuusrahoitteinen koulutus)\n' +
        '15.8.2008 Läsnä (valtionosuusrahoitteinen koulutus)')
    })

    describe('Muuttunut hetu', function() {
      before(page.openPage, page.oppijaHaku.searchAndSelect('280598-326W', '280598-2415'))

      it('hakee opiskeluoikeuden tiedot', function() {
        expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal('Opiskeluoikeuden voimassaoloaika : 15.8.2008 — 4.6.2016\n' +
          'Tila 4.6.2016 Valmistunut (valtionosuusrahoitteinen koulutus)\n' +
          '15.8.2008 Läsnä (valtionosuusrahoitteinen koulutus)')
      })
    })

    describe('Päättövaiheen opinnot', function() {
      before(opinnot.expandAll)
      it('näyttää suorituksen tiedot', function() {
        expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal('Koulutus Aikuisten perusopetuksen oppimäärä 201101 OPH-1280-2017\n' +
          'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
          'Suoritustapa Erityinen tutkinto\n' +
          'Suorituskieli suomi\n' +
          'Oman äidinkielen opinnot Arvosana 8\n' +
          'Kieli saame, lappi\n' +
          'Laajuus 1 kurssia\n'  +
          'Suoritus valmis Vahvistus : 4.6.2016 Jyväskylä Reijo Reksi , rehtori')
      })
      it('näyttää oppiaineiden arvosanat', function() {
        expect(extractAsText(S('.oppiaineet'))).to.equal(
          'Oppiaineiden arvosanat\n' +
          'Arvostelu 4-10, S (suoritettu) tai H (hylätty)\n' +
          'Pakolliset oppiaineet\n' +
          'Oppiaine Arvosana\n' +
          'Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9\nÄI1\n9 ÄI2\n9 ÄI3\n9 ÄI4\n4 ÄI10\n9\n' +
          'B1-kieli, ruotsi 8\n' +
          'A1-kieli, englanti 8\n' +
          'Uskonto/Elämänkatsomustieto 10\n' +
          'Uskonnon oppimäärä Ortodoksinen uskonto\n' +
          'Historia 8\n' +
          'Yhteiskuntaoppi 10\n' +
          'Matematiikka 9\n' +
          'Kemia 7\n' +
          'Fysiikka 9\n' +
          'Biologia 9\n' +
          'Maantieto 9\n' +
          'Musiikki 7\n' +
          'Kuvataide 8\n' +
          'Kotitalous 8\n' +
          'Terveystieto 8\n' +
          'Käsityö 9\n' +
          'Liikunta 9\n' +
          'Valinnaiset oppiaineet\n' +
          'Oppiaine Arvosana Laajuus\n' +
          'B1-kieli, ruotsi S 1 vuosiviikkotuntia\n' +
          'Kotitalous S 1 vuosiviikkotuntia\n' +
          'Liikunta S 0,5 vuosiviikkotuntia\n' +
          'B2-kieli, saksa 9 4 vuosiviikkotuntia\n' +
          'Tietokoneen hyötykäyttö 9\n' +
          'Kuvaus Kurssilla tarjotaan yksityiskohtaisempaa tietokoneen, oheislaitteiden sekä käyttöjärjestelmän ja ohjelmien tuntemusta.'
        )
      })

      describe('Tietojen muuttaminen', function() {
        describe('Kurssin lisääminen', function() {
          var äidinkieli = opinnot.oppiaineet.oppiaine(0)
          var a1 = opinnot.oppiaineet.oppiaine('A1')
          describe('Valtakunnallinen kurssi', function() {
            before(
              editor.edit,
              äidinkieli.avaaLisääKurssiDialog
            )
            describe('Ennen kurssin lisäämistä', function() {
              it('Voidaan lisätä vain ne kurssit, joita ei vielä ole lisätty', function() {
                expect(äidinkieli.lisääKurssiDialog.kurssit()).to.include('ÄI5 Puhe- ja vuorovaikutustaidot')
                expect(äidinkieli.lisääKurssiDialog.kurssit()).not.to.include('ÄI1 Suomen kielen ja kirjallisuuden perusteet')
              })
            })
            describe('Kun lisätään kurssi', function() {
              before(
                äidinkieli.lisääKurssiDialog.valitseKurssi('Puhe- ja vuorovaikutustaidot'),
                äidinkieli.lisääKurssiDialog.lisääKurssi
              )

              describe('Ennen arvosanan syöttöä', function() {
                it('Tallentaminen ei ole mahdollista ja virheilmoitus näytetään (koska päätason suoritus on valmis)', function() {
                  expect(editor.canSave()).to.equal(false)
                  expect(äidinkieli.errorText()).to.equal('Arvosana vaaditaan, koska päätason suoritus on merkitty valmiiksi.')
                })
              })

              describe('Kun annetaan arvosana ja tallennetaan', function() {
                before(
                  äidinkieli.kurssi('ÄI5').arviointi.click('.add-item a'),
                  äidinkieli.kurssi('ÄI5').arvosana.setValue('8'),
                  editor.saveChanges
                )
                it('Kurssin tiedot näytetään oikein', function() {
                  expect(äidinkieli.text()).to.equal('Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9\nÄI1\n9 ÄI2\n9 ÄI3\n9 ÄI4\n4 ÄI10\n9 ÄI5\n8')
                })

                describe('Kurssin poistaminen', function() {
                  before(
                    editor.edit,
                    äidinkieli.kurssi('ÄI5').poistaKurssi,
                    editor.saveChanges
                  )

                  it('Toimii', function() {
                    expect(äidinkieli.text()).to.equal('Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9\nÄI1\n9 ÄI2\n9 ÄI3\n9 ÄI4\n4 ÄI10\n9')
                  })
                })
              })

              describe('Kun päätason suoritus on KESKEN-tilassa', function() {
                before(
                  editor.edit,
                  äidinkieli.lisääKurssi('Puhe- ja vuorovaikutustaidot'),
                  editor.property('tila').removeItem(0),
                  opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi
                )
                describe('Kun oppiaineen suoritus on VALMIS-tilassa', function() {
                  it('Keskeneräista kurssisuoritusta ei voi tallentaa', function() {
                    expect(editor.canSave()).to.equal(false)
                  })
                })
                describe('Kun oppiaineen suoritus on KESKEN-tilassa', function() {
                  before(äidinkieli.arvosana.setValue('Ei valintaa'))
                  it('Keskeneräisen kurssisuorituksen voi tallentaa', function() {
                    expect(editor.canSave()).to.equal(true)
                  })
                })
              })
            })
          })

          describe('Äidinkielen kurssivalikoima', function() {
            function testaaÄidinkielenKurssivaihtoehdot(oppimäärä, odotetutKurssit) {
              describe('Kun oppimääräksi on valittuna "' + oppimäärä + '"', function () {
                before(
                  editor.edit,
                  äidinkieli.kurssi('ÄI1').poistaKurssi,
                  äidinkieli.kurssi('ÄI2').poistaKurssi,
                  äidinkieli.kurssi('ÄI3').poistaKurssi,
                  äidinkieli.kurssi('ÄI10').poistaKurssi,
                  äidinkieli.propertyBySelector('.kieli').selectValue(oppimäärä),
                  äidinkieli.avaaLisääKurssiDialog
                )

                it('Näytetään oikea kurssivalikoima', function() {
                  expect(äidinkieli.lisääKurssiDialog.kurssit()).to.deep.equal(odotetutKurssit)
                })

                after(
                  äidinkieli.lisääKurssiDialog.sulje,
                  editor.cancelChanges
                )
              })
            }

            testaaÄidinkielenKurssivaihtoehdot('Suomen kieli ja kirjallisuus', [
              'ÄI1 Suomen kielen ja kirjallisuuden perusteet',
              'ÄI10 Nykykulttuurin ilmiöitä ja kirjallisuutta',
              'ÄI2 Monimuotoiset tekstit',
              'ÄI3 Tekstien tuottaminen ja tulkitseminen',
              'ÄI5 Puhe- ja vuorovaikutustaidot',
              'ÄI6 Median maailma',
              'ÄI7 Kauno- ja tietokirjallisuuden lukeminen',
              'ÄI8 Tekstien tulkinta',
              'ÄI9 Tekstien tuottaminen',
              'Lisää paikallinen kurssi...'
            ])
            testaaÄidinkielenKurssivaihtoehdot('Suomi toisena kielenä ja kirjallisuus', [
              'S21 Opiskelutaitojen vahvistaminen',
              'S210 Ajankohtaiset ilmiöt Suomessa ja maailmalla',
              'S22 Luonnontieteen tekstit tutummiksi',
              'S23 Yhteiskunnallisten aineiden tekstit tutummiksi',
              'S24 Median tekstejä ja kuvia',
              'S25 Tiedonhankintataitojen syventäminen',
              'S26 Uutistekstit',
              'S27 Mielipiteen ilmaiseminen ja perusteleminen',
              'S28 Kaunokirjalliset tekstit tutuiksi',
              'S29 Kulttuurinen moninaisuus - moninainen kulttuuri',
              'Lisää paikallinen kurssi...'
            ])
            testaaÄidinkielenKurssivaihtoehdot('Ruotsin kieli ja kirjallisuus',  [
              'MO1 Ruotsin kielen ja kirjallisuuden perusteet',
              'MO10 Nykykulttuurin ilmiöitä ja kirjallisuutta',
              'MO2 Monimuotoiset tekstit',
              'MO3 Tekstien tuottaminen ja tulkitseminen',
              'MO4 Kieli ja kulttuuri',
              'MO5 Puhe- ja vuorovaikutustaidot',
              'MO6 Median maailma',
              'MO7 Kauno- ja tietokirjallisuuden lukeminen',
              'MO8 Tekstien tulkinta',
              'MO9 Tekstien tuottaminen',
              'Lisää paikallinen kurssi...'
            ])
          })

          describe('Kieliaineiden kurssit', function() {
            before(
              editor.edit,
              a1.avaaLisääKurssiDialog
            )
            it('Näytetään vain oikean oppiaineen ja kielen kurssit', function() {

              expect(a1.lisääKurssiDialog.kurssit()).to.deep.equal(['ENA1 Kehittyvä kielitaito: Työelämässä toimiminen ja muita muodollisia tilanteita',
                'ENA2 Kehittyvä kielitaito: Palvelu- ja viranomaistilanteet ja osallistuva kansalainen',
                'ENA3 Kehittyvä kielitaito: Kertomuksia minusta ja ympäristöstäni',
                'ENA4 Kehittyvä kielitaito: Ajankohtaiset ilmiöt',
                'ENA5 Kulttuurikohtaamisia',
                'ENA6 Globaalienglanti',
                'ENA7 Liikkuvuus ja kansainvälisyys',
                'ENA8 Avaimet elinikäiseen kieltenopiskeluun',
                'Lisää paikallinen kurssi...'])
            })

            after(
              a1.lisääKurssiDialog.sulje,
              editor.cancelChanges
            )
          })

          describe('Paikallinen kurssi', function() {
            before(
              editor.edit,
              äidinkieli.avaaLisääKurssiDialog,
              äidinkieli.lisääKurssiDialog.valitseKurssi('Lisää paikallinen kurssi...'),
              äidinkieli.lisääKurssiDialog.property('koodiarvo').setValue('ÄIX1'),
              äidinkieli.lisääKurssiDialog.property('nimi').setValue('Äidinkielen paikallinen erikoiskurssi'),
              äidinkieli.lisääKurssiDialog.lisääKurssi,
              äidinkieli.kurssi('ÄIX1').arviointi.click('.add-item a'),
              editor.saveChanges
            )
            it('Toimii', function() {
            })
          })
        })
      })

      describe('Oman äidinkielen opinnot', function() {
        before(
          editor.edit
        )

        it('Näyttää arvosananvalinnat oikeassa järjestyksessä', function() {
          expect(Page(S('.omanÄidinkielenOpinnot')).getInputOptions('.arvosana .dropdown')).to.deep.equal([
            '4', '5', '6', '7', '8', '9', '10', 'O'
          ])
        })

        after(editor.cancelChanges)
      })
    })

    describe('Alkuvaiheen opinnot', function() {
      before(opinnot.valitseSuoritus(undefined, 'Aikuisten perusopetuksen oppimäärän alkuvaihe'), opinnot.expandAll)
      it('näyttää suorituksen tiedot', function() {
        expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal('Koulutus Aikuisten perusopetuksen oppimäärän alkuvaihe aikuistenperusopetuksenoppimaaranalkuvaihe OPH-1280-2017\n' +
          'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
          'Suoritustapa Erityinen tutkinto\n' +
          'Suorituskieli suomi\n' +
          'Oman äidinkielen opinnot Arvosana 8\n' +
          'Kieli saame, lappi\n' +
          'Laajuus 1 kurssia\n' +
          'Suoritus valmis Vahvistus : 4.6.2016 Jyväskylä Reijo Reksi , rehtori')
      })
      it('näyttää oppiaineiden arvosanat', function() {
        expect(extractAsText(S('.oppiaineet'))).to.equal('Oppiaineiden arvosanat\nArvostelu 4-10, S (suoritettu) tai H (hylätty)\n' +
          'Oppiaine Arvosana\n' +
          'Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9\nLÄI1\n9 LÄI2\n9 LÄI3\n9 LÄI4\n9 LÄI5\n9 LÄI6\n9 LÄI7\nO LÄI8\n9 LÄI9\n9 AÄI1\n9 AÄI2\n9 AÄI3\n9 AÄI4\n9 AÄI5\n9 AÄI6\n9\n' +
          'Vieras kieli, englanti 7\nAENA1\n9 AENA2\n9 AENA3\n9 AENA4\n9\n' +
          'Matematiikka 10\nLMA1\n9 LMA2\n9 LMA3\n9\n' +
          'Yhteiskuntatietous ja kulttuurintuntemus 8\nLYK1\n9 LYK2\n9 LYKX\n9 LYKY\n9\n' +
          'Ympäristö- ja luonnontieto 8\nLYL1\n9\n' +
          'Terveystieto 10\nATE1\n9\n' +
          'Opinto-ohjaus ja työelämän taidot S')
      })

      describe('Tietojen muuttaminen', function() {
        describe('Oppiaineen arvosana', function() {
          var matematiikka = editor.subEditor('.MA > tr:first-child')
          before(
            editor.edit,
            matematiikka.propertyBySelector('.arvosana').selectValue('Ei valintaa'),
            editor.saveChanges,
            wait.until(page.isSavedLabelShown)
          )

          it('Ei ole pakollinen', function () { })
        })

        describe('Kurssin lisääminen', function() {
          var äidinkieli = opinnot.oppiaineet.oppiaine(0)
          describe('Valtakunnallinen kurssi', function() {
            before(
              editor.edit,
              äidinkieli.avaaLisääKurssiDialog,
              äidinkieli.lisääKurssiDialog.valitseKurssi('AÄI7'),
              äidinkieli.lisääKurssiDialog.lisääKurssi,
              äidinkieli.kurssi('AÄI7').arviointi.click('.add-item a'),
              äidinkieli.kurssi('AÄI7').arvosana.setValue('8'),
              editor.saveChanges
            )

            it('Kurssin tiedot näytetään oikein', function() {
              expect(äidinkieli.text()).to.equal('Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9\nLÄI1\n9 LÄI2\n9 LÄI3\n9 LÄI4\n9 LÄI5\n9 LÄI6\n9 LÄI7\nO LÄI8\n9 LÄI9\n9 AÄI1\n9 AÄI2\n9 AÄI3\n9 AÄI4\n9 AÄI5\n9 AÄI6\n9 AÄI7\n8')
            })
          })

          describe('Paikallinen kurssi', function() {
            before(
              editor.edit,
              äidinkieli.avaaLisääKurssiDialog,
              äidinkieli.lisääKurssiDialog.valitseKurssi('Lisää paikallinen kurssi...'),
              äidinkieli.lisääKurssiDialog.property('koodiarvo').setValue('ÄIX1'),
              äidinkieli.lisääKurssiDialog.property('nimi').setValue('Äidinkielen paikallinen erikoiskurssi'),
              äidinkieli.lisääKurssiDialog.lisääKurssi,
              äidinkieli.kurssi('ÄIX1').arviointi.click('.add-item a'),
              äidinkieli.kurssi('ÄIX1').arvosana.setValue('10'),
              editor.saveChanges
            )
            it('Toimii', function() {
            })
          })
        })
      })
    })
  })

  describe('Aikuisten perusopetuksen opiskeluoikeuden tilan asettaminen Valmistunut tilaan', function() {
    before(
      page.openPage,
      page.oppijaHaku.searchAndSelect('280598-2415'),
      editor.edit
    )

    describe('Kun vain oppimäärällä on vahvistus', function () {
      before(
        editor.property('tila').removeItem(0),
        opinnot.valitseSuoritus(undefined, 'Aikuisten perusopetuksen oppimäärän alkuvaihe'),
        opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi,
        opinnot.avaaLisaysDialogi
      )

      it('Valmistunut tila voidaan asettaa', function () {
        expect(OpiskeluoikeusDialog().radioEnabled('valmistunut')).to.equal(true)
      })

      after(opinnot.suljeLisaysDialogi, editor.cancelChanges)
    })

    describe('Alkuvaiheen olleessa ainut suoritus', function () {
      before(
        editor.edit,
        opinnot.deletePäätasonSuoritus,
        wait.prepareForNavigation,
        opinnot.confirmDeletePäätasonSuoritus,
        wait.forNavigation,
        wait.until(page.isPäätasonSuoritusDeletedMessageShown),
        wait.until(page.isReady),
        opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('aikuistenperusopetus')
      )

      describe('Ja sillä on vahvistus', function () {
        before(
          editor.property('tila').removeItem(0),
          opinnot.avaaLisaysDialogi
        )

        it('Valmistunu tila voidaan asettaa', function () {
          expect(OpiskeluoikeusDialog().radioEnabled('valmistunut')).to.equal(true)
        })

        after(opinnot.suljeLisaysDialogi)
      })

      describe('Ja sillä ei ole vahvistusta', function () {
        before(
          opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi,
          opinnot.avaaLisaysDialogi
        )

        it('Valmistunut tila on estetty', function () {
          expect(OpiskeluoikeusDialog().radioEnabled('valmistunut')).to.equal(false)
        })
      })
    })

    after(opinnot.suljeLisaysDialogi, editor.cancelChanges)
  })

  describe('Hetuton oppija', function() {
    before(page.openPage, page.oppijaHaku.searchAndSelect('1.2.246.562.24.99999999123', 'Hetuton'))

    it('näyttää syntymäajan hetun sijaan', function() {
      expect(page.getSelectedOppija()).to.equal('Hetuton, Heikki (24.2.1977)')
    })

    it('näyttää opiskeluoikeuden tiedot', function() {
      expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal(
        'Opiskeluoikeuden voimassaoloaika : 15.8.2008 —\n' +
        'Tila 15.8.2008 Läsnä'
      )
    })
  })

  describe('Tietojen muuttaminen', function() {
    before(page.openPage, page.oppijaHaku.searchAndSelect('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'))

    describe('Opiskeluoikeuden tiedot', function() {
      it('Alkutila', function() {
        expect(opinnot.opiskeluoikeusEditor().päättymispäivä()).to.equal('4.6.2016')
        expect(opinnot.opiskeluoikeusEditor().subEditor('.property.tila').propertyBySelector('label.tila:contains("Valmistunut")').isVisible()).to.equal(true)
      })

      describe('Opiskeluoikeuden tila', function() {
        before(editor.edit, editor.property('tila').removeItem(0), editor.saveChanges, wait.until(page.isSavedLabelShown))

        describe('Perusopetuksen tilat', function() {
          before(editor.edit, opinnot.avaaLisaysDialogi)
          it('Ei sisällä loma-tilaa', function() {
            expect(opiskeluoikeus.tilat()).to.deep.equal(
                [
                  'koskiopiskeluoikeudentila_eronnut',
                  'koskiopiskeluoikeudentila_katsotaaneronneeksi',
                  'koskiopiskeluoikeudentila_lasna',
                  'koskiopiskeluoikeudentila_peruutettu',
                  'koskiopiskeluoikeudentila_valmistunut',
                  'koskiopiskeluoikeudentila_valiaikaisestikeskeytynyt'
                ]
            )
          })
        })

        describe('Eronnut', function() {
          it('Alkutila', function() {
            expect(opinnot.opiskeluoikeusEditor().päättymispäivä()).to.equal('')
          })

          describe('Kun lisätään', function() {
            before(editor.edit, opinnot.avaaLisaysDialogi, opiskeluoikeus.tila().aseta('eronnut'), opiskeluoikeus.tallenna, editor.saveChanges)
            it('Opiskeluoikeuden päättymispäivä asetetaan', function() {
              expect(opinnot.opiskeluoikeusEditor().päättymispäivä()).to.equal(currentDateStr)
            })

            it('Opiskeluoikeuden tilaa ei voi lisätä kun opiskeluoikeus on päättynyt', function() {
              expect(isElementVisible(S('.opiskeluoikeuden-tiedot .add-item a'))).to.equal(false)
            })
          })

          describe('Kun poistetaan', function() {
            before(editor.edit, editor.property('tila').removeItem(0), editor.saveChanges)

            it('Opiskeluoikeuden päättymispäivä poistetaan', function() {
              expect(opinnot.opiskeluoikeusEditor().päättymispäivä()).to.equal('')
            })

            describe('editoitaessa', function() {
              before(editor.edit)
              it('Opiskeluoikeuden tilan voi lisätä', function() {
                expect(isElementVisible(S('.opiskeluoikeuden-tiedot .add-item a'))).to.equal(true)
              })
              after(editor.cancelChanges)
            })

          })
        })

        describe('Valmistunut', function() {
          it('Alkutila', function() {
            expect(opinnot.opiskeluoikeusEditor().päättymispäivä()).to.equal('')
          })

          describe('Kun lisätään', function() {
            before(editor.edit, opinnot.avaaLisaysDialogi, opiskeluoikeus.tila().aseta('valmistunut'), opiskeluoikeus.tallenna, editor.saveChanges)

            it('Opiskeluoikeuden päättymispäivä asetetaan', function() {
              expect(opinnot.opiskeluoikeusEditor().päättymispäivä()).to.equal(currentDateStr)
            })


            describe('editoitaessa', function() {
              before(editor.edit)
              it('Opiskeluoikeuden tilaa ei voi lisätä kun opiskeluoikeus on päättynyt', function() {
                expect(isElementVisible(S('.opiskeluoikeuden-tiedot .add-item a'))).to.equal(false)
              })
              after(editor.cancelChanges)
            })

          })

          describe('Kun poistetaan', function() {
            before(editor.edit, editor.property('tila').removeItem(0), editor.saveChanges)

            it('Opiskeluoikeuden päättymispäivä poistetaan', function() {
              expect(opinnot.opiskeluoikeusEditor().päättymispäivä()).to.equal('')
            })

            describe('editoitaessa', function() {
              before(editor.edit)
              it('Opiskeluoikeuden tilan voi lisätä', function() {
                expect(isElementVisible(S('.opiskeluoikeuden-tiedot .add-item a'))).to.equal(true)
              })
              after(editor.cancelChanges)
            })
          })

          describe('Kun suoritus on kesken', function() {
            before(page.openPage, page.oppijaHaku.searchAndSelect('160932-311V'), opinnot.valitseSuoritus(undefined, 'Päättötodistus'), opinnot.opiskeluoikeusEditor().edit, opinnot.avaaLisaysDialogi)

            it('Valmistunut-tilaa ei voi lisätä', function() {
              expect(OpiskeluoikeusDialog().radioEnabled('valmistunut')).to.equal(false)
            })

            describe('Kun suorituksella on vahvistus tulevaisuudessa', function() {
              var tilaJaVahvistus = opinnot.tilaJaVahvistus
              before(
                opiskeluoikeus.peruuta,
                opinnot.oppiaineet.merkitseOppiaineetValmiiksi(),
                tilaJaVahvistus.merkitseValmiiksi,
                tilaJaVahvistus.lisääVahvistus('11.4.2117'),
                opinnot.avaaLisaysDialogi
              )

              it('Valmistunut-tilan voi lisätä', function() {
                expect(OpiskeluoikeusDialog().radioEnabled('valmistunut')).to.equal(true)
              })
            })

            after(opiskeluoikeus.peruuta, editor.cancelChanges)
          })
        })

        describe('Peruutettu', function() {
          before(page.oppijaHaku.searchAndSelect('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'), editor.edit)
          it('Alkutila', function() {
            expect(opinnot.opiskeluoikeusEditor().päättymispäivä()).to.equal('')
          })

          describe('Kun lisätään', function() {
            before(opinnot.avaaLisaysDialogi, opiskeluoikeus.tila().aseta('peruutettu'), opiskeluoikeus.tallenna, editor.saveChanges, wait.until(page.isSavedLabelShown))

            it('Opiskeluoikeuden päättymispäivä asetetaan', function() {
              expect(opinnot.opiskeluoikeusEditor().päättymispäivä()).to.equal(currentDateStr)
            })

            it('Opiskeluoikeuden tilaa ei voi lisätä kun opiskeluoikeus on päättynyt', function() {
              expect(isElementVisible(S('.opiskeluoikeuden-tiedot .add-item a'))).to.equal(false)
            })
          })

          describe('Kun poistetaan', function() {
            before(editor.edit, editor.property('tila').removeItem(0), editor.saveChanges)

            it('Opiskeluoikeuden päättymispäivä poistetaan', function() {
              expect(opinnot.opiskeluoikeusEditor().property('päättymispäivä').isVisible()).to.equal(false)
            })
          })
        })

        describe('Läsnä', function() {
          it('Alkutila', function() {
            expect(opinnot.opiskeluoikeusEditor().property('päättymispäivä').isVisible()).to.equal(false)
          })
          describe('Kun lisätään', function() {
            before(editor.edit, opinnot.avaaLisaysDialogi, opiskeluoikeus.alkuPaiva().setValue('26.12.2018'), opiskeluoikeus.tila().aseta('lasna'), opiskeluoikeus.tallenna, editor.saveChanges)

            it('Opiskeluoikeuden päättymispäivää ei aseteta', function() {
              expect(opinnot.opiskeluoikeusEditor().property('päättymispäivä').isVisible()).to.equal(false)
            })

            describe('editoitaessa', function() {
              before(editor.edit)
              it('Opiskeluoikeuden tilan voi lisätä', function() {
                expect(isElementVisible(S('.opiskeluoikeuden-tiedot .add-item a'))).to.equal(true)
              })
              after(editor.cancelChanges)
            })
          })
        })

        describe('Väliaikaisesti keskeytynyt', function() {
          it('Alkutila', function() {
            expect(opinnot.opiskeluoikeusEditor().property('päättymispäivä').isVisible()).to.equal(false)
          })
          describe('Kun lisätään', function() {
            before(editor.edit, opinnot.avaaLisaysDialogi, opiskeluoikeus.tila().aseta('valiaikaisestikeskeytynyt'), opiskeluoikeus.tallenna, editor.saveChanges)

            it('Opiskeluoikeuden päättymispäivää ei aseteta', function() {
              expect(opinnot.opiskeluoikeusEditor().property('päättymispäivä').isVisible()).to.equal(false)
            })
          })
        })

        describe('Päivämäärän validointi', function() {
          before(editor.edit, editor.property('tila').removeItem(0), opinnot.avaaLisaysDialogi)
          describe('Virheellinen päivämäärä', function() {
            before(opiskeluoikeus.tila().aseta('eronnut'), opiskeluoikeus.alkuPaiva().setValue('11.1.200'))
            it('Tallennus on estetty', function() {
              expect(opiskeluoikeus.isEnabled()).to.equal(false)
            })
          })

          describe('Virheellinen päivämäärä ja tilan muutos', function() {
            before(opiskeluoikeus.tila().aseta('eronnut'), opiskeluoikeus.alkuPaiva().setValue('11.1.200'), opiskeluoikeus.tila().aseta('valmistunut'))
            it('Tallennus on estetty', function() {
              expect(opiskeluoikeus.isEnabled()).to.equal(false)
            })
          })

          describe('Uusi alkupäivä on aikaisempi kuin viimeisen tilan alkupäivämäärä', function() {
            before(opiskeluoikeus.tila().aseta('eronnut'), opiskeluoikeus.alkuPaiva().setValue('14.8.2008'))
            it('Tallennus on estetty', function() {
              expect(opiskeluoikeus.isEnabled()).to.equal(false)
            })
          })

          describe('Uusi alkupäivä on sama kuin viimeisen tilan alkupäivämäärä', function() {
            before(opiskeluoikeus.tila().aseta('eronnut'), opiskeluoikeus.alkuPaiva().setValue('15.8.2008'))
            it('Tallennus on estetty', function() {
              expect(opiskeluoikeus.isEnabled()).to.equal(false)
            })
          })

          describe('Virheellinen päivämäärä korjattu oikeelliseksi', function() {
            before(opiskeluoikeus.tila().aseta('eronnut'), opiskeluoikeus.alkuPaiva().setValue('11.1.200'), opiskeluoikeus.alkuPaiva().setValue(currentDateStr))
            it('Tallennus on sallittu', function() {
              expect(opiskeluoikeus.isEnabled()).to.equal(true)
            })
          })
        })

        describe('Aktiivinen tila', function() {
          var tila = opinnot.opiskeluoikeusEditor().property('tila')
          it('Viimeinen tila kun päivämäärä ei ole tulevaisuudessa', function() {
            expect(findSingle('.opiskeluoikeusjakso', tila.getItems()[0].elem)().hasClass('active')).to.equal(true)
          })

          describe('Kun lisätään useita tähän päivään', function() {
            before(editor.edit,
              opinnot.avaaLisaysDialogi,
              opiskeluoikeus.alkuPaiva().setValue(date2017Str),
              opiskeluoikeus.tila().aseta('valiaikaisestikeskeytynyt'),
              opiskeluoikeus.tallenna,
              opinnot.avaaLisaysDialogi,
              opiskeluoikeus.tila().aseta('lasna'),
              opiskeluoikeus.tallenna,
              editor.saveChanges)

            it('Viimeinen tila on aktiivinen', function() {
              var jaksoElems = tila.elem().find('.opiskeluoikeusjakso')
              toArray(jaksoElems).slice(1).forEach(function(e) {
                expect(S(e).hasClass('active')).to.equal(false)
              })
              expect(S(jaksoElems[0]).hasClass('active')).to.equal(true)
            })
          })

          describe('Kun lisätään tulevaisuuteen "väliaikaisesti keskeytynyt"', function() {
            before(editor.edit,
              opinnot.avaaLisaysDialogi,
              opiskeluoikeus.tila().aseta('valiaikaisestikeskeytynyt'),
              opiskeluoikeus.alkuPaiva().setValue('9.5.2117'),
              opiskeluoikeus.tallenna,
              editor.saveChanges)

            describe('Käyttöliittymän tila', function() {
              it('Nykyinen tila "läsnä" on aktiivinen', function() {
                expect(findSingle('.opiskeluoikeusjakso', tila.getItems()[1].elem)().hasClass('active')).to.equal(true)
              })
            })

            describe('Oppijataulukossa', function() {
              before(
                syncPerustiedot,
                opinnot.backToList,
                page.oppijataulukko.filterBy('nimi', 'Koululainen Kaisa'), page.oppijataulukko.filterBy('tyyppi', 'Perusopetus')
              )

              it('Näytetään nykyinen tila "läsnä" (flaky)', function() {
                expect(page.oppijataulukko.isVisible()).to.equal(true)
                expect(page.oppijataulukko.findOppija('Koululainen, Kaisa', 'Perusopetus')).to.deep.equal([ 'Koululainen, Kaisa',
                  'Perusopetus',
                  'Perusopetuksen oppimäärä',
                  'Perusopetus',
                  'Läsnä',
                  'Jyväskylän normaalikoulu',
                  '15.8.2008',
                  '',
                  '9C' ])

              })
            })
          })
        })

      })

      describe('Opiskeluoikeuden lisätiedot', function() {
        before(page.oppijaHaku.selectOppija('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'))
        before(editor.edit, opinnot.expandAll, editor.property('perusopetuksenAloittamistaLykätty').setValue(true), editor.saveChanges, wait.until(page.isSavedLabelShown))
        describe('Lisätietojen lisäys', function() {
          it('Toimii', function() {
            expect(editor.property('perusopetuksenAloittamistaLykätty').getValue()).to.equal('kyllä')
          })
        })

        describe('Kun lisätiedot piilotetaan ja näytetään uudestaan', function() {
          before(opinnot.collapseAll, opinnot.expandAll)
          it('Toimii', function() {
            expect(editor.property('perusopetuksenAloittamistaLykätty').getValue()).to.equal('kyllä')
          })
        })

        describe('Kun lisätiedot piilotetaan, siirrytään muokkaukseen, avataan lisätiedot, poistutaan muokkauksesta', function() {
          before(opinnot.collapseAll, editor.edit, opinnot.expandAll, editor.cancelChanges)
          it('Toimii', function() {
            expect(editor.property('perusopetuksenAloittamistaLykätty').getValue()).to.equal('kyllä')
          })
          after(editor.edit, opinnot.expandAll, editor.property('perusopetuksenAloittamistaLykätty').setValue(false), editor.saveChanges)
        })

        describe('Erityisen tuen päätös', function() {
          describe('lisätään, kun valinnainen malli lisätään', function() {
            before(
              editor.edit,
              opinnot.expandAll,
              editor.property('erityisenTuenPäätökset').addItem,
              editor.saveChanges,
              wait.until(page.isSavedLabelShown)
            )
            it('Toimii', function() {
              expect(isElementVisible(S('.property.erityisenTuenPäätökset'))).to.equal(true)
              expect(editor.property('opiskeleeToimintaAlueittain').getValue()).to.equal('ei')
            })
          })
          describe('päivittyy, kun erityisen tuen tietoja muutetaan', function() {
            before(
              editor.edit,
              opinnot.expandAll,
              editor.property('opiskeleeToimintaAlueittain').setValue(true),
              editor.saveChanges,
              wait.until(page.isSavedLabelShown)
            )
            it('Toimii', function() {
              expect(editor.property('opiskeleeToimintaAlueittain').getValue()).to.equal('kyllä')
            })
          })
          describe('poistetaan, kun valinnainen malli poistetaan', function() {
            before(
              editor.edit,
              opinnot.expandAll,
              editor.property('erityisenTuenPäätökset').removeItem(0),
              editor.saveChanges,
              wait.until(page.isSavedLabelShown)
            )
            it('Toimii', function() {
              expect(isElementVisible(S('.property.erityisenTuenPäätökset'))).to.equal(false)
            })
          })
        })

        describe('Päivämäärän syöttö', function() {
          var pidennettyOppivelvollisuusProperty = editor.property('pidennettyOppivelvollisuus')
          var pidennettyOppivelvollisuus = pidennettyOppivelvollisuusProperty.toPäivämääräväli()

          before(editor.edit, opinnot.expandAll, pidennettyOppivelvollisuusProperty.addValue)

          describe('Virheellinen päivämäärä', function() {
            before(pidennettyOppivelvollisuus.setAlku('34.9.2000'))
            it('Estää tallennuksen', function() {
              expect(opinnot.onTallennettavissa()).to.equal(false)
            })
          })
          describe('Oikeellinen päivämäärä', function() {
            before(pidennettyOppivelvollisuus.setAlku(currentDateStr), editor.saveChanges, wait.until(page.isSavedLabelShown))
            it('Tallennus onnistuu', function() {
              expect(pidennettyOppivelvollisuus.getAlku()).to.equal(currentDateStr)
            })
          })

          after(editor.edit, pidennettyOppivelvollisuusProperty.removeValue, editor.saveChanges, wait.until(page.isSavedLabelShown))
        })

        describe('Virheellinen päivämääräväli', function() {
          var pidennettyOppivelvollisuusProperty = editor.property('pidennettyOppivelvollisuus')
          var pidennettyOppivelvollisuus = pidennettyOppivelvollisuusProperty.toPäivämääräväli()

          before(
            editor.edit,
            opinnot.expandAll,
            pidennettyOppivelvollisuusProperty.addValue,
            pidennettyOppivelvollisuus.setAlku(currentDateStr),
            pidennettyOppivelvollisuus.setLoppu('1.2.2008')
          )

          it('Estää tallennuksen', function() {
            expect(pidennettyOppivelvollisuus.isValid()).to.equal(false)
            expect(opinnot.onTallennettavissa()).to.equal(false)
          })
          after(pidennettyOppivelvollisuus.setLoppu(currentDateStr), editor.saveChanges, wait.until(page.isSavedLabelShown))
        })

        // Ks. https://github.com/Opetushallitus/koski/pull/876 : väliaikaisesti disabloitu
        describe.skip('Tukimuodot', function() {
          describe('Lisättäessä ensimmäinen', function() {
            before(
              editor.edit,
              editor.propertyBySelector('.erityisenTuenPäätökset').addItem,
              editor.propertyBySelector('.erityisenTuenPäätökset .tukimuodot .add-item').setValue('Erityiset apuvälineet'),
              editor.saveChanges
            )
            it('Toimii', function() {
              expect(editor.property('tukimuodot').getValue()).to.equal('Erityiset apuvälineet')
            })

            describe('Lisättäessä toinen', function() {
              before(editor.edit, editor.propertyBySelector('.tukimuodot .add-item').setValue('Tukiopetus'), editor.saveChanges)
              it('Toimii', function() {
                expect(editor.property('tukimuodot').getValue()).to.equal('Erityiset apuvälineetTukiopetus')
              })

              describe('Poistettaessa ensimmäinen', function() {
                before(editor.edit, editor.property('tukimuodot').removeItem(0), editor.saveChanges)
                it('Toimii', function() {
                  expect(editor.property('tukimuodot').getValue()).to.equal('Tukiopetus')
                })

                describe('Poistettaessa viimeinen', function() {
                  before(editor.edit, editor.property('tukimuodot').removeItem(0), editor.saveChanges)
                  it('Toimii', function() {
                    expect(editor.property('tukimuodot').isVisible()).to.equal(false)
                  })
                })
              })
            })
          })
        })
      })
    })

    describe('Suoritusten tiedot', function() {
      describe('Luokan muuttaminen', function() {
        before(opinnot.valitseSuoritus(undefined, '9. vuosiluokka'), editor.edit)
        describe('Tyhjäksi', function() {
          before(editor.property('luokka').setValue(''))
          it('Näyttää validaatiovirheen', function() {
            expect(editor.property('luokka').isValid()).to.equal(false)
          })
          it('Tallennus on estetty', function() {
            expect(editor.canSave()).to.equal(false)
          })
          it('Näytetään virheviesti myös tallennuspalkissa', function() {
            expect(editor.getEditBarMessage()).to.equal('Korjaa virheelliset tiedot.')
          })
        })
        describe('Ei tyhjäksi', function() {
          before(editor.property('luokka').setValue('9C'))
          it('Poistaa validaatiovirheen', function() {
            expect(editor.property('luokka').isValid()).to.equal(true)
          })
          it('Tallennus on sallittu', function() {
            expect(editor.canSave()).to.equal(true)
          })
          it('Poistetaan virheviesti myös tallennuspalkista', function() {
            expect(editor.getEditBarMessage()).to.equal('Tallentamattomia muutoksia')
          })

          describe('Tallennus', function() {
            before(editor.saveChanges, wait.until(page.isSavedLabelShown))
            it('Onnistuu', function() {})
          })
        })
      })
      describe('Tutkinnon perusteen diaarinumero', function() {
        var diaarinumero = editor.propertyBySelector('.diaarinumero')
        before(editor.edit, diaarinumero.setValue('1/011/2004 Perusopetuksen opetussuunnitelman perusteet 2004'), editor.saveChanges)
        it('toimii', function () {
          expect(diaarinumero.getValue()).to.equal('1/011/2004')
        })
      })
      describe('Suorituskielen lisäys', function() {
        before(opinnot.valitseSuoritus(undefined, 'Päättötodistus'), editor.edit)
        describe('Lisättäessä', function() {
          it('Näytetään oletus', function() {
            expect(editor.property('suorituskieli').getValue()).to.equal('suomi')
          })
        })
        describe('Lisäyksen jälkeen', function() {
          before(editor.property('suorituskieli').selectValue('suomi'), editor.saveChanges, wait.until(page.isSavedLabelShown))
          it('muutettu suorituskieli näytetään', function() {
            expect(editor.property('suorituskieli').getValue()).to.equal('suomi')
          })
        })
      })
      describe('Todistuksella näkyvät lisätiedot', function() {
        describe('lisäys', function() {
          var lisätiedot = editor.property('todistuksellaNäkyvätLisätiedot')
          before(opinnot.valitseSuoritus(undefined, 'Päättötodistus'), editor.edit, lisätiedot.setValue('Testitesti'), editor.saveChanges, wait.until(page.isSavedLabelShown))
          it('Uudet lisätiedot näytetään', function() {
            expect(lisätiedot.getValue()).to.equal('Testitesti')
          })
        })
        describe('poisto', function() {
          var lisätiedot = editor.property('todistuksellaNäkyvätLisätiedot')
          before(opinnot.valitseSuoritus(undefined, 'Päättötodistus'),
            editor.edit,
            lisätiedot.setValue('Testitesti2'),
            editor.saveChanges,
            wait.until(page.isSavedLabelShown),
            editor.edit,
            lisätiedot.setValue(''),
            editor.saveChanges,
            wait.until(page.isSavedLabelShown))
          it('Lisätiedot piilotetaan', function() {
            expect(lisätiedot.isVisible()).to.equal(false)
          })
        })
        describe('lisäys ja poisto kerralla', function() {
          var lisätiedot = editor.property('todistuksellaNäkyvätLisätiedot')
          before(opinnot.valitseSuoritus(undefined, 'Päättötodistus'), editor.edit, lisätiedot.setValue('Testitesti'), wait.forAjax, lisätiedot.setValue(''), editor.saveChanges, wait.until(page.isSavedLabelShown))
          it('Lisätiedot piilotetaan', function() {
            expect(lisätiedot.isVisible()).to.equal(false)
          })
        })
      })
      describe('Vieraan kielen valinta', function() {

        describe('kielivalinnan muuttaminen', function() {
          var b1kieli = opinnot.oppiaineet.oppiaine('valinnainen.B1')
          var kieli = b1kieli.propertyBySelector('.oppiaine')
          before(editor.edit, editor.property('laajuus').setValue('1'), kieli.selectValue('saksa'), editor.saveChanges)
          it('muutettu kielivalinta näytetään', function() {
            expect(kieli.getValue()).to.equal('saksa')
          })
        })
        describe('kielien järjestys listassa', function() {
          before(editor.edit)
          it('on oikein', function() {
            expect(textsOf(S('.oppiaine.B1.pakollinen .oppiaine .options li'))).to.deep.equal(
              ['suomi',
              'ruotsi',
              'englanti',
              'Ei suoritusta',
              'arabia',
              'espanja',
              'heprea',
              'italia',
              'japani',
              'kiina',
              'korea',
              'kreikka',
              'kurdi',
              'latina',
              'latvia, lätti',
              'liettua',
              'muu kieli',
              'nepali',
              'portugali',
              'puola',
              'ranska',
              'saame, lappi',
              'saksa',
              'thai',
              'turkki',
              'ukraina',
              'unkari',
              'venäjä',
              'vietnam',
              'viittomakieli',
              'viro, eesti'])
          })
          after(editor.cancelChanges)
        })
      })

      describe('Uskonto', function() {
        describe('oppimäärän muuttaminen', function() {
          var uskonto = opinnot.oppiaineet.oppiaine('pakollinen.KT')
          var oppimäärä = uskonto.propertyBySelector('.uskonnonOppimäärä')
          before(editor.edit, uskonto.expand, oppimäärä.selectValue('Islam'), editor.saveChanges, uskonto.expand)
          it('muutettu kielivalinta näytetään', function() {
            expect(oppimäärä.getValue()).to.equal('Islam')
          })
        })

        describe('Virkailijalle jolla ei ole luottamuksellisten tietojen katseluoikeutta', function() {
          before(Authentication().logout, Authentication().login('jyvas-eiluottoa'), page.openPage, page.oppijaHaku.searchAndSelect('220109-784L'))

          it('oppimäärää ei näytetä', function() {
            expect(opinnot.oppiaineet.oppiaine('pakollinen.KT').expandable()).to.equal(false)
          })

          after(resetFixtures, Authentication().logout, Authentication().login(), page.openPage, page.oppijaHaku.searchAndSelect('220109-784L'))
        })
      })

      describe('Oppiaineen laajuuden muutos', function() {
        var valinnainenLiikunta = opinnot.oppiaineet.oppiaine('valinnainen.LI')
        before(editor.edit, valinnainenLiikunta.property('laajuus').setValue('1.5'), editor.saveChanges, wait.until(page.isSavedLabelShown))
        it('muutettu laajuus näytetään', function() {
          expect(valinnainenLiikunta.property('laajuus').getValue()).to.equal('1,5')
        })
      })
      describe('Oppiaineen arvosanan muutos', function() {
        var äidinkieli = opinnot.oppiaineet.oppiaine(0)
        var arvosana = äidinkieli.propertyBySelector('.arvosana')
        before(opinnot.valitseSuoritus(undefined, '7. vuosiluokka'))

        describe('Kun annetaan numeerinen arvosana', function() {
          before(editor.edit, arvosana.selectValue('5'), editor.saveChanges)

          it('muutettu arvosana näytetään', function() {
            expect(arvosana.getValue()).to.equal('5')
          })
        })

        describe('Kun annetaan arvosana S', function() {
          before(editor.edit, arvosana.selectValue('S'), opinnot.expandAll)

          describe('Sanallinen arviointi', function() {
            var sanallinenArviointi = äidinkieli.propertyBySelector('.sanallinen-arviointi .kuvaus')
            before(editor.edit, sanallinenArviointi.setValue('Hienoa työtä'), editor.saveChanges, opinnot.expandAll)
            it('Voidaan syöttää ja näytetään', function() {
              expect(sanallinenArviointi.isVisible()).to.equal(true)
              expect(sanallinenArviointi.getValue()).to.equal('Hienoa työtä')
            })

            describe('Kun vaihdetaan numeeriseen arvosanaan', function() {
              before(editor.edit, arvosana.selectValue(8), opinnot.expandAll)

              it('Sanallinen arviointi piilotetaan', function() {
                expect(sanallinenArviointi.isVisible()).to.equal(false)
              })
            })
          })
        })

        describe('Kun poistetaan ensimmäinen oppiaine ja annetaan toiselle arvosana (bug fix)', function() {
          before(opinnot.collapseAll, editor.edit, äidinkieli.propertyBySelector('>tr:first-child').removeValue, arvosana.selectValue('9'), editor.saveChanges)
          it('Toimii', function() {
            expect(arvosana.getValue()).to.equal('9')
          })
        })
      })
      describe('Yksilöllistäminen', function() {
        before(resetFixtures, page.openPage, page.oppijaHaku.searchAndSelect('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'), editor.edit, opinnot.expandAll, editor.property('yksilöllistettyOppimäärä').setValue(true), editor.saveChanges)
        it('toimii', function() {
          expect(opinnot.oppiaineet.oppiaine(0).text()).to.equal('Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9 *')
        })
        after(editor.edit, opinnot.expandAll, editor.property('yksilöllistettyOppimäärä').setValue(false), editor.saveChanges, wait.until(page.isSavedLabelShown))
      })
      describe('Painotus', function() {
        before(editor.edit, opinnot.expandAll, editor.property('painotettuOpetus').setValue(true), editor.saveChanges, wait.until(page.isSavedLabelShown))
        it('toimii', function() {
          expect(opinnot.oppiaineet.oppiaine(0).text()).to.equal('Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9 **')
        })
        after(editor.edit, opinnot.expandAll, editor.property('painotettuOpetus').setValue(false), editor.saveChanges)
      })
      describe('Käyttäytymisen arvioinnin lisäys', function() {
        var arvosana = editor.subEditor('.kayttaytyminen').property('arvosana')
        before(opinnot.valitseSuoritus(undefined, '7. vuosiluokka'), editor.edit, editor.propertyBySelector('.kayttaytyminen').addValue, editor.saveChanges)
        describe('Muuttamatta arviointia', function() {
          it('Näyttää oletusarvon S', function() {
            expect(arvosana.getValue()).to.equal('S')
          })
        })
        describe('Kun muutetaan arvosanaa', function() {
          before(editor.edit, arvosana.selectValue('10'), editor.saveChanges)
          it('Näyttää muutetun arvon', function() {
            expect(arvosana.getValue()).to.equal('10')
          })
        })
        describe('Kun lisätään sanallinen kuvaus', function() {
          var kuvaus = editor.subEditor('.kayttaytyminen').property('kuvaus')
          before(editor.edit, kuvaus.setValue('Hyvää käytöstä'), editor.saveChanges)
          it('Näyttää muutetun arvon', function() {
            expect(kuvaus.getValue()).to.equal('Hyvää käytöstä')
          })
        })
      })
      describe('Liitetiedot', function() {
        // Liitetiedot testataan isolla testisetillä, joilla varmistetaan ArrayEditorin toiminta
        var liitetiedot = editor.property('liitetiedot')
        describe('Liitetietojen lisäys', function() {
          before(opinnot.valitseSuoritus(undefined, '7. vuosiluokka'), editor.edit, liitetiedot.addItem, liitetiedot.property('kuvaus').setValue('TestiTesti'))
          it('Editoinnin aikana ei näytetä ylimääräistä poistonappia', function() {
            expect(liitetiedot.isRemoveValueVisible()).to.equal(false)
          })
          describe('Lisäyksen jälkeen', function() {
            before(editor.saveChanges)
            it('Näyttää uudet liitetiedot', function() {
              expect(liitetiedot.getText()).to.equal('Liitetiedot Tunniste Käyttäytyminen\nKuvaus TestiTesti')
            })
            describe('Toisen liitetiedon lisäys', function() {
              before(editor.edit, liitetiedot.addItem, liitetiedot.itemEditor(1).property('kuvaus').setValue('Testi2'), editor.saveChanges)
              it('Näyttää uudet liitetiedot', function() {
                expect(liitetiedot.getText()).to.equal('Liitetiedot Tunniste Käyttäytyminen\nKuvaus TestiTesti\nTunniste Käyttäytyminen\nKuvaus Testi2')
              })
              describe('Ensimmäisen liitetiedon poisto', function() {
                before(editor.edit, liitetiedot.removeItem(0), editor.saveChanges)
                it('Näyttää vain toisen lisätiedon', function() {
                  expect(liitetiedot.getText()).to.equal('Liitetiedot Tunniste Käyttäytyminen\nKuvaus Testi2')
                })
                describe('Lisäys ja poisto samalla kertaa', function() {
                  before(editor.edit, liitetiedot.addItem, liitetiedot.itemEditor(1).property('kuvaus').setValue('Testi3'), liitetiedot.removeItem(0))
                  it('Välitulos: poisto toimii editoitaessa', function() {
                    expect(liitetiedot.itemEditor(0).property('kuvaus').getValue()).to.equal('Testi3')
                  })
                  describe('Editoinnin jälkeen', function() {
                    before(editor.saveChanges)
                    it('Näyttää oikeellisesti vain toisen lisätiedon', function() {
                      expect(liitetiedot.getText()).to.equal('Liitetiedot Tunniste Käyttäytyminen\nKuvaus Testi3')
                    })

                    describe('Viimeisen alkion poisto', function() {
                      before(editor.edit, liitetiedot.removeItem(0))
                      it('Välitulos: poisto toimii editoitaessa', function() {
                        expect(liitetiedot.getItems().length).to.equal(0)
                      })
                      describe('poiston jälkeen', function() {
                        before(editor.saveChanges)
                        it('Näytetään tyhjät liitetiedot', function() {
                          expect(liitetiedot.isVisible()).to.equal(false)
                        })
                      })
                    })
                  })
                })
              })
            })
          })
        })
        describe('Useamman liitetiedon poisto kerralla', function() {
          before(opinnot.valitseSuoritus(undefined, '7. vuosiluokka'),
            editor.edit,
            liitetiedot.addItem, liitetiedot.itemEditor(0).property('kuvaus').setValue('T1'),
            liitetiedot.addItem, liitetiedot.itemEditor(1).property('kuvaus').setValue('T2'),
            editor.saveChanges,
            editor.edit,
            liitetiedot.removeItem(0),
            liitetiedot.removeItem(0),
            editor.saveChanges
          )

          it('Kaikki liitetiedot on poistettu', function() {
            expect(liitetiedot.isVisible()).to.equal(false)
          })
        })
      })

      describe('Valinnainen oppiaine', function() {
        before(opinnot.valitseSuoritus(undefined, '7. vuosiluokka'))
        var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine('.valinnaiset')
        var uusiPakollinenOppiaine = opinnot.oppiaineet.uusiOppiaine('.pakolliset')
        describe('Valtakunnallisen oppiaineen lisääminen', function() {
          var historia = editor.subEditor('.valinnainen.HI:eq(0)')
          var historia2 = editor.subEditor('.valinnainen.HI:eq(1)')
          before(
            editor.edit,
            uusiOppiaine.selectValue('Historia'),
            historia.propertyBySelector('.arvosana').selectValue('9'),
            historia.propertyBySelector('.property.laajuus .value').setValue('1'),
            uusiOppiaine.selectValue('Historia'),
            historia2.propertyBySelector('.arvosana').selectValue('8')
          )

          describe('Ennen tallennusta', function() {
            it('Kuvaus näytetään', function () {
              expect(historia.property('kuvaus').isVisible()).to.equal(true)
            })
            describe('Tallennus', function() {
              before(
                editor.saveChanges,
                wait.until(page.isSavedLabelShown)
              )
              it('Toimii', function() {
                expect(extractAsText(S('.oppiaineet'))).to.contain('Historia 9')
                expect(extractAsText(S('.oppiaineet'))).to.contain('Historia 8')
              })
            })
          })

          describe('Poistaminen', function () {
            before(editor.edit, historia.propertyBySelector('>tr:first-child').removeValue, editor.saveChanges, wait.until(page.isSavedLabelShown))
            it('toimii', function () {
              expect(extractAsText(S('.oppiaineet'))).to.not.contain('Historia 9')
            })
          })
        })

        describe('Uuden paikallisen oppiaineen lisääminen', function() {
          var uusiPaikallinen = editor.subEditor('.valinnainen.paikallinen')
          before(editor.edit, uusiOppiaine.selectValue('Lisää'),
            uusiPaikallinen.propertyBySelector('.arvosana').selectValue('7'),
            uusiPaikallinen.propertyBySelector('.koodi').setValue('TNS'),
            uusiPaikallinen.propertyBySelector('.nimi').setValue('Tanssi'))

          describe('Ennen tallennusta', function() {
            it('Uusi oppiaine näytetään avattuna', function() {
              expect(uusiPaikallinen.property('kuvaus').isVisible()).to.equal(true)
            })
          })

          describe('Tallennuksen jälkeen', function() {
            before(editor.saveChanges, wait.until(page.isSavedLabelShown))
            it('Toimii', function () {
              expect(extractAsText(S('.oppiaineet'))).to.contain('Tanssi 7')
            })

            describe('Lisäyksen jälkeen', function() {
              var tanssi = editor.subEditor('.valinnainen.TNS')
              before(editor.edit)

              it('Uusi oppiaine on valittavissa myös pakollisissa oppiaineissa', function() {
                expect(uusiPakollinenOppiaine.getOptions().includes('Tanssi')).to.equal(true)
              })
              it('Valinnaisen oppiaineen voi lisätä useaan kertaan', function() {
                expect(uusiOppiaine.getOptions().includes('Tanssi')).to.equal(true)
              })

              describe('Poistettaessa suoritus', function() {
                before(tanssi.propertyBySelector('>tr:first-child').removeValue)
                it('Uusi oppiaine löytyy pudotusvalikosta', function() {
                  expect(uusiOppiaine.getOptions()).to.include('Tanssi')
                })

                describe('Muutettaessa lisätyn oppiaineen kuvausta, tallennettaessa ja poistettaessa oppiaine', function() {
                  before(uusiOppiaine.selectValue('Tanssi'), tanssi.propertyBySelector('.arvosana').selectValue('7'), tanssi.propertyBySelector('.nimi').setValue('Tanssi ja liike'), editor.saveChanges, editor.edit)
                  before(tanssi.propertyBySelector('>tr:first-child').removeValue)

                  describe('Käyttöliittymän tila', function() {
                    it('Muutettu oppiaine löytyy pudotusvalikosta', function() {
                      expect(uusiOppiaine.getOptions()[0]).to.equal('Tanssi ja liike')
                    })
                  })

                  describe('Poistettaessa oppiaine pudotusvalikosta', function() {
                    before(uusiOppiaine.removeFromDropdown('Tanssi ja liike'))

                    it('Oppiainetta ei löydy enää pudotusvalikosta', function() {
                      expect(uusiOppiaine.getOptions()).to.not.include('Tanssi ja liike')
                    })
                  })

                  after(editor.cancelChanges)
                })
              })
            })
          })
        })
      })

      describe('Pakollinen oppiaine', function() {
        var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine('.pakolliset')
        var uusiValinnainenOppiaine = opinnot.oppiaineet.uusiOppiaine('.valinnaiset')
        var filosofia = editor.subEditor('.pakollinen.FI')
        before(
          opinnot.valitseSuoritus(undefined, 'Päättötodistus'),
          editor.edit,
          uusiOppiaine.selectValue('Filosofia'),
          filosofia.propertyBySelector('.arvosana').selectValue('8')
        )

        describe('Lisääminen', function() {
          describe('Ennen tallennnusta', function() {
            it('Uusi oppiaine näytetään avattuna', function() {
              expect(filosofia.property('yksilöllistettyOppimäärä').isVisible()).to.equal(true)
            })
            it('Kuvaus on piilotettu', function() {
              expect(filosofia.property('kuvaus').isVisible()).to.equal(false)
            })
          })
          describe('Tallennuksen jälkeen', function() {
            before(editor.saveChanges, wait.until(page.isSavedLabelShown))
            it('Uusi oppiaine näytetään', function () {
              expect(extractAsText(S('.oppiaineet'))).to.contain('Filosofia 8')
            })
          })
        })

        describe('Poistaminen', function () {
          before(editor.edit, filosofia.propertyBySelector('>tr:first-child').removeValue, editor.saveChanges, wait.until(page.isSavedLabelShown))
          it('toimii', function () {
            expect(extractAsText(S('.oppiaineet'))).to.not.contain('Filosofia 8')
          })
        })

        describe('Uuden paikallisen oppiaineen lisääminen', function() {
          var uusiPaikallinen = editor.subEditor('.pakollinen.paikallinen')
          before(editor.edit, uusiOppiaine.selectValue('Lisää'),
            uusiPaikallinen.propertyBySelector('.arvosana').selectValue('7'),
            uusiPaikallinen.propertyBySelector('.koodi').setValue('TNS'),
            uusiPaikallinen.propertyBySelector('.nimi').setValue('Tanssi'))

          describe('Ennen tallennusta', function() {
            it('Uusi oppiaine näytetään avattuna', function() {
              expect(uusiPaikallinen.property('kuvaus').isVisible()).to.equal(true)
            })
            it('Kuvaus on näkyvissä', function() {
              expect(uusiPaikallinen.property('kuvaus').isVisible()).to.equal(true)
            })
          })

          describe('Tallennuksen jälkeen', function() {
            before(editor.saveChanges, wait.until(page.isSavedLabelShown))
            it('Toimii', function () {
              expect(extractAsText(S('.oppiaineet'))).to.contain('Tanssi 7')
            })

            describe('Lisäyksen jälkeen', function() {
              var tanssi = editor.subEditor('.pakollinen.TNS')
              before(editor.edit)

              it('Uusi oppiaine on valittavissa myös valinnaisissa oppiaineissa', function() {
                expect(uusiValinnainenOppiaine.getOptions().includes('Tanssi')).to.equal(true)
              })
              it('Oppiainetta ei voi lisätä useaan kertaan', function() {
                expect(uusiOppiaine.getOptions().includes('Tanssi')).to.equal(false)
              })

              describe('Poistettaessa suoritus', function() {
                before(tanssi.propertyBySelector('>tr:first-child').removeValue)
                it('Uusi oppiaine löytyy pudotusvalikosta', function() {
                  expect(uusiOppiaine.getOptions()).to.include('Tanssi')
                })

                describe('Muutettaessa lisätyn oppiaineen kuvausta, tallennettaessa ja poistettaessa oppiaine', function() {
                  before(uusiOppiaine.selectValue('Tanssi'), tanssi.propertyBySelector('.arvosana').selectValue('7'), tanssi.propertyBySelector('.nimi').setValue('Tanssi ja liike'), editor.saveChanges, editor.edit)
                  before(tanssi.propertyBySelector('>tr:first-child').removeValue)

                  describe('Käyttöliittymän tila', function() {
                    it('Muutettu oppiaine löytyy pudotusvalikosta', function() {
                      expect(uusiOppiaine.getOptions()[0]).to.equal('Tanssi ja liike')
                    })
                  })

                  describe('Poistettaessa oppiaine pudotusvalikosta', function() {
                    before(uusiOppiaine.removeFromDropdown('Tanssi ja liike'))

                    it('Oppiainetta ei löydy enää pudotusvalikosta', function() {
                      expect(uusiOppiaine.getOptions()).to.not.include('Tanssi ja liike')
                    })
                  })

                  after(editor.cancelChanges)
                })
              })
            })
          })
        })
      })
    })

    describe('Päätason suorituksen poistaminen', function() {
      describe('Nuorten perusopetus', function() {
        before(editor.edit)

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
              before(
                wait.prepareForNavigation,
                opinnot.confirmDeletePäätasonSuoritus,
                wait.forNavigation,
                wait.until(page.isPäätasonSuoritusDeletedMessageShown)
              )

              it('Päätason suoritus poistetaan', function() {})

              describe('Poistettua päätason suoritusta', function() {
                before(
                  wait.until(page.isReady),
                  opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'),
                  wait.until(function() {
                    return opinnot.suoritusTabs()[0] === '9. vuosiluokka'
                  })
                )

                it('Ei näytetä', function () {
                  expect(opinnot.suoritusTabs()).to.deep.equal([ '9. vuosiluokka', '8. vuosiluokka', '7. vuosiluokka' ])
                })
              })
            })
          })
        })

        describe('Opiskeluoikeudelle, jossa on vain yksi päätason suoritus', function() {
          before(Authentication().logout, Authentication().login(), page.openPage, page.oppijaHaku.searchAndSelect('010100-325X'), editor.edit)

          it('Ei näytetä', function() {
            expect(opinnot.confirmDeletePäätasonSuoritusIsShown()).to.equal(false)
          })
        })

        describe('Opiskeluoikeudelle, joka on peräisin ulkoisesta järjestelmästä', function() {
          describe('Kun kirjautunut oppilaitoksen tallentajana', function() {
            before(Authentication().logout, Authentication().login(), page.openPage, page.oppijaHaku.searchAndSelect('010100-071R'))
            it('Ei näytetä poistopainiketta', function() {
              expect(opinnot.deletePäätasonSuoritusIsShown()).to.equal(false)
            })
          })

          describe('Kun kirjautunut oppilaitoksen pääkäyttäjänä', function() {
            before(Authentication().logout, Authentication().login('stadin-pää'), page.openPage, page.oppijaHaku.searchAndSelect('010100-071R'))
            it('Näytetään poistopainike', function() {
              expect(opinnot.deletePäätasonSuoritusIsShown()).to.equal(true)
            })
          })
        })
      })

      describe('Aikuisten perusopetus', function() {
        before(Authentication().logout, Authentication().login(), page.openPage, page.oppijaHaku.searchAndSelect('280598-2415'), editor.edit)

        describe('Mitätöintilinkki', function () {
          it('Näytetään', function () {
            expect(opinnot.deletePäätasonSuoritusIsShown()).to.equal(true)
          })

          describe('Vahvistusviestin', function() {
            before(opinnot.hideInvalidateMessage, wait.untilFalse(page.isOpiskeluoikeusInvalidatedMessageShown))
            it('Voi piilottaa', function () {
              expect(page.isOpiskeluoikeusInvalidatedMessageShown()).to.equal(false)
            })
          })
        })

        after(resetFixtures)
      })
    })

    describe('Navigointi pois sivulta', function() {
      describe('Kun ei ole tallentamattomia muutoksia', function() {
        before(editor.edit, page.oppijaHaku.searchAndSelect('280618-402H'))

        it('Onnistuu normaalisti', function() {})

        after(page.oppijaHaku.searchAndSelect('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'))
      })

      describe('Kun on tallentamattomia muutoksia', function() {
        before(
          editor.edit,
          editor.property('suoritustapa').setValue('Erityinen tutkinto')
        )
        describe('Kun käyttäjä valitsee Peruuta', function() {
          before(
            function() { testFrame().confirm = function(msg) { return false } },
            opinnot.backToList
          )
          it('Pysytään muokkauksessa', function() {
            expect(editor.canSave()).to.equal(true)
          })
        })

        describe('Kun käyttäjä valitsee Jatka', function() {
          before(
            function() { testFrame().confirm = function(msg) { return true } },
            opinnot.backToList
          )
          it('Navigointi toimii', function() {
            expect(page.oppijataulukko.isVisible()).to.equal(true)
          })

          after(page.oppijaHaku.selectOppija('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'))
        })
      })
    })

    describe('Virhetilanteet', function() {
      describe('Kun tallennus epäonnistuu', function() {
        before(
          editor.edit,
          editor.property('todistuksellaNäkyvätLisätiedot').setValue('blah'),
          mockHttp('/koski/api/oppija', { status: 500}),
          editor.saveChangesAndExpectError
        )
        describe('Käyttöliittymän tila', function(){
          it('Näytetään virheilmoitus', function() {
            expect(page.getErrorMessage()).to.equal('Järjestelmässä tapahtui odottamaton virhe. Yritä myöhemmin uudelleen.')
          })
          it('Tallenna-nappi on jälleen enabloitu', function() {
            expect(opinnot.onTallennettavissa()).to.equal(true)
          })
          it('Käyttöliittymä on edelleen muokkaustilassa', function() {
            expect(opinnot.isEditing()).to.equal(true)
          })
        })

        describe('Kun tallennetaan uudestaan', function() {
          before(editor.saveChanges)
          it('Tallennus onnistuu', function() {
            expect(editor.property('todistuksellaNäkyvätLisätiedot').getValue()).to.equal('blah')
          })
        })
      })

      describe('Kun tallennuksen jälkeinen lataus epäonnistuu', function( ){
        before(
          editor.edit,
          editor.property('todistuksellaNäkyvätLisätiedot').setValue('blerg'),
          mockHttp('/koski/api/editor', { status: 500}),
          editor.saveChangesAndExpectError,
          wait.until(page.isTopLevelError)
        )
        describe('Käyttöliittymän tila', function(){
          it('Näytetään koko ruudun virheilmoitus', function() {
            expect(page.getErrorMessage()).to.equal('Järjestelmässä tapahtui odottamaton virhe. Yritä myöhemmin uudelleen. Yritä uudestaan.')
          })
        })
      })
    })
  })

  describe('Opiskeluoikeuden versiot', function() {
    var versiohistoria = opinnot.versiohistoria

    before(Authentication().login(), resetFixtures, page.openPage, page.oppijaHaku.searchAndSelect('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetus'), versiohistoria.avaa)

    it('Alussa on vain yksi versio', function() {
      expect(versiohistoria.listaa()).to.deep.equal(['v1'])
    })

    describe('Muutoksen jälkeen', function() {
      var liitetiedot = editor.property('liitetiedot')
      before(
        versiohistoria.sulje,
        opinnot.valitseSuoritus(undefined, '7. vuosiluokka'),
        editor.edit,
        liitetiedot.addItem, liitetiedot.itemEditor(0).property('kuvaus').setValue('T2'),
        editor.saveChanges,
        versiohistoria.avaa
      )

      it('On kaksi versiota', function() {
        expect(versiohistoria.listaa()).to.deep.equal(['v1', 'v2'])
        expect(liitetiedot.getItems().length).to.equal(1)
      })

      describe('Valittaessa vanha versio', function() {
        before(versiohistoria.valitse('v1'))
        it('Näytetään vanha versio', function() {
          expect(liitetiedot.isVisible()).to.equal(false)
        })

        it('Näytetään oppiaineiden arvosanat', function() {
          expect(extractAsText(S('.oppiaineet'))).to.equal(
            'Oppiaineiden arvosanat\n' +
            'Arvostelu 4-10, S (suoritettu) tai H (hylätty)\n' +
            'Pakolliset oppiaineet\n' +
            'Oppiaine Arvosana\n' +
            'Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 9\n' +
            'B1-kieli, ruotsi 8\n' +
            'A1-kieli, englanti 8\n' +
            'Uskonto/Elämänkatsomustieto 10\n' +
            'Historia 8\n' +
            'Yhteiskuntaoppi 10\n' +
            'Matematiikka 9\n' +
            'Kemia 7\n' +
            'Fysiikka 9\n' +
            'Biologia 9 *\n' +
            'Maantieto 9\n' +
            'Musiikki 7\n' +
            'Kuvataide 8\n' +
            'Kotitalous 8\n' +
            'Terveystieto 8\n' +
            'Käsityö 9\n' +
            'Liikunta 9 **\n' +
            'Valinnaiset oppiaineet\n' +
            'Oppiaine Arvosana Laajuus\n' +
            'B1-kieli, ruotsi S 1 vuosiviikkotuntia\n' +
            'Kotitalous S 1 vuosiviikkotuntia\n' +
            'Liikunta S 0,5 vuosiviikkotuntia\n' +
            'B2-kieli, saksa 9 4 vuosiviikkotuntia\n' +
            'Tietokoneen hyötykäyttö 9\n' +
            '* = yksilöllistetty oppimäärä, ** = painotettu opetus'
          )
        })
      })
    })
  })

  describe('Useita opiskeluoikeuksia', function() {
    before(Authentication().login(), resetFixtures, page.openPage, page.oppijaHaku.searchAndSelect('180497-112F'))
    describe('Alussa', function() {
      it('Uusimpien suoritusten välilehti valittu', function() {
        expect(opinnot.suoritusOnValittu('Jyväskylän', '9. vuosiluokka')).to.equal(true)
        expect(opinnot.suoritusOnValittu('Kulosaaren', '7. vuosiluokka')).to.equal(true)
      })
    })
    describe('Kun valitaan, ensimmäisestä opiskeluoikeudesta', function() {
      before(opinnot.valitseSuoritus('Jyväskylän', '8. vuosiluokka'))
      it('Toimii', function() {
        expect(opinnot.suoritusOnValittu('Jyväskylän', '9. vuosiluokka')).to.equal(false)
        expect(opinnot.suoritusOnValittu('Jyväskylän', '8. vuosiluokka')).to.equal(true)
        expect(opinnot.suoritusOnValittu('Kulosaaren', '7. vuosiluokka')).to.equal(true)
      })

      describe('Kun valitaan, molemmista opiskeluoikeuksista', function() {
        before(opinnot.valitseSuoritus('Kulosaaren', '6. vuosiluokka'), opinnot.valitseSuoritus('Jyväskylän', '9. vuosiluokka'))
        it('Toimii', function() {
          expect(opinnot.suoritusOnValittu('Jyväskylän', '9. vuosiluokka')).to.equal(true)
          expect(opinnot.suoritusOnValittu('Jyväskylän', '8. vuosiluokka')).to.equal(false)
          expect(opinnot.suoritusOnValittu('Kulosaaren', '7. vuosiluokka')).to.equal(false)
          expect(opinnot.suoritusOnValittu('Kulosaaren', '6. vuosiluokka')).to.equal(true)
        })
      })
    })
  })

  describe('Opiskeluoikeuden lisääminen', function() {
    describe('Perusopetuksen oppimäärä', function() {
      before(prepareForNewOppija('kalle', '230872-7258'))

      describe('Aluksi', function() {
        it('Lisää-nappi on disabloitu', function() {
          expect(addOppija.isEnabled()).to.equal(false)
        })
      })

      describe('Kun syötetään validit tiedot', function() {
        before(addOppija.enterValidDataPerusopetus({suorituskieli: 'ruotsi'}))

        describe('Käyttöliittymän tila', function() {
          it('Lisää-nappi on enabloitu', function() {
            expect(addOppija.isEnabled()).to.equal(true)
          })

          it('Ei näytetä opintojen rahoitus -kenttää', function() {
            expect(addOppija.rahoitusIsVisible()).to.equal(false)
          })

          it('Oikeat tilat tilavaihtoehtoina', function() {
            expect(addOppija.opiskeluoikeudenTilat()).to.deep.equal([
              'Eronnut',
              'Katsotaan eronneeksi',
              'Läsnä',
              'Peruutettu',
              'Valmistunut',
              'Väliaikaisesti keskeytynyt'
            ])
          })
        })

        describe('Kun painetaan Lisää-nappia', function() {
          before(addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Päättötodistus'))

          var esitäytetytOppiaineet = ['Äidinkieli ja kirjallisuus,',
            'A1-kieli,',
            'B1-kieli,',
            'Matematiikka',
            'Biologia',
            'Maantieto',
            'Fysiikka',
            'Kemia',
            'Terveystieto',
            'Uskonto/Elämänkatsomustieto',
            'Historia',
            'Yhteiskuntaoppi',
            'Musiikki',
            'Kuvataide',
            'Käsityö',
            'Liikunta',
            'Kotitalous',
            'Opinto-ohjaus']

          it('lisätty oppija näytetään', function() {})

          describe('Käyttöliittymän tila', function() {
            it('Lisätty opiskeluoikeus näytetään', function() {
              expect(opinnot.getTutkinto()).to.equal('Perusopetus')
              expect(opinnot.getOppilaitos()).to.equal('Jyväskylän normaalikoulu')
              expect(opinnot.getSuorituskieli()).to.equal('ruotsi')
              expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('104/011/2014')
            })

            it('Esitäytetyt oppiaineet näytetään', function() {
              expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal(esitäytetytOppiaineet)
            })
          })

          describe('Painettaessa muokkaa-nappia', function() {
            before(editor.edit)
            it('Esitäytetyt oppiaineet näytetään', function() {
              expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal(esitäytetytOppiaineet)
            })

            describe('Toiminta-alueittain opiskelevalle', function() {
              before(
                opinnot.expandAll,
                editor.property('erityisenTuenPäätökset').addItem,
                editor.property('opiskeleeToimintaAlueittain').setValue(true)
              )

              it('Esitäyttää toiminta-alueet', function() {
                expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal([
                  'motoriset taidot',
                  'kieli ja kommunikaatio',
                  'sosiaaliset taidot',
                  'päivittäisten toimintojen taidot',
                  'kognitiiviset taidot'
                ])
              })

              describe('Kun muutoksia oppiaineisiin', function() {
                var biologia = editor.subEditor('.BI')
                before(editor.property('opiskeleeToimintaAlueittain').setValue(false), biologia.propertyBySelector('.arvosana').selectValue('8'), editor.property('opiskeleeToimintaAlueittain').setValue(true))
                it('esitäyttöjä ei muuteta', function() {
                  expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal(['Äidinkieli ja kirjallisuus,', 'A1-kieli,', 'B1-kieli,', 'Matematiikka', 'Biologia', 'Maantieto', 'Fysiikka', 'Kemia',
                    'Terveystieto', 'Uskonto/Elämänkatsomustieto', 'Historia', 'Yhteiskuntaoppi', 'Musiikki', 'Kuvataide', 'Käsityö', 'Liikunta', 'Kotitalous', 'Opinto-ohjaus'])
                })

                describe('Esitäyttöjen poistaminen', function () {
                  var äidinkieli = editor.subEditor('.pakollinen.AI')
                  before(editor.cancelChanges, editor.edit, äidinkieli.propertyBySelector('>tr:first-child').removeValue)
                  it('toimii', function () {
                    expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal([
                      'A1-kieli,', 'B1-kieli,', 'Matematiikka', 'Biologia', 'Maantieto', 'Fysiikka', 'Kemia', 'Terveystieto',
                      'Uskonto/Elämänkatsomustieto', 'Historia', 'Yhteiskuntaoppi', 'Musiikki', 'Kuvataide', 'Käsityö', 'Liikunta', 'Kotitalous',
                      'Opinto-ohjaus'])
                  })
                })
              })
            })

            after(editor.cancelChanges)
          })

          describe('Toisen samanlaisen opiskeluoikeuden lisääminen kun opiskeluoikeus on voimassa', function() {
            before(
              opinnot.opiskeluoikeudet.lisääOpiskeluoikeus,
              addOppija.selectOppilaitos('Jyväskylän normaalikoulu'),
              addOppija.submitModal,
              wait.until(page.isErrorShown)
            )
            it('Lisääminen ei onnistu', function() {
              expect(page.getErrorMessage()).to.equal('Opiskeluoikeutta ei voida lisätä, koska oppijalla on jo vastaava opiskeluoikeus.')
              expect(editor.isEditBarVisible()).to.equal(false)
            })
          })

          describe('Toisen samanlaisen opiskeluoikeuden lisääminen kun opiskeluoikeus on päättynyt', function() {
            before(
              editor.edit, opinnot.avaaLisaysDialogi, opiskeluoikeus.alkuPaiva().setValue('1.1.2117'), opiskeluoikeus.tila().aseta('eronnut'), opiskeluoikeus.tallenna, editor.saveChanges,
              opinnot.opiskeluoikeudet.lisääOpiskeluoikeus,
              addOppija.selectOppilaitos('Jyväskylän normaalikoulu'),
              addOppija.submitAndExpectSuccessModal('Tyhjä, Tero (230872-7258)', 'Päättötodistus'),
              wait.until(function() { return opinnot.opiskeluoikeudet.opiskeluoikeuksienMäärä() == 2 })
            )
            it('Lisääminen onnistuu', function() {
              expect(opinnot.getOppilaitos(0)).to.equal('Jyväskylän normaalikoulu')
              expect(opinnot.getOppilaitos(1)).to.equal('Jyväskylän normaalikoulu')
            })
          })

          describe('Toisen opiskeluoikeuden lisääminen (ammatillinen tutkinto)', function() {
            before(
              opinnot.opiskeluoikeudet.lisääOpiskeluoikeus,
              addOppija.selectOppilaitos('Omnia'),
              addOppija.selectOpiskeluoikeudenTyyppi('Ammatillinen koulutus'),
              addOppija.selectTutkinto('Autoalan perustutkinto'),
              addOppija.selectSuoritustapa('Ammatillinen perustutkinto'),
              addOppija.selectAloituspäivä('1.1.2018'),
              addOppija.selectOpintojenRahoitus('Valtionosuusrahoitteinen koulutus'),
              addOppija.submitAndExpectSuccessModal('Tyhjä, Tero (230872-7258)', 'Autoalan perustutkinto')
            )
            it('Onnistuu ja uusi ammatillinen opiskeluoikeus tulee valituksi', function( ){
            })

            describe('Kolmannen opiskeluoikeuden lisääminen (oppiaineen oppimäärä)', function() {
              before(
                opinnot.opiskeluoikeudet.lisääOpiskeluoikeus,
                addOppija.selectOppilaitos('Kulosaaren ala-aste'),
                addOppija.selectOpiskeluoikeudenTyyppi('Aikuisten perusopetus'),
                addOppija.selectOppimäärä('Perusopetuksen oppiaineen oppimäärä'),
                addOppija.selectOppiaine('Fysiikka'),
                addOppija.selectOpintojenRahoitus('Valtionosuusrahoitteinen koulutus'),
                addOppija.submitAndExpectSuccessModal('Tyhjä, Tero (230872-7258)', 'Fysiikka')
              )
              it('toimii', function( ){

              })
            })
          })
        })
      })
    })

    describe('Nuorten perusopetuksen oppiaineen oppimäärä', function() {
      before(
        prepareForNewOppija('kalle', '230872-7258'),
        addOppija.enterValidDataPerusopetus(),
        addOppija.selectOpiskeluoikeudenTyyppi('Perusopetus')
      )

      describe('Käyttöliittymän tila', function() {
        it('Näytetään oppimäärävaihtoehdot', function() {
          expect(addOppija.oppimäärät()).to.deep.equal(['Nuorten perusopetuksen oppiaineen oppimäärä', 'Perusopetuksen oppimäärä'])
        })
      })

      describe('Ei-tiedossa oppiaine', function() {
        before(
          addOppija.selectOppimäärä('Nuorten perusopetuksen oppiaineen oppimäärä')
        )

        it('on valittavissa', function() {
          expect(addOppija.oppiaineet()).to.contain('Ei tiedossa')
        })
      })

      describe('Kun valitaan oppiaineen oppimäärä ja oppiaine', function() {
        before(
          addOppija.selectOppimäärä('Nuorten perusopetuksen oppiaineen oppimäärä'),
          addOppija.selectOppiaine('A1-kieli')
        )

        describe('Kun kielivalinta puuttuu', function() {
          it('Lisäys ei ole mahdollista', function() {
            expect(addOppija.isEnabled()).to.equal(false)
          })
        })

        describe('Kun valitaan kieli ja lisätään oppiaine', function() {
          before(
            addOppija.selectKieli('englanti'),
            wait.forMilliseconds(1000),
            addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'A1-kieli')
          )

          it('Luodaan opiskeluoikeus, jolla on oppiaineen oppimäärän suoritus', function() {
            expect(opinnot.getSuorituskieli()).to.equal('suomi')
            expect(editor.propertyBySelector('.perusteenDiaarinumero').getValue()).to.equal('104/011/2014')
          })

          it('Näytetään suorituksen tyyppi opiskeluoikeuden otsikossa', function() {
            expect(S('.opiskeluoikeus h3 .koulutus').text()).to.equal('Perusopetuksen oppiaineen oppimäärä')
          })

          describe('Toisen oppiaineen lisääminen', function() {
            var lisääSuoritus = opinnot.lisääSuoritusDialog
            before(editor.edit, lisääSuoritus.open('lisää oppiaineen suoritus'), wait.forAjax,
              lisääSuoritus.property('tunniste').setValue('Matematiikka'),
              lisääSuoritus.toimipiste.select('Jyväskylän normaalikoulu, alakoulu'),
              lisääSuoritus.lisääSuoritus
            )

            it('Näytetään uusi suoritus', function() {
              expect(opinnot.suoritusTabs()).to.deep.equal(['A1-kieli', 'Matematiikka'])
            })

            it('Näytetään suorituksen tyypppi opiskeluoikeuden otsikossa', function() {
              expect(S('.opiskeluoikeus h3 .koulutus').text()).to.equal('Perusopetuksen oppiaineen oppimäärä')
            })
          })
        })
      })
    })

    describe('Aikuisten perusopetus, uusi oppija', function() {
      this.timeout(20000)

      before(
        timeout.overrideWaitTime(20000),
        prepareForNewOppija('kalle', '230872-7258'),
        addOppija.enterValidDataPerusopetus(),
        addOppija.selectOpiskeluoikeudenTyyppi('Aikuisten perusopetus')
      )

      after(timeout.resetDefaultWaitTime())

      it('Näytetään opintojen rahoitus-kenttä', function() {
        expect(addOppija.rahoitusIsVisible()).to.equal(true)
      })
    })

    describe('Aikuisten perusopetus', function() {
      this.timeout(20000)

      before(
        timeout.overrideWaitTime(20000),
        prepareForNewOppija('kalle', '230872-7258'),
        addOppija.enterValidDataPerusopetus(),
        addOppija.selectOpiskeluoikeudenTyyppi('Aikuisten perusopetus'),
        addOppija.selectOppimäärä('Aikuisten perusopetuksen oppimäärä'),
        addOppija.selectOpintojenRahoitus('Valtionosuusrahoitteinen koulutus'),
        addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Aikuisten perusopetuksen oppimäärä')
      )

      after(timeout.resetDefaultWaitTime())

      describe('Lisäyksen jälkeen', function() {
        it('Näytetään oikein', function() {
          expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Aikuisten perusopetuksen oppimäärä')
          expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('OPH-1280-2017')
          expect(opinnot.getSuorituskieli()).to.equal('suomi')
        })
        describe('Oppiaineiden suoritukset', function() {
          before(editor.edit)
          it('Esitäyttää pakolliset oppiaineet', function() {
            expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal(['Äidinkieli ja kirjallisuus,',
              'A1-kieli,',
              'B1-kieli,',
              'Matematiikka',
              'Biologia',
              'Maantieto',
              'Fysiikka',
              'Kemia',
              'Terveystieto',
              'Uskonto/Elämänkatsomustieto',
              'Historia',
              'Yhteiskuntaoppi',
              'Musiikki',
              'Kuvataide',
              'Käsityö',
              'Liikunta',
              'Kotitalous',
              'Opinto-ohjaus'])
            expect(S('.oppiaineet .oppiaine .kieli input').val()).to.equal('Suomen kieli ja kirjallisuus')
          })
          after(editor.cancelChanges)
        })
      })

      describe('Oppiaineiden näyttäminen', function() {
        it('Arvioimattomia ei näytetä', function() {
          expect(toArray(S('.oppiaineet td.oppiaine')).length).to.equal(0)
        })

        describe('Arvioimaton oppiane jolla arvioitu kurssi', function () {
          var äidinkieli = opinnot.oppiaineet.oppiaine('AI')
          before(
            editor.edit,
            äidinkieli.avaaLisääKurssiDialog,
            äidinkieli.lisääKurssiDialog.valitseKurssi('Kieli ja kulttuuri'),
            äidinkieli.lisääKurssiDialog.lisääKurssi,
            äidinkieli.kurssi('ÄI4').arviointi.click('.add-item a'),
            äidinkieli.kurssi('ÄI4').arvosana.setValue('8'),
            editor.saveChanges
          )

          it('näytetään', function() {
            expect(toArray(S('.oppiaineet td.oppiaine')).length).to.equal(1)
          })
        })
      })

      describe('Alkuvaiheen opintojen lisääminen', function() {
        before(
          editor.edit, opinnot.lisääSuoritusDialog.clickLink('lisää opintojen alkuvaiheen suoritus'), editor.saveChanges
        )

        it('Näytetään uusi suoritus', function() {
          expect(opinnot.suoritusTabs()).to.deep.equal(['Aikuisten perusopetuksen oppimäärä', 'Aikuisten perusopetuksen oppimäärän alkuvaihe'])
        })
      })
    })

    describe('Aikuisten perusopetuksen alkuvaihe', function() {
      this.timeout(20000)

      before(
        timeout.overrideWaitTime(20000),
        prepareForNewOppija('kalle', '230872-7258'),
        addOppija.enterValidDataPerusopetus(),
        addOppija.selectOpiskeluoikeudenTyyppi('Aikuisten perusopetus'),
        addOppija.selectOppimäärä('Aikuisten perusopetuksen oppimäärän alkuvaihe'),
        addOppija.selectOpintojenRahoitus('Valtionosuusrahoitteinen koulutus'),
        addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Aikuisten perusopetuksen oppimäärän alkuvaihe')
      )

      it('Näytetään oikein', function() {
        expect(S('.koulutusmoduuli .tunniste').text()).to.equal('Aikuisten perusopetuksen oppimäärän alkuvaihe')
        expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('OPH-1280-2017')
        expect(opinnot.getSuorituskieli()).to.equal('suomi')
      })

      describe('Tietojen muuttaminen', function() {
        before(editor.edit)
        describe('Oppiaineen lisäys', function() {
          var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine()
          describe('Valtakunnallisen oppiaineen lisääminen', function() {
            var opintoOhjaus = editor.subEditor('.valinnainen.OP')
            before(uusiOppiaine.selectValue('Opinto-ohjaus ja työelämän taidot'), opintoOhjaus.propertyBySelector('.arvosana').selectValue('9'), editor.saveChanges, wait.until(page.isSavedLabelShown))
            it('Toimii', function () {
              expect(extractAsText(S('.oppiaineet'))).to.contain('Opinto-ohjaus ja työelämän taidot 9')
            })

            describe('Poistaminen', function () {
              before(editor.edit, opintoOhjaus.propertyBySelector('>tr:first-child').removeValue, editor.saveChanges, wait.until(page.isSavedLabelShown))
              it('toimii', function () {
                expect(extractAsText(S('.oppiaineet'))).to.not.contain('Opinto-ohjaus ja työelämän taidot 9')
              })
            })
          })

          describe('Uuden paikallisen oppiaineen lisääminen', function() {
            var uusiPaikallinen = editor.subEditor('.valinnainen.paikallinen')
            before(editor.edit, uusiOppiaine.selectValue('Lisää'),
              uusiPaikallinen.propertyBySelector('.arvosana').selectValue('7'),
              uusiPaikallinen.propertyBySelector('.koodi').setValue('TNS'),
              uusiPaikallinen.propertyBySelector('.nimi').setValue('Tanssi'))

            describe('Ennen tallennusta', function() {
              it('Uusi oppiaine näytetään avattuna', function() {
                expect(uusiPaikallinen.property('kuvaus').isVisible()).to.equal(true)
              })
            })

            describe('Tallennuksen jälkeen', function() {
              before(editor.saveChanges, wait.until(page.isSavedLabelShown))
              it('Toimii', function () {
                expect(extractAsText(S('.oppiaineet'))).to.contain('Tanssi 7')
              })
            })
          })
        })
      })

      describe('Päättövaiheen opintojen lisääminen', function() {
        before(
          editor.edit, opinnot.lisääSuoritusDialog.clickLink('lisää opintojen päättövaiheen suoritus')
        )

        describe('Lisäyksen jälkeen', function() {
          it('Lisäyslinkkiä ei näytetä lisäyksen jälkeen', function() {
            expect(opinnot.lisääSuoritusDialog.isLinkVisible('lisää opintojen päättövaiheen suoritus')).to.equal(false)
          })

          it('Esitäyttää pakolliset oppiaineet', function() {
            expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal(['Äidinkieli ja kirjallisuus,',
              'A1-kieli,',
              'B1-kieli,',
              'Matematiikka',
              'Biologia',
              'Maantieto',
              'Fysiikka',
              'Kemia',
              'Terveystieto',
              'Uskonto/Elämänkatsomustieto',
              'Historia',
              'Yhteiskuntaoppi',
              'Musiikki',
              'Kuvataide',
              'Käsityö',
              'Liikunta',
              'Kotitalous',
              'Opinto-ohjaus'])
            expect(S('.oppiaineet .oppiaine .kieli input').val()).to.equal('Suomen kieli ja kirjallisuus')
          })
        })

        describe('Tallennuksen jälkeen', function() {
          before(editor.saveChanges)
          it('Näytetään uusi suoritus', function() {
            expect(opinnot.suoritusTabs()).to.deep.equal(['Aikuisten perusopetuksen oppimäärä', 'Aikuisten perusopetuksen oppimäärän alkuvaihe'])
          })

          it('Näytetään aikuisten perusopetuksen oppimäärä opiskeluoikeuden otsikossa', function() {
            expect(S('.opiskeluoikeus h3 .koulutus').text()).to.equal('Aikuisten perusopetuksen oppimäärä')
          })
        })
      })

      after(timeout.resetDefaultWaitTime())
    })

    describe('Perusopetuksen oppiaineen oppimäärä', function() {
      before(
        prepareForNewOppija('kalle', '230872-7258'),
        addOppija.enterValidDataPerusopetus(),
        addOppija.selectOpiskeluoikeudenTyyppi('Aikuisten perusopetus')
      )

      describe('Käyttöliittymän tila', function() {
        it('Näytetään oppimäärävaihtoehdot', function() {
          expect(addOppija.oppimäärät()).to.deep.equal(['Aikuisten perusopetuksen oppimäärä', 'Aikuisten perusopetuksen oppimäärän alkuvaihe', 'Perusopetuksen oppiaineen oppimäärä'])
        })
      })

      describe('Ei-tiedossa oppiaine', function() {
        before(
          addOppija.selectOppimäärä('Perusopetuksen oppiaineen oppimäärä')
        )

        it('on valittavissa', function() {
          expect(addOppija.oppiaineet()).to.contain('Ei tiedossa')
        })
      })

      describe('Kun valitaan oppiaineen oppimäärä ja oppiaine', function() {
        before(
          addOppija.selectOppimäärä('Perusopetuksen oppiaineen oppimäärä'),
          addOppija.selectOppiaine('A1-kieli')
        )

        describe('Kun kielivalinta puuttuu', function() {
          it('Lisäys ei ole mahdollista', function() {
            expect(addOppija.isEnabled()).to.equal(false)
          })
        })

        describe('Kun valitaan kieli ja lisätään oppiaine', function() {
          before(
            timeout.overrideWaitTime(20000),
            addOppija.selectKieli('englanti'),
            wait.forMilliseconds(1000),
            addOppija.selectOpintojenRahoitus('Valtionosuusrahoitteinen koulutus'),
            addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'A1-kieli')
          )

          it('Luodaan opiskeluoikeus, jolla on oppiaineen oppimäärän suoritus', function() {
            expect(opinnot.getSuorituskieli()).to.equal('suomi')
            expect(editor.propertyBySelector('.perusteenDiaarinumero').getValue()).to.equal('OPH-1280-2017')
          })

          it('Näytetään suorituksen tyyppi opiskeluoikeuden otsikossa', function() {
            expect(S('.opiskeluoikeus h3 .koulutus').text()).to.equal('Perusopetuksen oppiaineen oppimäärä')
          })

          describe('Toisen oppiaineen lisääminen', function() {
            var lisääSuoritus = opinnot.lisääSuoritusDialog
            before(editor.edit, lisääSuoritus.open('lisää oppiaineen suoritus'), wait.forAjax,
              lisääSuoritus.property('tunniste').setValue('Matematiikka'),
              lisääSuoritus.toimipiste.select('Jyväskylän normaalikoulu, alakoulu'),
              lisääSuoritus.lisääSuoritus
            )

            it('Näytetään uusi suoritus', function() {
              expect(opinnot.suoritusTabs()).to.deep.equal(['A1-kieli', 'Matematiikka'])
            })

            it('Näytetään suorituksen tyyppi opiskeluoikeuden otsikossa', function() {
              expect(S('.opiskeluoikeus h3 .koulutus').text()).to.equal('Perusopetuksen oppiaineen oppimäärä')
            })
          })
        })
      })
    })

    describe('Back-nappi', function() {
      describe('Kun täytetään tiedot ja palataan hakuun', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataPerusopetus(),
          wait.prepareForNavigation,
          goBack,
          wait.forNavigation,
          wait.until(page.oppijataulukko.isVisible)
        )
        describe('Käyttöliittymän tila', function() {
          it('Syötetty henkilötunnus näytetään', function() {
            expect(page.oppijaHaku.getSearchString()).to.equal('230872-7258')
          })
          it('Uuden oppijan lisäys on mahdollista', function() {
            expect(page.oppijaHaku.canAddNewOppija()).to.equal(true)
          })
        })
        describe('Kun täytetään uudestaan', function() {
          before(
            page.oppijaHaku.addNewOppija,
            addOppija.enterValidDataPerusopetus()
          )
          it('Lisää-nappi on enabloitu', function() {
            expect(addOppija.isEnabled()).to.equal(true)
          })
        })
        describe('Back-nappi lisäyksen jälkeen', function() {
          before(
            addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Päättötodistus'),
            wait.prepareForNavigation,
            goBack,
            wait.forNavigation,
            addOppija.enterValidDataPerusopetus()
          )
          it('Uuden oppijan lisäys on mahdollista', function() {
            expect(addOppija.isEnabled()).to.equal(true)
          })

          describe('Lisättäessä uudelleen', function() {
            before(addOppija.submit, wait.until(page.isErrorShown))

            it('Lisäys epäonnistuu, koska oppijalla on jo vastaava opiskeluoikeus läsnä-tilassa', function() {
              expect(page.getErrorMessage()).to.equal('Opiskeluoikeutta ei voida lisätä, koska oppijalla on jo vastaava opiskeluoikeus.')
            })
          })
        })
      })
    })

  })

  describe('Vuosiluokan suorituksen lisääminen', function() {
    var lisääSuoritus = opinnot.lisääSuoritusDialog

    before(
      prepareForNewOppija('kalle', '230872-7258'),
      addOppija.enterValidDataPerusopetus(),
      addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Päättötodistus'),
      editor.edit,
      editor.property('tila').removeItem(0),
      opinnot.avaaLisaysDialogi,
      opiskeluoikeus.alkuPaiva().setValue(date2017Str),
      opiskeluoikeus.tila().aseta('lasna'),
      opiskeluoikeus.tallenna,
    )
    describe('Kun opiskeluoikeus on tilassa VALMIS', function() {
      before(opinnot.avaaLisaysDialogi, opiskeluoikeus.tila().aseta('valmistunut'), opiskeluoikeus.tallenna)

      it('Päätason suoritusta ei voi lisätä', function() {
        expect(lisääSuoritus.isLinkVisible('lisää vuosiluokan suoritus')).to.equal(false)
      })

      after(editor.property('tila').removeItem(0))
    })
    describe('Kun opiskeluoikeus on tilassa LÄSNÄ', function() {
      describe('Ennen lisäystä', function() {
        it('Päätason suorituksen voi lisätä', function() {
          expect(lisääSuoritus.isLinkVisible('lisää vuosiluokan suoritus')).to.equal(true)
        })
        it('Näytetään muut päätason suoritukset', function() {
          expect(opinnot.suoritusTabs()).to.deep.equal(['Päättötodistus'])
        })
      })
      describe('Lisättäessä ensimmäinen', function() {
        before(lisääSuoritus.open('lisää vuosiluokan suoritus'))
        describe('Aluksi', function() {
          it('Lisää-nappi on disabloitu', function() {
            expect(lisääSuoritus.isEnabled()).to.equal(false)
          })
          it('Valitsee automaattisesti pienimmän puuttuvan luokka-asteen', function( ){
            expect(lisääSuoritus.property('tunniste').getValue()).to.equal('1. vuosiluokka')
          })
        })
        describe('Kun syötetään luokkatieto ja valitaan toimipiste', function() {
          before(lisääSuoritus.property('luokka').setValue('1a'), lisääSuoritus.toimipiste.select('Jyväskylän normaalikoulu, alakoulu'))
          it('Lisää-nappi on disabloitu', function() {
            expect(lisääSuoritus.isEnabled()).to.equal(false)
          })

          describe('Kun syötetään vielä alkamispäivä', function () {
            before(lisääSuoritus.property('alkamispäivä').setValue(date2017Str))
            it('Lisää nappi on enabloitu', function () {
              expect(lisääSuoritus.isEnabled()).to.equal(true)
            })

            describe('Kun painetaan Lisää-nappia', function() {
              var äidinkieli = opinnot.oppiaineet.oppiaine(0)
              var arvosana = äidinkieli.propertyBySelector('.arvosana')
              before(lisääSuoritus.lisääSuoritus)
              describe('Käyttöliittymän tila', function() {
                it('Näytetään uusi suoritus', function() {
                  expect(opinnot.suoritusTabs()).to.deep.equal(['Päättötodistus', '1. vuosiluokka'])
                })
                it('Uusi suoritus on valittuna', function() {
                  expect(opinnot.getTutkinto()).to.equal('1. vuosiluokka')
                })
                it('Toimipiste on oikein', function() {
                  expect(editor.property('toimipiste').getValue()).to.equal('Jyväskylän normaalikoulu, alakoulu')
                })
                describe('Tutkinnon peruste', function() {
                  before(editor.saveChanges)
                  it('Esitäyttää perusteen diaarinumeron', function() {
                    expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('104/011/2014')
                  })
                })
              })
              describe('Annettaessa oppiaineelle arvosana', function() {
                before(editor.edit, arvosana.selectValue('5'), editor.saveChanges)
                it('muutettu arvosana näytetään', function() {
                  expect(arvosana.getValue()).to.equal('5')
                })
                it('suoritus siirtyy VALMIS-tilaan', function() {
                  expect(äidinkieli.elem().hasClass('valmis')).to.equal(true)
                })

                describe('Poistettaessa arvosana', function() {
                  before(editor.edit, opinnot.expandAll, arvosana.selectValue('Ei valintaa'), editor.saveChanges, wait.until(page.isSavedLabelShown))

                  it('Arvosana poistetaan', function() {
                    // Arvosanataulukko näytetään, vaikka kaikki oppiaineet ovat KESKEN-tilassa
                    expect(opinnot.oppiaineet.isVisible()).to.equal(true)
                  })
                })
              })

              describe('Merkitseminen valmiiksi', function() {
                before(editor.edit)
                var dialog = tilaJaVahvistus.merkitseValmiiksiDialog
                describe('Aluksi', function() {
                  it('Tila on "kesken"', function() {
                    expect(tilaJaVahvistus.text()).to.equal('Suoritus kesken')
                  })
                })
                describe('Kun on keskeneräisiä oppiaineita', function() {
                  it('Merkitse valmiiksi -nappi on disabloitu', function() {
                    expect(tilaJaVahvistus.merkitseValmiiksiEnabled()).to.equal(false)
                  })
                })
                describe('Kun kaikki oppiaineet on merkitty valmiiksi', function() {
                  before(opinnot.oppiaineet.merkitseOppiaineetValmiiksi(), editor.edit)
                  describe('Aluksi', function() {
                    it('Merkitse valmiiksi -nappi näytetään', function() {
                      expect(tilaJaVahvistus.merkitseValmiiksiEnabled()).to.equal(true)
                    })
                  })
                  describe('Kun merkitään valmiksi', function() {
                    before(
                      tilaJaVahvistus.merkitseValmiiksi,
                      dialog.editor.property('päivä').setValue(currentDateStr),
                      dialog.myöntäjät.itemEditor(0).setValue('Lisää henkilö'),
                      dialog.myöntäjät.itemEditor(0).propertyBySelector('.nimi').setValue('Reijo Reksi'),
                      dialog.myöntäjät.itemEditor(0).propertyBySelector('.titteli').setValue('rehtori')
                    )

                    describe('Merkitse valmiiksi -dialogi', function (){
                      it('Esitäyttää paikkakunnan valitun organisaation mukaan', function() {
                        expect(dialog.editor.property('paikkakunta').getValue()).to.equal('Jyväskylä')
                      })
                    })

                    describe('Kun painetaan Merkitse valmiiksi -nappia', function() {
                      before(
                        dialog.editor.property('paikkakunta').setValue('Jyväskylä mlk'),
                        dialog.merkitseValmiiksi
                      )


                      describe('Käyttöliittymän tila', function() {
                        it('Tila on "valmis" ja vahvistus näytetään', function() {
                          expect(tilaJaVahvistus.text()).to.equal('Suoritus valmis Vahvistus : ' + currentDateStr + ' Jyväskylä mlk Reijo Reksi , rehtori\nSiirretään seuraavalle luokalle')
                        })

                        it('Merkitse valmiiksi -nappia ei näytetä', function() {
                          expect(tilaJaVahvistus.merkitseValmiiksiEnabled()).to.equal(false)
                        })
                      })

                      describe('Kun muutetaan takaisin keskeneräiseksi', function() {
                        before(tilaJaVahvistus.merkitseKeskeneräiseksi)
                        it('Tila on "kesken" ja vahvistus on poistettu', function() {
                          expect(tilaJaVahvistus.text()).to.equal('Suoritus kesken')
                        })
                      })

                      describe('Lisättäessä toinen', function() {
                        before(editor.edit, lisääSuoritus.open('lisää vuosiluokan suoritus'))
                        describe('Aluksi', function() {
                          it('Lisää-nappi on disabloitu', function() {
                            expect(lisääSuoritus.isEnabled()).to.equal(false)
                          })
                          it('Valitsee automaattisesti pienimmän puuttuvan luokka-asteen', function( ){
                            expect(lisääSuoritus.property('tunniste').getValue()).to.equal('2. vuosiluokka')
                          })
                          it('Käytetään oletusarvona edellisen luokan toimipistettä', function() {
                            expect(editor.property('toimipiste').getValue()).to.equal('Jyväskylän normaalikoulu, alakoulu')
                          })
                        })
                        describe('Lisäyksen jälkeen', function() {
                          before(
                            lisääSuoritus.property('luokka').setValue('2a'),
                            lisääSuoritus.property('alkamispäivä').setValue(date2018Str),
                            lisääSuoritus.lisääSuoritus
                          )

                          it('Uusin suoritus näytetään täbeissä viimeisenä', function() {
                            expect(opinnot.suoritusTabs()).to.deep.equal(['Päättötodistus', '1. vuosiluokka', '2. vuosiluokka'])
                          })

                          it('Uusi suoritus on valittuna', function() {
                            expect(opinnot.getTutkinto()).to.equal('2. vuosiluokka')
                            expect(editor.property('luokka').getValue()).to.equal('2a')
                          })

                          describe('Kun merkitään valmiiksi, jää luokalle', function() {
                            var tilaJaVahvistus = opinnot.tilaJaVahvistus
                            var dialog = tilaJaVahvistus.merkitseValmiiksiDialog
                            var dialogEditor = dialog.editor
                            var myöntäjät = dialogEditor.property('myöntäjäHenkilöt')
                            before(
                              opinnot.oppiaineet.merkitseOppiaineetValmiiksi(),
                              tilaJaVahvistus.merkitseValmiiksi,
                              dialogEditor.propertyBySelector('.jaa-tai-siirretaan').setValue(false),
                              dialogEditor.property('päivä').setValue(date2018Str),
                              dialogEditor.property('paikkakunta').setValue('Jyväskylä mlk')
                            )

                            describe('Myöntäjät-lista', function() {
                              it('Edellisen suorituksen vahvistaja löytyy listalta', function() {
                                expect(myöntäjät.getOptions()).to.deep.equal(['Reijo Reksi, rehtori', 'Lisää henkilö'])
                              })
                            })

                            describe('Kun jatketaan valmiiksi merkintää käyttäen edellistä myöntäjä-henkilöä', function() {
                              before(
                                myöntäjät.itemEditor(0).setValue('Reijo Reksi'),
                                dialog.merkitseValmiiksi
                              )

                              it('Tila on "valmis" ja vahvistus näytetään', function() {
                                expect(tilaJaVahvistus.text()).to.equal('Suoritus valmis Vahvistus : ' + date2018Str + ' Jyväskylä mlk Reijo Reksi , rehtori\nEi siirretä seuraavalle luokalle')
                              })

                              describe('Seuraavan luokka-asteen lisäyksessä', function() {
                                before(lisääSuoritus.open('lisää vuosiluokan suoritus'))
                                it('On mahdollista lisätä sama luokka-aste uudelleen', function() {
                                  expect(lisääSuoritus.property('tunniste').getValue()).to.equal('2. vuosiluokka')
                                })

                                describe('Lisättäessä toinen 2. luokan suoritus', function() {
                                  before(
                                    lisääSuoritus.property('luokka').setValue('2x'),
                                    lisääSuoritus.property('alkamispäivä').setValue(date2019Str),
                                    lisääSuoritus.lisääSuoritus
                                  )

                                  it('Uusi suoritus tulee valituksi', function() {
                                    expect(editor.property('luokka').getValue()).to.equal('2x')
                                  })

                                  describe('Tallennettaessa', function() {
                                    before(editor.saveChanges)
                                    it('Uusi suoritus on edelleen valittu', function() {
                                      expect(editor.property('luokka').getValue()).to.equal('2x')
                                    })

                                    it('Uusi suoritus on täbeissä ennen vanhempaa 2.luokan suoritusta', function() {
                                      expect(opinnot.suoritusTabIndex()).to.equal(1)
                                    })

                                    describe('Kun kaikki luokka-asteet on lisätty', function() {
                                      before(editor.edit)
                                      for (var i = 3; i <= 9; i++) {
                                        before(lisääSuoritus.open('lisää vuosiluokan suoritus'), lisääSuoritus.property('luokka').setValue(i + 'a'), lisääSuoritus.property('alkamispäivä').setValue(currentDatePlusYears(i)), lisääSuoritus.lisääSuoritus)
                                      }

                                      it('Suorituksia ei voi enää lisätä', function() {
                                        expect(lisääSuoritus.isLinkVisible('lisää vuosiluokan suoritus')).to.equal(false)
                                      })

                                      it('9. luokalle ei esitäytetä oppiaineita', function() {
                                        expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal([])
                                      })

                                      describe('Uudempi 2.luokan suoritus', function() {
                                        before(editor.saveChanges, opinnot.valitseSuoritus(undefined, '2. vuosiluokka'))
                                        it('On edelleen täbeissä ennen vanhempaa 2.luokan suoritusta', function() {
                                          expect(editor.property('luokka').getValue()).to.equal('2x')
                                        })
                                      })

                                      describe('Kun poistetaan myöntäjä listalta', function() {
                                        before(
                                          opinnot.valitseSuoritus(undefined, '2. vuosiluokka'),
                                          editor.edit,
                                          opinnot.oppiaineet.merkitseOppiaineetValmiiksi(),
                                          tilaJaVahvistus.merkitseValmiiksi,
                                          myöntäjät.removeFromDropdown('Reijo Reksi'),
                                          dialog.peruuta,
                                          tilaJaVahvistus.merkitseValmiiksi
                                        )
                                        it('Uudelleen avattaessa myöntäjää ei enää ole listalla', function() {
                                          expect(myöntäjät.getOptions()).to.deep.equal(['Lisää henkilö'])
                                        })
                                      })
                                    })
                                  })
                                })
                              })
                            })
                          })
                        })
                      })
                    })
                  })
                })
              })
            })
          })
        })
      })

      describe('Oppiaineiden esitäyttö', function() {
        before(
          prepareForNewOppija('kalle', '230872-7258'),
          addOppija.enterValidDataPerusopetus(),
          addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Päättötodistus'),
          editor.edit
        )

        function lisääVuosiluokka(luokkaAste) {
          before(
            lisääSuoritus.open('lisää vuosiluokan suoritus'),
            lisääSuoritus.property('tunniste').setValue(luokkaAste + '. vuosiluokka'),
            lisääSuoritus.property('luokka').setValue(luokkaAste + 'a'),
            lisääSuoritus.toimipiste.select('Jyväskylän normaalikoulu, alakoulu'),
            lisääSuoritus.property('alkamispäivä').setValue(currentDateStr),
            lisääSuoritus.lisääSuoritus
          )
        }

        describe('Luokat 1-2', function() {
          lisääVuosiluokka('1')
          it('Esitäyttää pakolliset oppiaineet', function() {
            expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal(['Äidinkieli ja kirjallisuus,',
              'Matematiikka',
              'Ympäristöoppi',
              'Uskonto/Elämänkatsomustieto',
              'Musiikki',
              'Kuvataide',
              'Käsityö',
              'Liikunta',
              'Opinto-ohjaus'])
            expect(S('.oppiaineet .oppiaine .kieli input').val()).to.equal('Suomen kieli ja kirjallisuus')
          })
        })

        describe('Luokat 3-6', function() {
          lisääVuosiluokka('3')
          it('Esitäyttää pakolliset oppiaineet', function() {
            expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal(['Äidinkieli ja kirjallisuus,',
              'A1-kieli,',
              'Matematiikka',
              'Ympäristöoppi',
              'Uskonto/Elämänkatsomustieto',
              'Historia',
              'Yhteiskuntaoppi',
              'Musiikki',
              'Kuvataide',
              'Käsityö',
              'Liikunta',
              'Opinto-ohjaus'])
            expect(S('.oppiaineet .oppiaine .kieli input').val()).to.equal('Suomen kieli ja kirjallisuus')
          })
        })

        describe('Luokat 7-8', function() {
          lisääVuosiluokka('7')
          it('Esitäyttää pakolliset oppiaineet', function() {
            expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal(['Äidinkieli ja kirjallisuus,',
              'A1-kieli,',
              'B1-kieli,',
              'Matematiikka',
              'Biologia',
              'Maantieto',
              'Fysiikka',
              'Kemia',
              'Terveystieto',
              'Uskonto/Elämänkatsomustieto',
              'Historia',
              'Yhteiskuntaoppi',
              'Musiikki',
              'Kuvataide',
              'Käsityö',
              'Liikunta',
              'Kotitalous',
              'Opinto-ohjaus'])
            expect(S('.oppiaineet .oppiaine .kieli input').val()).to.equal('Suomen kieli ja kirjallisuus')
          })
        })

        describe('Luokka 9', function() {
          lisääVuosiluokka('9')
          it('Ei esitäytetä eikä näytetä oppiaineita', function() {
            expect(textsOf(S('.oppiaineet .oppiaine .nimi'))).to.deep.equal([])
            expect(opinnot.oppiaineet.isVisible()).to.equal(false)
          })
        })
      })
    })
  })

  describe('9. vuosiluokan oppilas', function() {
    before(Authentication().login(), page.openPage, page.oppijaHaku.searchAndSelect('160932-311V'))

    describe('Aluksi', function() {
      it('Näytetään 9. luokan suoritus, koska oppijalla ei ole päättötodistusta', function() {
        expect(opinnot.getTutkinto()).to.equal('9. vuosiluokka')
      })

      it('Oppiaineita ei näytetä', function() {
        expect(opinnot.oppiaineet.isVisible()).to.equal(false)
      })
    })

    describe('Muokattaessa', function() {
      before(editor.edit)

      it('Merkitse valmiiksi -nappia ei näytetä', function() {
        expect(tilaJaVahvistus.merkitseValmiiksiEnabled()).to.equal(false)
      })

      describe('Kun oppilas jää luokalle', function() {
        before(editor.property('jääLuokalle').setValue(true))

        describe('Käyttöliittymän tila', function() {
          it('Oppiaineet näytetään', function() {
            expect(opinnot.oppiaineet.isVisible()).to.equal(true)
          })

          it('Oppiaineet esitäytetään', function() {
            expect(textsOf(S('.oppiaineet .oppiaine .nimi')).length).to.be.greaterThan(0)
          })
        })

        describe('Kun merkitään valmiiksi', function() {
          before(opinnot.oppiaineet.merkitseOppiaineetValmiiksi(), tilaJaVahvistus.merkitseValmiiksi)

          it('Siirretään seuraavalle luokalle -riviä ei näytetä', function() {
            expect(tilaJaVahvistus.merkitseValmiiksiDialog.editor.propertyBySelector('.jaa-tai-siirretaan').isVisible()).to.equal(false)
          })

          describe('Kun on merkitty valmiiksi', function() {
            before(
              opinnot.tilaJaVahvistus.lisääVahvistus('31.7.2020'),
              editor.saveChanges,
              wait.until(page.isSavedLabelShown)
            )
            it('Tallennus onnistuu', function() {

            })

            describe('Kun poistetaan luokalleen jäänti ja merkitään jälleen keskeneräiseksi', function() {
              before(
                editor.edit,
                editor.property('jääLuokalle').setValue(false),
                tilaJaVahvistus.merkitseKeskeneräiseksi,
                editor.saveChanges
              )
              it('Tallennus onnistuu', function() {

              })
            })
          })
        })
      })

      describe('Kun oppilas ei jää luokalle', function() {
        before(editor.edit, editor.property('jääLuokalle').setValue(false))

        it('Merkitse valmiiksi -nappia ei näytetä', function() {
          expect(tilaJaVahvistus.merkitseValmiiksiEnabled()).to.equal(false)
        })

        describe('Perusopetuksen oppimäärän vahvistaminen', function() {
          before(
            opinnot.valitseSuoritus(undefined, 'Päättötodistus'),
            wait.forAjax,
            opinnot.oppiaineet.uusiOppiaine('.pakolliset').selectValue('Matematiikka'),
            opinnot.oppiaineet.oppiaine(0).property('yksilöllistettyOppimäärä').setValue(true),
            opinnot.oppiaineet.oppiaine(0).propertyBySelector('.arvosana').selectValue('S'),
            opinnot.oppiaineet.oppiaine(0).propertyBySelector('.sanallinen-arviointi .kuvaus').setValue('Hienoa työtä'),
            editor.saveChanges
          )

          it('Ensin piilottaa oppiaineiden arvosanat (ja sanallisen arvioinnin)', function() {
            expect(extractAsText(S('.oppiaineet'))).to.equal(
              'Oppiaineiden arvosanat\nArvostelu 4-10, S (suoritettu) tai H (hylätty)\nPakolliset oppiaineet\nOppiaine\nMatematiikka *\n* = yksilöllistetty oppimäärä'
            )
          })

          describe('Kun merkitään valmiiksi', function() {
            before(
              editor.edit,
              tilaJaVahvistus.merkitseValmiiksi,
              tilaJaVahvistus.lisääVahvistus('31.7.2020'),
              editor.saveChanges
            )

            it('näyttää oppiaineiden arvosanat', function() {
              expect(extractAsText(S('.oppiaineet'))).to.equal(
                'Oppiaineiden arvosanat\nArvostelu 4-10, S (suoritettu) tai H (hylätty)\nPakolliset oppiaineet\nOppiaine Arvosana\nMatematiikka S *\nSanallinen arviointi Hienoa työtä\n* = yksilöllistetty oppimäärä'
              )
            })

            describe('Käyttöliittymän tila', function() {
              before(
                opinnot.valitseSuoritus(undefined, '9. vuosiluokka')
              )
              it('Merkitsee myös 9. vuosiluokan suorituksen valmiiksi', function() {
                expect(tilaJaVahvistus.tila()).to.equal('Suoritus valmis')
              })
            })

            describe('Kun palautetaan päättötodistus KESKEN-tilaan', function() {
              before(
                page.openPage,
                page.oppijaHaku.searchAndSelect('160932-311V'),
                editor.edit,
                tilaJaVahvistus.merkitseKeskeneräiseksi,
                editor.saveChanges
              )
              it('Pysytään päättötodistus -täbillä', function() {
                expect(opinnot.getTutkinto()).to.equal('Perusopetus')
              })
            })
          })
        })
      })
    })
  })

  describe('Perusopetuksen oppiaineen oppimäärän suoritus', function() {
    before(Authentication().login(), page.openPage, page.oppijaHaku.searchAndSelect('110738-839L'))
    describe('Kaikki tiedot näkyvissä', function() {
      before(opinnot.expandAll)
      it('näyttää opiskeluoikeuden tiedot', function() {
        expect(opinnot.opiskeluoikeudet.opiskeluoikeuksienOtsikot()).to.deep.equal(['Jyväskylän normaalikoulu, Perusopetuksen oppiaineen oppimäärä (2008—2016, valmistunut)'])
        expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal(
          'Opiskeluoikeuden voimassaoloaika : 15.8.2008 — 4.6.2016\n' +
          'Tila 4.6.2016 Valmistunut (valtionosuusrahoitteinen koulutus)\n' +
          '15.8.2008 Läsnä (valtionosuusrahoitteinen koulutus)\n' +
          'Lisätiedot\n' +
          'Vaikeimmin kehitysvammainen 6.6.2014 —')
      })

      it('näyttää suorituksen tiedot', function() {
        expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
          'Oppiaine Äidinkieli ja kirjallisuus\n' +
          'Kieli Suomen kieli ja kirjallisuus\n' +
          'Peruste 19/011/2015\n' +
          'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
          'Arvosana 9\n' +
          'Suoritustapa Erityinen tutkinto\n' +
          'Suorituskieli suomi\n' +
          'Suoritus valmis Vahvistus : 4.6.2016 Jyväskylä Reijo Reksi , rehtori')
      })
    })

    describe('Monta oppiainetta', function() {
      before(page.openPage, page.oppijaHaku.searchAndSelect('131298-5248'))
      it('näyttää opiskeluoikeuden otsikon oikein', function() {
        expect(opinnot.opiskeluoikeudet.opiskeluoikeuksienOtsikot()).to.deep.equal(['Jyväskylän normaalikoulu, Perusopetuksen oppiaineen oppimäärä (2008—2016, valmistunut)'])
      })
    })

    describe('Tietojen muuttaminen', function() {
      var arvosana = editor.property('arviointi')

      before(page.openPage, page.oppijaHaku.searchAndSelect('110738-839L'))
      before(editor.edit, editor.property('tila').removeItem(0)) // opiskeluoikeus: läsnä

      describe('Kun arviointi poistetaan', function() {
        before(
          arvosana.setValue('Ei valintaa'),
          editor.saveChanges
        )

        it('Suoritus siirtyy tilaan KESKEN', function() {
          expect(tilaJaVahvistus.text()).to.equal('Suoritus kesken')
        })

        describe('Kun muokataan suoritusta', function() {
          before(
            editor.edit
          )

          it('Valmiiksi merkintä on estetty', function() {
            expect(tilaJaVahvistus.merkitseValmiiksiEnabled()).to.equal(false)
          })

          describe('Kun lisätään arvosana', function() {
            before(arvosana.setValue('8'),
              tilaJaVahvistus.merkitseValmiiksi,
              tilaJaVahvistus.merkitseValmiiksiDialog.myöntäjät.itemEditor(0).setValue('Lisää henkilö'),
              tilaJaVahvistus.merkitseValmiiksiDialog.myöntäjät.itemEditor(0).propertyBySelector('.nimi').setValue('Reijo Reksi'),
              tilaJaVahvistus.merkitseValmiiksiDialog.myöntäjät.itemEditor(0).propertyBySelector('.titteli').setValue('rehtori'),
              tilaJaVahvistus.merkitseValmiiksiDialog.merkitseValmiiksi,
              editor.saveChanges
            )

            it('Valmiiksi merkintä on mahdollista', function() {

            })
          })
        })
      })
      describe('Kurssit', function() {
        describe('Kurssin lisääminen päättövaiheen kursseista', function() {

          var äidinkieli = Oppiaine(findSingle('.perusopetuksenoppiaineenoppimaaransuoritus'))

          before(
            editor.edit,
            tilaJaVahvistus.merkitseKeskeneräiseksi,
            äidinkieli.avaaLisääKurssiDialog
          )
          it('Näytetään vain oikean oppiaineen kurssit', function() {
            expect(äidinkieli.lisääKurssiDialog.kurssit().length).to.equal(11)
          })

          describe('Kun lisätään kurssi', function() {
            before(
              äidinkieli.lisääKurssiDialog.valitseKurssi('Uutisia ja mielipiteitä'),
              äidinkieli.lisääKurssiDialog.lisääKurssi
            )

            describe('Kun annetaan arvosana ja tallennetaan', function() {
              before(
                äidinkieli.kurssi('S21').arviointi.click('.add-item a'),
                äidinkieli.kurssi('S21').arvosana.setValue('8'),
                editor.saveChanges
              )

              it('toimii', function() {

              })
            })
          })
        })

        describe('Kurssin lisääminen alkuvaiheen kursseista', function() {

          var äidinkieli = Oppiaine(findSingle('.perusopetuksenoppiaineenoppimaaransuoritus'))

          before(
            editor.edit,
            äidinkieli.avaaAlkuvaiheenLisääKurssiDialog
          )
          it('Näytetään kaikki alkuvaiheen äidinkielen kurssit', function() {
            expect(äidinkieli.lisääKurssiDialog.kurssit().length).to.equal(50)
          })

          describe('Kun lisätään kurssi', function() {
            before(
              äidinkieli.lisääKurssiDialog.valitseKurssi('Kehittyvä kielitaito: Asuminen'),
              äidinkieli.lisääKurssiDialog.lisääKurssi
            )

            describe('Kun annetaan arvosana ja tallennetaan', function() {
              before(
                äidinkieli.kurssi('AS211').arviointi.click('.add-item a'),
                äidinkieli.kurssi('AS211').arvosana.setValue('5'),
                editor.saveChanges
              )

              it('toimii', function() {

              })
            })
          })
        })
      })
    })
  })


  describe('Perusopetuksen useamman oppiaineen aineopiskelija', function() {
    describe('Opiskeluoikeuden tilaa', function () {
      before(
        page.openPage,
        page.oppijaHaku.searchAndSelect('131298-5248'),
        editor.edit,
        editor.property('tila').removeItem(0),
        opinnot.valitseSuoritus(undefined, 'Äidinkieli ja kirjallisuus'),
        opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi,
        opinnot.valitseSuoritus(undefined, 'Yhteiskuntaoppi'),
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
      var lisääSuoritus = opinnot.lisääSuoritusDialog
      before(
        page.openPage,
        page.oppijaHaku.searchAndSelect('131298-5248'),
        editor.edit,
        editor.property('tila').removeItem(0),
        lisääSuoritus.open('lisää oppiaineen suoritus'), wait.forAjax,
        lisääSuoritus.property('tunniste').setValue('Ei tiedossa'),
        lisääSuoritus.toimipiste.select('Jyväskylän normaalikoulu, alakoulu'),
        lisääSuoritus.lisääSuoritus,
        opinnot.avaaLisaysDialogi
      )

      it('Opiskeluoikeuden tilaa ei voi merkitä valmiiksi', function () {
        expect(OpiskeluoikeusDialog().radioEnabled('valmistunut')).to.equal(false)
      })

      after(editor.cancelChanges)
    })
  })


  describe('Perusopetuksen lisäopetus', function() {
    before(page.openPage, page.oppijaHaku.searchAndSelect('131025-6573'))
    describe('Kaikki tiedot näkyvissä', function() {
      before(opinnot.expandAll)

      it('näyttää opiskeluoikeuden tiedot', function() {
        expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal(
          'Opiskeluoikeuden voimassaoloaika : 15.8.2008 — 4.6.2016\n' +
          'Tila 4.6.2016 Valmistunut\n' +
          '15.8.2008 Läsnä\n' +
          'Lisätiedot\n' +
          'Pidennetty oppivelvollisuus 15.8.2008 — 4.6.2016')
      })

      it('näyttää suorituksen tiedot', function() {
        expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
          'Koulutus Perusopetuksen lisäopetus 020075 105/011/2014\n' +
          'Luokka 10A\n' +
          'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
          'Suorituskieli suomi\n' +
          'Suoritus valmis Vahvistus : 4.6.2016 Jyväskylä Reijo Reksi , rehtori')
      })
      it('näyttää oppiaineiden arvosanat', function() {
        expect(extractAsText(S('.oppiaineet'))).to.equal('Oppiaineiden arvosanat\n' +
          'Arvostelu 4-10, S (suoritettu) tai H (hylätty)\n' +
          'Pakolliset oppiaineet\n' +
          'Oppiaine Arvosana\n' +
          'Äidinkieli ja kirjallisuus, Suomen kieli ja kirjallisuus 7 †\n' +
          'A1-kieli, englanti 10 †\n' +
          'B1-kieli, ruotsi 6 †\n' +
          'Matematiikka 6 †\n' +
          'Biologia 10 †\n' +
          'Maantieto 9 †\n' +
          'Fysiikka 8 †\n' +
          'Kemia 9 †\n' +
          'Terveystieto 8 †\n' +
          'Historia 7\n' +
          'Yhteiskuntaoppi 8 †\n' +
          'Kuvataide 8\n' +
          'Liikunta 7 * †\n' +
          'Valinnaiset oppiaineet\n' +
          'Oppiaine Arvosana\n' +
          'Monialainen oppimiskokonaisuus S\n' +
          'Kuvaus Tehtiin ryhmätyönä webbisivusto, jossa kerrotaan tupakoinnin haitoista\n' +
          '* = yksilöllistetty oppimäärä, † = perusopetuksen päättötodistuksen arvosanan korotus')
      })
    })
    describe('Tietojen muuttaminen', function() {
      before(page.openPage, page.oppijaHaku.searchAndSelect('131025-6573'))
      describe('Oppiaineen arvosanan muutos', function() {
        var äidinkieli = opinnot.oppiaineet.oppiaine(0)
        var arvosana = äidinkieli.propertyBySelector('.arvosana')
        describe('Kun annetaan numeerinen arvosana', function() {
          before(editor.edit, arvosana.selectValue('5'), editor.saveChanges)

          it('muutettu arvosana näytetään', function() {
            expect(arvosana.getValue()).to.equal('5')
          })
        })
      })

      describe('Kurssin kuvauksen ja sanallisen arvion muuttaminen', function() {
        var kurssi = opinnot.oppiaineet.oppiaine('xxx')
        var sanallinenArviointi = kurssi.propertyBySelector('.kuvaus:eq(0)')
        var kurssinKuvaus = kurssi.propertyBySelector('.kuvaus:eq(1)') // Yes, they both have the same class "kuvaus", which is exactly why testing this is important

        before(editor.edit, opinnot.expandAll, sanallinenArviointi.setValue('Uusi arviointi'), kurssinKuvaus.setValue('Uusi kuvaus'), editor.saveChanges, opinnot.expandAll)
        it('Toimii', function() {
          expect(sanallinenArviointi.getValue()).to.equal('Uusi arviointi')
          expect(kurssinKuvaus.getValue()).to.equal('Uusi kuvaus')
        })
      })


      describe('Pakollinen oppiaine', function() {
        var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine('.pakolliset')
        var filosofia = editor.subEditor('.pakollinen.FI')
        before(editor.edit, uusiOppiaine.selectValue('Filosofia'), filosofia.propertyBySelector('.arvosana').selectValue('8'), editor.saveChanges, wait.until(page.isSavedLabelShown))

        describe('Lisääminen', function() {
          it('Uusi oppiaine näytetään', function () {
            expect(extractAsText(S('.oppiaineet'))).to.contain('Filosofia 8')
          })
        })

        describe('Poistaminen', function () {
          before(editor.edit, filosofia.propertyBySelector('>tr:first-child').removeValue, editor.saveChanges, wait.until(page.isSavedLabelShown))
          it('toimii', function () {
            expect(extractAsText(S('.oppiaineet'))).to.not.contain('Filosofia 8')
          })
        })
      })

      describe('Opiskelu toiminta-alueittain', function() {
        describe('Toiminta-alueen lisääminen', function() {
          var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine()
          var kognitiivisetTaidot = editor.subEditor('.valinnainen.5')
          before(
            editor.edit,
            opinnot.expandAll,
            editor.property('erityisenTuenPäätökset').addItem,
            editor.property('opiskeleeToimintaAlueittain').setValue(true),
            uusiOppiaine.selectValue('kognitiiviset taidot'),
            kognitiivisetTaidot.propertyBySelector('.arvosana').selectValue('8'),
            editor.saveChanges
          )

          it('Toimii', function() {
            expect(kognitiivisetTaidot.propertyBySelector('.arvosana').getValue()).to.equal('8')
          })
        })
      })
    })
    describe('Opiskeluoikeuden lisääminen', function() {
      before(prepareForNewOppija('kalle', '230872-7258'))
      before(addOppija.enterValidDataPerusopetus(), addOppija.selectOpiskeluoikeudenTyyppi('Perusopetuksen lisäopetus'))
      before(addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Perusopetuksen lisäopetus'))
      it('Lisätty opiskeluoikeus näytetään', function() {
        expect(opinnot.getTutkinto()).to.equal('Perusopetuksen lisäopetus')
        expect(opinnot.getOppilaitos()).to.equal('Jyväskylän normaalikoulu')
        expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('105/011/2014')
        expect(opinnot.getSuorituskieli()).to.equal('suomi')
      })
    })
  })

  describe('Perusopetukseen valmistava opetus', function() {
    before(Authentication().login(), page.openPage, page.oppijaHaku.searchAndSelect('220109-784L'), opinnot.opiskeluoikeudet.valitseOpiskeluoikeudenTyyppi('perusopetukseenvalmistavaopetus'))
    describe('Kaikki tiedot näkyvissä', function() {
      before(opinnot.expandAll)
      it('näyttää opiskeluoikeuden tiedot', function() {
        expect(extractAsText(S('.opiskeluoikeuden-tiedot'))).to.equal(
          'Opiskeluoikeuden voimassaoloaika : 15.8.2007 — 1.6.2008\n' +
          'Tila 1.6.2008 Valmistunut\n' +
          '6.1.2008 Läsnä\n' +
          '20.12.2007 Loma\n' +
          '15.8.2007 Läsnä')
      })
      it('näyttää suorituksen tiedot', function() {
        expect(extractAsText(S('.suoritus > .properties, .suoritus > .tila-vahvistus'))).to.equal(
          'Koulutus Perusopetukseen valmistava opetus 999905 57/011/2015\n' +
          'Oppilaitos / toimipiste Jyväskylän normaalikoulu\n' +
          'Suorituskieli suomi\n' +
          'Kokonaislaajuus 11 vuosiviikkotuntia\n' +
          'Suoritus valmis Vahvistus : 1.6.2008 Jyväskylä Reijo Reksi , rehtori')
      })
      it('näyttää oppiaineiden arvosanat', function() {
        expect(extractAsText(S('.oppiaineet'))).to.equal(
          'Oppiaineiden arvosanat\n' +
          'Arvostelu 4-10, S (suoritettu) tai H (hylätty)\n' +
          'Oppiaine Arvosana Laajuus\n' +
          'Äidinkieli S 10 vuosiviikkotuntia\n' +
          'Sanallinen arviointi Keskustelee sujuvasti suomeksi\n' +
          'Opetuksen sisältö Suullinen ilmaisu ja kuullun ymmärtäminen\n' +
          'Fysiikka 9 1 vuosiviikkotuntia\n' +
          'Suorituskieli suomi\n' +
          'Luokka-aste 7. vuosiluokka\n' +
          'Suoritustapa Erityinen tutkinto'
        )
      })
    })
    describe('Tietojen muuttaminen', function() {
      describe('Oppiaineen arvosanan muutos', function() {
        var äidinkieli = opinnot.oppiaineet.oppiaine(0)
        var arvosana = äidinkieli.propertyBySelector('.arvosana')
        before(editor.edit, arvosana.selectValue('H'), editor.saveChanges)
        it('muutettu arvosana näytetään', function() {
          expect(arvosana.getValue()).to.equal('H')
        })
        after(editor.edit, arvosana.selectValue('S'), editor.saveChanges, wait.until(page.isSavedLabelShown))
      })
      describe('Oppiaine', function() {
        var uusiOppiaine = opinnot.oppiaineet.uusiOppiaine('.uusi-perusopetukseen-valmistava-oppiaine')
        describe('Uuden oppiaineen lisääminen', function() {
          var uusiPaikallinen = editor.subEditor('.paikallinen:nth-child(4)')
          before(editor.edit, uusiOppiaine.selectValue('Lisää'),
            uusiPaikallinen.propertyBySelector('.koodi').setValue('TNS'),
            uusiPaikallinen.propertyBySelector('.nimi').setValue('Tanssi'),
            uusiPaikallinen.propertyBySelector('.arvosana').selectValue('S'),
            uusiPaikallinen.propertyBySelector('.property.laajuus .value').setValue('1'),
            uusiPaikallinen.propertyBySelector('.property.yksikko').setValue('vuosiviikkotuntia'),
            editor.saveChanges, wait.until(page.isSavedLabelShown))

          it('Toimii', function () {
            expect(extractAsText(S('.oppiaineet'))).to.contain('Tanssi S')
          })

          describe('Lisäyksen jälkeen', function() {
            var tanssi = editor.subEditor('.valinnainen.TNS')
            before(editor.edit)
            it('Valinnaisia aineita voi lisätä useaan kertaan', function() {
              expect(uusiOppiaine.getOptions().includes('Tanssi')).to.equal(true)
            })

            describe('Poistettaessa suoritus', function() {
              before(tanssi.propertyBySelector('>tr:first-child').removeValue)
              it('Uusi oppiaine löytyy listalta', function() {
                expect(uusiOppiaine.getOptions()).to.include('Tanssi')
              })

              describe('Muutettaessa lisätyn oppiaineen kuvausta, tallennettaessa ja poistettaessa oppiaine', function() {
                before(uusiOppiaine.selectValue('Tanssi'), tanssi.propertyBySelector('.arvosana').selectValue('H'), tanssi.propertyBySelector('.nimi').setValue('Tanssi ja liike'), editor.saveChanges, editor.edit)
                before(tanssi.propertyBySelector('>tr:first-child').removeValue)

                it('Muutettu oppiaine löytyy listalta', function() {
                  expect(uusiOppiaine.getOptions()[0]).to.equal('Tanssi ja liike')
                })

                after(editor.cancelChanges)
              })
            })
          })
        })

        describe('Uuden nuorten perusopetuksen oppiaineen lisääminen', function () {
          var uusiNuortenOppiaine = OpinnotPage().opiskeluoikeusEditor().propertyBySelector('.uusi-oppiaine:nth-child(2)')
          var historia = editor.subEditor('.HI')
          before(
            editor.edit,
            uusiNuortenOppiaine.selectValue('Historia'),
            historia.propertyBySelector('.arvosana').selectValue('8'),
            historia.propertyBySelector('.luokkaAste').selectValue('7. vuosiluokka'),
            historia.propertyBySelector('.property.laajuus .value').setValue('1')
          )

          it('Uusi oppiaine näytetään', function() {
            expect(historia.property('luokkaAste').isVisible()).to.equal(true)
          })

          describe('Nuorten perusopetuksen oppiaineen tallentaminen', function () {
            before(
              editor.saveChanges
            )

            it('toimii', function() {})
          })
        })
      })
    })
    describe('Opiskeluoikeuden lisääminen', function() {
      before(
        prepareForNewOppija('kalle', '230872-7258'),
        addOppija.enterValidDataPerusopetus({suorituskieli: 'englanti'}),
        addOppija.selectOpiskeluoikeudenTyyppi('Perusopetukseen valmistava opetus')
      )
      describe('Tietojen näyttäminen', function() {
        it('Näytetään tilavaihtoehdoissa kaikki tilat paitsi mitätöity', function() {
          expect(addOppija.opiskeluoikeudenTilat()).to.deep.equal([
            'Eronnut',
            'Katsotaan eronneeksi',
            'Loma',
            'Läsnä',
            'Peruutettu',
            'Valmistunut',
            'Väliaikaisesti keskeytynyt'
          ])
        })

        describe('Kun lisätään oppija', function() {
          before(addOppija.submitAndExpectSuccess('Tyhjä, Tero (230872-7258)', 'Perusopetukseen valmistava opetus'))
          it('Lisätty opiskeluoikeus näytetään', function() {
            expect(opinnot.getTutkinto()).to.equal('Perusopetukseen valmistava opetus')
            expect(opinnot.getOppilaitos()).to.equal('Jyväskylän normaalikoulu')
            expect(editor.propertyBySelector('.diaarinumero').getValue()).to.equal('57/011/2015')
            expect(opinnot.getSuorituskieli()).to.equal('englanti')
          })
        })
      })
    })
  })

  describe('Pakollisen oppiaineen laajuus nuorten päättötodistuksella ja vuosiluokan suorituksella', function () {
    before(
      resetFixtures,
      Authentication().login(),
      page.openPage,
      page.oppijaHaku.searchAndSelect('220109-784L'),
      editor.edit,
      editor.property('tila').removeItem(0),
      opinnot.tilaJaVahvistus.merkitseKeskeneräiseksi,
      editor.saveChanges
    )

    var matikka = opinnot.oppiaineet.oppiaine('pakollinen.MA')
    var ennenLeikkuriPäivää = '31.7.2020'
    var leikkuriPäivä = '1.8.2020'

    describe('Asetetaan pakolliselle oppiaineelle laajuus', function () {
      before(
        editor.edit,
        matikka.property('laajuus').setValue('4'),
        editor.saveChanges
      )
      it('Laajuutta ei näytetä, koska päätason suorituksella ei ole vahvistusta', function () {
        expect(matikka.property('laajuus').isVisible()).to.equal(false)
      })

      describe('Asetetaan päätason suoritukselle vahvistus ennen leikkuripäivää', function () {
        before(editor.edit,
          tilaJaVahvistus.merkitseValmiiksi,
          opinnot.tilaJaVahvistus.lisääVahvistus(ennenLeikkuriPäivää),
          editor.saveChanges
        )
        it('Laajuutta ei näytetä, koska vahvistus on ennen leikkuripäivää', function () {
          expect(matikka.property('laajuus').isVisible()).to.equal(false)
        })

        describe('Asetetaan päätason suoritukselle vahvistus leikkuripäivälle', function () {
          before(
            editor.edit,
            tilaJaVahvistus.merkitseKeskeneräiseksi,
            tilaJaVahvistus.merkitseValmiiksi,
            opinnot.tilaJaVahvistus.lisääVahvistus(leikkuriPäivä),
            editor.saveChanges
          )
          it('Laajuus näytetään', function () {
            expect(matikka.property('laajuus').isVisible()).to.equal(true)
          })
        })
      })
    })
  })
})
