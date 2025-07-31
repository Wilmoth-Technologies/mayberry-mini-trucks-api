package models

case class WTSContactRequest(
                           firstName: String,
                           lastName: String,
                           email: String,
                           phoneNumber: String,
                           description: String
                         )