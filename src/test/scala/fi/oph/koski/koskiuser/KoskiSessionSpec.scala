package fi.oph.koski.koskiuser

import java.net.InetAddress
import java.net.InetAddress.{getByName => inetAddress}

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.typesafe.config.ConfigFactory
import fi.oph.koski.cache.GlobalCacheManager
import fi.oph.koski.log.LogUserContext
import fi.oph.koski.organisaatio.MockOrganisaatiot.{helsinginKaupunki, lehtikuusentienToimipiste, oppilaitokset}
import fi.oph.koski.organisaatio.{MockOrganisaatioRepository, MockOrganisaatiot, Opetushallitus, OrganisaatioHierarkia}
import fi.oph.koski.schema.OpiskeluoikeudenTyyppi
import fi.oph.koski.schema.OpiskeluoikeudenTyyppi._
import fi.oph.koski.userdirectory.{DirectoryUser, OpintopolkuDirectoryClient}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import org.mockito.Mockito.{mock, when}
import org.scalatest.{FreeSpec, Matchers, _}
import org.scalatra.servlet.RichRequest

class KoskiSessionSpec extends FreeSpec with Matchers with EitherValues with OptionValues with BeforeAndAfterAll {
  implicit val jsonDefaultFormats = DefaultFormats.preservingEmptyValues
  private val config = ConfigFactory.parseString(
    """
      |authentication-service.useCas = false
      |opintopolku.virkailija.url = "http://localhost:9877"
      |opintopolku.virkailija.username = "foo"
      |opintopolku.virkailija.password = "bar"
    """.stripMargin)

  implicit val cacheManager = GlobalCacheManager
  private val wireMockServer = new WireMockServer(wireMockConfig().port(9877))
  private val directoryClient = new OpintopolkuDirectoryClient(config.getString("opintopolku.virkailija.url"), config)
  private val käyttöoikeusRepository = new KäyttöoikeusRepository(MockOrganisaatioRepository, directoryClient)

