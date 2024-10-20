package shared.exceptions

import play.api.http.Status

case class NotFoundException(notFoundDetails: String = "Not found") extends ApiException(Status.NOT_FOUND, notFoundDetails)

case class RecordAlreadyExists(recordDetails: String) extends ApiException(Status.CONFLICT, s"Record Already exists. Record Details: $recordDetails")

case object UnknownDeploymentRegion extends Exception

case object IncorrectPreferredRegionException extends Exception