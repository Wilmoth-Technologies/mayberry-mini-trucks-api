package clients

import javax.inject.Inject
import play.api.Logger
import play.api.libs.ws.{WSRequest, WSResponse}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class ApiClientException(inputMessage: String,
                              exceptionMessage: String,
                              exception: Throwable) extends RuntimeException

case object NonSuccessApiResponse extends Exception

class ApiWSClient @Inject(){
  private val logger = Logger(getClass)

  def invokeAPI(wsRequest: WSRequest,
                successfulResponseCodes: List[Int],
                attempt: Int = 1,
                maxAttempts: Int = 5)(implicit ec: ExecutionContext): Future[WSResponse] = {

    logger.debug(s"Attempt $attempt :for api : ${wsRequest}")

    wsRequest.withRequestTimeout(60.seconds).execute().map {
        response =>
          if (successfulResponseCodes.contains(response.status)) {
            logger.debug(s"Response from api: ${wsRequest.url} status: ${response.status} Response text: ${response.body} ")
            response
          } else {
            val exceptionMessage = s"Failed to get success resposne from api: ${wsRequest.url} Response status: ${response.status} Body: ${response.body}"
            logger.error(exceptionMessage)
            throw ApiClientException(s"${wsRequest.url}", exceptionMessage, NonSuccessApiResponse)
          }
      }
      .recoverWith {
        case ex: Exception =>
          if (attempt < maxAttempts) {
            logger.warn(s"Exception occurred in attempt $attempt for api: ${wsRequest.url}. ${ex.getStackTrace.mkString("\n")}")
            invokeAPI(wsRequest, successfulResponseCodes, attempt + 1)
          } else {
            val exceptionMessage = s"All attempts failed to get successful response for api: ${wsRequest.url}, ${ex.getMessage}"
            logger.error(exceptionMessage, ex)
            throw ApiClientException(s"${wsRequest.url}", exceptionMessage, NonSuccessApiResponse)
          }
      }
  }
}