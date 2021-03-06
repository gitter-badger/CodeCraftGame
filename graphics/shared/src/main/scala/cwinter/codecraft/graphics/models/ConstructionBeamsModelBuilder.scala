package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.primitives.{PartialPolygon, Polygon}
import cwinter.codecraft.graphics.worldstate.ConstructionBeamDescriptor
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, Vector2, VertexXY}
import cwinter.codecraft.util.modules.ModulePosition


private[graphics] case class ConstructionBeamsModelBuilder(
  signature: ConstructionBeamDescriptor
)(implicit rs: RenderStack) extends CompositeModelBuilder[ConstructionBeamDescriptor, Unit] {
  import signature._

  override protected def buildSubcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, Unit]]) = {
    val beams =
      for {
        (moduleIndex, active)<- modules
        modulePosition = ModulePosition(droneSize, moduleIndex)
      } yield constructionBeamModel(modulePosition, active)

    (beams, Seq.empty)
  }

  private def constructionBeamModel(position: VertexXY, active: Boolean): ModelBuilder[_, Unit] = {
    val dist = position.toVector2 - relativeConstructionPosition
    val angle =
      if (dist.x == 0 && dist.y == 0) 0
      else (position.toVector2 - relativeConstructionPosition).orientation.toFloat
    val radius = dist.length.toFloat
    val focusColor =
      if (active) ColorRGBA(0.5f * playerColor + 0.5f * ColorRGB(1, 1, 1), 0.9f)
      else ColorRGBA(playerColor, 0.7f)

    val n = 5
    val n2 = n - 1
    val midpoint = (n2 - 1) / 2f
    val range = (n2 + 1) / 2f
    PartialPolygon(
      rs.TranslucentAdditive,
      n,
      Seq.fill(n)(focusColor),
      ColorRGBA(0, 0, 0, 0) +: Seq.tabulate(n2)(i => {
        val color = 1 - Math.abs(i - midpoint) / range
        ColorRGBA(playerColor * color, 0)
      }).flatMap(x => Seq(x, x)) :+ ColorRGBA(0, 0, 0, 0),
      radius,
      position,
      0,
      angle,
      fraction = 0.05f
    )
  }
}

