package models

import play.api.libs.json.{Json, OWrites}

case class ErrorResponse(errorId: String,
                         message: String,
                         info: Option[String])

object ErrorResponse {
  implicit def writes: OWrites[ErrorResponse] = Json.writes[ErrorResponse]
}