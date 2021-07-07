import * as E from "fp-ts/Either"
import React from "react"
import ReactDOM from "react-dom"
import { fetchWindowProperties } from "./api/api"
import { enableFeature, Feature } from "./state/featureFlags"
import "./style/index.less"
import { ValpasApp } from "./views/ValpasApp"

declare global {
  interface Window {
    environment?: string
    virkailija_raamit_set_to_load?: boolean
    opintopolkuVirkailijaUrl?: string
    enableFeature?: (feature: Feature) => void
  }
}

async function loadWindowProperties() {
  const response = await fetchWindowProperties()
  if (E.isRight(response)) {
    const props = response.right.data
    window.valpasLocalizationMap = props.valpasLocalizationMap
    window.environment = props.environment
    window.opintopolkuVirkailijaUrl = props.opintopolkuVirkailijaUrl
  }
  if (E.isLeft(response)) {
    console.error("Konfiguraation haku epäonnistui:", response.left)
  }
}

async function main() {
  await loadWindowProperties()
  ReactDOM.render(<ValpasApp />, document.getElementById("app"))
}

main()

window.enableFeature = enableFeature
