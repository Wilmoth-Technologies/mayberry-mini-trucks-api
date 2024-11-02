package controllers

import dao.CosmosQuery.getNotificationWithinWindow
import dao.{CosmosDb, CosmosQuery}
import models.Notification
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import shared.AppFunctions._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NotificationController @Inject()(cc: ControllerComponents,
                                       cosmosDb: CosmosDb)
                                      (implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val notificationCollection: String = CosmosQuery.notificationCollection

  def convertToISO(dateStr: String): String = {
    val parts = dateStr.split("/")
    s"${parts(2)}-${parts(0).padTo(2, '0')}-${parts(1).padTo(2, '0')}"
  }

  def fetchNotificationsByDate(date: String): Action[AnyContent] =
    Action.async {
      for {
        res <- cosmosDb.runQuery[Notification](getNotificationWithinWindow(convertToISO(date)), notificationCollection)
      } yield listToJsonAllowEmpty(res)
    }
}
