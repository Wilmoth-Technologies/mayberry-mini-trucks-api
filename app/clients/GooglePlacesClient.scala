package clients

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import models.Result
import play.api.libs.ws.{WSClient, WSRequest}
import shared.AppFunctions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GooglePlacesClient @Inject()(ws: WSClient, apiWSClient: ApiWSClient)
                                  (implicit val ec: ExecutionContext, implicit val as: ActorSystem) {
  private val successResponseCodes: List[Int] = List(200)
  private val placesApiUrl = "https://maps.googleapis.com/maps/api/place/details/json"
  private val placeId = "ChIJhYIWxj0nUogRyvEv4_Zoyg0"
  private val apiKey = if (ConfigFactory.load.getBoolean("isSecretManagerSetup")) {
    sys.env("PLACES_API_KEY")
  } else {
    "INSERT_KEY_HERE"
  }

  def fetchBusinessDetails(): Future[Result] = {
    val requestUrl = s"$placesApiUrl?place_id=$placeId&key=$apiKey"

    val url = s"https://places.googleapis.com/v1/places/$placeId"
    val request: WSRequest = ws.url(requestUrl)
      .addHttpHeaders(
        "Content-Type" -> "application/json",
        "X-Goog-Api-Key" -> "AIzaSyC3rzUoA_HF5gMUgPbJzk3ZzX4pGSqL4NY",
        "X-Goog-FieldMask" -> "userRatingCount"
      ).withMethod("GET")

    apiWSClient.invokeAPI(request, successResponseCodes)
      .map(response => {
        println(response.body)
        AppFunctions.objectMapper.readValue(response.body, classOf[Result])
      })
  }
}