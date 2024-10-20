package dao

import com.typesafe.config.{Config, ConfigFactory}

object CosmosQuery {
  val config: Config = ConfigFactory.load()
  val inventoryCollection: String = config.getString("cosmosdb.collection.inventory")
}
