package models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import models.InventoryOptions.InventoryOptions

import java.sql.Timestamp

case class Inventory(
                    id: String,
                    vin: String,
                    shipmentNumber: String,
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
                    options: List[InventoryOption],
                    imageLinks: List[String],
                    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    updatedTimeStamp: Timestamp,
                    updatedBy: String,
                    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    creationTimeStamp: Timestamp,
                    )

case class InventoryOption(@JsonScalaEnumeration(classOf[InventoryOptionsType]) option: InventoryOptions)


