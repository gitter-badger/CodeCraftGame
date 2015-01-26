package robowars.graphics.materials

import javax.media.opengl.GL._
import javax.media.opengl.GL4

import robowars.graphics.model.{ColorRGB, VertexXYZ}


class MaterialXYZRGB(implicit gl: GL4)
extends Material[VertexXYZ, ColorRGB](
  gl = gl,
  vsPath = "src/main/shaders/xyz_rgb_vs.glsl",
  fsPath = "src/main/shaders/rgb0_fs.glsl",
  "vertexPos",
  Some("vertexCol"),
  GL_DEPTH_TEST
)