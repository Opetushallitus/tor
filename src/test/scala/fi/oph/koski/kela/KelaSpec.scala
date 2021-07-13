package fi.oph.koski.kela

import fi.oph.koski.KoskiHttpSpec
import fi.oph.koski.api.OpiskeluoikeusTestMethodsAmmatillinen
import fi.oph.koski.henkilo.KoskiSpecificMockOppijat
import fi.oph.koski.history.OpiskeluoikeusHistoryPatch
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.koskiuser.{MockUser, MockUsers}
import fi.oph.koski.log.{AccessLogTester, AuditLogTester}
import fi.oph.koski.schema._
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

import java.time.LocalDate

class KelaSpec extends FreeSpec with KoskiHttpSpec with OpiskeluoikeusTestMethodsAmmatillinen with Matchers with BeforeAndAfterAll {

  "Kelan yhden oppijan rajapinta" - {
    "Yhden oppijan hakeminen onnistuu ja tuottaa auditlog viestin" in {
      AuditLogTester.clearMessages
      postHetu(KoskiSpecificMockOppijat.amis.hetu.get) {
        verifyResponseStatusOk()
        AuditLogTester.verifyAuditLogMessage(Map("operation" -> "OPISKELUOIKEUS_KATSOMINEN", "target" -> Map("oppijaHenkiloOid" -> KoskiSpecificMockOppijat.amis.oid)))
      }
    }
    "Palautetaan 404 jos opiskelijalla ei ole ollenkaan Kelaa kiinnostavia opiskeluoikeuksia" in {
      postHetu(KoskiSpecificMockOppijat.monimutkainenKorkeakoululainen.hetu.get) {
        verifyResponseStatus(404, KoskiErrorCategory.notFound.oppijaaEiLöydyTaiEiOikeuksia("Oppijaa (hetu) ei löydy tai käyttäjällä ei ole oikeuksia tietojen katseluun."))
      }
    }
    "Oppijan opiskeluoikeuksista filtteröidään pois sellaiset opiskeluoikeuden tyypit jotka ei kiinnosta Kelaa" in {
      postHetu(KoskiSpecificMockOppijat.kelaErityyppisiaOpiskeluoikeuksia.hetu.get) {
        verifyResponseStatusOk()
        val response = JsonSerializer.parse[KelaOppija](body)

        response.henkilö.hetu should equal(KoskiSpecificMockOppijat.kelaErityyppisiaOpiskeluoikeuksia.hetu)
        response.opiskeluoikeudet.map(_.tyyppi.koodiarvo) should equal(List(OpiskeluoikeudenTyyppi.perusopetus.koodiarvo))
      }
    }
  }

