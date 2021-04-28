import {
  createHakutilannePathWithOrg,
  createHakutilannePathWithoutOrg,
  createOppijaPath,
} from "../../src/state/paths"
import {
  clickElement,
  textEventuallyEquals,
} from "../integrationtests-env/browser/content"
import {
  pathToUrl,
  urlIsEventually,
} from "../integrationtests-env/browser/core"
import { dataTableEventuallyEquals } from "../integrationtests-env/browser/datatable"
import { dropdownSelect } from "../integrationtests-env/browser/forms"
import { loginAs } from "../integrationtests-env/browser/reset"

const selectOrganisaatio = (index: number) =>
  dropdownSelect("#organisaatiovalitsin", index)
const clickOppija = (index: number) =>
  clickElement(`.hakutilanne tr:nth-child(${index + 1}) td:first-child a`)

const jklNormaalikouluTableContent = `
  Epäonninen Valpas                                       | 30.10.2005  | 9C | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Perusopetus
  Eroaja-myöhemmin Valpas                                 | 29.9.2005   | 9C | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Perusopetus
  Kahdella-oppija-oidilla Valpas                          | 15.2.2005   | 9C | Hakenut open_in_new  | Varasija: Ressun lukio      | –                         | doneJyväskylän normaalikoulu, Lukiokoulutus
  KasiinAstiToisessaKoulussaOllut Valpas                  | 17.8.2005   | 9C | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Perusopetus
  Kotiopetus-menneisyydessä Valpas                        | 6.2.2005    | 9C | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Perusopetus
  LukionAloittanut Valpas                                 | 29.4.2005   | 9C | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Lukiokoulutus
  LukionLokakuussaAloittanut Valpas                       | 18.4.2005   | 9C | Ei hakemusta         | –                           | –                         | hourglass_empty3.10.2021 alkaen: Jyväskylän normaalikoulu, Lukiokoulutus
  LuokallejäänytYsiluokkalainen Valpas                    | 2.8.2005    | 9A | 2 hakua              | –                           | –                         | doneJyväskylän normaalikoulu, Perusopetus
  LuokallejäänytYsiluokkalainenJatkaa Valpas              | 6.2.2005    | 9B | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Perusopetus
  LuokallejäänytYsiluokkalainenKouluvaihtoMuualta Valpas  | 2.11.2005   | 9B | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Perusopetus
  Oppivelvollinen-ysiluokka-kesken-keväällä-2021 Valpas   | 22.11.2005  | 9C | Hakenut open_in_new  | 2. Helsingin medialukio     | doneHelsingin medialukio  | doneJyväskylän normaalikoulu, Perusopetus
  Päällekkäisiä Oppivelvollisuuksia                       | 6.6.2005    | 9B | Hakenut open_in_new  | Hyväksytty (2 hakukohdetta) | doneOmnia                 | done2 opiskeluoikeutta
  Turvakielto Valpas                                      | 29.9.2004   | 9C | Hakenut open_in_new  | warningEi opiskelupaikkaa   | –                         | doneJyväskylän normaalikoulu, Perusopetus
  UseampiYsiluokkaSamassaKoulussa Valpas                  | 25.8.2005   | 9D | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Perusopetus
  UseampiYsiluokkaSamassaKoulussa Valpas                  | 25.8.2005   | 9C | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Perusopetus
  Ysiluokka-valmis-keväällä-2021 Valpas                   | 19.6.2005   | 9C | Ei hakemusta         | –                           | –                         | –
`

const hakutilannePath = createHakutilannePathWithoutOrg("/virkailija")
const jklHakutilannePath = createHakutilannePathWithOrg("/virkailija", {
  organisaatioOid: "1.2.246.562.10.14613773812",
})
const kulosaariOid = "1.2.246.562.10.64353470871"
const kulosaariHakutilannePath = createHakutilannePathWithOrg("/virkailija", {
  organisaatioOid: kulosaariOid,
})
const kulosaarenOppijaOid = "1.2.246.562.24.00000000029"
const saksalainenKouluOid = "1.2.246.562.10.45093614456"
const saksalainenKouluHakutilannePath = createHakutilannePathWithOrg(
  "/virkailija",
  {
    organisaatioOid: saksalainenKouluOid,
  }
)

describe("Hakutilannenäkymä", () => {
  it("Näyttää listan oppijoista", async () => {
    await loginAs(hakutilannePath, "valpas-jkl-normaali")
    await urlIsEventually(pathToUrl(jklHakutilannePath))
    await textEventuallyEquals(
      ".card__header",
      "Hakeutumisvelvollisia oppijoita (16)"
    )
    await dataTableEventuallyEquals(
      ".hakutilanne",
      jklNormaalikouluTableContent,
      "|"
    )
  })

  it("Näyttää tyhjän listan virheittä, jos ei oppijoita", async () => {
    await loginAs(hakutilannePath, "valpas-saksalainen")
    await urlIsEventually(pathToUrl(saksalainenKouluHakutilannePath))
    await textEventuallyEquals(
      ".card__header",
      "Hakeutumisvelvollisia oppijoita (0)"
    )
  })

  it("Vaihtaa taulun sisällön organisaatiovalitsimesta", async () => {
    await loginAs(hakutilannePath, "valpas-useampi-peruskoulu")

    await selectOrganisaatio(0)
    await urlIsEventually(pathToUrl(jklHakutilannePath))
    await textEventuallyEquals(
      ".card__header",
      "Hakeutumisvelvollisia oppijoita (16)"
    )
    await dataTableEventuallyEquals(
      ".hakutilanne",
      jklNormaalikouluTableContent,
      "|"
    )

    await selectOrganisaatio(1)
    await urlIsEventually(pathToUrl(kulosaariHakutilannePath))
    await textEventuallyEquals(
      ".card__header",
      "Hakeutumisvelvollisia oppijoita (4)"
    )
  })

  it("Käyminen oppijakohtaisessa näkymässä ei hukkaa valittua organisaatiota", async () => {
    await loginAs(hakutilannePath, "valpas-useampi-peruskoulu")

    await selectOrganisaatio(1)
    await urlIsEventually(pathToUrl(kulosaariHakutilannePath))

    await clickOppija(3)
    await urlIsEventually(
      pathToUrl(
        createOppijaPath("/virkailija", {
          oppijaOid: kulosaarenOppijaOid,
          organisaatioOid: kulosaariOid,
        })
      )
    )

    await clickElement(".oppijaview__backbutton a")
    await urlIsEventually(pathToUrl(kulosaariHakutilannePath))
  })

  it("Oppijasivulta, jolta puuttuu organisaatioreferenssi, ohjataan oikean organisaation hakutilannenäkymään", async () => {
    await loginAs(
      createOppijaPath("/virkailija", {
        oppijaOid: kulosaarenOppijaOid,
      }),
      "valpas-useampi-peruskoulu"
    )

    await clickElement(".oppijaview__backbutton a")
    await urlIsEventually(pathToUrl(kulosaariHakutilannePath))
  })
})
