package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.util.maths.{Geometry, Vector2}
import cwinter.codecraft.util.modules.ModulePosition
import GameConstants.{ModuleResourceCost, DroneConstructionTime}

import scala.scalajs.js.annotation.JSExportAll

/**
 * Specifies the modules equipped by a drone and computes various properties of a Drone with this
 * configuration of modules.
 *
 * Currently, the total number of modules is currently limited to 10 but this restriction will likely be
 * lifted in the future.
 *
 * @param storageModules Number of storage modules. Allows for storage of mineral crystals and energy globes.
 * @param missileBatteries Number of missile batteries. Allows for firing homing missiles.
 * @param constructors Number of constructors. Allows for constructing new drones and moving minerals from/to other drones.
 * @param engines Number of engines. Increases move speed.
 * @param shieldGenerators Number of shield generators. Gives the drone an additional 7 hitpoints each. Shields regenerate over time.
 */
@JSExportAll
case class DroneSpec(
  storageModules: Int = 0,
  missileBatteries: Int = 0,
  constructors: Int = 0,
  engines: Int = 0,
  shieldGenerators: Int = 0
) {
  require(storageModules >= 0)
  require(missileBatteries >= 0)
  require(constructors >= 0)
  require(engines >= 0)
  require(shieldGenerators >= 0)

  def this() = this(0)

  /** Total number of modules. */
  val moduleCount =
    storageModules + missileBatteries + constructors + engines + shieldGenerators

  require(moduleCount <= ModulePosition.MaxModules, s"A drone cannot have more than ${ModulePosition.MaxModules} modules")

  /** The number of sides that the drone will have.
    * E.g. a drone with two modules will be rectangular shaped and therefore has 4 sides.
    */
  val sides = ModulePosition.size(moduleCount)

  /** Returns the amount of hitpoints that a drone with this spec will have when it is at full health. */
  def maxHitpoints: Int = 2 * (sides - 1) + shieldGenerators * 7

  /** Returns the amount of resources it will cost to build a drone with this spec. */
  def resourceCost: Int = moduleCount * ModuleResourceCost

  /** Returns the number of timesteps it will take to build a drone of this size.
    * This time will be reduced if the constructing drone has more than one constructor module,
    * e.g. with two constructor modules it will take half as long.
    */
  def buildTime: Int = DroneConstructionTime * resourceCost

  /** Returns the weight of a drone with this spec.
    * Weight increases with sides and module count and a higher weight leads to a slower movement speed.
    */
  def weight = sides + moduleCount

  /** Returns the speed of a drone with this spec, measured in units distance per timestep. */
  def maxSpeed: Double = 30 * (1 + engines) / weight


  /** Returns the `radius` for a drone with this spec.
    * The `radius` is used to compute collisions with projectiles or other drones.
    */
  val radius: Double = {
    val radiusBody = 0.5f * 40 / math.sin(math.Pi / sides).toFloat
    radiusBody + 0.5f * Geometry.circumradius(4, sides)
  }

  /** Returns a copy of this object with `storageModules` set to the specified value. */
  def withStorageModules(storageModules: Int) = copy(storageModules = storageModules)

  /** Returns a copy of this object with `missileBatteries` set to the specified value. */
  def withMissileBatteries(missileBatteries: Int) = copy(missileBatteries = missileBatteries)

  /** Returns a copy of this object with `constructors` set to the specified value. */
  def withConstructors(constructors: Int) = copy(constructors = constructors)

  /** Returns a copy of this object with `engines` set to the specified value. */
  def withEngines(engines: Int) = copy(engines = engines)

  /** Returns a copy of this object with `shieldGenerators` set to the specified value. */
  def withShieldGenerators(shieldGenerators: Int) = copy(shieldGenerators = shieldGenerators)

  private[core] def constructDynamics(owner: DroneImpl, initialPos: Vector2, time: Double): DroneDynamics =
    if (owner.context.isLocallyComputed) new ComputedDroneDynamics(owner, maxSpeed, weight, radius, initialPos, time)
    else new RemoteDroneDynamics(initialPos)

  private[core] def constructStorage(owner: DroneImpl, startingResources: Int = 0): Option[DroneStorageModule] =
    if (storageModules > 0) Some(
      new DroneStorageModule(0 until storageModules, owner, startingResources)
    )
    else None

  private[core] def constructMissilesBatteries(owner: DroneImpl): Option[DroneMissileBatteryModule] =
    if (missileBatteries > 0) Some(
      new DroneMissileBatteryModule(storageModules until (storageModules + missileBatteries), owner)
    )
    else None

  private[core] def constructManipulatorModules(owner: DroneImpl): Option[DroneConstructorModule] =
    if (constructors > 0) {
      val startIndex = storageModules + missileBatteries
      Some(new DroneConstructorModule(startIndex until startIndex + constructors, owner))
    } else None

  private[core] def constructEngineModules(owner: DroneImpl): Option[DroneEnginesModule] =
    if (engines > 0) {
      val startIndex = storageModules + missileBatteries + constructors
      Some(new DroneEnginesModule(startIndex until startIndex + engines, owner))
    } else None

  private[core] def constructShieldGenerators(owner: DroneImpl): Option[DroneShieldGeneratorModule] =
    if (shieldGenerators > 0) {
      val startIndex = storageModules + missileBatteries + constructors + engines
      Some(new DroneShieldGeneratorModule(startIndex until startIndex + shieldGenerators, owner))
    } else None
}

