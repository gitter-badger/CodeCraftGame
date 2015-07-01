package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.LinePrimitive
import cwinter.codecraft.graphics.models.DroneColors.White
import cwinter.codecraft.graphics.worldstate.Player
import cwinter.codecraft.util.maths.{ColorRGBA, VertexXY}


object ManipulatorArmModelFactory {
  def build(player: Player, x1: Float, y1: Float, x2: Float, y2: Float)(implicit rs: RenderStack) = {

    LinePrimitive(
      rs.GaussianGlow,
      VertexXY(x1, y1),
      VertexXY(x2, y2),
      3,
      ColorRGBA(0.5f * player.color + 0.5f * White, 1),
      ColorRGBA(White, 0),
      zPos = 3
    ).noCaching.getModel
  }
}