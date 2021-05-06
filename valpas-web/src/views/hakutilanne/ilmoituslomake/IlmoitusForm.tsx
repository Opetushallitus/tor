import bem from "bem-ts"
import React, { useState } from "react"
import { RaisedButton } from "../../../components/buttons/RaisedButton"
import { LabeledCheckbox } from "../../../components/forms/Checkbox"
import { Dropdown, koodistoToOptions } from "../../../components/forms/Dropdown"
import { TextField } from "../../../components/forms/TextField"
import {
  CaretDownIcon,
  CaretUpIcon,
  SuccessCircleIcon,
} from "../../../components/icons/Icon"
import { Error } from "../../../components/typography/error"
import { SecondaryHeading } from "../../../components/typography/headings"
import { T, t } from "../../../i18n/i18n"
import { HenkilöSuppeatTiedot } from "../../../state/apitypes/henkilo"
import { KoodistoKoodiviite } from "../../../state/apitypes/koodistot"
import { OppijaHakutilanteillaSuppeatTiedot } from "../../../state/apitypes/oppija"
import { expectNonEmptyString } from "../../../state/formValidators"
import { FormValidators, useFormState } from "../../../state/useFormState"
import { plainComponent } from "../../../utils/plaincomponent"
import "./IlmoitusForm.less"

const b = bem("ilmoitusform")

const koodiarvoFinland = "246"

export type IlmoitusFormValues = {
  asuinkunta?: string
  yhteydenottokieli?: string
  maa?: string
  postinumero: string
  postitoimipaikka: string
  katuosoite: string
  puhelinnumero: string
  sähköposti: string
  hakenutOpiskelemaanYhteyshakujenUlkopuolella: boolean
}

const initialValues: IlmoitusFormValues = {
  asuinkunta: undefined,
  yhteydenottokieli: "FI",
  maa: koodiarvoFinland,
  postinumero: "",
  postitoimipaikka: "",
  katuosoite: "",
  puhelinnumero: "",
  sähköposti: "",
  hakenutOpiskelemaanYhteyshakujenUlkopuolella: false,
}

const validators: FormValidators<IlmoitusFormValues> = {
  asuinkunta: [expectNonEmptyString("ilmoituslomake__pakollinen_tieto")],
  yhteydenottokieli: [],
  maa: [],
  postitoimipaikka: [],
  postinumero: [],
  katuosoite: [],
  puhelinnumero: [],
  sähköposti: [],
  hakenutOpiskelemaanYhteyshakujenUlkopuolella: [],
}

export type IlmoitusFormProps = {
  oppija: OppijaHakutilanteillaSuppeatTiedot
  kunnat: Array<KoodistoKoodiviite>
  maat: Array<KoodistoKoodiviite>
  kielet: Array<KoodistoKoodiviite>
  formIndex: number
  numberOfForms: number
  prefilledValues: PrefilledIlmoitusFormValues[]
  onSubmit?: (values: IlmoitusFormValues) => void
}

export type PrefilledIlmoitusFormValues = {
  label: string
  values: Partial<IlmoitusFormValues>
}

