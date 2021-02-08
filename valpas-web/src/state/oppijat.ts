import {
  HakemuksentilaKoodistoviite,
  Opiskeluoikeudentyyppi,
  ValintatietotilaKoodistoviite,
} from "./koodistot"
import { ISODate, LocalizedString, Oid } from "./types"

export type Oppija = {
  henkilö: Henkilö
  opiskeluoikeudet: Opiskeluoikeus[]
  haut?: Haku[]
}

export type Henkilö = {
  oid: Oid
  hetu: string
  syntymäaika: ISODate
  etunimet: string
  sukunimi: string
}

export type Oppilaitos = {
  oid: Oid
  nimi: LocalizedString
}

export type Haku = {
  nimi: LocalizedString
  luotu: ISODate
  tila: HakemuksentilaKoodistoviite
  valintatiedot: Valintatieto[]
}

export type Valintatieto = {
  hakukohdenumero?: number
  hakukohde: Oppilaitos
  tila?: ValintatietotilaKoodistoviite
  pisteet?: number
  alinPistemäärä?: number
}

export type Opiskeluoikeus = {
  oid: Oid
  tyyppi: Opiskeluoikeudentyyppi
  oppilaitos: Oppilaitos
  alkamispäivä?: ISODate
  arvioituPäättymispäivä?: ISODate
  päättymispäivä?: ISODate
  ryhmä?: string
}

export type OpiskeluoikeudenTila = {}
