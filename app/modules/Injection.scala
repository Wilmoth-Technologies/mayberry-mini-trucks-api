package modules

import com.google.inject.AbstractModule
import dao.{CosmosDb, CosmosDbBuilder}
import services.InventoryNewsLetterService


class Injection extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[CosmosDbBuilder]).asEagerSingleton()
    bind(classOf[CosmosDb]).asEagerSingleton()
    bind(classOf[InventoryNewsLetterService]).asEagerSingleton()
  }
}