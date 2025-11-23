package models

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude}
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.`type`.TypeReference

class InventoryOptionsType extends TypeReference[InventoryOptions.type]

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_ABSENT, content = Include.NON_EMPTY)
object InventoryOptions extends Enumeration {
  type InventoryOptions = Value


  val twoWheelDrive: InventoryOptions.Value = Value("2WD")
  val fourWheelDrive: InventoryOptions.Value = Value("4WD")
  val allWheelDrive: InventoryOptions.Value = Value("AWD")
  val airbags: InventoryOptions.Value = Value("Airbags")
  val airConditioning: InventoryOptions.Value = Value("A/C")
  val crane: InventoryOptions.Value = Value("Crane")
  val diesel: InventoryOptions.Value = Value("Diesel")
  val diffLock: InventoryOptions.Value = Value("Diff Lock")
  val dump: InventoryOptions.Value = Value("Dump")
  val exLowFirst: InventoryOptions.Value = Value("Ex Low 1")
  val fireTruck: InventoryOptions.Value = Value("Fire Truck")
  val fuelInjected: InventoryOptions.Value = Value("Fuel Injected")
  val hiLow: InventoryOptions.Value = Value("Hi/Low")
  val powerLocks: InventoryOptions.Value = Value("Power Locks")
  val powerMirrors: InventoryOptions.Value = Value("Power Mirrors")
  val powerSteering: InventoryOptions.Value = Value("Power Steering")
  val powerWindows: InventoryOptions.Value = Value("Power Windows")
  val scissorLift: InventoryOptions.Value = Value("Scissor Lift")
  val supercharged: InventoryOptions.Value = Value("Supercharged")
  val turbo: InventoryOptions.Value = Value("Turbo")
  val ultraLowFirst: InventoryOptions.Value = Value("Ultra Low 1")
  val ultraLowReverse: InventoryOptions.Value = Value("Ultra Low Reverse")
  val van: InventoryOptions.Value = Value("Van")
  val vanWithDeck: InventoryOptions.Value = Value("Van w Deck")
  val attack: InventoryOptions.Value = Value("Attack")
  val jumbo: InventoryOptions.Value = Value("Jumbo")
}