package shared

import akka.actor.ActorSystem
import akka.pattern.after
import com.azure.cosmos.implementation.ConflictException
import models.ApplicationException

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object RunWithRetry {

  def retry[T](function: => Future[T], attempts: Int, delay: FiniteDuration)(
    implicit ec: ExecutionContext,
    system: ActorSystem): Future[T] = {

    if (attempts > 0) {
      function.recoverWith {
        case ex: ConflictException => throw ex

        case _: ApplicationException =>
          after(delay, system.scheduler) {
            retry(function, attempts - 1, delay)
          }
      }
    } else {
      function
    }
  }
}