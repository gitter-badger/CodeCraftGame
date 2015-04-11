import Dependencies._


lazy val graphics = (project in file("graphics")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.graphics",
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= graphicsDependencies
  )

lazy val collisions = (project in file("collisions")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= commonDependencies,
    name := "cg.collisions"
  )

lazy val simulation = (project in file("simulation")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.simulator",
    libraryDependencies ++= commonDependencies
  ).dependsOn(graphics, collisions)

