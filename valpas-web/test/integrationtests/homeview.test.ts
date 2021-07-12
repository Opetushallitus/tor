import { textEventuallyEquals } from "../integrationtests-env/browser/content"
import {
  dataTableEventuallyEquals,
  dataTableHeadersEventuallyEquals,
} from "../integrationtests-env/browser/datatable"
import { loginAs } from "../integrationtests-env/browser/reset"
import { jklNormaalikouluTableHead } from "./hakutilanne.shared"

describe("Etusivun väliaikainen näkymä", () => {
  it("Näyttää käyttäjän käyttöoikeudet", async () => {
    await loginAs("/virkailija/kayttooikeudet", "valpas-maksuttomuus-hki")

    await dataTableHeadersEventuallyEquals(
      ".kayttooikeudet",
      `
      Helsingin kaupunki
      Jyväskylän normaalikoulu
      `
    )
    await dataTableEventuallyEquals(
      ".kayttooikeudet",
      `
      Kunnan oppivelvollisuuden suorittamisen valvonta
      Oppilaitoksen opiskelun maksuttomuustietojen määrittely
      `
    )
  })

  it("Hakeutumisvelvollisuuden valvonnallinen käyttäjä ohjautuu hakeutumisvelvollisuusvalvonnan etusivulle", async () => {
    await loginAs("/virkailija", "valpas-jkl-normaali")

    await textEventuallyEquals(".card__header", jklNormaalikouluTableHead, 5000)
  })

  it("Hakeutumisvelvollisuuden valvonnallinen koulutustoimijatason käyttäjä ohjautuu hakeutumisvelvollisuusvalvonnan etusivulle", async () => {
    await loginAs("/virkailija", "valpas-helsinki-peruskoulu")

    await textEventuallyEquals(
      ".card__header",
      "Hakeutumisvelvollisia oppijoita (0)",
      5000
    )
  })

  // TODO: Testin pitäisi olla pelkästään "Pääkäyttäjä ohjautuu hakeutumisvelvollisuusvalvonnan etusivulle"
  // Toistaiseksi pääkäyttäjälle ei osata kuitenkaan palauttaa backendistä listaa kaikista oppilaitoksista, minkä vuoksi käyttöliittymä
  // tulkitsee, ettei hänellä niitä ole. Koko organisaatiohierarkian palauttaminen backendistä olisi helppo tehdä, mutta koska nykyinen
  // toteutus palauttaa koko organisaatiohierarkian detaljitietoineen vieläpä monta kertaa, niin datan määrä kasvaisi pääkäyttäjäjällä
  // todella isoksi. Pitäisi korjata tekemällä tarvittavat filtteröinnit organisaatiohierarkiaan jo backendissä, ja palauttaa vain
  // käyttöliittymän tarvitsemat kentät.
  it("Pääkäyttäjä ohjautuu hakutumisvelvollisuusvalvonnan etusivulle ja hänelle kerrotaan (virheellisesti), että hänellä ei olisi oikeuksia oppilaitoksiin", async () => {
    await loginAs("/virkailija", "valpas-pää")

    await textEventuallyEquals(
      ".error-message",
      "Sinulla ei ole oikeuksia nähdä yhdenkään oppilaitoksen tietoja",
      5000
    )

    // await textEventuallyEquals(
    //   ".card__header",
    //   "Hakeutumisvelvollisia oppijoita (0)",
    //   5000
    // )
  })

  it("Ei näytä käyttäjän Koski-käyttöoikeuksia", async () => {
    await loginAs("/virkailija/kayttooikeudet", "valpas-maksuttomuus-koski-hki")

    await dataTableHeadersEventuallyEquals(
      ".kayttooikeudet",
      `
      Jyväskylän normaalikoulu
      `
    )
    await dataTableEventuallyEquals(
      ".kayttooikeudet",
      `
      Oppilaitoksen opiskelun maksuttomuustietojen määrittely
      `
    )
  })
})
