package controllers

import akka.stream.Materializer
import com.azure.cosmos.models.PartitionKey
import dao.CosmosQuery.getResultsById
import dao.{CosmosDb, CosmosQuery}
import models.Inventory
import play.api.Logger
import play.api.libs.Files
import play.api.mvc._
import services.CloudStorageService
import shared.AppFunctions.multipartRequestToObject
import shared.exceptions.RecordAlreadyExists

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManagementController @Inject()(cc: ControllerComponents,
                                     gcsService: CloudStorageService,
                                     cosmosDb: CosmosDb)
                                    (implicit ec: ExecutionContext, mat: Materializer) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass)
  private val inventoryCollection: String = CosmosQuery.inventoryCollection

  def submitNewInventory: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData).async {
    implicit request => {
      logger.info(s"POST: Submit New Inventory: $request")
      val inventoryDetails = multipartRequestToObject[Inventory](request.body.dataParts.get("inventory").flatMap(_.headOption))

      for {
        response <- cosmosDb.runQuery[Inventory](getResultsById(inventoryDetails.vin), inventoryCollection, Some(new PartitionKey(inventoryDetails.vin)))
        _ = if (response.nonEmpty) {
          throw RecordAlreadyExists(s"VIN: ${inventoryDetails.vin} already exists in Inventory")
        }
        imageLinkList <- parseImages(request.body, inventoryDetails)
        _ <- cosmosDb.add(inventoryDetails.copy(id = inventoryDetails.vin, imageLinks = imageLinkList), inventoryCollection, new PartitionKey(inventoryDetails.vin))
      } yield Created(inventoryDetails.vin)
    }
  }

  def submitInventoryEdit(areImagesUpdated: Boolean = false): Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData).async {
    implicit request => {
      logger.info(s"PUT: Submit Inventory: $request")
      val inventoryDetails = multipartRequestToObject[Inventory](request.body.dataParts.get("inventory").flatMap(_.headOption))

      if (areImagesUpdated) {
        for {
          imageLinkList <- clearAndParseImages(request.body, inventoryDetails)
          _ <- cosmosDb.upsertDatabaseEntry(inventoryDetails.copy(id = inventoryDetails.vin, imageLinks = imageLinkList), inventoryCollection, inventoryDetails.vin, inventoryDetails.vin)
        } yield NoContent
      } else {
        for {
          _ <- cosmosDb.upsertDatabaseEntry(inventoryDetails.copy(id = inventoryDetails.vin), inventoryCollection, inventoryDetails.vin, inventoryDetails.vin)
        } yield NoContent
      }
    }
  }

  def deleteInventory(vin: String): Action[AnyContent] = Action.async {
    logger.info(s"Deleting Inventory Item vin: $vin")
    clearImagesIfBlobExists(vin)
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
    clearImagesIfBlobExists(inventory.vin)
    Future(body.files.zipWithIndex.map {
      case (file, index) =>
        val imageName: String = s"image-$index"
        gcsService.uploadImage(inventory.vin, imageName, file.ref.path)
    }.toList)
  }

  private def clearImagesIfBlobExists(blobName: String): Unit = {
    if (gcsService.blobExists(blobName)) {
      logger.info(s"Blob '$blobName' exists, proceeding to delete.")
      gcsService.deleteBlob(blobName)
    } else {
      logger.info(s"Blob '$blobName' does not exist in bucket.")
    }
  }
  /* END HELPER FUNCTIONS */
}