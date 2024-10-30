package controllers

import clients.GooglePlacesClient
import shared.AppFunctions._
import scala.concurrent.ExecutionContext
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class ReviewController @Inject()(cc: ControllerComponents, googlePlacesClient: GooglePlacesClient)
                                (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getBusinessDetails: Action[AnyContent] =
  Action.async {
    for {
      res <- googlePlacesClient.fetchBusinessDetails()
    } yield Ok(toJson(res.result))
  }
}