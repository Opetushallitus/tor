package fi.oph.koski.util

import java.sql.Date
import java.time.LocalDate

import slick.jdbc.SetParameter

object SQL {
  implicit val setLocalDate: SetParameter[LocalDate] =
    (localDate, params) => params.setDate(Date.valueOf(localDate))
}
