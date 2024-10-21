package dao

import com.azure.cosmos.models.{SqlParameter, SqlQuerySpec}
import com.typesafe.config.{Config, ConfigFactory}
import shared.AppConstants.parameterizedId

object CosmosQuery {
  val config: Config = ConfigFactory.load()
  val inventoryCollection: String = config.getString("cosmosdb.collection.inventory")

  def getResultsById(id: String)(collectionName: String): SqlQuerySpec =
    new SqlQuerySpec(
      s"""SELECT * FROM $collectionName c WHERE c.id = $parameterizedId""",
      List(new SqlParameter(parameterizedId, id)): _*
    )

  def getAllResults()(collectionName: String): SqlQuerySpec =
    new SqlQuerySpec(s"""SELECT * FROM $collectionName c""")
}
