package cwinter.codecraft.core.objects

import cwinter.codecraft.core._
import cwinter.codecraft.core.api.{GameConstants, Player}
import cwinter.codecraft.core.objects.drone.DroneImpl
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.Vector2
import GameConstants.{MissileLifetime, MissileSpeed}

private[core] class HomingMissile(
  val player: Player,
  initialPos: Vector2,
  val id: Int,
  time: Double,
  target: DroneImpl
) extends WorldObject {
  val dynamics: MissileDynamics =
    new MissileDynamics(MissileSpeed, target.dynamics, player.id, this, initialPos, time)
  val previousPositions = collection.mutable.Queue(initialPos)
  val positions = 7
  var lifetime = MissileLifetime
  var fading: Boolean = false

  def update(): Seq[SimulatorEvent] = {
    if (fading) {
      previousPositions.dequeue()
      if (previousPositions.isEmpty) {
        Seq(HomingMissileFaded(this))
      } else {
        Seq.empty[SimulatorEvent]
      }
    } else {
      dynamics.update()

      lifetime -= 1

      previousPositions.enqueue(position)
      if (previousPositions.length > positions) previousPositions.dequeue()
      while (previousPositions.length > lifetime + 1) previousPositions.dequeue()

      if (dynamics.removed) {
        fading = true
        Seq(MissileExplodes(this))
      } else if (lifetime == 0) {
        dynamics.remove()
        Seq(HomingMissileFaded(this))
      } else Seq.empty[SimulatorEvent]
    }
  }

  override def position: Vector2 = dynamics.pos
  override private[core] def descriptor: Seq[ModelDescriptor[_]] = Seq(
    ModelDescriptor(
      NullPositionDescriptor,
      HomingMissileDescriptor(
        previousPositions.map{case Vector2(x, y) => (x.toFloat, y.toFloat)},
        math.min(MissileLifetime - lifetime, positions), player.color)
    )
  )

  override private[core] def isDead = lifetime <= 0
}

