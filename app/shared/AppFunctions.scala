package shared

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode}
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results.{NotFound, _}

import java.sql.Timestamp

object AppFunctions {
  val objectMapper: JsonMapper with ClassTagExtensions = JsonMapper.builder().addModule(DefaultScalaModule).build() :: ClassTagExtensions
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  objectMapper.setSerializationInclusion(Include.NON_EMPTY)

  private val DateTimestampFormat: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  case class ValidationResult(isValid: Boolean, resultText: String)

  def toJson[T](obj: T): String = objectMapper.writeValueAsString(obj)

  def requestToObject[T](inRequest: Request[AnyContent])(implicit m: Manifest[T]): T = objectMapper
    .readValue[T](inRequest.body.asJson.get.toString)

  def jsonToObject[T](inJson: JsonNode)(implicit m: Manifest[T]): T = objectMapper.treeToValue[T](inJson)

  def jsonToObject[T](inJson: String)(implicit m: Manifest[T]): T = objectMapper.readValue[T](inJson)

  def multipartRequestToObject[T](body: Option[String])(implicit m: Manifest[T]): T = objectMapper
    .readValue[T](body.get)

  def listToJson[T](responseList: List[T]): Result = {
    if (responseList.nonEmpty)
      Ok(toJson(responseList))
    else
      NotFound("Data Not Found")
  }

  def listToJsonAllowEmpty[T](responseList: List[T]): Result = {
    Ok(toJson(responseList))
  }

  def currentDateTimeInTimeStamp: Timestamp = Timestamp.valueOf(DateTimestampFormat.print(DateTime.now))

  def toSha256(message: String): String =
    String.format("%064x", new java.math.BigInteger(1,
      java.security.MessageDigest.getInstance("SHA-256").digest(message.getBytes("UTF-8"))))
}
