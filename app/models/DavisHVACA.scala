package models

case class DavisScheduleService(
                           name: String,
                           email: String,
                           phoneNumber: String,
                           serviceType: String,
                           preferredDate: String,
                           time: String,
                           address: String,
                           details: String
                         )

case class DavisContactRequest(
                                name: String,
                                email: String,
                                phoneNumber: String,
                                subject: String,
                                description: String
                              )