ThisBuild / organization := "io.h8"

ThisBuild / crossPaths := false
ThisBuild / autoScalaLibrary := false

ThisBuild / javacOptions ++= Seq("--release", "11", "-Xlint:unchecked", "-Xlint:deprecation", "-Werror")

lazy val root = (project in file("."))
  .settings(
    name := "typesafe-config-yaml",

    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.8",
      "org.snakeyaml" % "snakeyaml-engine" % "3.0.1",
      "org.scalatest" % "scalatest_2.12" % "3.2.20" % Test
    )
  )
