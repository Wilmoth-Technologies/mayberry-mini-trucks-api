package modules

import com.google.inject.AbstractModule
import dao.{CosmosDb, CosmosDbBuilder}


class Injection extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[CosmosDbBuilder]).asEagerSingleton()
    bind(classOf[CosmosDb]).asEagerSingleton()
  }
}