package models

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude}
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.`type`.TypeReference

class StatusType extends TypeReference[Status.type]

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_ABSENT, content = Include.NON_EMPTY)
object Status extends Enumeration {
  type Status = Value

  val inStock: Status.Value = Value("In Stock")
  val pendingSale: Status.Value = Value("Pending Sale")
  val outOfService: Status.Value = Value("Out of Service")
  val sold: Status.Value = Value("Sold")
}