  "KoskiSession" - {
    "Client ip" - {
      "clientIp contains single address" in {
        mkSession("127.0.0.1").clientIp should be(inetAddress("127.0.0.1"))
      }

      "clientIp contains multiple addresses" in {
        mkSession("127.0.0.1, 192.168.0.1").clientIp should be(inetAddress("127.0.0.1"))
        mkSession("127.0.0.1,192.168.0.1").clientIp should be(inetAddress("127.0.0.1"))
        mkSession(" 127.0.0.1    ,  192.168.0.1  ").clientIp should be(inetAddress("127.0.0.1"))
      }
    }

    "resolves user and käyttöoikeudet" - {
      "usean organisaation tallentaja" in {
        createAndVerifySession("kalle", MockUsers.kalle.ldapUser)
      }
      "pääkäyttäjä" in {
        val session = createAndVerifySession("pää", MockUsers.paakayttaja.ldapUser)
        session.hasGlobalReadAccess should be(true)
        session.isRoot should be(true)
        session.hasTiedonsiirronMitätöintiAccess(helsinginKaupunki, None)
      }
      "viranomainen" in {
        createAndVerifySession("viranomais", MockUsers.viranomainen.ldapUser)
      }
      "epäluotettava-tallentaja" in {
        createAndVerifySession("epäluotettava-tallentaja", MockUsers.tallentajaEiLuottamuksellinen.ldapUser)
      }
      "palvelukäyttäjä" in {
        createAndVerifySession("omnia-palvelukäyttäjä", MockUsers.omniaPalvelukäyttäjä.ldapUser)
      }
      "vastuukäyttäjä" in {
        createAndVerifySession("stadin-vastuu", MockUsers.stadinVastuukäyttäjä.ldapUser)
      }
      "kahden organisaation palvelukäyttäjä" in {
        createAndVerifySession("palvelu2", MockUsers.kahdenOrganisaatioPalvelukäyttäjä.ldapUser)
      }
      "oppilaitos katselija" in {
        createAndVerifySession("omnia-katselija", MockUsers.omniaKatselija.ldapUser)
      }
      "oppilaitos esiopetuskatselija" in {
        createAndVerifySession("esiopetus", MockUsers.jyväskylänKatselijaEsiopetus.ldapUser)
      }
      "oppilaitos tallentaja" in {
        createAndVerifySession("omnia-tallentaja", MockUsers.omniaTallentaja.ldapUser)
      }
      "oppilaitos ei Koski-oikeuksia" in {
        val session = createAndVerifySession("Otto", MockUsers.eiOikkia.ldapUser)
        session.allowedOpiskeluoikeusTyypit should be(empty)
        session.hasAnyReadAccess should be(false)
      }
      "kela suppeat oikeudet" in {
        val session = createAndVerifySession("Suppea", MockUsers.kelaSuppeatOikeudet.ldapUser)
        session.sensitiveDataAllowed(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA)) should be(false)
        session.sensitiveDataAllowed(Set(Rooli.LUOTTAMUKSELLINEN_KELA_SUPPEA)) should be(true)
      }
      "kela laajat oikeudet" in {
        val session = createAndVerifySession("Laaja", MockUsers.kelaLaajatOikeudet.ldapUser)
        session.sensitiveDataAllowed(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT)) should be(false)
        session.sensitiveDataAllowed(Set(Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA)) should be(true)
      }
      "viranomainen kaikki koulutusmuodot ei arkaluontoisten tietojen oikeuksia" in {
        val session = createAndVerifySession("Eeva", MockUsers.evira.ldapUser)
        session.sensitiveDataAllowed(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA, Rooli.LUOTTAMUKSELLINEN_KELA_SUPPEA)) should be(false)
      }
      "viranomainen perusopetus" in {
        val session = createAndVerifySession("Pertti", MockUsers.perusopetusViranomainen.ldapUser)
        val expectedOpiskeluoikeustyypit = Set(esiopetus, perusopetus, aikuistenperusopetus, perusopetuksenlisaopetus, perusopetukseenvalmistavaopetus, internationalschool).map(_.koodiarvo)
        session.allowedOpiskeluoikeusTyypit should equal(expectedOpiskeluoikeustyypit)
      }
      "viranomainen toinen aste" in {
        val session = createAndVerifySession("Teuvo", MockUsers.toinenAsteViranomainen.ldapUser)
        val expectedOpiskeluoikeustyypit = Set(ammatillinenkoulutus, ibtutkinto, diatutkinto, lukiokoulutus, luva, vapaansivistystyonkoulutus, ylioppilastutkinto, internationalschool).map(_.koodiarvo)
        session.allowedOpiskeluoikeusTyypit should equal(expectedOpiskeluoikeustyypit)
      }
      "viranomainen korkeakoulu" in {
        val session = createAndVerifySession("Kaisa", MockUsers.korkeakouluViranomainen.ldapUser)
        session.allowedOpiskeluoikeusTyypit should equal(Set(OpiskeluoikeudenTyyppi.korkeakoulutus.koodiarvo))
      }
      "luovutuspalvelu arkaluontoisten tietojen oikeus" in {
        val session = createAndVerifySession("Antti", MockUsers.luovutuspalveluKäyttäjäArkaluontoinen.ldapUser)
        session.hasLuovutuspalveluAccess should be(true)
        session.sensitiveDataAllowed(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT)) should be(true)
      }
      "luovutuspalvelu ei arkaluontoisten tietojen oikeuksia" in {
        val session = createAndVerifySession("Lasse", MockUsers.luovutuspalveluKäyttäjä.ldapUser)
        session.hasLuovutuspalveluAccess should be(true)
        session.sensitiveDataAllowed(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT)) should be(false)
      }
    }
  }

  private def createAndVerifySession(username: String, expected: DirectoryUser) = {
    val authUser = AuthenticationUser.fromDirectoryUser(username, expected)
    val session = KoskiSession(authUser, req, käyttöoikeusRepository)

    session.lang should be("fi")
    session.clientIp should be(InetAddress.getByName("10.1.2.3"))
    session.userAgent should be("MockUserAgent/1.0")

    session.oid should be(expected.oid)
    session.user.name should be(expected.etunimet + " " + expected.sukunimi)
    session.username should be(username)

    val expectedKäyttöoikeudet = expected.käyttöoikeudet.toSet
    session.orgKäyttöoikeudet should be(expectedOrganisaatioKäyttöoikeudet(expected))
    session.globalKäyttöoikeudet should be(expectedKäyttöoikeudet.collect { case k : KäyttöoikeusGlobal if k.globalAccessType.contains(AccessType.read) => k })
    session.globalViranomaisKäyttöoikeudet should be(expectedKäyttöoikeudet.collect { case k : KäyttöoikeusViranomainen => k})

    val expectedAllowedOpiskeluoikeudenTyypit = expectedKäyttöoikeudet.flatMap(_.allowedOpiskeluoikeusTyypit)
    session.allowedOpiskeluoikeusTyypit should be(expectedAllowedOpiskeluoikeudenTyypit)
    session.hasKoulutusmuotoRestrictions should be(expectedAllowedOpiskeluoikeudenTyypit != OpiskeluoikeudenTyyppi.kaikkiTyypit.map(_.koodiarvo))
    session
  }

  private def expectedOrganisaatioKäyttöoikeudet(u: DirectoryUser) = {
    def mockOrganisaatioHierarkia(k: KäyttöoikeusOrg): List[OrganisaatioHierarkia] = {
      OrganisaatioHierarkia.flatten(MockOrganisaatioRepository.getOrganisaatioHierarkia(k.organisaatio.oid).toList)
    }

    u.käyttöoikeudet.collect { case k: KäyttöoikeusOrg if k.organisaatioAccessType.contains(AccessType.read) => k }.flatMap { k =>
      mockOrganisaatioHierarkia(k).map { org =>
        k.copy(organisaatio = org.toOrganisaatio, juuri = org.oid == k.organisaatio.oid, oppilaitostyyppi = org.oppilaitostyyppi)
      }
    }.toSet
  }

  private val req = mock(classOf[RichRequest])

  override def beforeAll {
    when(req.header("User-Agent")).thenReturn(Some("MockUserAgent/1.0"))
    when(req.header("HTTP_X_FORWARDED_FOR")).thenReturn(Some("10.1.2.3"))
    when(req.cookies).thenReturn(Map[String, String]())
    wireMockServer.start()
    mockEndpoints
  }

  override def afterAll: Unit = wireMockServer.stop()

  private def mockEndpoints = {
    val käyttöoikeusUrl = "/kayttooikeus-service/kayttooikeus/kayttaja"
    def henkilöUrl(username: String) = {
      val oid = MockUsers.users.find(_.username == username).map(_.oid).get
      s"/oppijanumerorekisteri-service/henkilo/$oid"
    }

    Responses.käyttöoikeusResponse.foreach { case (username, resp) =>
      wireMockServer.stubFor(
        get(urlPathEqualTo(käyttöoikeusUrl))
          .withQueryParam("username", equalTo(username))
          .willReturn(ok(resp))
      )

      wireMockServer.stubFor(
        get(urlPathEqualTo(henkilöUrl(username))).willReturn(ok(Responses.käyttäjäResponse(username)))
      )
    }
  }

  private def mkSession(ipStr: String) = {
    val ip = LogUserContext.toInetAddress(ipStr)
    new KoskiSession(AuthenticationUser("", "", "", None), "fi", ip, "", Set())
  }
}

