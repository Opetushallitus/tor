package fi.oph.koski.util

import java.sql.Date
import java.time.LocalDate

object SQL {
  /** Unsafe: does not escape single quotes so prone to SQL injection given bad input */
  def toSqlListUnsafe[T](xs: Iterable[T]) = xs.mkString("'", "','", "'")
  def toSqlDate(d: LocalDate) = Date.valueOf(d)
}