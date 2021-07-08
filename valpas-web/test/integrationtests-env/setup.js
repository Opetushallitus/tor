require("dotenv")
const { spawn } = require("child_process")

module.exports = async () => {
  global.__PARCEL_SERVE_PROCESS__ = await serve()
}

/*
 * TODO:
 *
 * Parcel API ei toimi kunnolla (tilanne versiolla 2.0.0-nightly.754):
 * palvelin ei lähde käyntiin ja koko prosessi vain kuolee ilman virheilmoitusta.
 *
 * Korjauksena käynnistetään palvelin tällä tavalla rumasti omaan prosessiin
 * ja kuunnellaan stdoutista milloinko bundle on saatu käännettyä.
 *
 * Kunhan tuo API joskus taas toimii, sen voisi ottaa käyttöön.
 * Lisätietoja: https://v2.parceljs.org/features/parcel-api/
 */
const serve = () =>
  new Promise((resolve, _reject) => {
    const parcel = spawn("npm", ["start", "--", "--no-hmr", "--no-cache"])
    parcel.stdout.on("data", (stdout) => {
      log(stdout)
      if (stdout.includes("✨ Built in")) {
        resolve(parcel)
      }
    })

    parcel.stderr.on("data", log)
    parcel.on("error", log)
    parcel.on("close", () => console.log("Parcel process closed"))
  })

const log = (buffer) => console.log(buffer.toString())
