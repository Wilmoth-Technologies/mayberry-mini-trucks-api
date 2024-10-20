package controllers

import akka.stream.Materializer
import com.azure.cosmos.models.PartitionKey
import dao.{CosmosDb, CosmosQuery}
import models.Inventory
import play.api.Logger
import play.api.libs.Files
import play.api.mvc._
import services.CloudStorageService
import shared.AppFunctions.multipartRequestToObject

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManagementController @Inject()(cc: ControllerComponents,
                                     gcsService: CloudStorageService,
                                     cosmosDb: CosmosDb)
                                    (implicit ec: ExecutionContext, mat: Materializer) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass)
  val inventoryCollection: String = CosmosQuery.inventoryCollection

  def submitInventory: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData).async {
    implicit request => {
      // Parse form fields
      logger.info(s"POST: Submit Inventory: $request")
      val inventoryDetails = multipartRequestToObject[Inventory](request.body.dataParts.get("inventory").flatMap(_.headOption))

      def parseImages: Future[List[String]] = {
        Future(request.body.files.zipWithIndex.map {
          case (file, index) =>
            val imageName: String = s"image-$index"
            gcsService.uploadImage("mayberry-mini-trucks-inventory-images", inventoryDetails.vin, imageName, file.ref.path)
        }.toList)
      }

      for {
        imageLinkList <- parseImages
        _ <- cosmosDb.add(inventoryDetails.copy(id = inventoryDetails.vin, imageLinks = imageLinkList), inventoryCollection, new PartitionKey(inventoryDetails.vin))
      } yield Ok(inventoryDetails.vin)
    }
  }
}