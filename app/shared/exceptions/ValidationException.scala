package shared.exceptions

import play.api.http.Status
import play.api.libs.json._
import shared.AppFunctions.ValidationResult

case class ValidationException(violations: List[ValidationResult])
  extends ApiException(
    Status.BAD_REQUEST,
    "Validation failed",
    Some("Invalid " + (violations.map(_.resultText).mkString(",")))
  )

object ValidationException {
  implicit val violationWrites: Writes[ValidationResult] = Writes[ValidationResult](x => JsString(x.toString))
}