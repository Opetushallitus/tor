const { createProxyMiddleware } = require("http-proxy-middleware")
const express = require("express")

const STATIC_RESPONSE_APP_PORT = 10101

const staticApp = express()
staticApp.get("/**", (_req, res) => res.send(""))
staticApp.listen(STATIC_RESPONSE_APP_PORT)

module.exports = function (app) {
  setupBackendProxy(app)
  setupRaamitProxy(app)
}

const setupBackendProxy = (app) => {
  const backendHost = process.env.BACKEND_HOST || "http://localhost:7021"
  app.use(
    createProxyMiddleware("/koski", {
      target: `${backendHost}/`,
      changeOrigin: true,
    })
  )
}

const setupRaamitProxy = (app) => {
  const target =
    process.env.VIRKAILIJA_RAAMIT_HOST ||
    `http://localhost:${STATIC_RESPONSE_APP_PORT}`
  app.use(
    createProxyMiddleware("/virkailija-raamit", {
      target,
      changeOrigin: true,
    })
  )
}
