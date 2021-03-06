package cwinter.codecraft.demos.graphics

import cwinter.codecraft.graphics.worldstate.{LightFlashDescriptor, ModelDescriptor, PositionDescriptor}


private[graphics] class MockLightFlash(val xPos: Float, val yPos: Float) extends MockObject {
  var stage: Float = 0

  def update(): Unit = stage += 1.0f / 24

  def state(): ModelDescriptor[_] = ModelDescriptor(
    PositionDescriptor(xPos, yPos, 0),
    LightFlashDescriptor(stage),
    LightFlashDescriptor(stage)
  )

  def dead: Boolean = stage > 1

  def hasVision = false
  def maxSpeed = 0
}
