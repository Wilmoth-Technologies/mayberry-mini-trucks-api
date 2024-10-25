package controllers

import akka.stream.Materializer
import com.azure.cosmos.models.PartitionKey
import dao.CosmosQuery._
import dao.{CosmosDb, CosmosQuery}
import models.{Inventory, InventoryPaginationData}
import play.api.Logger
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.CloudStorageService
import shared.AppFunctions.listToJson

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InventoryController @Inject()(cc: ControllerComponents,
                                    gcsService: CloudStorageService,
                                    cosmosDb: CosmosDb)
                                   (implicit ec: ExecutionContext, mat: Materializer) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass)
  private val inventoryCollection: String = CosmosQuery.inventoryCollection

  def fetchAllInventoryWithMetaData: Action[AnyContent] =
    Action.async {
      for {
        inventoryList <- cosmosDb.runQuery[Inventory](getNotSoldInventory(), inventoryCollection)
        mappedList = inventoryList.map(item => InventoryPaginationData(item.vin, item.modelCode, item.stockNumber, item.make,
          item.model, item.year, item.exteriorColor, item.interiorColor, item.mileage, item.transmission, item.engine,
          item.description, item.price, item.titleInHand, item.status, item.options, item.imageLinks.headOption.get))
      } yield listToJson(mappedList)
    }

  def fetchSingleInventoryItem(vin: String): Action[AnyContent] =
    Action.async {
      for {
        inventoryItem <- cosmosDb.runQuery[Inventory](getResultsById(vin), inventoryCollection, Some(new PartitionKey(vin)))
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