package controllers

import akka.stream.Materializer
import com.azure.cosmos.models.PartitionKey
import dao.CosmosQuery.{getAllResults, getResultsById}
import dao.{CosmosDb, CosmosQuery}
import models.{Inventory, InventoryTable}
import play.api.Logger
import play.api.libs.Files
import play.api.mvc._
import services.CloudStorageService
import shared.AppFunctions.{currentDateTimeInTimeStamp, listToJson, multipartRequestToObject}
import shared.exceptions.RecordAlreadyExists

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManagementController @Inject()(cc: ControllerComponents,
                                     gcsService: CloudStorageService,
                                     cosmosDb: CosmosDb)
                                    (implicit ec: ExecutionContext, mat: Materializer) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass)
  private val inventoryCollection: String = CosmosQuery.inventoryCollection

  def fetchAllPhotos(vin: String): Action[AnyContent] =
    Action.async {
      try {
        Future(Ok(gcsService.getBucketContents(vin)))
      } catch {
        case e: Exception =>
          Future(InternalServerError(s"Error listing image binaries: ${e.getMessage}"))
      }
    }

  def fetchAllVin: Action[AnyContent] =
    Action.async {
      for {
        inventoryList <- cosmosDb.runQuery[Inventory](getAllResults(), inventoryCollection)
        mappedList = inventoryList.map(item => item.vin)
      } yield listToJson(mappedList)
    }

  def fetchAllInventoryItems: Action[AnyContent] =
    Action.async {
      for {
        inventoryList <- cosmosDb.runQuery[Inventory](getAllResults(), inventoryCollection)
        mappedList = inventoryList.map(item => InventoryTable(item.vin, item.modelCode, item.stockNumber,
          item.purchaseDate, item.make, item.model, item.year, item.mileage, item.price, item.status))
      } yield listToJson(mappedList)
    }

  def fetchSingleInventoryItem(vin: String): Action[AnyContent] =
    Action.async {
      for {
        inventoryItem <- cosmosDb.runQuery[Inventory](getResultsById(vin), inventoryCollection, Some(new PartitionKey(vin)))
      } yield listToJson(inventoryItem)
    }
//TODO: Add in updatedBy field based on User Auth
  def submitNewInventory: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData).async {
    implicit request => {
      println(s"POST: Submit New Inventory: $request")
      val inventoryDetails = multipartRequestToObject[Inventory](request.body.dataParts.get("inventory").flatMap(_.headOption))

      for {
        response <- cosmosDb.runQuery[Inventory](getResultsById(inventoryDetails.vin), inventoryCollection, Some(new PartitionKey(inventoryDetails.vin)))
        _ = if (response.nonEmpty) {
          throw RecordAlreadyExists(s"VIN: ${inventoryDetails.vin} already exists in Inventory")
        }
        imageLinkList <- parseImages(request.body, inventoryDetails)
        _ <- cosmosDb.add(inventoryDetails.copy(id = inventoryDetails.vin, imageLinks = imageLinkList,
          updatedTimeStamp = currentDateTimeInTimeStamp, creationTimeStamp = currentDateTimeInTimeStamp),
          inventoryCollection, new PartitionKey(inventoryDetails.vin))
      } yield Created(inventoryDetails.vin)
    }
  }
  //TODO: Add in updatedBy field based on User Auth
  def submitInventoryEdit(areImagesUpdated: Boolean = false): Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData).async {
    implicit request => {
      println(s"PUT: Submit Inventory: $request")
      val inventoryDetails = multipartRequestToObject[Inventory](request.body.dataParts.get("inventory").flatMap(_.headOption))

      if (areImagesUpdated) {
        for {
          imageLinkList <- clearAndParseImages(request.body, inventoryDetails)
          _ <- cosmosDb.upsertDatabaseEntry(inventoryDetails.copy(id = inventoryDetails.vin, imageLinks = imageLinkList,
            updatedTimeStamp = currentDateTimeInTimeStamp), inventoryCollection, inventoryDetails.vin, inventoryDetails.vin)
        } yield NoContent
      } else {
        for {
          _ <- cosmosDb.upsertDatabaseEntry(inventoryDetails.copy(id = inventoryDetails.vin,
            updatedTimeStamp = currentDateTimeInTimeStamp), inventoryCollection, inventoryDetails.vin, inventoryDetails.vin)
        } yield NoContent
      }
    }
  }

  def deleteInventory(vin: String): Action[AnyContent] = Action.async {
    println(s"Deleting Inventory Item vin: $vin")
    gcsService.deleteBlob(vin)
    for {
      _ <- cosmosDb.deleteByIdAndKeyHelper(inventoryCollection, vin, vin)
    } yield NoContent
  }


  /* START HELPER FUNCTIONS */
  private def parseImages(body: MultipartFormData[Files.TemporaryFile], inventory: Inventory): Future[List[String]] = {
    Future(body.files.zipWithIndex.map {
      case (file, index) =>
        val imageName: String = s"image-$index"
        gcsService.uploadImage(inventory.vin, imageName, file.ref.path)
    }.toList)
  }

  private def clearAndParseImages(body: MultipartFormData[Files.TemporaryFile], inventory: Inventory): Future[List[String]] = {
    gcsService.deleteBlob(inventory.vin)
    Future(body.files.zipWithIndex.map {
      case (file, index) =>
        val imageName: String = s"image-$index"
        gcsService.uploadImage(inventory.vin, imageName, file.ref.path)
    }.toList)
  }
  /* END HELPER FUNCTIONS */
}