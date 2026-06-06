import sbt.*

val ProjectName = "typesafe-config-yaml"

lazy val root = (project in file("."))
  .settings(
    organization := "io.h8",
    name := ProjectName,

    organizationName := "H8IO",
    organizationHomepage := Some(url(s"https://github.com/h8io")),
    homepage := Some(url(s"https://github.com/h8io/$ProjectName")),

    scmInfo := Some(
      ScmInfo(
        url(s"https://github.com/h8io/$ProjectName"), s"scm:git@github.com:h8io/$ProjectName.git")),

    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),

    developers := List(
      Developer(
        id = "eshu",
        name = "Pavel",
        email = "tjano.xibalba@gmail.com",
        url = url("https://github.com/eshu/"))),

    versionScheme := Some("semver-spec"),

    scalaVersion := "3.3.7",

    crossPaths := false,
    autoScalaLibrary := false,

    javacOptions ++= Seq("--release", "11", "-Xlint:all", "-Werror"),
    Compile / doc / javacOptions -= "-Xlint:all",
    Compile / doc / javacOptions += "-quiet",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Werror",
      "-Wunused:all",
      "-Wvalue-discard"
    ),

    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.9",
      "org.snakeyaml" % "snakeyaml-engine" % "3.0.1",
      "org.scalatest" %% "scalatest" % "3.2.20" % Test
    ),

    dependencyOverrides += "org.scala-lang" %% "scala3-library" % scalaVersion.value
  )
