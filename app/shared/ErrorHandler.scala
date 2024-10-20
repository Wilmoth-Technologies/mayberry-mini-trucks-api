package shared

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import models.ErrorResponse

import javax.inject.Singleton
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import shared.exceptions.{ApiException, CosmosException}

import java.security.SecureRandom
import scala.concurrent.Future

@Singleton
class ErrorHandler extends HttpErrorHandler {
  val logger = Logger(this.getClass)
  val errorIdChars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val errorId = generateErrorId
    logger.error(s"Error $errorId: $message. Request: $request")
    Future.successful(
      Status(statusCode)(Json.toJson(
        ErrorResponse(
          errorId,
          message = message,
          info = None
        )
      ))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val errorId = generateErrorId
    logger.error(s"Error $errorId: Request: $request ; ${exception.getMessage}", exception)
    Future.successful(
      exception match {
        case e: CosmosException => Status(e.status)(Json.toJson(
          ErrorResponse(
            errorId,
            message = s"A CosmosDB Exception occurred: ${if (e.ex == null) "Undefined Exception Cause" else e.ex.getClass.getName}",
            info = if (e.ex == null) None else Some(e.ex.getMessage)
          )
        ))
        case e: ApiException => Status(e.status)(Json.toJson(
          ErrorResponse(
            errorId,
            message = e.message,
            info = (e.info)
          )
        ))
        case e: MismatchedInputException =>
          BadRequest(Json.toJson(
            ErrorResponse(
              errorId,
              message = "Mismatched input",
              info = Some(e.getMessage)
            )
          ))
        case _ =>
          InternalServerError(Json.toJson(
            ErrorResponse(
              errorId,
              message = "A server error occurred: " + exception.getClass.getName,
              info = Some(exception.getMessage)
            )
          ))
      }
    )
  }

  private def generateErrorId: String = {
    val builder = new StringBuilder
    val secureRandom = new SecureRandom
    for (_ <- 1 to 20)
      builder.append(errorIdChars(secureRandom.nextInt(errorIdChars.length)))
    builder.mkString
  }
}