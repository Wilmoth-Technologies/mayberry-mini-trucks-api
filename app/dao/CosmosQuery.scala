package dao

import com.azure.cosmos.models.{SqlParameter, SqlQuerySpec}
import com.typesafe.config.{Config, ConfigFactory}
import shared.AppConstants.parameterizedId

object CosmosQuery {
  val config: Config = ConfigFactory.load()
  val inventoryCollection: String = config.getString("cosmosdb.collection.inventory")
  val subscriberCollection: String = config.getString("cosmosdb.collection.subscriber")

  def getResultsById(id: String)(collectionName: String): SqlQuerySpec =
    new SqlQuerySpec(
      s"""SELECT * FROM $collectionName c WHERE c.id = $parameterizedId""",
      List(new SqlParameter(parameterizedId, id)): _*
    )

  def getAllResults()(collectionName: String): SqlQuerySpec =
    new SqlQuerySpec(s"""SELECT * FROM $collectionName c""")

  def getNotSoldInventory()(collectionName: String): SqlQuerySpec =
    new SqlQuerySpec(
      s"""SELECT * FROM $collectionName c
         |WHERE c.status != "Sold"
         |AND ARRAY_LENGTH(c.imageLinks) >= 1
         |ORDER BY c.status ASC""".stripMargin)

  def getInStockInventoryLimitTen()(collectionName: String): SqlQuerySpec =
    new SqlQuerySpec(
      s"""SELECT * FROM $collectionName c
         |WHERE c.status != "Sold"
         |AND c.status != "Pending Sale"
         |AND ARRAY_LENGTH(c.imageLinks) >= 1
         |OFFSET 0 LIMIT 10""".stripMargin)
}