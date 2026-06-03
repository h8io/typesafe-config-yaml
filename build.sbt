ThisBuild / organization := "io.h8"

ThisBuild / crossPaths := false
ThisBuild / autoScalaLibrary := false

ThisBuild / javacOptions ++= Seq("--release", "8")

lazy val root = (project in file("."))
  .settings(
    name := "typesafe-config-yaml",

    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.8",
      "org.yaml" % "snakeyaml" % "2.6",
      "org.scalatest" % "scalatest_2.12" % "3.2.19" % Test
    )
  )
