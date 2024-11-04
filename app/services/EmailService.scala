package services

import com.sendgrid.{Method, Request, SendGrid}
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.{Email, Personalization}
import com.typesafe.config.{Config, ConfigFactory}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()()(implicit ec: ExecutionContext) {
  val config: Config = ConfigFactory.load()

  private val apiKey = if (ConfigFactory.load.getBoolean("isSecretManagerSetup")) {
    sys.env("SEND_GRID_API_KEY")
  } else {
    ""
  }

  private val sendGridClient = new SendGrid(apiKey)

  def sendEmail(to: String, templateId: String, dynamicData: Map[String, String]): Future[Unit] = Future {
    val from = new Email("gabewilmoth@gmail.com")
    val toEmail = new Email(to)
    val mail = new Mail()
    mail.setFrom(from)
    mail.setTemplateId(templateId)

    val personalization = new Personalization()
    personalization.addTo(toEmail)

    dynamicData.foreach { case (key, value) =>
      personalization.addDynamicTemplateData(key, value)
    }
    mail.addPersonalization(personalization)

    val request = new Request()
    request.setMethod(Method.POST)
    request.setEndpoint("mail/send")
    request.setBody(mail.build())

    val response = sendGridClient.api(request)
    if (response.getStatusCode >= 400) {
      throw new Exception(s"Failed to send email: ${response.getBody}")
    }
  }

  def removeEmailFromGlobalUnsubscribe(email: String): Future[Unit] = Future {
    val request = new Request()
    request.setMethod(Method.DELETE)
    request.setEndpoint(s"asm/suppressions/global/$email")

    val response = sendGridClient.api(request)
    if (response.getStatusCode >= 400) {
      throw new Exception(s"Failed to remove from unsubscribe list: ${response.getBody}")
    }
  }

  def isEmailOnGlobalUnsubscribe(email: String): Future[Boolean] = Future[Boolean] {
    val request = new Request()
    request.setMethod(Method.GET)
    request.setEndpoint(s"/asm/suppressions/global/$email")
    val response = sendGridClient.api(request)
    response.getStatusCode == 200  // 200 if on list, 404 if not on list
  }
}