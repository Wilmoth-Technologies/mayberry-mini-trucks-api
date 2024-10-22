package dao

import akka.actor.ActorSystem
import com.azure.cosmos.{ConsistencyLevel, CosmosClient, CosmosClientBuilder, ThrottlingRetryOptions}
import com.typesafe.config.{Config, ConfigFactory}
import shared.SecretManager

import java.time.Duration
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CosmosDbBuilder @Inject()(secrets: SecretManager)(implicit ec: ExecutionContext, system: ActorSystem) {
  def build(): CosmosClient = {
    val config: Config = ConfigFactory.load()
    val throttlingRetryOptions = new ThrottlingRetryOptions()
    throttlingRetryOptions.setMaxRetryAttemptsOnThrottledRequests(5)
    throttlingRetryOptions.setMaxRetryWaitTime(Duration.ofSeconds(20))

    if (ConfigFactory.load.getBoolean("isSecretManagerSetup")) {
      new CosmosClientBuilder()
        .endpoint(config.getString("regioncosmosdb.endpoint"))
        .key(sys.env("COSMOS_DB_CONNECTION_KEY"))
        .throttlingRetryOptions(throttlingRetryOptions)
        .consistencyLevel(ConsistencyLevel.EVENTUAL)
        .contentResponseOnWriteEnabled(true)
        .buildClient()
    } else {
      new CosmosClientBuilder()
        .endpoint(config.getString("regioncosmosdb.endpoint"))
        .key(config.getString("cosmosdbKey"))
        .throttlingRetryOptions(throttlingRetryOptions)
        .consistencyLevel(ConsistencyLevel.EVENTUAL)
        .contentResponseOnWriteEnabled(true)
        .buildClient()
    }

  }
}
