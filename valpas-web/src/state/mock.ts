import { KoodistoKoodiviite } from "./koodistot"
import { Oppija } from "./oppijat"

const mockKoodistoviite = <T extends string>(koodistoUri: T) => <
  S extends string
>(
  koodiarvo: S,
  nimi?: string
): KoodistoKoodiviite<T, S> => ({
  koodistoUri,
  koodiarvo,
  nimi: { fi: `${nimi || koodiarvo}` },
})

// Mock-dataa, joka siirtyy myöhemmin backendin puolelle
const hakemuksentila = mockKoodistoviite("hakemuksentila")
const valintatieto = mockKoodistoviite("valintatietotila")
const opiskeluoikeudentyyppi = mockKoodistoviite("opiskeluoikeudentyyppi")

export const mockOppijat: Oppija[] = [
  {
    oid: "1.123.123.123.123.123.1",
    nimi: "Aaltonen Ada Adalmiina",
    hetu: "291105A636C",
    oppilaitos: {
      oid: "1.123.123.123.123.123.123",
      nimi: { fi: "Järvenpään yhteiskoulu" },
    },
    syntymaaika: "2005-07-31",
    ryhmä: "9A",
    haut: [
      {
        nimi: { fi: "Yhteishaku 2021" },
        luotu: "2021-02-03",
        tila: hakemuksentila("aktiivinen"),
        valintatiedot: [
          {
            hakukohdenumero: 1,
            hakukohde: {
              oid: "1.3.3.3.3.3.3",
              nimi: { fi: "Ressun lukio" },
            },
            tila: valintatieto("läsnä"),
            pisteet: 7.29,
            alinPistemäärä: 7.0,
          },
        ],
      },
    ],
    opiskeluoikeushistoria: [
      {
        oid: "1.2.3.4.5.6.7",
        tyyppi: opiskeluoikeudentyyppi("perusopetus", "Perusopetus"),
        oppilaitos: {
          oid: "1.3.4.5.6.7.8.9",
          nimi: { fi: "Järvenpään yhteiskoulu" },
        },
        ryhmä: "9A",
        alkamispäivä: "2012-08-01",
        arvioituPäättymispäivä: "2021-06-01",
      },
    ],
  },
  {
    oid: "1.123.123.123.123.123.2",
    nimi: "Kinnunen Jami Jalmari",
    hetu: "120605A823D",
    oppilaitos: {
      oid: "1.123.123.123.123.123.123",
      nimi: { fi: "Järvenpään yhteiskoulu" },
    },
    syntymaaika: "2005-07-31",
    ryhmä: "9A",
    haut: [
      {
        nimi: { fi: "Yhteishaku 2021" },
        luotu: "2021-02-03",
        tila: hakemuksentila("aktiivinen"),
        valintatiedot: [
          {
            hakukohdenumero: 1,
            hakukohde: {
              oid: "1.3.3.3.3.3.3",
              nimi: { fi: "Ressun lukio" },
            },
            tila: valintatieto("vastaanotettu"),
          },
        ],
      },
    ],
  },
  {
    oid: "1.123.123.123.123.123.3",
    nimi: "Laitela Niklas Henri",
    hetu: "240505A5385",
    oppilaitos: {
      oid: "1.123.123.123.123.123.123",
      nimi: { fi: "Järvenpään yhteiskoulu" },
    },
    syntymaaika: "2005-07-31",
    ryhmä: "9A",
    haut: [],
  },
  {
    oid: "1.123.123.123.123.123.4",
    nimi: "Mäkinen Tapio Kalervo",
    hetu: "140805A143C",
    oppilaitos: {
      oid: "1.123.123.123.123.123.123",
      nimi: { fi: "Järvenpään yhteiskoulu" },
    },
    syntymaaika: "2005-07-31",
    ryhmä: "9A",
    haut: [],
  },
  {
    oid: "1.123.123.123.123.123.5",
    nimi: "Ojanen Jani Kalle",
    hetu: "190605A037K",
    oppilaitos: {
      oid: "1.123.123.123.123.123.123",
      nimi: { fi: "Järvenpään yhteiskoulu" },
    },
    syntymaaika: "2005-07-31",
    ryhmä: "9A",
    haut: [],
  },
  {
    oid: "1.123.123.123.123.123.6",
    nimi: "Pohjanen Anna Maria",
    hetu: "060505A314A",
    oppilaitos: {
      oid: "1.123.123.123.123.123.123",
      nimi: { fi: "Järvenpään yhteiskoulu" },
    },
    syntymaaika: "2005-07-31",
    ryhmä: "9A",
    haut: [],
  },
  {
    oid: "1.123.123.123.123.123.7",
    nimi: "Raatikainen Hanna Sisko",
    hetu: "270805A578T",
    oppilaitos: {
      oid: "1.123.123.123.123.123.123",
      nimi: { fi: "Järvenpään yhteiskoulu" },
    },
    syntymaaika: "2005-07-31",
    ryhmä: "9A",
    haut: [],
  },
  {
    oid: "1.123.123.123.123.123.8",
    nimi: "Vuorenmaa Maija Kaarina",
    hetu: "240105A381V",
    oppilaitos: {
      oid: "1.123.123.123.123.123.123",
      nimi: { fi: "Järvenpään yhteiskoulu" },
    },
    syntymaaika: "2005-07-31",
    ryhmä: "9A",
    haut: [],
  },
  {
    oid: "1.123.123.123.123.123.9",
    nimi: "Ylänen Toni Vilhelm",
    hetu: "200705A606C",
    oppilaitos: {
      oid: "1.123.123.123.123.123.123",
      nimi: { fi: "Järvenpään yhteiskoulu" },
    },
    syntymaaika: "2005-07-31",
    ryhmä: "9A",
    haut: [],
  },
]