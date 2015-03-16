package robowars.worldstate


sealed trait WorldObject {
  val identifier: Int
  val xPos: Float
  val yPos: Float
  val orientation: Float
}

case class RobotObject(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,
  positions: Seq[(Float, Float, Float)],
  modules: Seq[RobotModule],
  hullState: Seq[Byte],
  size: Int
) extends WorldObject


sealed trait RobotModule

case class StorageModule(resourceCount: Int) extends RobotModule {
  assert(resourceCount >= 0)
  assert(resourceCount <= 6)
}
case class Engines(t: Int) extends RobotModule
case object ShieldGenerator extends RobotModule


case class MineralObject(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,

  size: Int
) extends WorldObject


case class LightFlash(
  identifier: Int,
  xPos: Float,
  yPos: Float,

  stage: Float
) extends WorldObject {
  val orientation = 0.0f
}


case class LaserMissile(
  identifier: Int,
  positions: Seq[(Float, Float)]
) extends WorldObject {
  val orientation = 0.0f
  val xPos = 0.0f
  val yPos = 0.0f
}

case object TestingObject extends WorldObject {
  val identifier = -1
  val xPos = 0f
  val yPos = 0f
  val orientation = 0f
}