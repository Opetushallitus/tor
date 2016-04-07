package fi.oph.tor.api

import fi.oph.tor.documentation.Examples.examples
import fi.oph.tor.json.Json
import fi.oph.tor.log.Logging
import org.scalatest.{FreeSpec, Matchers}

class TorOppijaExamplesTest extends FreeSpec with Matchers with LocalJettyHttpSpecification with Logging {
  "API examples" - {
    resetFixtures
    examples.foreach { example =>
      "POST " + example.name in {
        val body = Json.write(example.data).getBytes("utf-8")
        put("api/oppija", body = body, headers = authHeaders() ++ jsonContent) {
          verifyResponseStatus(200)
          logger.info(example.name + ": OK")
        }
      }
    }
  }
}
