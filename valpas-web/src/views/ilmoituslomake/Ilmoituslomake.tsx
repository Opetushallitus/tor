import React, { useState } from "react"
import { ModalButtonGroup } from "../../components/buttons/ModalButtonGroup"
import { RaisedButton } from "../../components/buttons/RaisedButton"
import { Modal } from "../../components/containers/Modal"
import { t, T } from "../../i18n/i18n"
import { HenkilöTiedot } from "../../state/apitypes/henkilo"
import { KuntailmoituksenTekijäLaajatTiedot } from "../../state/apitypes/kuntailmoitus"
import { KuntailmoitusPohjatiedot } from "../../state/apitypes/kuntailmoituspohjatiedot"
import { OpiskeluoikeusSuppeatTiedot } from "../../state/apitypes/opiskeluoikeus"
import { OpiskeluoikeusLisätiedot } from "../../state/apitypes/oppija"
import { Organisaatio } from "../../state/apitypes/organisaatiot"
import { Oid } from "../../state/common"
import { IlmoituksenTekijäForm } from "./IlmoituksenTekijäForm"
import { IlmoitusForm } from "./IlmoitusForm"

export type IlmoituslomakeOppijaTiedot = {
  henkilö: HenkilöTiedot
  opiskeluoikeudet?: OpiskeluoikeusSuppeatTiedot[]
  lisätiedot?: OpiskeluoikeusLisätiedot[]
}

export type IlmoituslomakeProps = {
  oppijat: IlmoituslomakeOppijaTiedot[]
  pohjatiedot: KuntailmoitusPohjatiedot
  tekijäorganisaatio: Organisaatio
  onClose: () => void
}

export const Ilmoituslomake = (props: IlmoituslomakeProps) => {
  const [submittedForms, setSubmittedForms] = useState<Oid[]>([])
  const allFormsSubmitted =
    submittedForms.length === props.pohjatiedot.oppijat.length

  const initialTekijä: KuntailmoituksenTekijäLaajatTiedot = {
    organisaatio: props.tekijäorganisaatio,
    henkilö: props.pohjatiedot.tekijäHenkilö,
  }

  const [
    tekijä,
    setTekijä,
  ] = useState<KuntailmoituksenTekijäLaajatTiedot | null>(null)

  if (!tekijä) {
    return (
      <Modal title={t("ilmoituslomake__otsikko")} onClose={props.onClose}>
        <p>
          <T id="ilmoituksentekijälomake__ohje" />
        </p>

        <IlmoituksenTekijäForm
          tekijä={initialTekijä}
          organisaatiot={props.pohjatiedot.mahdollisetTekijäOrganisaatiot}
          onSubmit={setTekijä}
        />
      </Modal>
    )
  }

  return (
    <Modal title={t("ilmoituslomake__otsikko")} onClose={props.onClose}>
      {!allFormsSubmitted && (
        <p>
          <T id="ilmoituslomake__ohje" />
        </p>
      )}

      {props.pohjatiedot.oppijat.map((oppija, index) => {
        const oppijaTiedot = props.oppijat.find(
          (o) => o.henkilö.oid === oppija.oppijaOid
        )!!

        return (
          <IlmoitusForm
            key={oppija.oppijaOid}
            formIndex={index}
            numberOfForms={props.pohjatiedot.oppijat.length}
            oppijaTiedot={oppijaTiedot.henkilö}
            lisätiedot={oppijaTiedot.lisätiedot}
            opiskeluoikeudet={oppijaTiedot.opiskeluoikeudet}
            pohjatiedot={oppija}
            kunnat={props.pohjatiedot.kunnat}
            maat={props.pohjatiedot.maat}
            tekijä={tekijä}
            kielet={props.pohjatiedot.yhteydenottokielet}
            onSubmit={() =>
              setSubmittedForms([...submittedForms, oppija.oppijaOid])
            }
          />
        )
      })}

      <ModalButtonGroup>
        {allFormsSubmitted ? (
          <RaisedButton hierarchy="primary" onClick={props.onClose}>
            <T id="ilmoituslomake__valmis" />
          </RaisedButton>
        ) : (
          <RaisedButton hierarchy="secondary" onClick={props.onClose}>
            <T id="ilmoituslomake__sulje" />
          </RaisedButton>
        )}
      </ModalButtonGroup>
    </Modal>
  )
}
