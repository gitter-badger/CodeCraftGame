package cwinter.codecraft.core.ai.basicplus

import cwinter.codecraft.core.api.{DroneSpec, MineralCrystal, Drone}
import cwinter.codecraft.util.maths.Vector2

class Mothership extends BaseController('Mothership) {
  val mothership = this
  var defenders = List.empty[Drone]
  var defenderCooldown: Int = 150

  var t = 0
  var minerals = Set.empty[MineralCrystal]
  var claimedMinerals = Set.empty[MineralCrystal]
  private[this] var _lastCapitalShipSighting: Option[Vector2] = None
  def lastCapitalShipSighting: Option[Vector2] = _lastCapitalShipSighting

  val scoutSpec = DroneSpec(storageModules = 1)
  val collectorSpec = DroneSpec(storageModules = 2)
  val hunterSpec = DroneSpec(missileBatteries = 1, engines = 1)
  val destroyerSpec = DroneSpec(missileBatteries = 3, shieldGenerators = 1)
  var searchTokens: Set[SearchToken] = null

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildDrone(scoutSpec, new ScoutingDroneController(this))
    searchTokens = genSearchTokens
  }

  override def onTick(): Unit = {
    t += 1
    defenderCooldown -= 1
    if (!isConstructing) {
      if (DroneCount('Harvester) < 1 ||
        (DroneCount('Hunter) > 0 && DroneCount('Harvester) < 3) ||
        (DroneCount('Destroyer) > 0 && DroneCount('Hunter) > 0 && DroneCount('Harvester) < 4)) {
        buildDrone(collectorSpec, new ScoutingDroneController(this))
      } else if (2 * DroneCount('Hunter) / math.max(DroneCount('Destroyer), 1) < 1) {
        buildDrone(hunterSpec, new Hunter(this))
      } else {
        buildDrone(destroyerSpec, new Destroyer(this))
      }
    }

    handleWeapons()
  }


  def needsDefender: Boolean = {
    if (hitpoints == 0) return false
    val strength = calculateStrength(defenders)
    val enemyStrength = calculateStrength(enemies)
    strength < enemyStrength
  }

  def registerDefender(droneHandle: Drone): Unit = {
    defenders ::= droneHandle
    defenderCooldown = 150
  }

  def allowsDefenderRelease: Boolean =
    defenderCooldown <= 0 && !needsDefender

  def unregisterDefender(droneHandle: Drone): Unit = {
    defenders = defenders.filter(_ != droneHandle)
  }

  def foundCapitalShip(drone: Drone): Unit = {
    _lastCapitalShipSighting = Some(drone.position)
  }

  def findClosestMineral(maxSize: Int, position: Vector2): Option[MineralCrystal] = {
    minerals = minerals.filter(!_.harvested)
    val filtered = minerals.filter(m => m.size <= maxSize && !claimedMinerals.contains(m))
    val result =
      if (filtered.isEmpty) None
      else Some(filtered.minBy(m => (m.position - position).lengthSquared))
    for (m <- result) {
      claimedMinerals += m
    }
    result
  }

  def registerMineral(mineralCrystal: MineralCrystal): Unit = {
    minerals += mineralCrystal
  }

  def abortHarvestingMission(mineralCrystal: MineralCrystal): Unit = {
    claimedMinerals -= mineralCrystal
  }

  def getSearchToken(pos: Vector2): Option[SearchToken] = {
    if (searchTokens.isEmpty) {
      None
    } else {
      val closest = searchTokens.minBy(t => (t.pos - pos).lengthSquared)
      searchTokens -= closest
      Some(closest)
    }
  }

  private def genSearchTokens: Set[SearchToken] = {
    val width = math.ceil(worldSize.width / DroneSpec.SightRadius).toInt
    val height = math.ceil(worldSize.height / DroneSpec.SightRadius).toInt
    val xOffset = (worldSize.width / DroneSpec.SightRadius / 2).toInt
    val yOffset = (worldSize.height / DroneSpec.SightRadius / 2).toInt
    val tokens = Seq.tabulate(width, height){
      (x, y) => SearchToken(x - xOffset, y - yOffset)
    }
    for (ts <- tokens; t <- ts) yield t
  }.toSet


  object DroneCount {
    private[this] var counts = Map.empty[Symbol, Int]

    def apply(name: Symbol): Int = {
      counts.getOrElse(name, 0)
    }

    def increment(name: Symbol): Unit = {
      counts = counts.updated(name, DroneCount(name) + 1)
    }

    def decrement(name: Symbol): Unit = {
      counts = counts.updated(name, DroneCount(name) - 1)
    }
  }
}