import React from "react"
import {
  fetchHenkilöhakuKunta,
  fetchHenkilöhakuKuntaCache,
} from "../../../api/api"
import { useApiMethod } from "../../../api/apiHooks"
import { Page } from "../../../components/containers/Page"
import { createKunnanHetuhakuPath } from "../../../state/paths"
import { OppijaSearch } from "../../../views/oppijasearch/OppijaSearch"
import { KuntaNavigation } from "../KuntaNavigation"

export const KuntaHetuhaku = () => {
  const search = useApiMethod(fetchHenkilöhakuKunta, fetchHenkilöhakuKuntaCache)

  return (
    <Page id="kuntahetuhaku">
      <KuntaNavigation />
      <OppijaSearch
        searchState={search}
        onQuery={search.call}
        prevPath={createKunnanHetuhakuPath()}
        eiLöytynytIlmoitusId={"oppijahaku__ei_tuloksia"}
        error403Id={"oppijahaku__ei_tuloksia"}
      />
    </Page>
  )
}
