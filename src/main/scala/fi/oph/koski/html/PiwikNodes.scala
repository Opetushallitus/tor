package fi.oph.koski.html

import fi.oph.koski.util.XML.CommentedPCData

import scala.xml.Node

trait PiwikNodes {
  def piwikSiteId: String

  def piwikTrackingScriptLoader(piwikHttpStatusCode: Option[Int] = None): Seq[Node] =
    <script type="text/javascript">
      {CommentedPCData("""
      console.log('piwikTrackingScriptLoader')
      window._paq = window._paq || []
      _paq.push(['trackPageView', """ + mkPageTitleForJsEval(piwikHttpStatusCode) + """])
      ;(function() {""" + mkPiwikInit + """})()
      """)}
    </script>

  def piwikTrackErrorObject: Seq[Node] =
    <script type="text/javascript">
      {CommentedPCData(
      """
      window.koskiError && window._paq && _paq.push(['trackEvent', 'LoadError', JSON.stringify({
        location: '' + document.location,
        httpStatus: koskiError.httpStatus,
        text: koskiError.text
      })])
      """)}
    </script>

  private def mkPageTitleForJsEval(httpStatusCode: Option[Int]): String = {
    val prefix = "document.location.pathname"
    httpStatusCode.map(code => prefix + s" + ' ($code)'").getOrElse(prefix)
  }

  private def mkPiwikInit: String =
    if (piwikSiteId.isEmpty) {
      // allow access to `window._paq` for tests, delete it after timeout to conserve memory
      // if this value is too short, piwikTrackingSpec will fail occasionally (especially when run on slow server)
      """
      console.log('mkPiwikInit, piwikSiteId is empty')
      setTimeout(function removePiwik() { console.log('deleting window._paq'); delete window._paq }, 20000)
      """
    } else {
      """
      console.log('mkPiwikInit, piwikSiteId NOT EMPTY?')
      var u = 'https://analytiikka.opintopolku.fi/piwik/'
      _paq.push(['setTrackerUrl', u+'piwik.php'])
      _paq.push(['setSiteId', '""" + piwikSiteId + """'])
      var d = document, g = d.createElement('script'), s = d.getElementsByTagName('script')[0]
      g.type = 'text/javascript'; g.async=true; g.defer=true; g.src = u+'piwik.js'; s.parentNode.insertBefore(g, s)
      """
    }
}
