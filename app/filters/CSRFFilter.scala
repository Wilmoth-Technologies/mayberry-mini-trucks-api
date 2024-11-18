package filters

import akka.stream.Materializer
import play.api.mvc.{Cookie, Filter, RequestHeader, Result}
import play.filters.csrf.CSRF

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CSRFFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val tokenOption = CSRF.getToken(requestHeader)

    nextFilter(requestHeader).map { result =>
      tokenOption match {
        case Some(token) =>
          result.withHeaders("Csrf-Token" -> token.value).withCookies(Cookie(
            name = "PLAY_CSRF_TOKEN",
            value = token.value,
            httpOnly = false,
            secure = true,  // Ensure the cookie is sent over HTTPS only
            sameSite = Some(play.api.mvc.Cookie.SameSite.None),  // Allow the cookie to be sent in cross-origin requests
            domain = Some(".mayberryminitrucks.com")   // Allow access from all subdomains (including api.mayberryminitrucks.com)
          ))
        case None => result
      }
    }
  }
}