package shared

import com.google.cloud.secretmanager.v1.{SecretManagerServiceClient, AccessSecretVersionRequest}

class SecretManager {
  def accessSecret(secretId: String, projectId: String): String = {
    // Initialize the client
    val client = SecretManagerServiceClient.create()

    // Construct the request to access the latest version of the secret
    val secretVersionName = s"projects/$projectId/secrets/$secretId/versions/latest"
    val request = AccessSecretVersionRequest.newBuilder().setName(secretVersionName).build()

    // Access the secret
    val response = client.accessSecretVersion(request)

    // Extract the secret payload
    val secretPayload = response.getPayload.getData.toStringUtf8

    client.close() // Always close the client after use
    secretPayload
  }
}
