package shared

object AppConstants {
  final val CosmosSuccessStatusCode = 200
  final val CosmosInsertionStatusCode = 201
  final val CosmosDeleteStatusCode = 204
  final val GCSBucketName = "mayberry-mini-trucks-inventory-images"
  final val parameterizedId = "@id"
  final val parameterizedDate = "@date"

  /** Contact-form senders blocked before SendGrid. Normalize emails to lowercase; phones to digits only. */
  final val BlockedContactEmails: Set[String] = Set(
    "gramsey1984@icloud.com"
  )
  final val BlockedContactPhones: Set[String] = Set(
    "3044047763"
  )
}
