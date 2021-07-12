import * as A from "fp-ts/Array"
import { pipe } from "fp-ts/lib/function"
import React, { useContext, useMemo } from "react"
import { Redirect } from "react-router-dom"
import {
  Kayttooikeusrooli,
  käyttöoikeusrooliEq,
  OrganisaatioJaKayttooikeusrooli,
} from "../state/common"
import { Organisaatio } from "./apitypes/organisaatiot"

export type AccessGuard = (roles: Kayttooikeusrooli[]) => boolean

export const hakeutumisenValvontaAllowed: AccessGuard = (roles) =>
  roles.includes("OPPILAITOS_HAKEUTUMINEN")

export const maksuttomuudenValvontaAllowed: AccessGuard = (roles) =>
  roles.includes("OPPILAITOS_MAKSUTTOMUUS")

export const suorittamisenValvontaAllowed: AccessGuard = (roles) =>
  roles.includes("OPPILAITOS_SUORITTAMINEN")

export const kuntavalvontaAllowed: AccessGuard = (roles) =>
  roles.includes("KUNTA")

export const someOf = (...accessGuards: AccessGuard[]): AccessGuard => (
  roles
) => accessGuards.some((guard) => guard(roles))

export type WithRequiresAccessRightsProps = {
  redirectUserWithoutAccessTo: string
}

const accessRightGuardHoc = (hasAccess: AccessGuard) => <P extends object>(
  Component: React.ComponentType<P>
): React.FC<P & WithRequiresAccessRightsProps> => (
  props: WithRequiresAccessRightsProps
) => {
  const roles = useKäyttöoikeusroolit()
  return hasAccess(roles) ? (
    <Component {...(props as P)} />
  ) : (
    <Redirect to={props.redirectUserWithoutAccessTo} />
  )
}

export const withRequiresHakeutumisenValvonta = accessRightGuardHoc(
  hakeutumisenValvontaAllowed
)

export const withRequiresJokinOikeus = accessRightGuardHoc(
  someOf(
    hakeutumisenValvontaAllowed,
    maksuttomuudenValvontaAllowed,
    kuntavalvontaAllowed,
    suorittamisenValvontaAllowed
  )
)

export const withRequiresKuntavalvonta = accessRightGuardHoc(
  kuntavalvontaAllowed
)

const käyttöoikeusroolitContext = React.createContext<
  OrganisaatioJaKayttooikeusrooli[]
>([])

export const KäyttöoikeusroolitProvider = käyttöoikeusroolitContext.Provider

export const useOrganisaatiotJaKäyttöoikeusroolit = () =>
  useContext(käyttöoikeusroolitContext)

export const useKäyttöoikeusroolit = (): Kayttooikeusrooli[] => {
  const data = useOrganisaatiotJaKäyttöoikeusroolit()
  return useMemo(
    () =>
      pipe(
        data,
        A.map((käyttöoikeus) => käyttöoikeus.kayttooikeusrooli),
        A.uniq(käyttöoikeusrooliEq)
      ),
    [data]
  )
}

export const useOrganisaatiot = () => {
  const organisaatiotJaKäyttöoikeusroolit = useOrganisaatiotJaKäyttöoikeusroolit()
  return useMemo(() => getOrganisaatiot(organisaatiotJaKäyttöoikeusroolit), [
    organisaatiotJaKäyttöoikeusroolit,
  ])
}

export const useOrganisaatiotOfRole = (accessGuard: AccessGuard) => {
  const organisaatiotJaKäyttöoikeusroolit = useOrganisaatiotJaKäyttöoikeusroolit()
  return useMemo(
    () => getOrganisaatiot(organisaatiotJaKäyttöoikeusroolit, accessGuard),
    [accessGuard, organisaatiotJaKäyttöoikeusroolit]
  )
}

const getOrganisaatiot = (
  orgs: OrganisaatioJaKayttooikeusrooli[],
  accessGuard?: AccessGuard
): Organisaatio[] =>
  orgs
    .filter((org) => !accessGuard || accessGuard([org.kayttooikeusrooli]))
    .map((org) => ({
      nimi: org.organisaatioHierarkia.nimi,
      oid: org.organisaatioHierarkia.oid,
    }))
