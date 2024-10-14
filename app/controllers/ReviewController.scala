package controllers

import clients.GooglePlacesClient
import shared.AppFunctions._
import scala.concurrent.ExecutionContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import javax.inject.{Inject, Singleton}
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.annotation.JsonInclude.Include
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class ReviewController @Inject()(cc: ControllerComponents, googlePlacesClient: GooglePlacesClient)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  // implement jackson parser
  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  objectMapper.setSerializationInclusion(Include.NON_EMPTY)

  def getReviewCount(placeId: String): Action[AnyContent] =
  Action.async {
    for {
      res <- googlePlacesClient.getPlacesViaId(placeId)
    } yield Ok(toJson(res))
  }

  // implicit val reviewWrites: Writes[Review] = Json.writes[Review]

  // def getReviews(placeId: String): Action[AnyContent] = Action.async {
  //     val requestUrl = s"https://places.googleapis.com/v1/places/${placeId}"
  //     val successResponseCodes: List[Int] = List
  //     val request: WSRequest = ws.url(requestUrl)
  //         .addHttpHeaders(
  //             "Content-Type" -> "application/json",
  //             "X-Goog-Api-Key" -> "AIzaSyC3rzUoA_HF5gMUgPbJzk3ZzX4pGSqL4NY",
  //             "X-Goog-FieldMask" -> "reviews"
  //         )
  //         .withMethod("GET")

  //     apiWSClient.invokeAPI(request, successResponseCodes).map {
  //         response => response.json
  //     }
  //     // request.get().map { response =>
  //     //     val reviews: Seq[Review] = (response.json \ "reviews").asOpt[Seq[JsValue]].getOrElse(Seq.empty).flatMap { reviewJson =>
  //     //         for {
  //     //             name <- (reviewJson \ "authorAttribution" \ "displayName").asOpt[String]
  //     //             date <- (reviewJson \ "publishTime").asOpt[String]
  //     //             profilePic <- (reviewJson \ "authorAttribution" \ "photoUri").asOpt[String]
  //     //             rating <- (reviewJson \ "rating").asOpt[Int]
  //     //             text <- (reviewJson \ "originalText" \ "text").asOpt[String]
  //     //         } yield Review(name, date, profilePic, rating, text)
  //     //     }

  //     //     Ok(Json.toJson(reviews))
  //     // }.recover {
  //     //     case ex: Exception => InternalServerError(s"An error occurred: ${ex.getMessage}")
  //     // }

  // }
}