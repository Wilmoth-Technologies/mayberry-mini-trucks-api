package filters

import akka.stream.Materializer
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.{JsArray, JsValue, Json}

import java.net.URL
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import javax.inject.Inject
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.io.Source

class AuthFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

//  private val routeToRequiredScopes: Map[String, Set[String]] = Map(
//    "/management/getInventoryPhotos" -> Set("manage:inventory"),
//    "/management/getAllVin" -> Set("manage:inventory"),
//    "/management/getAllInventory" -> Set("manage:inventory"),
//    "/management/getInventoryItem" -> Set("manage:inventory"),
//    "/management/addInventory" -> Set("manage:inventory"),
//    "/management/updateInventory" -> Set("manage:inventory"),
//    "/management/deleteInventory" -> Set("manage:inventory"),
//  )

  private val routeToRequiredScopes: Map[String, Set[String]] = Map(
    "/api/test" -> Set("manage:inventory"),
  )

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

  //TODO: Extract KID from the JWT Header
  //TODO: Cache Public Key to ensure we do not have to make this call on EVERY auth request. Note: This should refresh periodically
  //TODO: Refactor some of this into a helper/util component
  override def apply(nextFilter: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {

    // Extract the authorization header from the request
    request.headers.get("Authorization") match {
      case Some(token) if token.startsWith("Bearer ") =>
        val accessToken = token.stripPrefix("Bearer ")

        // Usage
        val jwksUrl = "https://dev-kss71gvvwi5vchr2.us.auth0.com/.well-known/jwks.json"
        val jwks = fetchJWKS(jwksUrl)
        val jwtKid = "0nyeBldgkORp8NQGPDboQ" //TODO: Extract this from the JWT header
        val publicKey: Option[RSAPublicKey] = getRSAPublicKey(jwks, jwtKid)

        if (publicKey.isDefined) {
          validateTokenAndCheckPermissions(accessToken, request, publicKey.get) match {
            case Right(_) =>
              // If the token is valid and the user has the required permissions, continue
              nextFilter(request)

            case Left(errorMessage) =>
              // If validation fails, return 403 Forbidden or 401 Unauthorized
              Future.successful(Forbidden(s"Access denied: $errorMessage"))
          }
        } else {
          Future.successful(InternalServerError(s"Unable to Fetch Public Key for Authentication"))
        }

      case _ =>
        // If no Authorization header and the path requires auth or the token is malformed, return 401 Unauthorized.
        // Otherwise, no auth is required and let the request pass through
        if (routeToRequiredScopes.getOrElse(request.path, Set.empty).nonEmpty) {
          Future.successful(Unauthorized("No valid access token provided"))
        } else {
          nextFilter(request)
        }
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

      // Get the required scopes for the route being accessed
      val requiredScopes: Set[String] = routeToRequiredScopes.getOrElse(request.path, Set.empty)

      // Check if the user has the required scopes
      if (userScopes.isDefined && requiredScopes.subsetOf(userScopes.get.toSet)) {
        Right(()) // Token is valid, and user has required permissions
      } else {
        Left(s"Insufficient permissions. Required: ${requiredScopes.mkString(", ")}")
      }

    } catch {
      case _: JWTVerificationException =>
        Left("Invalid access token")
      case _: Exception =>
        Left("An error occurred while verifying the token")
    }
  }
}
