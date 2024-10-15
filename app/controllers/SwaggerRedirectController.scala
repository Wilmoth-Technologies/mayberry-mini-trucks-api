package controllers

import play.api.libs.json.Json
import javax.inject.{Inject, Singleton}
import play.api.mvc.{
  AbstractController,
  Action,
  AnyContent,
  ControllerComponents
}
import scala.concurrent.ExecutionContext

@Singleton
class SwaggerRedirectController @Inject() (cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with App {

  def redirectDocs: Action[AnyContent] = Action {
    Redirect(
      url = "/api/index.html",
      queryStringParams = Map("url" -> Seq("/swagger.json"))
    )
  }
}