package fi.oph.koski.valpas.repository

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.db.PostgresDriverWithJsonSupport.plainAPI._
import fi.oph.koski.db.{DatabaseConverters, SQLHelpers}
import fi.oph.koski.log.Logging
import org.json4s.JValue
import slick.jdbc.GetResult

import java.time.LocalDate

case class ValpasOppijaRow(
  oppijaOid: String,
  hetu: Option[String],
  syntymäaika: Option[LocalDate],
  etunimet: String,
  sukunimi: String,
  oikeutetutOppilaitokset: Set[ValpasOppilaitos.Oid],
  opiskeluoikeudet: JValue
)

class ValpasDatabaseService(application: KoskiApplication) extends DatabaseConverters with Logging {
  private val db = application.raportointiDatabase

  def getPeruskoulunValvojalleNäkyväOppija(rajapäivät: Rajapäivät)(oppijaOid: String): Option[ValpasOppijaRow] =
    getOppijat(Some(oppijaOid), None, rajapäivät).headOption

  def getPeruskoulunValvojalleNäkyvätOppijat(rajapäivät: Rajapäivät)(oppilaitosOids: Set[String]): Seq[ValpasOppijaRow] =
    getOppijat(None, Some(oppilaitosOids.toSeq), rajapäivät)

  private implicit def getResult: GetResult[ValpasOppijaRow] = GetResult(r => {
    ValpasOppijaRow(
      oppijaOid = r.rs.getString("oppija_oid"),
      hetu = Option(r.rs.getString("hetu")),
      syntymäaika = Option(r.getLocalDate("syntymaaika")),
      etunimet = r.rs.getString("etunimet"),
      sukunimi = r.rs.getString("sukunimi"),
      oikeutetutOppilaitokset = r.getArray("oikeutetutOppilaitokset").toSet,
      opiskeluoikeudet = r.getJson("opiskeluoikeudet")
    )
  })

