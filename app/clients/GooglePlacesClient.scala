package clients

import akka.actor.ActorSystem
import models.ReviewCount
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import shared.AppFunctions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GooglePlacesClient @Inject()(ws: WSClient, apiWSClient: ApiWSClient)
                                  (implicit val ec: ExecutionContext, implicit val as: ActorSystem) {

  def getPlacesViaId(placeId: String): Future[ReviewCount] = {
    val url = s"https://places.googleapis.com/v1/places/${placeId}"
    val successResponseCodes: List[Int] = List(200)
    val request: WSRequest = ws.url(url)
      .addHttpHeaders(
        "Content-Type" -> "application/json",
        "X-Goog-Api-Key" -> "AIzaSyC3rzUoA_HF5gMUgPbJzk3ZzX4pGSqL4NY",
        "X-Goog-FieldMask" -> "userRatingCount"
      ).withMethod("GET")

    apiWSClient.invokeAPI(request, successResponseCodes).map {
      response => AppFunctions.objectMapper.readValue(response.body, classOf[ReviewCount])
    }
  }

}
