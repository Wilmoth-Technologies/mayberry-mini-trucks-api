package dao

import akka.actor.ActorSystem
import com.azure.cosmos._
import com.azure.cosmos.implementation.{ConflictException, NotFoundException}
import com.azure.cosmos.models._
import com.fasterxml.jackson.databind.JsonNode
import com.typesafe.config.{Config, ConfigFactory}

import javax.inject.{Inject, Singleton}
import java.util.ArrayList
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.NoContent
import shared.AppConstants.{CosmosDeleteStatusCode, CosmosInsertionStatusCode, CosmosSuccessStatusCode}
import shared.AppFunctions.{jsonToObject, objectMapper, toJson}
import shared.RunWithRetry
import shared.exceptions.{CosmosException, NotFoundCosmosException, RecordAlreadyExists}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._

@Singleton
class CosmosDb @Inject()(builder: CosmosDbBuilder)(implicit ec: ExecutionContext, system: ActorSystem) {
  val logger: Logger = play.api.Logger(this.getClass)
  val noOfAttempts: Int = ConfigFactory.load.getInt("retry.maxretryattempt")
  val retryInterval: Int = ConfigFactory.load.getInt("retry.interval")
  val config: Config = ConfigFactory.load()
  val cosmosDbClient: CosmosClient = builder.build()

  lazy val database: String = config.getString("regioncosmosdb.databasename")

  // prevents having to recreate the containers on every call
  private val containers: Map[String, CosmosContainer] =
    List(
      config.getString(s"cosmosdb.collection.inventory"),
    ).map(collection => collection -> createCosmosContainer(collection)
    ).toMap

  private def createCosmosContainer(collection: String) =
    cosmosDbClient
      .getDatabase(database)
      .getContainer(collection)

  private def getCosmosContainer(collection: String) =
    containers.getOrElse(collection, createCosmosContainer(collection))

  //Execute Query : Mainly GET
  def runQuery[T: TypeTag](queryString: String => SqlQuerySpec, collection: String, partitionKey: Option[PartitionKey] = None)(implicit m: Manifest[T]): Future[List[T]] = {
    val queryOptions = new CosmosQueryRequestOptions()
    queryOptions.setQueryMetricsEnabled(true)
    if (partitionKey.isDefined) queryOptions.setPartitionKey(partitionKey.get)

    val queryResult: Future[List[JsonNode]] = RunWithRetry
      .retry(Future {
        getCosmosContainer(collection)
          .queryItems(queryString(collection), queryOptions, classOf[JsonNode])
          .iterator().asScala.toList
      }, noOfAttempts, retryInterval.seconds)

    queryResult.map(_.map(jsonToObject[T]))
  }.recoverWith {
    case ex: Exception =>
      logger.error(s"All Attempts failed to get cosmos data for query $queryString ${ex.getMessage} ", ex)
      throw CosmosException(ex)
  }

  //CREATE query
  def add[T](objectToAdd: T, collection: String, partitionKey: PartitionKey): Future[Boolean] = {
    RunWithRetry.retry(Future {
        val statusCode = getCosmosContainer(collection).createItem(objectMapper.valueToTree(objectToAdd),
          partitionKey, new CosmosItemRequestOptions()).getStatusCode
        statusCode == CosmosInsertionStatusCode
      }, noOfAttempts, retryInterval.seconds)
      .recoverWith({
        case exception: ConflictException =>
          logger.error(s"Failed to add data in Cosmos collection $collection. Record already exists. ${exception.getMessage} ", exception)
          throw RecordAlreadyExists(s"Collection: $collection ; Record: ${toJson(objectToAdd)}")
        case exception: Exception =>
          logger.error(s"All attempts failed to add object: ${toJson(objectToAdd)} in Cosmos collection: $collection for partitionKey: $partitionKey." +
            s"${exception.getMessage}", exception)
          throw CosmosException(exception)
      })
  }

  //UPDATE query
  def updateDatabaseEntry[T](updatedObj: T, collection: String, id: String, partitionKeyVal: String): Future[Boolean] = {
    RunWithRetry.retry(Future {
        val connection: CosmosContainer = getCosmosContainer(collection)
        val statusCode = connection.replaceItem(objectMapper.valueToTree(updatedObj), id,
          new PartitionKey(partitionKeyVal), new CosmosItemRequestOptions()).getStatusCode
        statusCode == CosmosSuccessStatusCode
      }, noOfAttempts, retryInterval.seconds)
      .recoverWith({
        case notFoundException: NotFoundException =>
          logger.error(s"Failed to update record $id in collection $collection because it does not exist.", notFoundException)
          throw NotFoundCosmosException(notFoundException)
        case exception: Exception =>
          logger.error(s"Failed to update record $id in collection $collection. ${exception.getMessage} ", exception)
          throw CosmosException(exception)
      })
  }

