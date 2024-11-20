package controllers

import dao.CosmosQuery._
import dao.{CosmosDb, CosmosQuery}
import models.{ContactRequest, Inventory, InventoryLandingScroller, InventoryPaginationData}
import play.api.Logger
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.{CloudStorageService, EmailService}
import shared.AppFunctions.{listToJson, requestToObject}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InventoryController @Inject()(cc: ControllerComponents,
                                    gcsService: CloudStorageService,
                                    cosmosDb: CosmosDb,
                                    emailService: EmailService)
                                   (implicit ec: ExecutionContext) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass)
  private val inventoryCollection: String = CosmosQuery.inventoryCollection

  def contactUs: Action[AnyContent] =
    Action.async {
      implicit request =>
        val contactRequest = requestToObject[ContactRequest](request)
        println(s"Sending contact request for: ${contactRequest.email}")

        val subject: String = if (contactRequest.isFailedFilter) {
          s"Contact Request on Failed Inventory Search from ${contactRequest.firstName} ${contactRequest.lastName}"
        } else {
          s"Contact Request on VIN: ${contactRequest.vin} from ${contactRequest.firstName} ${contactRequest.lastName}"
        }

        val requestMap = Map(
          "first_name" -> contactRequest.firstName,
          "last_name" -> contactRequest.lastName,
          "description" -> contactRequest.description,
          "phone_number" -> contactRequest.phoneNumber,
          "email" -> contactRequest.email,
          "listing_link" -> s"http://mayberryminitrucks.com/inventory/${contactRequest.vin}",
          "vin" -> contactRequest.vin,
          "subject" -> subject
        )
        for {
          _ <- emailService.sendEmail("sales@mayberryminitrucks.com", "d-d955035d6b614bb4b4b68ab6a956dc50", requestMap)
        } yield Created

    }

  def generalContactUs: Action[AnyContent] =
    Action.async {
      implicit request =>
        val contactRequest = requestToObject[ContactRequest](request)
        println(s"Sending contact request for: ${contactRequest.email}")

        val requestMap = Map(
          "first_name" -> contactRequest.firstName,
          "last_name" -> contactRequest.lastName,
          "description" -> contactRequest.description,
          "phone_number" -> contactRequest.phoneNumber,
          "email" -> contactRequest.email,
          "subject" -> s"Contact Request from ${contactRequest.firstName} ${contactRequest.lastName}"
        )
        for {
          _ <- emailService.sendEmail("sales@mayberryminitrucks.com", "d-df8007f9b3d345d8a2d34f97c507ebcc", requestMap)
        } yield Created
    }

  def fetchAllInventoryWithMetaData: Action[AnyContent] =
    Action.async {
      for {
        inventoryList <- cosmosDb.runQuery[Inventory](getNotSoldInventory(), inventoryCollection)
        mappedList = inventoryList.map(item => InventoryPaginationData(item.vin, item.modelCode, item.stockNumber, item.make,
          item.model, item.year, item.exteriorColor, item.interiorColor, item.mileage, item.transmission, item.engine,
          item.description, item.price, item.titleInHand, item.status, item.options, item.imageLinks.headOption.get))
      } yield listToJson(mappedList)
    }

  def fetchTopTenInventoryWithMetaData: Action[AnyContent] =
    Action.async {
      for {
        inventoryList <- cosmosDb.runQuery[Inventory](getInStockInventoryLimitTen(), inventoryCollection)
        mappedList = inventoryList.map(item => InventoryLandingScroller(item.vin, item.make, item.model, item.year,
          item.price, item.mileage, item.imageLinks.headOption.get))
      } yield listToJson(mappedList)
    }

  def fetchSingleInventoryItem(vin: String): Action[AnyContent] =
    Action.async {
      for {
        inventoryItem <- cosmosDb.runQuery[Inventory](getResultsById(vin), inventoryCollection)
      } yield listToJson(inventoryItem)
    }

  def fetchSingleInventoryItemPhotos(vin: String): Action[AnyContent] =
    Action.async {
      try {
        Future(Ok(gcsService.getBucketContents(vin)))
      } catch {
        case e: Exception =>
          Future(InternalServerError(s"Error listing image binaries: ${e.getMessage}"))
      }
    }
}