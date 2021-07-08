import { LocalizationMap } from "../i18n/i18n"
import { HenkilöhakuResult } from "../state/apitypes/henkilohaku"
import { KuntailmoitusLaajatTiedot } from "../state/apitypes/kuntailmoitus"
import { KuntailmoitusPohjatiedot } from "../state/apitypes/kuntailmoituspohjatiedot"
import { OpiskeluoikeusSuppeatTiedot } from "../state/apitypes/opiskeluoikeus"
import {
  OppijaHakutilanteillaLaajatTiedot,
  OppijaHakutilanteillaSuppeatTiedot,
  OppijaKuntailmoituksillaSuppeatTiedot,
} from "../state/apitypes/oppija"
import { UusiOppivelvollisuudenKeskeytys } from "../state/apitypes/oppivelvollisuudenkeskeytys"
import {
  Hetu,
  Oid,
  OrganisaatioJaKayttooikeusrooli,
  User,
} from "../state/common"
import { queryPath } from "../state/paths"
import { tapLeftP } from "../utils/either"
import { ApiFailure, apiGet, apiPost, apiPut } from "./apiFetch"
import { createLocalThenApiCache, createPreferLocalCache } from "./cache"

export const healthCheck = async () =>
  apiGet<string>("api/healthcheck/internal")

/**
 * Window propreties
 */
export const fetchWindowProperties = () =>
  apiGet<{
    valpasLocalizationMap: LocalizationMap
    environment: string
    opintopolkuVirkailijaUrl: string
  }>("valpas/localization/window-properties")

/**
 * Login
 */
export const fetchLogin = async (username: string, password: string) =>
  apiPost<User>("valpas/login", {
    body: {
      username,
      password,
    },
  })

/**
 * Hae kirjautuneen käyttäjän tiedot
 */
export const fetchCurrentUser = async () => apiGet<User>("valpas/api/user")

/**
 * Hae lista organisaatioista käyttöoikeuksien kanssa
 */
export const fetchYlatasonOrganisaatiotJaKayttooikeusroolit = async () =>
  handleExpiredSession(
    apiGet<OrganisaatioJaKayttooikeusrooli[]>(
      "valpas/api/organisaatiot-ja-kayttooikeusroolit"
    )
  )

export const fetchYlatasonOrganisaatiotJaKayttooikeusroolitCache = createLocalThenApiCache(
  fetchYlatasonOrganisaatiotJaKayttooikeusroolit
)

/**
 * Hae suppeat tiedot oppijoista
 */
export const fetchOppijat = (organisaatioOid: Oid) =>
  handleExpiredSession(
    apiGet<OppijaHakutilanteillaSuppeatTiedot[]>(
      `valpas/api/oppijat/${organisaatioOid}`
    )
  )

export const fetchOppijatCache = createPreferLocalCache(fetchOppijat)

/**
 * Hae yksittäisen oppijan laajat tiedot
 */
export const fetchOppija = (oppijaOid: Oid) =>
  handleExpiredSession(
    apiGet<OppijaHakutilanteillaLaajatTiedot>(`valpas/api/oppija/${oppijaOid}`)
  )

export const fetchOppijaCache = createLocalThenApiCache(fetchOppija)

/**
 * Etsi henkilöä hetulla/oidilla maksuttomuuskäyttäjänä
 */
export const fetchHenkilöhakuMaksuttomuus = (query: Oid | Hetu) =>
  handleExpiredSession(
    apiGet<HenkilöhakuResult>(`valpas/api/henkilohaku/maksuttomuus/${query}`)
  )

export const fetchHenkilöhakuMaksuttomuusCache = createLocalThenApiCache(
  fetchHenkilöhakuMaksuttomuus
)

/**
 * Etsi henkilöä hetulla/oidilla suorittamisen valvojana
 */
export const fetchHenkilöhakuSuorittaminen = (query: Oid | Hetu) =>
  handleExpiredSession(
    apiGet<HenkilöhakuResult>(`valpas/api/henkilohaku/suorittaminen/${query}`)
  )

export const fetchHenkilöhakuSuorittaminenCache = createLocalThenApiCache(
  fetchHenkilöhakuSuorittaminen
)

/**
 * Etsi henkilöä hetulla/oidilla kunnan käyttäjänä
 */
export const fetchHenkilöhakuKunta = (query: Oid | Hetu) =>
  handleExpiredSession(
    apiGet<HenkilöhakuResult>(`valpas/api/henkilohaku/kunta/${query}`)
  )

export const fetchHenkilöhakuKuntaCache = createLocalThenApiCache(
  fetchHenkilöhakuKunta
)

/**
 * Kuntailmoituksen pohjatietojen haku
 */
export const fetchKuntailmoituksenPohjatiedot = (
  oppijaOids: Oid[],
  tekijäOrganisaatioOid?: Oid
) =>
  handleExpiredSession(
    apiPost<KuntailmoitusPohjatiedot>("valpas/api/kuntailmoitus/pohjatiedot", {
      body: {
        tekijäOrganisaatio: tekijäOrganisaatioOid
          ? {
              oid: tekijäOrganisaatioOid,
            }
          : undefined,
        oppijaOidit: oppijaOids,
      },
    })
  )

/**
 * Kuntailmoituksen tallennus
 */
export const createKuntailmoitus = (
  oppijaOid: Oid,
  kuntailmoitus: KuntailmoitusLaajatTiedot
) =>
  handleExpiredSession(
    apiPost<void>("valpas/api/kuntailmoitus", {
      body: {
        oppijaOid,
        kuntailmoitus,
      },
    })
  )

/**
 * Kuntailmoitusten hakeminen
 */
export const fetchKuntailmoitukset = (kuntaOid: Oid) =>
  handleExpiredSession(
    apiGet<OppijaKuntailmoituksillaSuppeatTiedot[]>(
      `valpas/api/kuntailmoitus/oppijat/${kuntaOid}/aktiiviset`
    )
  )

export const fetchKuntailmoituksetCache = createPreferLocalCache(
  fetchKuntailmoitukset
)

/**
 * Tallenna muu haku -valitsimen tila
 * @param oppijaOid
 * @param opiskeluoikeusOid
 * @param oppilaitosOid
 * @param value
 * @returns
 */
export const setMuuHaku = async (
  oppijaOid: Oid,
  opiskeluoikeus: OpiskeluoikeusSuppeatTiedot,
  value: boolean
) =>
  handleExpiredSession(
    apiPut(
      queryPath(`valpas/api/oppija/${oppijaOid}/set-muu-haku`, {
        opiskeluoikeusOid: opiskeluoikeus.oid,
        oppilaitosOid: opiskeluoikeus.oppilaitos.oid,
        value,
      })
    )
  )

/**
 * Oppivelvollisuuden keskeytyksen lisäys
 */
export const createOppivelvollisuudenKeskeytys = (
  keskeytys: UusiOppivelvollisuudenKeskeytys
) =>
  handleExpiredSession(
    apiPost<void>("valpas/api/oppija/ovkeskeytys", { body: keskeytys })
  )

// Virhetilanteiden hallinta

const handleExpiredSession = tapLeftP((failure: ApiFailure) => {
  if (failure.status === 401) {
    // 401 Unauthorized -> käyttäjä ei ole (enää) kirjautunut -> lähetä kirjautumiseen lataamalla sivu uudelleen.
    // Kirjautuminen ei laukea siitä, että käyttäjä yritti nähdä tietoa, johon hänellä ei ole oikeutta,
    // koska Valpas-APIt palauttavat siinä tapauksessa 403 Forbidden.
    location.reload()
  }
})
