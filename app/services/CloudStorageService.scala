package services

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.{BlobId, BlobInfo, Bucket, Storage, StorageOptions}
import com.typesafe.config.{Config, ConfigFactory}
import play.api.Logger
import shared.AppConstants.GCSBucketName
import shared.AppFunctions.toJson

import java.io.FileInputStream
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

class CloudStorageService {
  val logger: Logger = Logger(this.getClass)
  val config: Config = ConfigFactory.load()

  private def getStorage: Storage = {
    // Load the credentials from the service account JSON file
    val credentials = ServiceAccountCredentials.fromStream(new FileInputStream(config.getString("accountJsonPath")))

    // Build the Storage client with the credentials
    val storage: Storage = StorageOptions.newBuilder()
      .setCredentials(credentials)
      .build()
      .getService

    storage
  }

  def uploadImage(uniqueId: String, fileName: String, filePath: Path): String = {
    val storage = getStorage
    val blobId = BlobId.of(GCSBucketName, s"$uniqueId/$fileName")
    val blobInfo = BlobInfo.newBuilder(blobId).build()

    val fileBytes = Files.readAllBytes(filePath)
    storage.create(blobInfo, fileBytes)

    s"https://storage.googleapis.com/$GCSBucketName/$uniqueId/$fileName"
  }

  // Function to check if a blob exists
  def blobExists(blobName: String): Boolean = {
    val storage = getStorage
    // List objects in the bucket with the folder prefix
    val blobs = storage.list(GCSBucketName, Storage.BlobListOption.prefix(blobName))
    blobs.getValues.asScala.nonEmpty
  }

  // Function to delete all objects in a blob
  def deleteBlob(blobName: String): Unit = {
    val storage = getStorage
    val blobs = storage.list(GCSBucketName, Storage.BlobListOption.prefix(blobName))

    blobs.getValues.asScala.foreach { blob =>
      logger.info(s"Deleting: ${blob.getName}")
      blob.delete()
    }

    logger.info(s"Blob '$blobName' deleted from bucket '$GCSBucketName'.")
  }

  def getBucketContents(blobName: String): String = {
    val storage = getStorage
    val bucket: Bucket = storage.get(GCSBucketName)
    val blobs = bucket.list(Storage.BlobListOption.prefix(s"$blobName/")).iterateAll().asScala

    // For each image, retrieve the binary data and construct the URL
    val imageData = blobs.map { blob =>
      val inputStream = Channels.newInputStream(blob.reader())
      val binaryData = Stream.continually(inputStream.read()).takeWhile(_ != -1).map(_.toByte).toArray

      // Return image metadata, binary data, and URL
      Map(
        "name" -> blob.getName,
        "url" -> s"https://storage.googleapis.com/$GCSBucketName/${blob.getName}",
        "contentType" -> blob.getContentType,
        "binaryData" -> binaryData.map(_.toString) // Converting binary data to string for JSON
      )
    }

    toJson(imageData)
  }
}
