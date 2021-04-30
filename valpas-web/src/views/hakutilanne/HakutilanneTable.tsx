import * as A from "fp-ts/lib/Array"
import { pipe } from "fp-ts/lib/function"
import { isNonEmpty, NonEmptyArray } from "fp-ts/lib/NonEmptyArray"
import * as O from "fp-ts/lib/Option"
import React, { useMemo } from "react"
import { Link } from "react-router-dom"
import {
  FutureSuccessIcon,
  SuccessIcon,
  WarningIcon,
} from "../../components/icons/Icon"
import { ExternalLink } from "../../components/navigation/ExternalLink"
import { DataTable, Datum, Value } from "../../components/tables/DataTable"
import {
  SelectableDataTable,
  SelectableDataTableProps,
} from "../../components/tables/SelectableDataTable"
import { getLocalized, t, Translation } from "../../i18n/i18n"
import { HakuSuppeatTiedot, selectByHakutoive } from "../../state/apitypes/haku"
import {
  isEiPaikkaa,
  isHyväksytty,
  isVarasijalla,
  isVastaanotettu,
  SuppeaHakutoive,
} from "../../state/apitypes/hakutoive"
import {
  OpiskeluoikeusSuppeatTiedot,
  taulukossaNäytettäväOpiskeluoikeus,
  valvottavatOpiskeluoikeudet,
} from "../../state/apitypes/opiskeluoikeus"
import { OppijaHakutilanteillaSuppeatTiedot } from "../../state/apitypes/oppija"
import {
  isVoimassa,
  isVoimassaTulevaisuudessa,
} from "../../state/apitypes/valpasopiskeluoikeudentila"
import { useBasePath } from "../../state/basePath"
import { Oid } from "../../state/common"
import { isFeatureFlagEnabled } from "../../state/featureFlags"
import { createOppijaPath } from "../../state/paths"
import { nonEmptyEvery, nonNull } from "../../utils/arrays"
import { formatDate, formatNullableDate } from "../../utils/date"

export type HakutilanneTableProps = {
  data: OppijaHakutilanteillaSuppeatTiedot[]
  organisaatioOid: string
} & Pick<SelectableDataTableProps, "onCountChange" | "onSelect">

const useOppijaData = (
  organisaatioOid: Oid,
  data: OppijaHakutilanteillaSuppeatTiedot[]
) => {
  const basePath = useBasePath()
  return useMemo(
    () => A.flatten(data.map(oppijaToTableData(basePath, organisaatioOid))),
    [organisaatioOid, data, basePath]
  )
}

export const HakutilanneTable = (props: HakutilanneTableProps) => {
  const data = useOppijaData(props.organisaatioOid, props.data)
  const TableComponent = isFeatureFlagEnabled("ilmoittaminen")
    ? SelectableDataTable
    : DataTable

  return (
    <TableComponent
      storageName="hakutilannetaulu"
      className="hakutilanne"
      columns={[
        {
          label: t("hakutilanne__taulu_nimi"),
          filter: "freetext",
          size: "large",
        },
        {
          label: t("hakutilanne__taulu_syntymäaika"),
          size: "small",
        },
        {
          label: t("hakutilanne__taulu_ryhma"),
          filter: "dropdown",
          size: "xsmall",
        },
        {
          label: t("hakutilanne__taulu_hakemuksen_tila"),
          filter: "dropdown",
        },
        {
          label: t("hakutilanne__taulu_valintatieto"),
          filter: "dropdown",
          indicatorSpace: "auto",
        },
        {
          label: t("hakutilanne__taulu_opiskelupaikka_vastaanotettu"),
          filter: "dropdown",
          indicatorSpace: "auto",
        },
        {
          label: t("hakutilanne__taulu_voimassaolevia_opiskeluoikeuksia"),
          filter: "dropdown",
          indicatorSpace: "auto",
        },
      ]}
      data={data}
      onCountChange={props.onCountChange}
      onSelect={props.onSelect}
    />
  )
}

