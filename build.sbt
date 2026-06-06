import sbt.*

val ProjectName = "typesafe-config-yaml"

ThisBuild / organization := "io.h8"
ThisBuild / organizationName := "H8IO"
ThisBuild / organizationHomepage := Some(url(s"https://github.com/h8io"))
ThisBuild / homepage := Some(url(s"https://github.com/h8io/$ProjectName"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url(s"https://github.com/h8io/$ProjectName"), s"scm:git@github.com:h8io/$ProjectName.git"))

ThisBuild / licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

ThisBuild / developers := List(
  Developer(
    id = "eshu",
    name = "Pavel",
    email = "tjano.xibalba@gmail.com",
    url = url("https://github.com/eshu/")))

ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / dynverSeparator := "-"

ThisBuild / scalaVersion := "3.3.7"

ThisBuild / crossPaths := false
ThisBuild / autoScalaLibrary := false

ThisBuild / javacOptions ++= Seq("--release", "11", "-Xlint:all", "-Werror")
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Werror",
  "-Wunused:all",
  "-Wvalue-discard"
)

lazy val root = (project in file("."))
  .settings(
    name := ProjectName,

    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.9",
      "org.snakeyaml" % "snakeyaml-engine" % "3.0.1",
      "org.scalatest" %% "scalatest" % "3.2.20" % Test
    ),

    dependencyOverrides += "org.scala-lang" %% "scala3-library" % scalaVersion.value
  )
