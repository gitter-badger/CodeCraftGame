package robowars.graphics.model


trait Vertex {
  def apply(i: Int): Float
}

case class VertexXY(x: Float, y: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => x
    case 1 => y
    case _ => throw new IndexOutOfBoundsException("VerteXY only has 2 components.")
  }
}

case class ColorRGB(r: Float, g: Float, b: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => r
    case 1 => g
    case 2 => b
    case _ => throw new IndexOutOfBoundsException("ColorRGB only has 2 components.")
  }
}


object EmptyVertex extends Vertex {
  def apply(i: Int) =
    throw new IndexOutOfBoundsException("EmptyVertex does not have any components.")
}


trait VertexManifest[TVertex <: Vertex] {
  val nComponents: Int
}

object VertexManifest {
  implicit object VertexXY extends VertexManifest[VertexXY] {
    val nComponents = 2
  }

  implicit object ColorRGB extends VertexManifest[ColorRGB] {
    val nComponents = 3
  }

  implicit object EmptyVertexManifest extends VertexManifest[EmptyVertex.type] {
    val nComponents = 0
  }
}

