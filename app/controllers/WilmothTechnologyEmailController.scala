package controllers

import models.{DavisContactRequest, DavisScheduleService, WTSContactRequest}
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

  val davisHVACemail: String = "info@staycoolstaycozy.com"

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


  def contactUsDavisHVAC: Action[AnyContent] =
    Action.async {
      implicit request =>
        val contactRequest = requestToObject[DavisContactRequest](request)
        println(s"Sending davis HVAC contact request for: ${contactRequest.email}")

        val requestMap = Map(
          "name" -> contactRequest.name,
          "email" -> contactRequest.email,
          "phoneNumber" -> contactRequest.phoneNumber,
          "subject" -> contactRequest.subject,
          "description" -> contactRequest.description,
        )
        for {
          _ <- emailService.sendEmailWTS(davisHVACemail, davisHVACemail, "d-c219dce72f824fbe81a73fb1bea75b44", requestMap)
        } yield Created

    }

  def scheduleApptDavisHVAC: Action[AnyContent] =
    Action.async {
      implicit request =>
        val contactRequest = requestToObject[DavisScheduleService](request)
        println(s"Sending schedule appt request for: ${contactRequest.email}")

        val requestMap = Map(
          "name" -> contactRequest.name,
          "email" -> contactRequest.email,
          "phoneNumber" -> contactRequest.phoneNumber,
          "serviceType" -> contactRequest.serviceType,
          "preferredDate" -> contactRequest.preferredDate,
          "time" -> contactRequest.time,
          "address" -> contactRequest.address,
          "details" -> contactRequest.details
        )
        for {
          _ <- emailService.sendEmailWTS(davisHVACemail, davisHVACemail, "d-a64cb9621176442e84b7215ba889cfed", requestMap)
        } yield Created

    }
}