package services

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.{BlobId, BlobInfo, Storage, StorageOptions}
import play.api.Logger
import shared.AppConstants.GCSBucketName

import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

class CloudStorageService {
  val logger: Logger = Logger(this.getClass)

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

  def uploadImage(uniqueId: String, fileName: String, filePath: Path): String = {
    val storage = getStorage("/Users/gabewilmoth/Desktop/Mayberry Mini Trucks/mayberry-mini-trucks-api/conf/gcp-service-account.json")
    val blobId = BlobId.of(GCSBucketName, s"$uniqueId/$fileName")
    val blobInfo = BlobInfo.newBuilder(blobId).build()

    val fileBytes = Files.readAllBytes(filePath)
    storage.create(blobInfo, fileBytes)

    s"https://storage.googleapis.com/$GCSBucketName/$uniqueId/$fileName"
  }

  // Function to check if a blob exists
  def blobExists(blobName: String): Boolean = {
    val storage = getStorage("/Users/gabewilmoth/Desktop/Mayberry Mini Trucks/mayberry-mini-trucks-api/conf/gcp-service-account.json")
    // List objects in the bucket with the folder prefix
    val blobs = storage.list(GCSBucketName, Storage.BlobListOption.prefix(blobName))
    blobs.getValues.asScala.nonEmpty
  }

  // Function to delete all objects in a blob
  def deleteBlob(blobName: String): Unit = {
    val storage = getStorage("/Users/gabewilmoth/Desktop/Mayberry Mini Trucks/mayberry-mini-trucks-api/conf/gcp-service-account.json")
    val blobs = storage.list(GCSBucketName, Storage.BlobListOption.prefix(blobName))

    blobs.getValues.asScala.foreach { blob =>
      logger.info(s"Deleting: ${blob.getName}")
      blob.delete()
    }

    logger.info(s"Blob '$blobName' deleted from bucket '$GCSBucketName'.")
  }
}
