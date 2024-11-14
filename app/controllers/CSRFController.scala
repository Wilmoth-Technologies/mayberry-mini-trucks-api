package controllers

import play.api.mvc.ControllerComponents
import play.filters.csrf.CSRF

import javax.inject.Inject
import play.api.mvc._

class CSRFController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def csrfToken() = Action { implicit request =>
    // Get the token from the current request
    CSRF.getToken(request).map { token =>
      Ok.withHeaders("Csrf-Token" -> token.value)
    }.getOrElse {
      BadRequest("No CSRF token could be generated")
    }
  }
}
