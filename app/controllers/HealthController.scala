package controllers

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class HealthController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with App {

  def health: Action[AnyContent] = Action { Ok("Server Running") }

//  def ready: Action[AnyContent] = Action.async {
//    healthChecker.check().map { result =>
//      val body = Json.obj(
//        "successes" -> result.successes
//          .map(tm => Json.obj(tm.tag -> tm.message)),
//        "errors" -> result.errors
//          .map(te => Json.obj(te.tag -> te.error.getMessage))
//      )
//      if (result.isSuccess)
//        Ok(body)
//      else
//        ServiceUnavailable(body)
//    }
//  }
}