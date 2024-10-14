package controllers

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class HealthController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with App {

  def health: Action[AnyContent] = Action { Ok("Server Running") }
}