const oppijaToTableData = (basePath: string, organisaatioOid: string) => (
  oppija: OppijaHakutilanteillaSuppeatTiedot
): Array<Datum> => {
  const henkilö = oppija.oppija.henkilö

  return valvottavatOpiskeluoikeudet(
    organisaatioOid,
    oppija.oppija.opiskeluoikeudet
  ).map((opiskeluoikeus) => ({
    key: opiskeluoikeus.oid,
    values: [
      {
        value: `${henkilö.sukunimi} ${henkilö.etunimet}`,
        display: (
          <Link
            to={createOppijaPath(basePath, {
              organisaatioOid,
              oppijaOid: henkilö.oid,
            })}
          >
            {henkilö.sukunimi} {henkilö.etunimet}
          </Link>
        ),
      },
      {
        value: henkilö.syntymäaika,
        display: formatNullableDate(henkilö.syntymäaika),
      },
      {
        value: opiskeluoikeus?.ryhmä,
      },
      hakemuksenTila(oppija, basePath),
      fromNullableValue(valintatila(oppija.hakutilanteet)),
      fromNullableValue(vastaanottotieto(oppija.hakutilanteet)),
      fromNullableValue(opiskeluoikeustiedot(oppija.oppija.opiskeluoikeudet)),
    ],
  }))
}

const hakemuksenTila = (
  oppija: OppijaHakutilanteillaSuppeatTiedot,
  basePath: string
): Value => {
  const { hakutilanteet, hakutilanneError } = oppija
  const oppijaOid = oppija.oppija.henkilö.oid

  const hakemuksenTilaValue = hakemuksenTilaT(
    hakutilanteet.length,
    hakutilanneError
  )
  return {
    value: hakemuksenTilaValue,
    display: hakemuksenTilaDisplay(
      hakutilanteet,
      hakemuksenTilaValue,
      oppijaOid,
      basePath
    ),
    tooltip: hakutilanteet.map(hakuTooltip).join("\n"),
  }
}

const hakemuksenTilaT = (
  hakemusCount: number,
  hakutilanneError?: string
): Translation => {
  if (hakutilanneError) return t("oppija__hakuhistoria_virhe")
  else if (hakemusCount == 0) return t("hakemuksentila__ei_hakemusta")
  else if (hakemusCount == 1) return t("hakemuksentila__hakenut")
  else return t("hakemuksentila__n_hakua", { lukumäärä: hakemusCount })
}

const hakemuksenTilaDisplay = (
  hakutilanteet: HakuSuppeatTiedot[],
  hakemuksenTilaValue: Translation,
  oppijaOid: Oid,
  basePath: string
) =>
  pipe(
    A.head(hakutilanteet),
    O.map((hakutilanne) =>
      hakutilanteet.length == 1 ? (
        <ExternalLink to={hakutilanne.hakemusUrl}>
          {hakemuksenTilaValue}
        </ExternalLink>
      ) : (
        <Link to={createOppijaPath(basePath, { oppijaOid })}>
          {hakemuksenTilaValue}
        </Link>
      )
    ),
    O.toNullable
  )

const hakuTooltip = (haku: HakuSuppeatTiedot): string =>
  t("hakemuksentila__tooltip", {
    haku: getLocalized(haku.hakuNimi) || "?",
    muokkausPvm: formatNullableDate(haku.muokattu),
  })

const fromNullableValue = (value: Value | null): Value =>
  value || {
    value: "–",
  }

const valintatila = (haut: HakuSuppeatTiedot[]): Value | null => {
  const hyväksytytHakutoiveet = selectByHakutoive(haut, isHyväksytty)
  if (isNonEmpty(hyväksytytHakutoiveet)) {
    return hyväksyttyValintatila(hyväksytytHakutoiveet)
  }

  const [varasija] = selectByHakutoive(haut, isVarasijalla)
  if (varasija) {
    return {
      value: t("valintatieto__varasija"),
      display: t("valintatieto__varasija_hakukohde", {
        hakukohde: getLocalized(varasija.organisaatioNimi) || "?",
      }),
    }
  }

  if (
    nonEmptyEvery(haut, (haku) => nonEmptyEvery(haku.hakutoiveet, isEiPaikkaa))
  ) {
    return {
      value: t("valintatieto__ei_opiskelupaikkaa"),
      icon: <WarningIcon />,
    }
  }

  return null
}

