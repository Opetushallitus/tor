import React from "react"
import {
  fetchHenkilöhakuSuorittaminen,
  fetchHenkilöhakuSuorittaminenCache,
} from "../../api/api"
import { useApiMethod } from "../../api/apiHooks"
import { Page } from "../../components/containers/Page"
import { T } from "../../i18n/i18n"
import { createSuorittaminenPath } from "../../state/paths"
import { OppijaSearch } from "../oppijasearch/OppijaSearch"

export const SuorittaminenView = () => {
  const search = useApiMethod(
    fetchHenkilöhakuSuorittaminen,
    fetchHenkilöhakuSuorittaminenCache
  )

  return (
    <Page id="suorittaminen">
      <p>
        <T id={"oppijahaku__suorittaminen_väliaikainen_viesti"} />
      </p>
      <OppijaSearch
        searchState={search}
        onQuery={search.call}
        prevPath={createSuorittaminenPath()}
        eiLöytynytIlmoitusId={"oppijahaku__ei_tuloksia"}
        error403Id={"oppijahaku__ei_tuloksia"}
      />
    </Page>
  )
}
