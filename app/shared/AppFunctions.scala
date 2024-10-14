package shared

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}

object AppFunctions {
  val objectMapper: JsonMapper with ClassTagExtensions = JsonMapper.builder().addModule(DefaultScalaModule).build() :: ClassTagExtensions
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  objectMapper.setSerializationInclusion(Include.NON_EMPTY)

  def toJson[T](obj: T): String = objectMapper.writeValueAsString(obj)
}