const hyväksyttyValintatila = (
  hyväksytytHakutoiveet: NonEmptyArray<SuppeaHakutoive>
): Value => {
  const buildHyväksyttyValue = (hakutoive: SuppeaHakutoive) => {
    return {
      value: t("valintatieto__hyväksytty", {
        hakukohde: orderedHakukohde(
          hakutoive.hakutoivenumero,
          t("valintatieto__hakukohde_lc")
        ),
      }),
      display: orderedHakukohde(
        hakutoive.hakutoivenumero,
        getLocalized(hakutoive.organisaatioNimi) || "?"
      ),
    }
  }

  if (hyväksytytHakutoiveet.length === 1) {
    return buildHyväksyttyValue(hyväksytytHakutoiveet[0])
  }

  return {
    value: t("valintatieto__hyväksytty_n_hakutoivetta", {
      lukumäärä: hyväksytytHakutoiveet.length,
    }),
    filterValues: hyväksytytHakutoiveet.map(
      (hakutoive) => buildHyväksyttyValue(hakutoive).value
    ),
    tooltip: hyväksytytHakutoiveet
      .map((ht) => buildHyväksyttyValue(ht).display)
      .join("\n"),
  }
}

const orderedHakukohde = (
  hakutoivenumero: number | undefined,
  hakukohde: string
) => (hakutoivenumero ? `${hakutoivenumero}. ${hakukohde}` : hakukohde)

const vastaanottotieto = (hakutilanteet: HakuSuppeatTiedot[]): Value | null => {
  const vastaanotetut = selectByHakutoive(hakutilanteet, isVastaanotettu)
  switch (vastaanotetut.length) {
    case 0:
      return null
    case 1:
      return {
        value: getLocalized(vastaanotetut[0]?.organisaatioNimi),
        icon: <SuccessIcon />,
      }
    default:
      return {
        value: t("vastaanotettu__n_paikkaa", {
          lukumäärä: vastaanotetut.length,
        }),
        tooltip: vastaanotetut
          .map((vo) => getLocalized(vo.organisaatioNimi))
          .join("\n"),
        icon: <SuccessIcon />,
      }
  }
}

const opiskeluoikeustiedot = (
  opiskeluoikeudet: OpiskeluoikeusSuppeatTiedot[]
): Value | null => {
  const oos = opiskeluoikeudet.filter(taulukossaNäytettäväOpiskeluoikeus)

  const toValue = (oo: OpiskeluoikeusSuppeatTiedot) => {
    const kohde = [
      getLocalized(oo.oppilaitos.nimi),
      getLocalized(oo.tyyppi.nimi),
    ]
      .filter(nonNull)
      .join(", ")

    return isVoimassa(oo.tarkastelupäivänTila)
      ? kohde
      : t("opiskeluoikeudet__pvm_alkaen_kohde", {
          päivämäärä: formatDate(oo.alkamispäivä),
          kohde,
        })
  }

  const icon = oos.some((oo) => isVoimassa(oo.tarkastelupäivänTila)) ? (
    <SuccessIcon />
  ) : oos.some((oo) => isVoimassaTulevaisuudessa(oo.tarkastelupäivänTila)) ? (
    <FutureSuccessIcon />
  ) : undefined

  switch (oos.length) {
    case 0:
      return null
    case 1:
      return { value: toValue(oos[0]!!), icon }
    default:
      const filterValues = oos.map(toValue).filter(nonNull)
      return {
        value: t("opiskeluoikeudet__n_opiskeluoikeutta", {
          lukumäärä: oos.length,
        }),
        filterValues,
        tooltip: filterValues.join("\n"),
        icon,
      }
  }
}
