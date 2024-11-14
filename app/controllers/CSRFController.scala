package controllers

import play.api.mvc.ControllerComponents
import play.filters.csrf.CSRF

import javax.inject.Inject
import play.api.mvc._

class CSRFController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def csrfToken() = Action { implicit request =>
    // Get the token from the current request
    CSRF.getToken(request).map { token =>
      val csrfCookie = Cookie(
        name = "PLAY_CSRF_TOKEN",
        value = token.value,
        httpOnly = false,
        secure = true,  // Ensure the cookie is sent over HTTPS only
        sameSite = Some(play.api.mvc.Cookie.SameSite.None),  // Allow the cookie to be sent in cross-origin requests
        domain = Some(".mayberryminitrucks.com")   // Allow access from all subdomains (including api.mayberryminitrucks.com)
      )
      Ok.withHeaders("Csrf-Token" -> token.value).withCookies(csrfCookie)
    }.getOrElse {
      BadRequest("No CSRF token could be generated")
    }
  }
}