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
                    titleInHand: Boolean,
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
                      actions: String = "action",
                    )

case class InventoryOption(@JsonScalaEnumeration(classOf[InventoryOptionsType]) option: InventoryOptions)


