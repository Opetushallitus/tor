package fi.oph.koski.valpas.valpasrepository

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.db.DatabaseConverters
import fi.oph.koski.log.Logging
import fi.oph.koski.util.Timing

class
ValpasDatabaseService(application: KoskiApplication) extends DatabaseConverters with Logging with Timing {
  private val db = application.masterDatabase

  // TODO
}