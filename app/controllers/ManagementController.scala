package controllers

import actions.AuthAction
import akka.stream.Materializer
import com.azure.cosmos.models.PartitionKey
import dao.CosmosQuery.{getAllResults, getResultsById}
import dao.{CosmosDb, CosmosQuery}
import models.{Inventory, InventoryTable, Notification, Subscribers}
import play.api.Logger
import play.api.libs.Files
import play.api.mvc._
import services.{CloudStorageService, InventoryNewsLetterService}
import shared.AppFunctions.{currentDateTimeInTimeStamp, listToJson, multipartRequestToObject, requestToObject, toSha256}
import shared.exceptions.RecordAlreadyExists

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManagementController @Inject()(cc: ControllerComponents,
                                     gcsService: CloudStorageService,
                                     cosmosDb: CosmosDb,
                                     inventoryNewsLetterService: InventoryNewsLetterService,
                                     authAction: AuthAction)
                                    (implicit ec: ExecutionContext, mat: Materializer) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass)
  private val inventoryCollection: String = CosmosQuery.inventoryCollection
  private val subscriberCollection: String = CosmosQuery.subscriberCollection
  private val notificationCollection: String = CosmosQuery.notificationCollection

  def triggerInventoryNewsLetter: Action[AnyContent] =
    authAction.async {
      println("Starting Manual Execution of Inventory Email Newsletter")
      inventoryNewsLetterService.executeEmailNewsLetter()
      Future(Ok("Triggered News Letter"))
    }

  def fetchNotificationList: Action[AnyContent] =
    authAction.async {
      for {
        notificationList <- cosmosDb.runQuery[Notification](getAllResults(), notificationCollection)
        mappedRes = notificationList.map(item => item.copy(startDate = convertDateFormat(item.startDate), endDate = convertDateFormat(item.endDate)))
      } yield listToJson(mappedRes)
    }

  def deleteNotification(id: String): Action[AnyContent] = authAction.async {
    println(s"Deleting Notification Item id: $id")
    for {
      _ <- cosmosDb.deleteByIdAndKeyHelper(notificationCollection, id, id)
    } yield NoContent
  }

  def addNotification: Action[AnyContent] =
    authAction.async {
      implicit request =>
        val notification = requestToObject[Notification](request)
        val notificationId = toSha256(notification.startDate + notification.endDate + notification.description)
        println(s"Creating Notification via id: ${notification.id}")


        for {
          _ <- cosmosDb.add(notification.copy(id = notificationId), notificationCollection, new PartitionKey(notificationId))
        } yield Created(notificationId)
    }

  def fetchSubscriberList: Action[AnyContent] =
    authAction.async {
      for {
        subscriberList <- cosmosDb.runQuery[Subscribers](getAllResults(), subscriberCollection)
      } yield listToJson(subscriberList)
    }

  def fetchAllPhotos(vin: String): Action[AnyContent] =
    authAction.async {
      try {
        Future(Ok(gcsService.getBucketContents(vin)))
      } catch {
        case e: Exception =>
          Future(InternalServerError(s"Error listing image binaries: ${e.getMessage}"))
      }
    }

  def fetchAllVin: Action[AnyContent] =
    authAction.async {
      for {
        inventoryList <- cosmosDb.runQuery[Inventory](getAllResults(), inventoryCollection)
        mappedList = inventoryList.map(item => item.vin)
      } yield listToJson(mappedList)
    }

  def fetchAllInventoryItems: Action[AnyContent] =
    authAction.async {
      for {
        inventoryList <- cosmosDb.runQuery[Inventory](getAllResults(), inventoryCollection)
        mappedList = inventoryList.map(item => InventoryTable(item.vin, item.modelCode, item.stockNumber,
          item.purchaseDate, item.make, item.model, item.year, item.mileage, item.price, item.status, if (item.titleInHand) "Yes" else "No"))
      } yield listToJson(mappedList)
    }

  def fetchSingleInventoryItem(vin: String): Action[AnyContent] =
    authAction.async {
      for {
        inventoryItem <- cosmosDb.runQuery[Inventory](getResultsById(vin), inventoryCollection)
      } yield listToJson(inventoryItem)
    }

  def submitNewInventory: Action[MultipartFormData[Files.TemporaryFile]] = authAction(parse.multipartFormData(maxLength = 500 * 1024 * 1024)).async {
    implicit request => {
      println(s"POST: Submit New Inventory: $request")
      val inventoryDetails = multipartRequestToObject[Inventory](request.body.dataParts.get("inventory").flatMap(_.headOption))

      for {
        response <- cosmosDb.runQuery[Inventory](getResultsById(inventoryDetails.vin), inventoryCollection, Some(new PartitionKey(inventoryDetails.year)))
        _ = if (response.nonEmpty) {
          throw RecordAlreadyExists(s"VIN: ${inventoryDetails.vin} already exists in Inventory")
        }
        imageLinkList <- parseImages(request.body, inventoryDetails)
        _ <- cosmosDb.add(inventoryDetails.copy(id = inventoryDetails.vin, imageLinks = imageLinkList,
          updatedTimeStamp = currentDateTimeInTimeStamp, creationTimeStamp = currentDateTimeInTimeStamp),
          inventoryCollection, new PartitionKey(inventoryDetails.year))
      } yield Created(inventoryDetails.vin)
    }
  }

  def submitInventoryEdit(areImagesUpdated: Boolean = false): Action[MultipartFormData[Files.TemporaryFile]] = authAction(parse.multipartFormData(maxLength = 500 * 1024 * 1024)).async {
    implicit request => {
      println(s"PUT: Submit Inventory: $request")
      val inventoryDetails = multipartRequestToObject[Inventory](request.body.dataParts.get("inventory").flatMap(_.headOption))

      if (areImagesUpdated) {
        for {
          imageLinkList <- clearAndParseImages(request.body, inventoryDetails)
          imageListWithCacheBreak = if (imageLinkList.nonEmpty) {
            imageLinkList.updated(0, imageLinkList.head + s"?v=$currentDateTimeInTimeStamp")
          } else {
            imageLinkList
          }
          _ <- cosmosDb.upsertDatabaseEntry(inventoryDetails.copy(id = inventoryDetails.vin, imageLinks = imageListWithCacheBreak,
            updatedTimeStamp = currentDateTimeInTimeStamp), inventoryCollection, inventoryDetails.vin, inventoryDetails.year)
        } yield NoContent
      } else {
        for {
          _ <- cosmosDb.upsertDatabaseEntry(inventoryDetails.copy(id = inventoryDetails.vin,
            updatedTimeStamp = currentDateTimeInTimeStamp), inventoryCollection, inventoryDetails.vin, inventoryDetails.year)
        } yield NoContent
      }
    }
  }

  def deleteInventory(vin: String, year: String): Action[AnyContent] = authAction.async {
    println(s"Deleting Inventory Item vin: $vin")
    gcsService.deleteBlob(vin)
    for {
      _ <- cosmosDb.deleteByIdAndKeyHelper(inventoryCollection, vin, year)
    } yield NoContent
  }


  /* START HELPER FUNCTIONS */
  private def parseImages(body: MultipartFormData[Files.TemporaryFile], inventory: Inventory): Future[List[String]] = {
    Future(body.files.zipWithIndex.map {
      case (file, index) =>
        val imageName: String = s"image-$index"
        gcsService.uploadImage(inventory.vin, imageName, file.ref.path)
    }.toList.filter(res => res != ""))
  }

  private def clearAndParseImages(body: MultipartFormData[Files.TemporaryFile], inventory: Inventory): Future[List[String]] = {
    gcsService.deleteBlob(inventory.vin)
    Future(body.files.zipWithIndex.map {
      case (file, index) =>
        val imageName: String = s"image-$index"
        gcsService.uploadImage(inventory.vin, imageName, file.ref.path)
    }.toList)
  }

  def convertDateFormat(dateStr: String): String = {
    val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val outputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    val date = LocalDate.parse(dateStr, inputFormatter)
    date.format(outputFormatter)
  }
  /* END HELPER FUNCTIONS */
}