  // Huom: Luotetaan siihen, että käyttäjällä on oikeudet nimenomaan annettuihin oppilaitoksiin!
  // Huom2: Tämä toimii vain peruskoulun hakeutumisen valvojille (ei esim. 10-luokille tai toisen asteen näkymiin yms.)
  // Huom3: Tämä ei filteröi opiskeluoikeuksia sen mukaan, minkä tiedot kuuluisi näyttää listanäkymässä, jos samalla oppijalla on useita opiskeluoikeuksia.
  //        Valinta voidaan jättää joko Scalalle, käyttöliitymälle tai tehdä toinen query, joka tekee valinnan SQL:ssä.
  private def getOppijat(
    oppijaOid: Option[String],
    oppilaitosOids: Option[Seq[String]],
    rajapäivät: Rajapäivät
  ): Seq[ValpasOppijaRow] = {
    val lakiVoimassaPeruskoulustaValmistuneillaAlku = rajapäivät.lakiVoimassaPeruskoulustaValmistuneillaAlku
    val keväänValmistumisjaksoAlku = rajapäivät.keväänValmistumisjaksoAlku
    val keväänValmistumisjaksoLoppu = rajapäivät.keväänValmistumisjaksoLoppu
    val keväänUlkopuolellaValmistumisjaksoAlku = rajapäivät.keväänUlkopuolellaValmistumisjaksoAlku
    val tarkasteluPäivä = rajapäivät.tarkasteluPäivä
    val keväänValmistumisjaksollaValmistuneidenViimeinenTarkastelupäivä = rajapäivät.keväänValmistumisjaksollaValmistuneidenViimeinenTarkastelupäivä

    db.runDbSync(SQLHelpers.concatMany(
      Some(sql"""
WITH
  """),
      oppijaOid.map(oid => sql"""
  -- CTE: jos pyydettiin vain yhtä oppijaa, hae hänen master oid:nsa
  pyydetty_oppija AS (
    SELECT r_henkilo.master_oid
    FROM r_henkilo
    WHERE r_henkilo.oppija_oid = $oid
  ),
      """),
      Some(sql"""
  -- CTE: kaikki oppijat, joilla on vähintään yksi kelpuutettava peruskoulun opetusoikeus, mukana
  -- myös taulukko kelpuutettavien opiskeluoikeuksien oppilaitoksista käyttöoikeustarkastelua varten.
  oppija AS (
    SELECT
      DISTINCT r_henkilo.master_oid,
      r_henkilo.hetu,
      r_henkilo.syntymaaika,
      r_henkilo.etunimet,
      r_henkilo.sukunimi,
      array_agg(DISTINCT r_opiskeluoikeus.oppilaitos_oid) AS oikeutettu_oppilaitos_oids
    FROM
      r_henkilo
      JOIN r_opiskeluoikeus ON r_opiskeluoikeus.oppija_oid = r_henkilo.oppija_oid
      """),
      oppilaitosOids.map(oids => sql"AND r_opiskeluoikeus.oppilaitos_oid = any($oids)"),
      oppijaOid.map(oid => sql"JOIN pyydetty_oppija ON pyydetty_oppija.master_oid = r_henkilo.master_oid"),
      Some(sql"""
      JOIN r_paatason_suoritus ON r_paatason_suoritus.opiskeluoikeus_oid = r_opiskeluoikeus.opiskeluoikeus_oid
      -- Lasketaan voimassaolevien kotiopetusjaksojen määrä ehtoa 4a varten
      CROSS JOIN LATERAL (
        SELECT
          count(*) AS count
        FROM
          jsonb_array_elements(r_opiskeluoikeus.data -> 'lisätiedot' -> 'kotiopetusjaksot') jaksot
        WHERE
          jaksot ->> 'loppu' IS NULL
            OR $tarkasteluPäivä BETWEEN jaksot ->> 'alku' AND jaksot ->> 'loppu'
      ) kotiopetusjaksoja
    WHERE
      -- (1) oppija on potentiaalisesti oppivelvollinen, eli syntynyt 2004 tai myöhemmin
      EXTRACT(YEAR FROM r_henkilo.syntymaaika) >= 2004
      -- (2) oppijalla on peruskoulun opiskeluoikeus
      AND r_opiskeluoikeus.koulutusmuoto = 'perusopetus'
      AND r_paatason_suoritus.suorituksen_tyyppi = 'perusopetuksenvuosiluokka'
      -- (3) kyseisessä opiskeluoikeudessa on yhdeksännen luokan suoritus.
      AND r_paatason_suoritus.koulutusmoduuli_koodiarvo = '9'
      -- (4a) valvojalla on oppilaitostason oppilaitosoikeus ja opiskeluoikeuden lisätiedoista ei löydy kotiopetusjaksoa, joka osuu tälle hetkelle (TODO: tutkittavalle ajanhetkelle)
      --      TODO (4b): puuttuu, koska ei vielä ole selvää, miten kotiopetusoppilaat halutaan käsitellä
      AND kotiopetusjaksoja.count = 0
      -- (5)  opiskeluoikeus ei ole eronnut tilassa tällä hetkellä (TODO: tutkittavalla ajanhetkellä)
      AND r_opiskeluoikeus.viimeisin_tila <> 'eronnut'
      AND (
        -- (6a) opiskeluoikeus on läsnä tai väliaikaisesti keskeytynyt tällä hetkellä (TODO: tutkittavalla ajanhetkellä)
        r_opiskeluoikeus.viimeisin_tila = any('{lasna, valiaikaisestikeskeytynyt}')
        -- TAI:
        OR (
          -- (6b.1 ) opiskeluoikeus on valmistunut-tilassa, ja siitä löytyy vahvistettu päättötodistus
          r_opiskeluoikeus.viimeisin_tila = 'valmistunut'
          -- (6b.2 ) ministeriön määrittelemä aikaraja ei ole kulunut umpeen henkilön valmistumisajasta.
          AND (
            (
              -- keväällä valmistunut ja tarkastellaan heille määrättyä rajapäivää aiemmin
              (r_opiskeluoikeus.paattymispaiva BETWEEN $keväänValmistumisjaksoAlku AND $keväänValmistumisjaksoLoppu)
              AND $tarkasteluPäivä <= $keväänValmistumisjaksollaValmistuneidenViimeinenTarkastelupäivä
            )
            OR (
              -- tai muuna aikana valmistunut ja tarkastellaan heille määrättyä rajapäivää aiemmin
              (r_opiskeluoikeus.paattymispaiva NOT BETWEEN $keväänValmistumisjaksoAlku AND $keväänValmistumisjaksoLoppu)
              AND (r_opiskeluoikeus.paattymispaiva >= $keväänUlkopuolellaValmistumisjaksoAlku)
            )
          )
        )
      )
    GROUP BY
      r_henkilo.master_oid,
      r_henkilo.hetu,
      r_henkilo.syntymaaika,
      r_henkilo.etunimet,
      r_henkilo.sukunimi
  )
  -- CTE: kaikki oppija_oidit, joilla pitää opiskeluoikeuksia etsiä
  , oppija_oid AS (
	SELECT
	  DISTINCT oppija_oid,
	  oppija.master_oid
	FROM
	  r_henkilo
	  JOIN oppija ON oppija.master_oid = r_henkilo.master_oid
  )
  -- Tilamäppäykset Kosken tarkkojen ja Valppaan yksinkertaisempien tilojen välillä
  , valpastila AS (
    SELECT
      column1 AS koskiopiskeluoikeudentila,
      column2 AS valpasopiskeluoikeudentila
    FROM
      (VALUES
        ('lasna', 'voimassa'),
        ('valiaikaisestikeskeytynyt', 'voimassa'),
        ('loma', 'voimassa'),
        ('valmistunut', 'valmistunut'),
        ('eronnut', 'eronnut'),
        ('katsotaaneronneeksi', 'katsotaaneronneeksi'),
        ('peruutettu', 'peruutettu'),
        ('mitatoity', 'mitatoity'),
        (NULL, 'tuntematon')
      ) t
  )
  -- CTE: peruskoulun opiskeluoikeudet (ei sama lista kuin ekassa CTE:ssä, koska voi olla rinnakkaisia tai peräkkäisiä muita peruskoulun opiskeluoikeuksia.
  -- Teoriassa varmaan voisi tehostaa kyselyä jotenkin ja välttää näiden hakeminen uudestaan, mutta kysely voisi mennä melko monimutkaiseksi.)
  -- Tässä lisäksi selvitetään se, ettei oppijalla ole peruskoulun opiskeluoikeutta, joka on valmistunut ennen 1.1.2021: tällöin henkilö ei ole
  -- oppivelvollisuuslain piirissä, eikä mitään hänen tietojaan saa Valppaassa näyttää. Tätä tarkistusta ei voi tehdä osana aiempaa peruskoulun queryä,
  -- koska siinä ei tutkita oppijan mahdollisia rinnakkaisia peruskoulun opiskeluoikeuksia, jotka voivat olla valmistuneita.
  , peruskoulun_opiskeluoikeus AS (
     SELECT
       oppija_oid.master_oid,
       r_opiskeluoikeus.opiskeluoikeus_oid,
       r_opiskeluoikeus.koulutusmuoto,
       r_opiskeluoikeus.oppilaitos_oid,
       r_opiskeluoikeus.oppilaitos_nimi,
       valittu_r_paatason_suoritus.toimipiste_oid,
       valittu_r_paatason_suoritus.toimipiste_nimi,
       r_opiskeluoikeus.alkamispaiva,
       r_opiskeluoikeus.paattymispaiva,
       coalesce(valittu_r_paatason_suoritus.data ->> 'luokka', r_opiskeluoikeus.luokka) AS ryhmä,
       r_opiskeluoikeus.viimeisin_tila,
       (r_opiskeluoikeus.viimeisin_tila = 'valmistunut' AND r_opiskeluoikeus.paattymispaiva < $lakiVoimassaPeruskoulustaValmistuneillaAlku) AS aiemmin_valmistunut,
       CASE
         WHEN $tarkasteluPäivä < r_opiskeluoikeus.alkamispaiva THEN 'voimassatulevaisuudessa'
         WHEN $tarkasteluPäivä > r_opiskeluoikeus.paattymispaiva THEN valpastila_viimeisin.valpasopiskeluoikeudentila
         ELSE valpastila_aikajakson_keskella.valpasopiskeluoikeudentila
       END tarkastelupäivan_tila
     FROM
       oppija_oid
       JOIN r_opiskeluoikeus ON r_opiskeluoikeus.oppija_oid = oppija_oid.oppija_oid
         AND r_opiskeluoikeus.koulutusmuoto = 'perusopetus'
       LEFT JOIN r_opiskeluoikeus_aikajakso aikajakson_keskella ON aikajakson_keskella.opiskeluoikeus_oid = r_opiskeluoikeus.opiskeluoikeus_oid
         AND $tarkasteluPäivä BETWEEN aikajakson_keskella.alku AND aikajakson_keskella.loppu
       LEFT JOIN valpastila valpastila_aikajakson_keskella ON valpastila_aikajakson_keskella.koskiopiskeluoikeudentila = aikajakson_keskella.tila
       LEFT JOIN valpastila valpastila_viimeisin ON valpastila_viimeisin.koskiopiskeluoikeudentila = r_opiskeluoikeus.viimeisin_tila
       -- Haetaan päätason suoritus, jonka dataa halutaan näyttää (toistaiseksi valitaan alkamispäivän perusteella uusin)
       -- TODO: Ei välttämättä osu oikeaan, koska voi olla esim. monen eri tyyppisiä peruskoulun päätason suorituksia, ja pitäisi oikeasti filteröidä myös tyypin perusteella.
       -- TODO: Pitää toteuttaa tutkittavan ajanhetken tarkistus tähänkin, että näytetään luokkatieto sen mukaan, millä luokalla on ollut tutkittavalla ajanhetkellä.
       CROSS JOIN LATERAL (
         SELECT
           *
         FROM
           r_paatason_suoritus inner_r_paatason_suoritus
         WHERE
           inner_r_paatason_suoritus.opiskeluoikeus_oid = r_opiskeluoikeus.opiskeluoikeus_oid
           AND inner_r_paatason_suoritus.suorituksen_tyyppi = 'perusopetuksenvuosiluokka'
         ORDER BY
           -- hae vain uusin vuosiluokan suoritus (toistaiseksi, möyhemmin pitää pystyä valitsemaan myös esim. edelliseltä keväältä parametrina annetun päivämäärän perusteella)
           inner_r_paatason_suoritus.DATA ->> 'alkamispäivä' DESC
        LIMIT
          1
        ) valittu_r_paatason_suoritus
  )
  -- CTE: Muut kuin peruskoulun opiskeluoikeudet
  -- Pitää hakea erikseen, koska säännöt päätason suorituksen kenttien osalta ovat erilaiset, koska datat ovat erilaiset
  , muu_opiskeluoikeus AS (
     SELECT
       oppija_oid.master_oid,
       r_opiskeluoikeus.opiskeluoikeus_oid,
       r_opiskeluoikeus.koulutusmuoto,
       r_opiskeluoikeus.oppilaitos_oid,
       r_opiskeluoikeus.oppilaitos_nimi,
       valittu_r_paatason_suoritus.toimipiste_oid,
       valittu_r_paatason_suoritus.toimipiste_nimi,
       r_opiskeluoikeus.alkamispaiva,
       r_opiskeluoikeus.paattymispaiva,
       coalesce(valittu_r_paatason_suoritus.data ->> 'ryhmä', r_opiskeluoikeus.luokka) AS ryhmä,
       r_opiskeluoikeus.viimeisin_tila,
       FALSE AS aiemmin_valmistunut,
       CASE
         WHEN $tarkasteluPäivä < r_opiskeluoikeus.alkamispaiva THEN 'voimassatulevaisuudessa'
         WHEN $tarkasteluPäivä > r_opiskeluoikeus.paattymispaiva THEN valpastila_viimeisin.valpasopiskeluoikeudentila
         ELSE valpastila_aikajakson_keskella.valpasopiskeluoikeudentila
       END tarkastelupäivan_tila
     FROM
       oppija_oid
       JOIN r_opiskeluoikeus ON r_opiskeluoikeus.oppija_oid = oppija_oid.oppija_oid
         AND r_opiskeluoikeus.koulutusmuoto <> 'perusopetus'
       LEFT JOIN r_opiskeluoikeus_aikajakso aikajakson_keskella ON aikajakson_keskella.opiskeluoikeus_oid = r_opiskeluoikeus.opiskeluoikeus_oid
         AND $tarkasteluPäivä BETWEEN aikajakson_keskella.alku AND aikajakson_keskella.loppu
       LEFT JOIN valpastila valpastila_aikajakson_keskella ON valpastila_aikajakson_keskella.koskiopiskeluoikeudentila = aikajakson_keskella.tila
       LEFT JOIN valpastila valpastila_viimeisin ON valpastila_viimeisin.koskiopiskeluoikeudentila = r_opiskeluoikeus.viimeisin_tila
       -- Haetaan päätason suoritus, jonka dataa halutaan näyttää (TODO: toistaiseksi tulos on random, jos ei ole vahvistusta tai arviointia, mutta data ei riitä muuten.
       -- Teoriassa voisi tutkia päätason suorituksen osasuorituksiin kirjattuja päivämääriä, mutta se on aika monimutkaista ja luultavasti myös hidasta.
       CROSS JOIN LATERAL (
         SELECT
           *
         FROM
           r_paatason_suoritus inner_r_paatason_suoritus
         WHERE
           inner_r_paatason_suoritus.opiskeluoikeus_oid = r_opiskeluoikeus.opiskeluoikeus_oid
         ORDER BY
           inner_r_paatason_suoritus.vahvistus_paiva DESC NULLS FIRST,
           inner_r_paatason_suoritus.arviointi_paiva DESC NULLS FIRST
        LIMIT
          1
        ) valittu_r_paatason_suoritus
  )
  -- CTE: Oppijat, jotka ovat rajapäivää aimmmin valmistuneita
  , rajapaivaa_aiemmin_valmistuneet_oppijat AS (
    SELECT
      DISTINCT oppija.master_oid
    FROM
      oppija
      JOIN peruskoulun_opiskeluoikeus ON peruskoulun_opiskeluoikeus.master_oid = oppija.master_oid
    WHERE
      peruskoulun_opiskeluoikeus.aiemmin_valmistunut IS TRUE
  )
  -- CTE: Oppijat, jotka eivät ole rajapäivää aiemmin valmistuneita, ja siten oppivelvollisia
  , oppivelvollinen_oppija AS (
    SELECT
      DISTINCT oppija.master_oid,
      oppija.hetu,
      oppija.syntymaaika,
      oppija.etunimet,
      oppija.sukunimi,
      oppija.oikeutettu_oppilaitos_oids
    FROM
      oppija
      LEFT JOIN rajapaivaa_aiemmin_valmistuneet_oppijat ON rajapaivaa_aiemmin_valmistuneet_oppijat.master_oid = oppija.master_oid
    WHERE
      rajapaivaa_aiemmin_valmistuneet_oppijat.master_oid IS NULL
  )
  -- CTE: Yhdistettynä peruskoulun ja muut opiskeluoikeudet
  , opiskeluoikeus AS (
    SELECT
      *
    FROM
     peruskoulun_opiskeluoikeus
    UNION ALL
    SELECT
      *
    FROM
     muu_opiskeluoikeus
  )
  -- Päätason SELECT: Muodostetaan palautettava rakenne
  SELECT
    oppivelvollinen_oppija.master_oid AS oppija_oid,
    oppivelvollinen_oppija.hetu,
    oppivelvollinen_oppija.syntymaaika,
    oppivelvollinen_oppija.etunimet,
    oppivelvollinen_oppija.sukunimi,
    oppivelvollinen_oppija.oikeutettu_oppilaitos_oids AS oikeutetutOppilaitokset,
    json_agg(
      json_build_object(
        'oid', opiskeluoikeus.opiskeluoikeus_oid,
        'tyyppi', json_build_object(
          'koodiarvo', opiskeluoikeus.koulutusmuoto,
          'koodistoUri', 'opiskeluoikeudentyyppi'
        ),
        'oppilaitos', json_build_object(
          'oid', opiskeluoikeus.oppilaitos_oid,
          'nimi', json_build_object(
            'fi', opiskeluoikeus.oppilaitos_nimi
          )
        ),
        'toimipiste', json_build_object(
          'oid', opiskeluoikeus.toimipiste_oid,
          'nimi', json_build_object(
            'fi', opiskeluoikeus.toimipiste_nimi
          )
        ),
        'alkamispäivä', opiskeluoikeus.alkamispaiva,
        'päättymispäivä', opiskeluoikeus.paattymispaiva,
        'ryhmä', opiskeluoikeus.ryhmä,
        'viimeisinTila', json_build_object(
          'koodiarvo', opiskeluoikeus.viimeisin_tila,
          'koodistoUri', 'koskiopiskeluoikeudentila'
        ),
        'tarkastelupäivänTila', json_build_object(
          'koodiarvo', opiskeluoikeus.tarkastelupäivan_tila,
          'koodistoUri', 'valpasopiskeluoikeudentila'
        )
      ) ORDER BY
        opiskeluoikeus.alkamispaiva DESC,
        -- Alkamispäivä varmaan riittäisi käyttöliitymälle, mutta lisätään muita kenttiä testien pitämiseksi deteministisempinä myös päällekäisillä opiskeluoikeuksilla:
        opiskeluoikeus.paattymispaiva DESC,
        opiskeluoikeus.koulutusmuoto,
        opiskeluoikeus.ryhmä DESC NULLS LAST,
        opiskeluoikeus.viimeisin_tila
    ) opiskeluoikeudet
  FROM
    opiskeluoikeus
    JOIN oppivelvollinen_oppija ON oppivelvollinen_oppija.master_oid = opiskeluoikeus.master_oid
  GROUP BY
    oppivelvollinen_oppija.master_oid,
    oppivelvollinen_oppija.hetu,
    oppivelvollinen_oppija.syntymaaika,
    oppivelvollinen_oppija.etunimet,
    oppivelvollinen_oppija.sukunimi,
    oppivelvollinen_oppija.oikeutettu_oppilaitos_oids
  ORDER BY
    oppivelvollinen_oppija.sukunimi,
    oppivelvollinen_oppija.etunimet
    """)).as[ValpasOppijaRow])
  }
}
