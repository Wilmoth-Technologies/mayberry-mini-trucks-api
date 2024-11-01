package controllers

import com.azure.cosmos.models.PartitionKey
import dao.{CosmosDb, CosmosQuery}
import models.Subscribers
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import shared.AppFunctions.{currentDateTimeInTimeStamp, requestToObject}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SubscriptionController @Inject()(cc: ControllerComponents,
                                       cosmosDb: CosmosDb)
                                      (implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val subscriberCollection: String = CosmosQuery.subscriberCollection

  def addSubscriber: Action[AnyContent] =
    Action.async {
      implicit request =>
        val subscriber = requestToObject[Subscribers](request)
        println(s"Adding Subscriber via email: ${subscriber.email}")

        for {
          _ <- cosmosDb.add(subscriber.copy(id = subscriber.email, subscribeDate = currentDateTimeInTimeStamp), subscriberCollection, new PartitionKey(subscriber.email))
          //TODO: Add in a flow to send a Welcome email to the user...
        } yield Created(subscriber.email)
    }
}