object Responses {
  implicit val jsonDefaultFormats = DefaultFormats.preservingEmptyValues
  val kallenOppilaitokset = lehtikuusentienToimipiste :: oppilaitokset
  val käyttöoikeusResponse: Map[String, String] = Map(
    "kalle" -> List(Map(
      "oidHenkilo" -> MockUsers.kalle.oid,
      "organisaatiot" -> kallenOppilaitokset.map(oid => Map(
        "organisaatioOid" -> oid,
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "READ_UPDATE"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
        )
      )))),
    "pää" -> List(Map(
      "oidHenkilo" -> MockUsers.paakayttaja.oid,
      "organisaatiot" -> List(
        Map(
          "organisaatioOid" -> "1.2.246.562.10.00000000001",
          "kayttooikeudet" -> List(
            Map("palvelu" -> "KOSKI", "oikeus" -> "OPHPAAKAYTTAJA"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "YLLAPITAJA"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
          )
        ),
        Map(
          "organisaatioOid" -> "1.2.246.562.10.00000000001",
          "kayttooikeudet" -> List(Map("palvelu" -> "LOKALISOINTI", "oikeus" -> "CRUD"))
        ),
        Map(
          "organisaatioOid" -> "1.2.246.562.10.00000000001",
          "kayttooikeudet" -> List(Map("palvelu" -> "OPPIJANUMEROREKISTERI", "oikeus" -> "REKISTERINPITAJA"))
        )
      )
    )),
    "viranomais" -> List(Map(
      "oidHenkilo" -> MockUsers.viranomainen.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> "1.2.246.562.10.00000000001",
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "OPHKATSELIJA"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
        )
      ))
    )),
    "epäluotettava-tallentaja" -> List(Map(
      "oidHenkilo" -> MockUsers.tallentajaEiLuottamuksellinen.oid,
      "organisaatiot" -> List(
        Map(
          "organisaatioOid" -> "1.2.246.562.10.51720121923",
          "kayttooikeudet" -> List(
            Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "READ_UPDATE"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
          )
        ),
        Map(
          "organisaatioOid" -> "1.2.246.562.10.14613773812",
          "kayttooikeudet" -> List(
            Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "READ_UPDATE"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
          )
        )
      )
    )),
    "omnia-palvelukäyttäjä" -> List(Map(
      "oidHenkilo" -> MockUsers.omniaPalvelukäyttäjä.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> "1.2.246.562.10.51720121923",
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "READ_UPDATE"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "TIEDONSIIRTO"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
        )
      ))
    )),
    "stadin-vastuu" -> List(Map(
      "oidHenkilo" -> MockUsers.stadinVastuukäyttäjä.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> "1.2.246.562.10.346830761110",
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
        )
      ))
    )),
    "Otto" -> List(Map(
      "oidHenkilo" -> MockUsers.eiOikkia.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.lehtikuusentienToimipiste,
        "kayttooikeudet" -> List(Map("palvelu" -> "OPPIJANUMEROREKISTERI", "oikeus" -> "READ"))
      ))
    )),
    "palvelu2" -> List(Map(
      "oidHenkilo" -> MockUsers.kahdenOrganisaatioPalvelukäyttäjä.oid,
      "organisaatiot" -> List(
        Map(
          "organisaatioOid" -> "1.2.246.562.10.346830761110",
          "kayttooikeudet" -> List(
            Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "READ_UPDATE"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "TIEDONSIIRTO"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
          )
        ),
        Map(
          "organisaatioOid" -> "1.2.246.562.10.51720121923",
          "kayttooikeudet" -> List(
            Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "READ_UPDATE"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "TIEDONSIIRTO"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"),
            Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
          )
        ))
    )),
    "omnia-katselija" -> List(Map(
      "oidHenkilo" -> MockUsers.omniaKatselija.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.omnia,
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
        )
      ))
    )),
    "omnia-tallentaja" -> List(Map(
      "oidHenkilo" -> MockUsers.omniaTallentaja.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.omnia,
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "READ_UPDATE"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "KAIKKI_OPISKELUOIKEUS_TYYPIT")
        )
      ))
    )),
    "Suppea" -> List(Map(
      "oidHenkilo" -> MockUsers.kelaSuppeatOikeudet.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.kela,
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_PERUSOPETUS"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_TOINEN_ASTE"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_KORKEAKOULU"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KELA_SUPPEA"))
      ))
    )),
    "Laaja" -> List(Map(
      "oidHenkilo" -> MockUsers.kelaLaajatOikeudet.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.kela,
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_PERUSOPETUS"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_TOINEN_ASTE"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_KORKEAKOULU"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KELA_LAAJA"))
      ))
    )),
    "Eeva" -> List(Map(
      "oidHenkilo" -> MockUsers.evira.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.evira,
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_PERUSOPETUS"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_TOINEN_ASTE"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_KORKEAKOULU"))
      ))
    )),
    "Pertti" -> List(Map(
      "oidHenkilo" -> MockUsers.perusopetusViranomainen.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.evira,
        "kayttooikeudet" -> List(Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_PERUSOPETUS"))
      ))
    )),
    "Teuvo" -> List(Map(
      "oidHenkilo" -> MockUsers.toinenAsteViranomainen.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.evira,
        "kayttooikeudet" -> List(Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_TOINEN_ASTE"))
      ),
      Map(
        "organisaatioOid" -> Opetushallitus.organisaatioOid,
        "kayttooikeudet" -> List(Map("palvelu" -> "OPPIJANUMEROREKISTERI", "oikeus" -> "REKISTERINPITAJA"))
      ))
    )),
    "Kaisa" -> List(Map(
      "oidHenkilo" -> MockUsers.korkeakouluViranomainen.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.evira,
        "kayttooikeudet" -> List(Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_KORKEAKOULU"))
      ),
      Map(
        "organisaatioOid" -> Opetushallitus.organisaatioOid,
        "kayttooikeudet" -> List()
      ))
    )),
    "Antti" -> List(Map(
      "oidHenkilo" -> MockUsers.luovutuspalveluKäyttäjäArkaluontoinen.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.evira,
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "TIEDONSIIRTO_LUOVUTUSPALVELU"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_PERUSOPETUS"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_TOINEN_ASTE"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_KORKEAKOULU"))
      ))
    )),
    "Lasse" -> List(Map(
      "oidHenkilo" -> MockUsers.luovutuspalveluKäyttäjä.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.evira,
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "TIEDONSIIRTO_LUOVUTUSPALVELU"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_PERUSOPETUS"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_TOINEN_ASTE"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "GLOBAALI_LUKU_KORKEAKOULU"))))
    )),
    "esiopetus" -> List(Map(
      "oidHenkilo" -> MockUsers.jyväskylänKatselijaEsiopetus.oid,
      "organisaatiot" -> List(Map(
        "organisaatioOid" -> MockOrganisaatiot.jyväskylänNormaalikoulu,
        "kayttooikeudet" -> List(
          Map("palvelu" -> "KOSKI", "oikeus" -> "READ"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "LUKU_ESIOPETUS"),
          Map("palvelu" -> "KOSKI", "oikeus" -> "LUOTTAMUKSELLINEN_KAIKKI_TIEDOT"))
      ))
    ))
  ).map { case (username, data) => (username, write(data)) }

  val käyttäjäResponse: Map[String, String] = MockUsers.users.map { u =>
    (u.username, write(Map(
      "oidHenkilo" -> u.oid,
      "sukunimi" -> u.ldapUser.sukunimi,
      "etunimet" -> u.ldapUser.etunimet,
      "asiointiKieli" -> Map("kieliKoodi" -> u.ldapUser.asiointikieli.getOrElse("fi"))
    )))
  }.toMap
}
