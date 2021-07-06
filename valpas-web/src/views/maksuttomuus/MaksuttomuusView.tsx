import React from "react"
import {
  fetchHenkilöhakuMaksuttomuus,
  fetchHenkilöhakuMaksuttomuusCache,
} from "../../api/api"
import { useApiMethod } from "../../api/apiHooks"
import { Page } from "../../components/containers/Page"
import { createMaksuttomuusPath } from "../../state/paths"
import { OppijaSearch } from "../oppijasearch/OppijaSearch"

export const MaksuttomuusView = () => {
  const search = useApiMethod(
    fetchHenkilöhakuMaksuttomuus,
    fetchHenkilöhakuMaksuttomuusCache
  )

  return (
    <Page id="maksuttomuus">
      <OppijaSearch
        searchState={search}
        onQuery={search.call}
        prevPath={createMaksuttomuusPath()}
        eiLöytynytIlmoitusId={
          "oppijahaku__maksuttomuutta_ei_pysty_päättelemään"
        }
        error403Id={"oppijahaku__maksuttomuus_ei_näytettävä_oppija"}
      />
    </Page>
  )
}
