package controllers

import actions.AuthAction
import clients.GooglePlacesClient
import com.azure.cosmos.models.PartitionKey
import dao.CosmosQuery.getLatestItemByTimestamp
import dao.{CosmosDb, CosmosQuery}
import models.BusinessDetails
import shared.AppFunctions._

import scala.concurrent.ExecutionContext
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class ReviewController @Inject()(cc: ControllerComponents,
                                 googlePlacesClient: GooglePlacesClient,
                                 authAction: AuthAction,
                                 cosmosDb: CosmosDb)
                                (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val reviewsCollection: String = CosmosQuery.reviewsCollection

  def getBusinessDetails: Action[AnyContent] =
  Action.async {
    for {
      res <- cosmosDb.runQuery[BusinessDetails](getLatestItemByTimestamp(), reviewsCollection)
      _ = println(res)
    } yield Ok(toJson(res.head))
  }

  def updateGoogleReviews: Action[AnyContent] =
    authAction.async {
      println("Updating Google Reviews")
      for {
        res <- googlePlacesClient.fetchBusinessDetails()
        id = currentDateTimeInTimeStamp.toString
        _ = cosmosDb.add(res.result.copy(id = id), reviewsCollection, new PartitionKey(id))
      } yield Created(id)
    }
}