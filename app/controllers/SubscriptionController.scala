package controllers

import com.azure.cosmos.models.PartitionKey
import dao.{CosmosDb, CosmosQuery}
import models.Subscribers
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.EmailService
import shared.AppFunctions.{currentDateTimeInTimeStamp, requestToObject}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionController @Inject()(cc: ControllerComponents,
                                       cosmosDb: CosmosDb,
                                       emailService: EmailService)
                                      (implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val subscriberCollection: String = CosmosQuery.subscriberCollection

  def addSubscriber: Action[AnyContent] =
    Action.async {
      implicit request =>
        val subscriber = requestToObject[Subscribers](request)
        println(s"Adding Subscriber via email: ${subscriber.email}")

        for {
          isEmailOnUnsubscribedList <- emailService.isEmailOnGlobalUnsubscribe(subscriber.email)
          _ <- if (isEmailOnUnsubscribedList) {
            emailService.removeEmailFromGlobalUnsubscribe(subscriber.email)
          } else {
            Future(null)
          }
          _ <- cosmosDb.add(subscriber.copy(id = subscriber.email, subscribeDate = currentDateTimeInTimeStamp), subscriberCollection, new PartitionKey(subscriber.email))
          _ <- emailService.sendEmail(subscriber.email, "d-751582f888a04224bdb02bc12098de8d", Map("first_name" -> subscriber.firstName))
        } yield Created(subscriber.email)
    }

  def removeSubscriber(email: String): Action[AnyContent] =
    Action.async {
      println(s"Removing Subscriber via email: $email")

      for {
        _ <- cosmosDb.deleteByIdAndKeyHelper(subscriberCollection, email, email)
      } yield NoContent
    }
}