  //UPSERT query
  //https://docs.microsoft.com/en-us/dotnet/api/microsoft.azure.documents.client.documentclient.upsertdocumentasync?view=azure-dotnet
  def upsertDatabaseEntry[T](updatedObj: T, collection: String, id: String, partitionKeyVal: String): Future[Boolean] = {
    RunWithRetry.retry(Future {
        val connection: CosmosContainer = getCosmosContainer(collection)
        val statusCode = connection.upsertItem(objectMapper.valueToTree(updatedObj),
          new PartitionKey(partitionKeyVal), new CosmosItemRequestOptions()).getStatusCode
        statusCode == CosmosSuccessStatusCode
      }, noOfAttempts, retryInterval.seconds)
      .recover({
        case exception: Exception =>
          logger.error(s"Failed to update record $id in collection $collection. Exception: ${exception.toString}")
          throw CosmosException(exception)
      })
  }

  //DELETE query
  def deleteByIdAndKeyHelper(collection: String, id: String, partitionKeyVal: String): Future[Result] = {

    def deleteByBU(id: String, collection: String, partitionKey: PartitionKey): Future[Boolean] = {
      RunWithRetry.retry(Future {
        val statusCode = getCosmosContainer(collection)
          .deleteItem(id, partitionKey, new CosmosItemRequestOptions())
          .getStatusCode
        statusCode == CosmosDeleteStatusCode
      }, noOfAttempts, retryInterval.seconds)
    }

    deleteByBU(id, collection, new PartitionKey(partitionKeyVal))
      .map(isDeleted => {
        if (isDeleted) {
          NoContent
        } else {
          throw new NotFoundException()
        }
      })
      .recover {
        case notFoundException: NotFoundException =>
          logger.info(s"could not find row in collection $collection with partition key: $partitionKeyVal and id: $id")
          throw NotFoundCosmosException(notFoundException)
        case exception: Exception =>
          logger.error(s"Failed to delete row with id: $id ${exception.getMessage}", exception)
          throw CosmosException(exception)
      }
  }

  //Bulk UPSERT query
  def bulkUpsertSproc(objectList: ArrayList[AnyRef], collection: String, partitionKey: PartitionKey): Future[Boolean] = {
    val cosmoStoreOptions = new CosmosStoredProcedureRequestOptions
    cosmoStoreOptions.setPartitionKey(partitionKey)

    val documentList = new ArrayList[AnyRef]()
    documentList.add(objectList)

    RunWithRetry.retry(Future {
        val statusCode = getCosmosContainer(collection)
          .getScripts.getStoredProcedure(config.getString(s"cosmosdb.stored-proc.upsert"))
          .execute(documentList, cosmoStoreOptions)
          .getStatusCode
        statusCode == CosmosSuccessStatusCode
      }, noOfAttempts, retryInterval.seconds)
      .recover({
        case exception: Exception =>
          logger.error(s"Failed to upsert records $documentList in collection $collection. Exception: ${exception.toString}")
          throw CosmosException(exception)
      })
  }

  def bulkReplaceSproc(objectList: ArrayList[AnyRef], collection: String, partitionKey: PartitionKey): Future[Boolean] = {
    val cosmoStoreOptions = new CosmosStoredProcedureRequestOptions
    cosmoStoreOptions.setPartitionKey(partitionKey)

    val documentList = new ArrayList[AnyRef]()
    documentList.add(objectList)

    RunWithRetry.retry(Future {
        val statusCode = getCosmosContainer(collection)
          .getScripts.getStoredProcedure(config.getString(s"cosmosdb.stored-proc.replace"))
          .execute(documentList, cosmoStoreOptions)
          .getStatusCode
        statusCode == CosmosSuccessStatusCode
      }, noOfAttempts, retryInterval.seconds)
      .recover({
        case exception: Exception =>
          logger.error(s"Failed to replace records $documentList in collection $collection. Exception: ${exception.toString}")
          throw CosmosException(exception)
      })
  }
}