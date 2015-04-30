package cwinter.graphics.models

import cwinter.codinggame.util.maths.ColorRGBA
import cwinter.graphics.engine.RenderStack
import cwinter.graphics.materials.Intensity
import cwinter.graphics.model._
import cwinter.worldstate.LightFlashDescriptor


case class LightFlashSign(rs: RenderStack)

class LightFlashModelBuilder(lightFlash: LightFlashDescriptor)(implicit val rs: RenderStack)
  extends ModelBuilder[LightFlashSign, LightFlashDescriptor] {
  val signature = LightFlashSign(rs)

  override protected def buildModel: Model[LightFlashDescriptor] = {
    val flash = Polygon(
      rs.GaussianGlowPIntensity,
      25,
      ColorRGBA(1, 1, 1, 0),
      ColorRGBA(1, 1, 1, 1),
      radius = 1,
      zPos = -1
    ).getModel.scalable

    new LightFlashModel(flash)
  }
}


class LightFlashModel(val flash: Model[(Intensity, Float)]) extends CompositeModel[LightFlashDescriptor] {
  val models = Seq(flash)

  override def update(lightFlash: LightFlashDescriptor): Unit = {
    val intensity = Intensity(1 - lightFlash.stage)
    val radius = 60 * lightFlash.stage + 5

    flash.update((intensity, radius))
  }
}