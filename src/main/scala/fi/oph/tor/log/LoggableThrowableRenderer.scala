package fi.oph.tor.log

import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.log4j.DefaultThrowableRenderer
import org.apache.log4j.spi.ThrowableRenderer

class LoggableThrowableRenderer extends ThrowableRenderer {

  val renderer = new DefaultThrowableRenderer

  override def doRender(t: Throwable): Array[String] = ExceptionUtils.getRootCause(t) match {
    case t: Loggable => Array(t.getClass.getName + ": " + t.toString)
    case _ => renderer.doRender(t)
  }
}
