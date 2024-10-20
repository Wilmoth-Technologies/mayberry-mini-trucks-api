package services

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.{BlobId, BlobInfo, Storage, StorageOptions}

import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path

class CloudStorageService {
  private def getStorage(serviceAccountPath: String): Storage = {
    // Load the credentials from the service account JSON file
    val credentials = ServiceAccountCredentials.fromStream(new FileInputStream(serviceAccountPath))

    // Build the Storage client with the credentials
    val storage: Storage = StorageOptions.newBuilder()
      .setCredentials(credentials)
      .build()
      .getService

    storage
  }

  def uploadImage(bucketName: String, uniqueId: String, fileName: String, filePath: Path): String = {
    val storage = getStorage("/Users/gabewilmoth/Desktop/Mayberry Mini Trucks/mayberry-mini-trucks-api/conf/gcp-service-account.json")
    val blobId = BlobId.of(bucketName, s"$uniqueId/$fileName")
    val blobInfo = BlobInfo.newBuilder(blobId).build()

    val fileBytes = Files.readAllBytes(filePath)
    storage.create(blobInfo, fileBytes)

    s"gs://$bucketName/$uniqueId/$fileName"
  }
}
