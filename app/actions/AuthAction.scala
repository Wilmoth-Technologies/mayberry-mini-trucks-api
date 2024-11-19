package actions

import play.api.mvc._
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.Results.{Forbidden, InternalServerError}

import java.net.URL
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.io.Source

class AuthAction @Inject()(parser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) {

  // Function to fetch JWKS from Auth0
  private def fetchJWKS(jwksUrl: String): JsValue = {
    val response = Source.fromURL(new URL(jwksUrl)).mkString
    Json.parse(response)
  }

  // Function to extract the RSA public key from JWKS based on the `kid`
  private def getRSAPublicKey(jwks: JsValue, kid: String): Option[RSAPublicKey] = {
    // Find the key with the matching `kid`
    (jwks \ "keys").as[JsArray].value.find(key => (key \ "kid").as[String] == kid).flatMap { key =>
      val n = (key \ "n").as[String] // Modulus
      val e = (key \ "e").as[String] // Exponent

      try {
        // Decode the Base64 URL-encoded strings
        val modulusBytes = Base64.getUrlDecoder.decode(n)
        val exponentBytes = Base64.getUrlDecoder.decode(e)

        // Create the RSA public key spec
        val spec = new RSAPublicKeySpec(new java.math.BigInteger(1, modulusBytes), new java.math.BigInteger(1, exponentBytes))
        val keyFactory = KeyFactory.getInstance("RSA")

        // Generate the public key from the spec
        Some(keyFactory.generatePublic(spec).asInstanceOf[RSAPublicKey])
      } catch {
        case _: Exception => None
      }
    }
  }

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    // Extract token from Authorization header
    request.headers.get("Authorization").flatMap { authHeader =>
      authHeader.split(" ").lastOption // Expecting "Bearer <token>"
    } match {
      case Some(token) =>
        try {
          if (token.equals(sys.env("SCHEDULER_API_KEY"))) {
            block(request)
          } else {
            val jwksUrl = "https://dev-kss71gvvwi5vchr2.us.auth0.com/.well-known/jwks.json"
            val jwks = fetchJWKS(jwksUrl)
            val jwtKid = "0nyeBldgkORp8NQGPDboQ" //TODO: Extract this from the JWT header
            val publicKey: Option[RSAPublicKey] = getRSAPublicKey(jwks, jwtKid)

            if (publicKey.isDefined) {
              validateTokenAndCheckPermissions(token, request, publicKey.get) match {
                case Right(_) =>
                  // If the token is valid and the user has the required permissions, continue
                  block(request)
                case Left(errorMessage) =>
                  // If validation fails, return 403 Forbidden or 401 Unauthorized
                  Future.successful(Forbidden(s"Access denied: $errorMessage"))
              }
            } else {
              Future.successful(InternalServerError(s"Unable to Fetch Public Key for Authentication"))
            }
          }
        } catch {
          case _: Exception => Future.successful(Results.Unauthorized("Invalid token"))
        }
      case None => Future.successful(Results.Unauthorized("Missing Required Token"))
    }
  }

  // Validate the JWT token and check for required scopes
  private def validateTokenAndCheckPermissions(token: String, request: RequestHeader, publicKey: RSAPublicKey): Either[String, Unit] = {
    try {
      // Decode and verify the JWT
      val algorithm = Algorithm.RSA256(publicKey)
      val verifier = JWT.require(algorithm).build()
      val decodedJWT = verifier.verify(token)

      // Extract the user's permissions from the token
      val claim = decodedJWT.getClaim("permissions")
      val userScopes = if (!claim.isNull) {
        Some(claim.asList(classOf[String]).asScala.toList)
      } else {
        None
      }

      // Check if the user has the required scopes
      if (userScopes.isDefined && userScopes.get.toSet.contains("manage:inventory")) {
        Right(()) // Token is valid, and user has required permissions
      } else {
        Left("Insufficient permissions. Required: manage:inventory")
      }

    } catch {
      case _: JWTVerificationException =>
        Left("Invalid access token")
      case _: Exception =>
        Left("An error occurred while verifying the token")
    }
  }
}
