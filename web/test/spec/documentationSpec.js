describe('Dokumentaatio', function() {
  var page = DocumentationPage()
  describe('Dokumentaatio-sivu', function() {
    before(page.openPage)
    it('näytetään', function() {
      expect(textsOf(S('h2'))).to.deep.equal(['Koski-tiedonsiirtoprotokolla'])
    })
  })

  describe('Skeeman kuvaus', function() {
    before(openPage('/koski/dokumentaatio/koski-oppija-schema.html?entity=ammatillinenopiskeluoikeus'))
    it('Toimii', function() {
      expect(toArray(S('h3')).length).to.be.above(10)
    })
  })
})