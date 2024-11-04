package models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import models.InventoryOptions.InventoryOptions
import models.Status.Status

import java.sql.Timestamp

case class Inventory(
                    id: String,
                    vin: String,
                    modelCode: String,
                    stockNumber: String,
                    make: String,
                    model: String,
                    year: String,
                    exteriorColor: String,
                    interiorColor: String,
                    mileage: Int,
                    transmission: String,
                    engine: String,
                    purchaseDate: String,
                    description: String,
                    price: BigDecimal,
                    titleInHand: Boolean = false,
                    @JsonScalaEnumeration(classOf[StatusType])
                    status: Status,
                    embeddedVideoLink: String,
                    options: List[InventoryOption],
                    imageLinks: List[String],
                    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    updatedTimeStamp: Timestamp,
                    updatedBy: String,
                    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    creationTimeStamp: Timestamp,
                    )

case class InventoryTable(
                      vin: String,
                      modelCode: String,
                      stockNumber: String,
                      purchaseDate: String,
                      make: String,
                      model: String,
                      year: String,
                      mileage: Int,
                      price: BigDecimal,
                      @JsonScalaEnumeration(classOf[StatusType])
                      status: Status,
                      titleInHand: String,
                      actions: String = "action",
                    )

case class InventoryPaginationData(
                              vin: String,
                              modelCode: String,
                              stockNumber: String,
                              make: String,
                              model: String,
                              year: String,
                              exteriorColor: String,
                              interiorColor: String,
                              mileage: Int,
                              transmission: String,
                              engine: String,
                              description: String,
                              price: BigDecimal,
                              titleInHand: Boolean,
                              @JsonScalaEnumeration(classOf[StatusType])
                              status: Status,
                              options: List[InventoryOption],
                              imageLinks: String,
                            )

case class InventoryLandingScroller(
                                    vin: String,
                                    make: String,
                                    model: String,
                                    year: String,
                                    price: BigDecimal,
                                    mileage: Int,
                                    imageLinks: String,
                                  )

case class InventoryOption(@JsonScalaEnumeration(classOf[InventoryOptionsType]) option: InventoryOptions)

case class Subscribers(
                      id: String,
                      firstName: String,
                      lastName: String,
                      phoneNumber: String,
                      email: String,
                      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                      subscribeDate: Timestamp
                      )

case class Notification(
                         id: String,
                         startDate: String,
                         endDate: String,
                         description: String,
                         actions: String = "action",
                       )

case class ContactRequest(
                         firstName: String,
                         lastName: String,
                         email: String,
                         phoneNumber: String,
                         description: String,
                         vin: String = "",
                         isFailedFilter: Boolean = false
                         )


case class InventoryDetailsForTemplate(
                                      photoUrl: String,
                                      year: String,
                                      make: String,
                                      model: String,
                                      price: String,
                                      mileage: String,
                                      engine: String,
                                      transmission: String,
                                      color: String,
                                      itemUrl: String
                                      )