import sbt._
import Keys._

object Commons {
  val appVersion = "0.1.6"

  val settings: Seq[Def.Setting[_]] = Seq(
    organization := "org.cwinter",
    version := appVersion,
    scalaVersion := "2.11.6",
    scalacOptions := Seq(
      "-Xlint",
      "-deprecation",
      "-Xfatal-warnings",
      "-feature"
    )
  )
}
