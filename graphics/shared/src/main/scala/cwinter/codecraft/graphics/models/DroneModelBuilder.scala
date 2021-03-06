package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.materials.Intensity
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.primitives.{Polygon, PolygonRing}
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.Geometry.circumradius
import cwinter.codecraft.util.maths._
import cwinter.codecraft.util.modules.ModulePosition

import scala.math._


private[graphics] class DroneModelBuilder(
  val drone: DroneDescriptor,
  val timestep: Int
)(implicit val rs: RenderStack) extends CompositeModelBuilder[DroneDescriptor, DroneModelParameters] {

  def signature = drone

  override protected def buildSubcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, DroneModelParameters]]) = {
    import drone._
    val colorPalette =
      if (signature.isBuilding) MutedDroneColors
      else DefaultDroneColors
    val playerColor =
      if (isBuilding) signature.playerColor * 0.5f
      else signature.playerColor
    import colorPalette._

    val sideLength = 40
    val radiusBody = 0.5f * sideLength / sin(Pi / sides).toFloat
    val radiusHull = radiusBody + circumradius(4, sides)

    val body =
      Polygon(
        rs.MaterialXYZRGB,
        sides,
        ColorBody,
        ColorBody,
        radius = radiusBody
      )

    val hullColors = hullState.map {
      case 2 => ColorHull
      case 1 => ColorHullDamaged
      case 0 => ColorHullBroken
    }
    val hull =
      PolygonRing(
        rs.MaterialXYZRGB,
        sides,
        playerColor +: hullColors,
        playerColor +: hullColors,
        radiusBody,
        radiusHull,
        NullVectorXY,
        0,
        0
      )

    val modulesModel =
      for {
        module <- modules
      } yield module match {
        case EnginesDescriptor(position) =>
          DroneEnginesModel(ModulePosition(sides, position), colorPalette, playerColor, animationTime)
        case MissileBatteryDescriptor(position, n) =>
          DroneMissileBatteryModelBuilder(colorPalette, playerColor, ModulePosition(sides, position), n)
        case ShieldGeneratorDescriptor(position) =>
          DroneShieldGeneratorModel(ModulePosition(sides, position), colorPalette, playerColor)
        case StorageModuleDescriptor(position, contents) =>
          DroneStorageModelBuilder(ModulePosition(sides, position), colorPalette, contents)
        case ManipulatorDescriptor(position) =>
          DroneConstructorModelBuilder(colorPalette, playerColor, ModulePosition(sides, position))
      }


    val staticModels = body +: hull +: modulesModel

    var dynamicModels = Seq.empty[ModelBuilder[_, DroneModelParameters]]
    if (hasShields) {
      dynamicModels :+=
        PolygonRing(
          material = rs.TranslucentAdditivePIntensity,
          n = 50,
          colorInside = ColorRGBA(ColorThrusters, 0f),
          colorOutside = ColorRGBA(White, 0.7f),
          outerRadius = radiusHull + 2,
          innerRadius = Geometry.inradius(radiusHull, sides) * 0.85f
        ).wireParameters[DroneModelParameters](d => Intensity(d.shieldState.get))
    }

    // TODO: make this work again
    /*
    if (!isBuilding) {
      dynamicModels :+=
        new DynamicModel(
          new DroneThrusterTrailsModelFactory(
            sideLength, radiusHull, sides, playerColor
          ).buildModel
        ).wireParameters[DroneDescriptor](d => d.positions)
    }*/

    (staticModels, dynamicModels)
  }


  override protected def decorate(model: Model[DroneModelParameters]): Model[DroneModelParameters] =
    if (signature.isBuilding)
      model
        .translated(VertexXYZ(0, 0, -3), rs.modelviewTranspose)
        .withDynamicVertexCount
        .wireParameters[DroneModelParameters](d => (d.constructionState.get, d))
    else model
}


private[graphics] trait DroneColors {
  val Black: ColorRGB
  val White: ColorRGB
  val ColorBody: ColorRGB
  val ColorHull: ColorRGB
  val ColorHullDamaged: ColorRGB
  val ColorHullBroken: ColorRGB
  val ColorThrusters: ColorRGB
  val ColorBackplane: ColorRGB
}

private[graphics] object DefaultDroneColors extends DroneColors {
  final val Black = ColorRGB(0, 0, 0)
  final val White = ColorRGB(1, 1, 1)
  final val ColorBody = Black
  final val ColorHull = ColorRGB(0.95f, 0.95f, 0.95f)
  final val ColorHullDamaged = ColorRGB(0.6f, 0.6f, 0.6f)
  final val ColorHullBroken = ColorRGB(0.2f, 0.2f, 0.2f)
  final val ColorThrusters = ColorRGB(0, 0, 1)
  final val ColorBackplane = ColorRGB(0.1f, 0.1f, 0.1f)
}

private[graphics] object MutedDroneColors extends DroneColors {
  final val dimmingFactor = 0.5f
  final val Black = DefaultDroneColors.Black * dimmingFactor
  final val White = DefaultDroneColors.White * dimmingFactor
  final val ColorBody = DefaultDroneColors.ColorBody * dimmingFactor
  final val ColorHull = DefaultDroneColors.ColorHull * dimmingFactor
  final val ColorHullDamaged = DefaultDroneColors.ColorHullDamaged * dimmingFactor
  final val ColorHullBroken = DefaultDroneColors.ColorHullBroken * dimmingFactor
  final val ColorThrusters = DefaultDroneColors.ColorThrusters * dimmingFactor
  final val ColorBackplane = DefaultDroneColors.ColorBackplane * dimmingFactor
}


