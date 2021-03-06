package cwinter.codecraft.graphics.materials

import javax.media.opengl.GL._
import javax.media.opengl.GL2ES2._
import javax.media.opengl._

import com.jogamp.common.nio.Buffers
import cwinter.codecraft.util.maths.matrices.Matrix4x4
import cwinter.codecraft.graphics.model.{JVMVBO, VBO}
import cwinter.codecraft.util.maths.{VertexManifest, Vertex}

import scala.io.Source
import scala.language.implicitConversions


/**
 * Vertex shader: code run on GPU to transform vertex positions
 * Fragment shader: code run on GPU to determine pixel colours
 * Program: can be used to store and then reference a vertex + fragment shader on the GPU
 * Vertex Buffer Object: unstructured vertex data
 * (Vertex) Attribute: input parameter to a shader
 * Vertex Attribute Object: maps data from robowars.graphics.model.VBO to one or more attributes
 */
private[graphics] class JVMMaterial[TPosition <: Vertex, TColor <: Vertex, TParams](
  gl: GL,
  vsPath: String,
  fsPath: String,
  attributeNamePos: String,
  attributeNameCol: Option[String],
  enableCaps: Int*
)(implicit
  val posVM: VertexManifest[TPosition],
  val colVM: VertexManifest[TColor]
) extends Material[TPosition, TColor, TParams] {

  val nCompPos = posVM.nComponents
  val nCompCol = colVM.nComponents
  val nComponents = nCompPos + nCompCol


  /******************
   * INITIALISATION *
   ******************/

  private[this] val gl2 = gl.getGL2
  import gl2._

  // compile shaders and attach to program
  protected val programID = glCreateProgram()
  protected val vertexShaderID = compileShader(vsPath, GL_VERTEX_SHADER, programID)
  protected val fragmentShaderID = compileShader(fsPath, GL_FRAGMENT_SHADER, programID)
  glLinkProgram(programID)
  checkProgramInfoLog(programID)

  val uniformProjection = glGetUniformLocation(programID, "projection")
  val uniformModelview = glGetUniformLocation(programID, "modelview")

  val attributePos = glGetAttribLocation(programID, attributeNamePos)
  val attributeCol = attributeNameCol.map(glGetAttribLocation(programID, _))

  implicit def VBOToJSVBO(vbo: VBO): JVMVBO = {
    assert(vbo.isInstanceOf[JVMVBO], s"Expected vbo of type JVMVBO. Actual: ${vbo.getClass.getName}")
    vbo.asInstanceOf[JVMVBO]
  }


  /********************
   * PUBLIC INTERFACE *
   ********************/

  def beforeDraw(projection: Matrix4x4): Unit = {
    glUseProgram(programID)

    glUniformMatrix4fv(
      uniformProjection,
      1 /* only setting 1 matrix */,
      true /* transpose? */,
      projection.data,
      0 /* offset */)

    enableCaps.foreach(glEnable)
  }

  def draw(vbo: VBO, modelview: Matrix4x4): Unit = {
    Material._drawCalls += 1

    // upload modelview
    glUniformMatrix4fv(uniformModelview, 1, true, modelview.data, 0)

    // bind vbo and enable attributes
    glBindVertexArray(vbo.vao)
    glBindBuffer(GL_ARRAY_BUFFER, vbo.id)

    glEnableVertexAttribArray(attributePos)
    attributeCol.foreach(glEnableVertexAttribArray)

    // actual drawing call
    glDrawArrays(GL_TRIANGLES, 0, vbo.size)
  }

  def afterDraw(): Unit = {
    enableCaps.foreach(glDisable)

    // disable attributes
    glDisableVertexAttribArray(attributePos)
    attributeCol.foreach(glDisableVertexAttribArray)

    // check logs for errors
    checkProgramInfoLog(programID)
    checkShaderInfoLog(fragmentShaderID)
    checkShaderInfoLog(vertexShaderID)

    glUseProgram(0)
    glBindVertexArray(0)
  }


  /**
   * Allocates a VBO handle, loads vertex data into GPU and defines attribute pointers.
   * @param data The data for the VBO.
   * @return Returns a `robowars.graphics.model.VBO` class which give the handle and number of data of the vbo.
   */
  def createVBO(data: Array[Float], dynamic: Boolean): VBO = {
    // create vbo handle
    val vboRef = new Array[Int](1)
    glGenBuffers(1, vboRef, 0)
    val vboHandle = vboRef(0)

    val vaoRef = new Array[Int](1)
    glGenVertexArrays(1, vaoRef, 0)
    val vao = vaoRef(0)

    glBindVertexArray(vao)


    // store data to GPU
    glBindBuffer(GL_ARRAY_BUFFER, vboHandle)
    val numBytes = data.length * 4
    val verticesBuffer = Buffers.newDirectFloatBuffer(data)
    glBufferData(GL_ARRAY_BUFFER, numBytes, verticesBuffer, if (dynamic) GL_DYNAMIC_DRAW else GL_STATIC_DRAW)

    // bind shader attributes (input parameters)
    glVertexAttribPointer(attributePos, nCompPos, GL_FLOAT, false, 4 * nComponents, 0)
    attributeCol.foreach(glVertexAttribPointer(_, nCompCol, GL_FLOAT, false, 4 * nComponents, 4 * nCompPos))

    glBindBuffer(GL_ARRAY_BUFFER, 0)

    VBO._count += 1
    JVMVBO(vboHandle, data.length / nComponents, vao)
  }


  /*******************
   * PRIVATE METHODS *
   *******************/


  /**
   * Compile a shader and attach to a program.
   * @param filename The source code for the shader.
   * @param shaderType The type of shader (`GL2ES2.GL_VERTEX_SHADER` or `GL2ES2.GL_FRAGMENT_SHADER`)
   * @param programID The handle to the program.
   * @return
   */
  protected def compileShader(filename: String, shaderType: Int, programID: Int): Int = {
    // Create GPU shader handles
    // OpenGL returns an index id to be stored for future reference.
    val shaderHandle = glCreateShader(shaderType)

    // bind shader to program
    glAttachShader(programID, shaderHandle)


    // Load shader source code and compile into a program
    val stream = getClass.getResourceAsStream("/" + filename)
    if (stream == null) println("Couldn't get shader resource: " + filename)
    val lines = Array(Source.fromInputStream(stream).mkString)
    val lengths = lines.map(_.length)
    glShaderSource(shaderHandle, lines.length, lines, lengths, 0)
    glCompileShader(shaderHandle)


    // Check compile status.
    val compiled = new Array[Int](1)
    glGetShaderiv(shaderHandle, GL_COMPILE_STATUS, compiled, 0)
    if (compiled(0) == 0) {
      println("Error compiling shader:")
      checkShaderInfoLog(shaderHandle)
    }

    shaderHandle
  }


  /**
   * Print out errors from the program info log, if any.
   */
  protected def checkProgramInfoLog(programID: Int): Unit = {
    // obtain log message byte count
    val logLength = new Array[Int](1)
    glGetProgramiv(programID, GL_INFO_LOG_LENGTH, logLength, 0)

    if (logLength(0) > 1) {
      val log = new Array[Byte](logLength(0))
      glGetProgramInfoLog(programID, logLength(0), null, 0, log, 0)
      println(s"Program Error:\n${new String(log)}")
    }
  }


  /**
   * Print out errors from the shader info log, if any.
   */
  protected def checkShaderInfoLog(shaderID: Int): Unit = {
    val logLength = new Array[Int](1)
    glGetShaderiv(shaderID, GL_INFO_LOG_LENGTH, logLength, 0)

    if (logLength(0) > 1) {
      val log = new Array[Byte](logLength(0))
      glGetShaderInfoLog(shaderID, logLength(0), null, 0, log, 0)

      println(new String(log))
    }
  }
}

