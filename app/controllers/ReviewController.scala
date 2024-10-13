package controllers

import models.{Review, ApiWSClient, ReviewCount}
import play.api.libs.ws._
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.{WSClient, WSRequest}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule}
import play.api.libs.json.{Json, Writes}
import javax.inject.Inject
import play.api.http.Status
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.annotation.JsonInclude.Include

@Inject
class ReviewController @Inject()(ws: WSClient, apiWSClient: ApiWSClient)(implicit ec: ExecutionContext) {
    // implement jackson parser
    val objectMapper = new ObjectMapper()
    objectMapper.registerModule(DefaultScalaModule)
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    objectMapper.setSerializationInclusion(Include.NON_EMPTY)

    def getReviewCount(placeId: String): Future[ReviewCount] = {
        val url = s"https://places.googleapis.com/v1/places/${placeId}"
        val successResponseCodes: List[Int] = List(200)
        val request: WSRequest = ws.url(url)
            .addHttpHeaders(
                "Content-Type" -> "application/json",
                "X-Goog-Api-Key" -> "AIzaSyC3rzUoA_HF5gMUgPbJzk3ZzX4pGSqL4NY",
                "X-Goog-FieldMask" -> "userRatingCount"
            ).withMethod("GET")
        
        apiWSClient.invokeAPI(request, successResponseCodes).map {
            response => objectMapper.readValue(response.body, classOf[ReviewCount])
        }
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