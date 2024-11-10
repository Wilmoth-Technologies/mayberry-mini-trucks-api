package services

import akka.actor.{ActorSystem, Cancellable}
import dao.{CosmosDb, CosmosQuery}
import dao.CosmosQuery.{getAllResults, getInStockInventoryAddedInLastWeek}
import models.{Inventory, Subscribers}
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
      inventoryMap = inventoryList.zipWithIndex.flatMap {
        case (vehicle, index) => Map(
          s"photoUrl$index" -> vehicle.imageLinks.head,
          s"year$index" -> vehicle.year,
          s"make$index" -> vehicle.make,
          s"model$index" -> vehicle.model,
          s"price$index" -> formatPrice(vehicle.price),
          s"mileage$index" -> formatNumberWithCommas(vehicle.mileage.toString),
          s"engine$index" -> vehicle.engine,
          s"transmission$index" -> vehicle.transmission,
          s"color$index" -> vehicle.exteriorColor,
          s"itemURL$index" -> s"http://mayberryminitrucks.com/inventory/${vehicle.vin}"
        )
      }.toMap
      subscriberList <- cosmosDb.runQuery[Subscribers](getAllResults(), subscriberCollection)
      _ = subscriberList.map(subscriber =>
        if(inventoryList.length >= 6) {
          emailService.sendEmail(subscriber.email, "d-965b9c678e364190a1d8aef756faa080", inventoryMap)
        } else if (inventoryList.length >= 3) {
          emailService.sendEmail(subscriber.email, "d-287fccd69f8e479c88b1234cb078b5f1", inventoryMap)
        }
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
    calculateInitialDelay(), //Change me to 10.millis to run quicker at startup
    7.days
  )(new Runnable {
    override def run(): Unit = task()
  })

  // Optionally add a stop hook for cleanup on application shutdown
  def cancel(): Unit = cancellable.cancel()
}