package cwinter.codecraft.demos.graphics

import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.{Float0To1, ColorRGB}

import scala.collection.mutable
import scala.util.Random


private[graphics] class MockDrone(
  var xPos: Float,
  var yPos: Float,
  var orientation: Float,
  val size: Int,
  var modules: Seq[DroneModuleDescriptor],
  val undamaged: Boolean = false,
  val dontMove: Boolean = false
) extends MockObject {
  private[this] var targetOrientation = orientation
  private[this] var stationary = false
  private[this] val hullState =
    if (rnd() < 0.8 || undamaged) Seq.fill(size - 1)(2.toByte)
    else Seq.fill(size - 1)(Random.nextInt(3).toByte)


  val speed = 2f
  val turnSpeed = 0.01f

  val oldPositions = mutable.Queue.empty[(Float, Float, Float)]
  val nPos = 12


  override def update(): Unit = {
    if (!dontMove) handleMovement()
  }

  private def handleMovement(): Unit = {
    // randomly choose new target orientation
    if (rnd() < 0.003) {
      val LimitX = 1500
      val LimitY = 1000
      if (xPos > LimitX) {
        targetOrientation = math.Pi.toFloat
      } else if (xPos < -LimitX) {
        targetOrientation = 0
      } else if (yPos > LimitY) {
        targetOrientation = 3 * math.Pi.toFloat / 2
      } else if (yPos < -LimitY) {
        targetOrientation = math.Pi.toFloat / 2
      } else {
        targetOrientation = (2 * math.Pi * rnd()).toFloat
      }
    }

    // adjust orientation towards target orientation
    if (targetOrientation != orientation) {
      val diff = targetOrientation - orientation
      val diffP = if (diff < 0) diff + 2 * math.Pi else diff
      if (diffP <= turnSpeed) {
        orientation = targetOrientation
      } else if (diffP < math.Pi) {
        orientation += turnSpeed
      } else {
        orientation -= turnSpeed
      }

      if (orientation < 0) orientation += 2 * math.Pi.toFloat
      if (orientation > 2 * math.Pi) orientation -= 2 * math.Pi.toFloat
    }

    if (stationary && rnd() < 0.01 || !stationary && rnd() < 0.001) {
      stationary = !stationary
    }

    // update positions
    if (!stationary) {
      xPos += vx
      yPos += vy
    }


    oldPositions.enqueue((xPos, yPos, orientation))
    if (oldPositions.length > nPos) oldPositions.dequeue()
  }

  override def state(): ModelDescriptor[_] = ModelDescriptor(
    PositionDescriptor(xPos, yPos, orientation),
    DroneDescriptor(
      size,
      modules,
      modules.contains(ShieldGeneratorDescriptor),
      hullState,
      isBuilding = false,
      0,
      ColorRGB(0, 0, 1)
    ),
    DroneModelParameters(
      if (modules.contains(ShieldGeneratorDescriptor)) Some(Float0To1(1)) else None,
      None
    )
  )


  def vx = math.cos(orientation).toFloat * speed

  def vy = math.sin(orientation).toFloat * speed


  def rnd() = Random.nextDouble().toFloat


  def dead = false

  def hasVision = true
  def maxSpeed = speed
}
