package services

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.{BlobId, BlobInfo, Bucket, Storage, StorageOptions}
import com.typesafe.config.{Config, ConfigFactory}
import org.imgscalr.Scalr
import play.api.Logger
import shared.AppConstants.GCSBucketName
import shared.AppFunctions.toJson

import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.nio.channels.Channels
import java.nio.file.Path
import javax.imageio.stream.ImageOutputStream
import javax.imageio.{ImageIO, ImageWriter}
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

    if (filePath.toFile.exists() && filePath.toFile.length() > 0) {
      // Load the image file
      val originalImage: BufferedImage = ImageIO.read(filePath.toFile)

      if (originalImage != null) {
        // Compress and resize the image
        val imageWithoutExif = stripExifData(originalImage, filePath.toFile)
        val compressedImage: BufferedImage = Scalr.resize(imageWithoutExif, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_EXACT, 800, 600)

        // Convert to byte array
        val byteArrayOutputStream = new ByteArrayOutputStream()
        ImageIO.write(compressedImage, "jpg", byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray
        storage.create(blobInfo, imageBytes)
        s"https://storage.googleapis.com/$GCSBucketName/$uniqueId/$fileName"
      } else {
        ""
      }
    } else {
      ""
    }
  }

  // Function to delete all objects in a blob
  def deleteBlob(blobName: String): Unit = {
    val storage = getStorage
    val blobs = storage.list(GCSBucketName, Storage.BlobListOption.prefix(blobName))

    if(blobs.getValues.asScala.nonEmpty) {
      blobs.getValues.asScala.foreach { blob =>
        if(blob.getName.split("/").head.equals(blobName)) {
          println(s"Deleting: ${blob.getName}")
          blob.delete()
          println(s"Blob '$blobName' deleted from bucket '$GCSBucketName'.")
        }
        println(s"Blob: ${blob.getName} does not match the requested to delete Blob of: $blobName")
      }
    }
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

  def stripExifData(image: BufferedImage, originalFile: File): BufferedImage = {
    val writer: ImageWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
    val ios: ImageOutputStream = ImageIO.createImageOutputStream(originalFile)
    writer.setOutput(ios)

    val imageType = image.getType
    writer.write(null, new javax.imageio.IIOImage(image, null, null), null)
    ios.close()

    // Read the stripped image back from the original file
    ImageIO.read(originalFile)
  }

}