  "Usean oppijan rajapinta" - {
    "Voidaan hakea usea oppija, jos jollain oppijalla ei löydy Kosken kantaan tallennettuja opintoja, se puuttuu vastauksesta" in {
      val hetut = List(
        KoskiSpecificMockOppijat.amis,
        KoskiSpecificMockOppijat.ibFinal,
        KoskiSpecificMockOppijat.koululainen,
        KoskiSpecificMockOppijat.ylioppilas
      ).map(_.hetu.get)

      postHetut(hetut) {
        verifyResponseStatusOk()
        val response = JsonSerializer.parse[List[KelaOppija]](body)
        response.map(_.henkilö.hetu.get).sorted should equal(hetut.sorted.filterNot(_ == KoskiSpecificMockOppijat.ylioppilas.hetu.get))
      }
    }
    "Luo AuditLogin" in {
      AuditLogTester.clearMessages
      postHetut(List(KoskiSpecificMockOppijat.amis.hetu.get)) {
        verifyResponseStatusOk()
        AuditLogTester.verifyAuditLogMessage(Map("operation" -> "OPISKELUOIKEUS_KATSOMINEN", "target" -> Map("oppijaHenkiloOid" -> KoskiSpecificMockOppijat.amis.oid)))
      }
    }
    "Ei luo AuditLogia jos hetulla löytyvä oppija puuttuu vastauksesta" in {
      AuditLogTester.clearMessages
      postHetut(List(KoskiSpecificMockOppijat.korkeakoululainen.hetu.get)) {
        verifyResponseStatusOk()
        AuditLogTester.getLogMessages.length should equal(0)
      }
    }
    "Sallitaan 1000 hetua" in {
      val hetut = List.fill(1000)(KoskiSpecificMockOppijat.amis.hetu.get)
      postHetut(hetut) {
        verifyResponseStatusOk()
      }
    }
    "Ei sallita yli 1000 hetua" in {
      val hetut = List.fill(1001)(KoskiSpecificMockOppijat.amis.hetu.get)
      postHetut(hetut) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest("Liian monta hetua, enintään 1000 sallittu"))
      }
    }
  }

  "Kelan käyttöoikeudet" - {
    "Suppeilla Kelan käyttöoikeuksilla ei nää kaikkia lisätietoja" in {
      postHetu(KoskiSpecificMockOppijat.amis.hetu.get, user = MockUsers.kelaSuppeatOikeudet) {
        verifyResponseStatusOk()
        val opiskeluoikeudet = JsonSerializer.parse[KelaOppija](body).opiskeluoikeudet
        val lisatiedot = opiskeluoikeudet.head.lisätiedot.get

        lisatiedot.hojks should equal(None)

        opiskeluoikeudet.length should be(1)
      }
    }
    "Laajoilla Kelan käyttöoikeuksilla näkee kaikki KelaSchema:n lisätiedot" in {
      postHetu(KoskiSpecificMockOppijat.amis.hetu.get, user = MockUsers.kelaLaajatOikeudet) {
        verifyResponseStatusOk()
        val opiskeluoikeudet = JsonSerializer.parse[KelaOppija](body).opiskeluoikeudet
        val lisatiedot = opiskeluoikeudet.head.lisätiedot.get

        lisatiedot.hojks shouldBe(defined)

        opiskeluoikeudet.length should be(1)
      }
    }
    "Osasuorituksen yksilöllistetty oppimäärä" - {
      def verify(user: MockUser, yksilöllistettyOppimääräShouldShow: Boolean): Unit = {
        postHetu(KoskiSpecificMockOppijat.koululainen.hetu.get, user = user) {
          verifyResponseStatusOk()
          val opiskeluoikeudet = JsonSerializer.parse[KelaOppija](body).opiskeluoikeudet
          val osasuoritukset = opiskeluoikeudet.flatMap(_.suoritukset.flatMap(_.osasuoritukset)).flatten
          osasuoritukset.exists(_.yksilöllistettyOppimäärä.isDefined) shouldBe(yksilöllistettyOppimääräShouldShow)
        }
      }
      "Näkyy laajoilla käyttöoikeuksilla" in {
        verify(MockUsers.kelaLaajatOikeudet, yksilöllistettyOppimääräShouldShow = true)
      }
      "Ei näy suppeilla käyttöoikeuksilla" in {
        verify(MockUsers.kelaSuppeatOikeudet, yksilöllistettyOppimääräShouldShow = false)
      }
    }
    "Osasuoritusten lisätiedot" - {
      "Ei näy suppeilla käyttöoikeuksilla" in {
        postHetu(KoskiSpecificMockOppijat.ammattilainen.hetu.get, MockUsers.kelaSuppeatOikeudet) {
          verifyResponseStatusOk()
          val opiskeluoikeudet = JsonSerializer.parse[KelaOppija](body).opiskeluoikeudet
          val osasuoritukset = opiskeluoikeudet.flatMap(_.suoritukset.flatMap(_.osasuoritukset)).flatten
          osasuoritukset.exists(_.lisätiedot.isDefined) shouldBe(false)
        }
      }
      "Näkyy laajoilla käyttöoikeuksilla vain jos lisätietojen tunnisteen koodiarvo on 'mukautettu'" in {
        postHetu(KoskiSpecificMockOppijat.ammattilainen.hetu.get, MockUsers.kelaLaajatOikeudet) {
          verifyResponseStatusOk()
          val opiskeluoikeudet = JsonSerializer.parse[KelaOppija](body).opiskeluoikeudet
          val osasuoritukset = opiskeluoikeudet.flatMap(_.suoritukset.flatMap(_.osasuoritukset)).flatten
          osasuoritukset.flatMap(_.lisätiedot).flatten.map(_.tunniste.koodiarvo) should equal(List("mukautettu"))
        }
      }
    }
  }

  "Perusopetuksen oppiaineen oppimäärän suorituksesta ei välitetä suoritustapaa Kelalle" in {
    postHetu(KoskiSpecificMockOppijat.montaOppiaineenOppimäärääOpiskeluoikeudessa.hetu.get) {
      verifyResponseStatusOk()
      val opiskeluoikeudet = JsonSerializer.parse[KelaOppija](body).opiskeluoikeudet

      opiskeluoikeudet.foreach(_.suoritukset.foreach(suoritus => {
        suoritus.suoritustapa should equal(None)
      }))
    }
  }

  "Vapaan sivistystyön opiskeluoikeuksista ei välitetä vapaatavoitteisin koulutuksen suorituksia" in {
    postHetu(KoskiSpecificMockOppijat.vapaaSivistystyöVapaatavoitteinenKoulutus.hetu.get) {
      verifyResponseStatus(404, KoskiErrorCategory.notFound())
    }
  }

  "Opiskeluoikeuden versiohistorian haku tuottaa AuditLogin" in {
    resetFixtures
    val opiskeluoikeus = lastOpiskeluoikeusByHetu(KoskiSpecificMockOppijat.amis)

    luoVersiohistoriaanRivi(KoskiSpecificMockOppijat.amis, opiskeluoikeus.asInstanceOf[AmmatillinenOpiskeluoikeus])

    AuditLogTester.clearMessages

    getVersiohistoria(opiskeluoikeus.oid.get) {
      verifyResponseStatusOk()
      val history = JsonSerializer.parse[List[OpiskeluoikeusHistoryPatch]](body)

      history.length should equal(2)
      AuditLogTester.verifyAuditLogMessage(Map("operation" -> "MUUTOSHISTORIA_KATSOMINEN", "target" -> Map("opiskeluoikeusOid" -> opiskeluoikeus.oid.get)))
    }
  }

  "Tietyn version haku opiskeluoikeudesta tuottaa AuditLogin" in {
    resetFixtures

    val opiskeluoikeus = lastOpiskeluoikeusByHetu(KoskiSpecificMockOppijat.amis)

    luoVersiohistoriaanRivi(KoskiSpecificMockOppijat.amis, opiskeluoikeus.asInstanceOf[AmmatillinenOpiskeluoikeus])

    AuditLogTester.clearMessages

    getOpiskeluoikeudenVersio(KoskiSpecificMockOppijat.amis.oid, opiskeluoikeus.oid.get, 1) {
      verifyResponseStatusOk()
      val response = JsonSerializer.parse[KelaOppija](body)

      response.opiskeluoikeudet.headOption.flatMap(_.versionumero) should equal(Some(1))
      AuditLogTester.verifyAuditLogMessage(Map("operation" -> "OPISKELUOIKEUS_KATSOMINEN", "target" -> Map("oppijaHenkiloOid" -> KoskiSpecificMockOppijat.amis.oid)))
    }
  }

  "Hetu ei päädy lokiin" in {
    AccessLogTester.clearMessages
    val maskedHetu = "******-****"
    getHetu(KoskiSpecificMockOppijat.amis.hetu.get) {
      verifyResponseStatusOk()
      AccessLogTester.getLatestMatchingAccessLog("/koski/kela") should include(maskedHetu)
    }
  }

  private def getHetu[A](hetu: String, user: MockUser = MockUsers.kelaSuppeatOikeudet)(f: => A)= {
    authGet(s"kela/$hetu", user)(f)
  }

  private def postHetu[A](hetu: String, user: MockUser = MockUsers.kelaLaajatOikeudet)(f: => A): A = {
    post(
      "api/luovutuspalvelu/kela/hetu",
      JsonSerializer.writeWithRoot(KelaRequest(hetu)),
      headers = authHeaders(user) ++ jsonContent
    )(f)
  }

  private def postHetut[A](hetut: List[String], user: MockUser = MockUsers.kelaLaajatOikeudet)(f: => A): A = {
    post(
      "api/luovutuspalvelu/kela/hetut",
      JsonSerializer.writeWithRoot(KelaBulkRequest(hetut)),
      headers = authHeaders(user) ++ jsonContent
    )(f)
  }

  private def getVersiohistoria[A](opiskeluoikeudenOid: String, user: MockUser = MockUsers.kelaLaajatOikeudet)(f: => A): A = {
    authGet(s"api/luovutuspalvelu/kela/versiohistoria/$opiskeluoikeudenOid", user)(f)
  }

  private def getOpiskeluoikeudenVersio[A](
    oppijaOid: String,
    opiskeluoikeudenOid: String,
    versio: Int,
    user: MockUser = MockUsers.kelaLaajatOikeudet
  )(f: => A): A = {
    authGet(s"api/luovutuspalvelu/kela/versiohistoria/$oppijaOid/$opiskeluoikeudenOid/$versio", user)(f)
  }

  private def luoVersiohistoriaanRivi(oppija: Henkilö, opiskeluoikeus: AmmatillinenOpiskeluoikeus): Unit = {
    createOrUpdate(oppija, opiskeluoikeus.copy(arvioituPäättymispäivä = Some(LocalDate.now)))
  }
}