export const IlmoitusForm = (props: IlmoitusFormProps) => {
  const form = useFormState({ initialValues, validators })
  const [isOpen, setOpen] = useState(true)
  const [isSubmitted, setSubmitted] = useState(false)

  const submit = form.submitCallback((formData) => {
    setSubmitted(true)
    if (props.onSubmit) {
      props.onSubmit(formData)
    }
  })

  return (
    <IlmoitusFormFrame>
      <IlmoitusHeader
        henkilö={props.oppija.oppija.henkilö}
        formIndex={props.formIndex}
        numberOfForms={props.numberOfForms}
        isOpen={isOpen}
        isSubmitted={isSubmitted}
        onClick={() => setOpen(!isOpen)}
      />
      {isOpen && !isSubmitted ? (
        <IlmoitusBody>
          <IlmoitusPrefillSelector
            prefilledValues={props.prefilledValues}
            onSelect={form.patch}
          />
          <Dropdown
            label={t("ilmoituslomake__asuinkunta")}
            required
            options={koodistoToOptions(props.kunnat)}
            {...form.fieldProps("asuinkunta")}
          />
          <Dropdown
            label={t("ilmoituslomake__yhteydenottokieli")}
            options={koodistoToOptions(props.kielet)}
            {...form.fieldProps("yhteydenottokieli")}
          />
          <SecondaryHeading className={b("muutyhteystiedototsikko")}>
            <T id="ilmoituslomake__muut_yhteystiedot" />
          </SecondaryHeading>
          <Dropdown
            label={t("ilmoituslomake__maa")}
            options={koodistoToOptions(props.maat)}
            {...form.fieldProps("maa")}
          />
          <TextField
            label={t("ilmoituslomake__postinumero")}
            {...form.fieldProps("postinumero")}
          />
          <TextField
            label={t("ilmoituslomake__postitoimipaikka")}
            {...form.fieldProps("postitoimipaikka")}
          />
          <TextField
            label={t("ilmoituslomake__katuosoite")}
            {...form.fieldProps("katuosoite")}
          />
          <TextField
            label={t("ilmoituslomake__puhelinnumero")}
            {...form.fieldProps("puhelinnumero")}
          />
          <TextField
            label={t("ilmoituslomake__sähköposti")}
            {...form.fieldProps("sähköposti")}
          />
          <LabeledCheckbox
            label={t(
              "ilmoituslomake__hakenut_opiskelemaan_yhteishakujen_ulkopuolella"
            )}
            {...form.fieldProps("hakenutOpiskelemaanYhteyshakujenUlkopuolella")}
          />
          {form.allFieldsValidated && !form.isValid ? (
            <Error>
              <T id="ilmoituslomake__täytä_pakolliset_tiedot" />
            </Error>
          ) : null}
          <RaisedButton
            disabled={form.isValid ? false : "byLook"}
            onClick={form.isValid ? submit : form.validateAll}
          >
            <T id="ilmoituslomake__ilmoita_asuinkunnalle" />
          </RaisedButton>
        </IlmoitusBody>
      ) : null}
    </IlmoitusFormFrame>
  )
}

export type IlmoitusHeaderProps = {
  henkilö: HenkilöSuppeatTiedot
  formIndex: number
  numberOfForms: number
  isOpen: boolean
  isSubmitted: boolean
  onClick: () => void
}

const IlmoitusHeader = (props: IlmoitusHeaderProps) => (
  <IlmoitusHeaderFrame onClick={props.onClick}>
    <IlmoitusTitle>
      <IlmoitusTitleIndex>
        {props.formIndex + 1}/{props.numberOfForms}
      </IlmoitusTitleIndex>
      <IlmoitusTitleTexts>
        <IlmoitusTitleText>
          {props.henkilö.sukunimi} {props.henkilö.etunimet}
        </IlmoitusTitleText>
        <IlmoitusSubtitle>Oppija {props.henkilö.oid}</IlmoitusSubtitle>
      </IlmoitusTitleTexts>
      {!props.isSubmitted ? (
        <IlmoitusTitleCaret>
          {props.isOpen ? <CaretDownIcon /> : <CaretUpIcon />}
        </IlmoitusTitleCaret>
      ) : null}
    </IlmoitusTitle>
    {props.isSubmitted ? (
      <IlmoitusSubmitted>
        <SuccessCircleIcon inline color="white" />
        <T id="ilmoituslomake__ilmoitus_lähetetty" />
      </IlmoitusSubmitted>
    ) : null}
  </IlmoitusHeaderFrame>
)

const IlmoitusFormFrame = plainComponent("div", b("frame"))
const IlmoitusHeaderFrame = plainComponent("header", b("header"))
const IlmoitusTitle = plainComponent("h3", b("title"))
const IlmoitusTitleIndex = plainComponent("div", b("titleindex"))
const IlmoitusTitleTexts = plainComponent("div", b("titletexts"))
const IlmoitusTitleText = plainComponent("div", b("titletext"))
const IlmoitusTitleCaret = plainComponent("div", b("titlecaret"))
const IlmoitusSubtitle = plainComponent("h4", b("subtitle"))
const IlmoitusSubmitted = plainComponent("div", b("submitted"))
const IlmoitusBody = plainComponent("div", b("body"))

type IlmoitusPrefillSelectorProps = {
  prefilledValues: PrefilledIlmoitusFormValues[]
  onSelect: (values: Partial<IlmoitusFormValues>) => void
}

const IlmoitusPrefillSelector = (props: IlmoitusPrefillSelectorProps) => (
  <div className={b("prefill")}>
    <T id="ilmoituslomake__esitäytä_yhteystiedoilla" />
    <ul className={b("prefilllist")}>
      {props.prefilledValues.map((prefill, index) => (
        <li
          key={index}
          className={b("prefillitem")}
          onClick={() => props.onSelect(prefill.values)}
        >
          {index + 1}) {prefill.label}
        </li>
      ))}
    </ul>
  </div>
)
