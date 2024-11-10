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
      if (sys.env("ENVIRONMENT").equals("dev")) {
        new CosmosClientBuilder()
          .endpoint("https://mayberry-mini-trucks.documents.azure.com:443/")
          .key(sys.env("COSMOS_DB_CONNECTION_KEY"))
          .throttlingRetryOptions(throttlingRetryOptions)
          .consistencyLevel(ConsistencyLevel.EVENTUAL)
          .contentResponseOnWriteEnabled(true)
          .buildClient()
      } else {
        new CosmosClientBuilder()
          .endpoint("https://mayberry-mini-trucks.documents.azure.com:443/")
          .key(sys.env("COSMOS_DB_CONNECTION_KEY"))
          .throttlingRetryOptions(throttlingRetryOptions)
          .consistencyLevel(ConsistencyLevel.EVENTUAL)
          .contentResponseOnWriteEnabled(true)
          .buildClient()
      }
    } else {
      new CosmosClientBuilder()
        .endpoint("https://mayberry-mini-trucks.documents.azure.com:443/")
        .key(config.getString("cosmosdbKey"))
        .throttlingRetryOptions(throttlingRetryOptions)
        .consistencyLevel(ConsistencyLevel.EVENTUAL)
        .contentResponseOnWriteEnabled(true)
        .buildClient()
    }

  }
}
