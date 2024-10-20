package shared.exceptions

import play.api.http.Status

case class CosmosException(ex: Throwable)
  extends ApiException(
    Status.INTERNAL_SERVER_ERROR,
    "Database Exception",
    Some("Error Caused While performing Cosmos DB Operation"),
    Some(ex)
  )

case class NotFoundCosmosException(ex: Throwable)
  extends ApiException(
    Status.NOT_FOUND,
    "Database Exception",
    Some("Resource not Found, While performing Cosmos DB Operation"),
    Some(ex)
  )