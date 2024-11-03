package services

import akka.actor.{ActorSystem, Cancellable}
import dao.{CosmosDb, CosmosQuery}
import dao.CosmosQuery.{getAllResults, getInStockInventoryAddedInLastWeek}
import models.{Inventory, InventoryDetailsForTemplate, InventoryTemplateTopLevel, Subscribers}
import shared.AppFunctions._

import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalAdjusters}
import java.time.{DayOfWeek, Duration, Instant, ZoneId, ZonedDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration, MILLISECONDS}

@Singleton
class InventoryNewsLetterService @Inject()(actorSystem: ActorSystem,
                                           cosmosDb: CosmosDb,
                                           emailService: EmailService)
                                          (implicit ec: ExecutionContext) {
  private val inventoryCollection: String = CosmosQuery.inventoryCollection
  private val subscriberCollection: String = CosmosQuery.subscriberCollection

  // Define the task to run
  def task(): Unit = {
    println("Running scheduled task at " + ZonedDateTime.now(ZoneId.of("America/New_York")))
    val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
    val formattedDate = DateTimeFormatter.ISO_INSTANT.format(sevenDaysAgo)


    for {
      inventoryList <- cosmosDb.runQuery[Inventory](getInStockInventoryAddedInLastWeek(formattedDate), inventoryCollection)
      _ = println(inventoryList)
      inventoryTemplate = inventoryList.map(item => InventoryDetailsForTemplate(
        item.imageLinks.head,
        item.year,
        item.make,
        item.model,
        formatPrice(item.price),
        formatNumberWithCommas(item.mileage.toString),
        item.engine,
        item.transmission,
        item.exteriorColor,
        s"http://localhost:3000/inventory/${item.vin}",
      ))

      subscriberList <- cosmosDb.runQuery[Subscribers](getAllResults(), subscriberCollection)
      _ = subscriberList.map(subscriber =>
        emailService.sendEmailWithStringData(subscriber.email, "d-965b9c678e364190a1d8aef756faa080", InventoryTemplateTopLevel(inventoryTemplate))
      )
    } yield ""
  }

  // Calculate the initial delay until the next Wednesday at 9 AM EST
  def calculateInitialDelay(): FiniteDuration = {
    val now = ZonedDateTime.now(ZoneId.of("America/New_York"))
    val nextRun = now.`with`(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
      .withHour(9)
      .withMinute(0)
      .withSecond(0)
      .withNano(0)

    val initialDelay = Duration.between(now, nextRun)
    FiniteDuration(initialDelay.toMillis, MILLISECONDS)
  }

  // Schedule the task to run weekly on Wednesdays at 9 AM EST
  private val cancellable: Cancellable = actorSystem.scheduler.scheduleAtFixedRate(
    10.millis, //TODO: Call calculateInitialDelay as this will ensure this runs every Wednesday
    7.days
  )(new Runnable {
    override def run(): Unit = task()
  })

  // Optionally add a stop hook for cleanup on application shutdown
  def cancel(): Unit = cancellable.cancel()
}