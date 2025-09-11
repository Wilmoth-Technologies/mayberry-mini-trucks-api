package controllers

import models.WTSContactRequest
import play.api.Logger
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.EmailService
import shared.AppFunctions.requestToObject

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class WilmothTechnologyEmailController @Inject()(cc: ControllerComponents,
                                                 emailService: EmailService)
                                                (implicit ec: ExecutionContext) extends AbstractController(cc) {
  val logger: Logger = Logger(this.getClass)

  def contactUs: Action[AnyContent] =
    Action.async {
      implicit request =>
        val contactRequest = requestToObject[WTSContactRequest](request)
        println(s"Sending contact request for: ${contactRequest.email}")

        val subject: String = s"Contact Request from ${contactRequest.firstName} ${contactRequest.lastName}"

        val requestMap = Map(
          "first_name" -> contactRequest.firstName,
          "last_name" -> contactRequest.lastName,
          "description" -> contactRequest.description,
          "phone_number" -> contactRequest.phoneNumber,
          "email" -> contactRequest.email,
          "subject" -> subject
        )
        for {
          _ <- emailService.sendEmailWTS("info@wilmothtechnologyservices.com", "info@wilmothtechnologyservices.com", "d-b124c3df45234d0c9545d2e9f7e62fbd", requestMap)
        } yield Created

    }
}