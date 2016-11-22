import sbt.inc.IncOptions

name := "aloha"

homepage := Some(url("https://github.com/eharmony/aloha"))

licenses := Seq("MIT License" -> url("http://opensource.org/licenses/MIT"))

description := """Scala-based machine learning library with generic models and lazily created features."""

// ===========================================================================
//  Build Settings
// ===========================================================================

lazy val commonSettings = Seq(
  organization := "com.eharmony",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.5", "2.11.8", "2.12.0"),
  crossPaths := true,
  incOptions := incOptions.value.withNameHashing(true),
  javacOptions ++= Seq("-Xlint:unchecked"),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Xverify",
    "-Ywarn-inaccessible",
    "-Ywarn-dead-code"
  )
)

lazy val versionDependentSettings = Seq(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor == 10 => Seq(
        "-Yinline",
        "-Yclosure-elim",
        "-Ydead-code"
      )
      case Some((2, scalaMajor)) if scalaMajor == 11 => Seq(
        "-Ywarn-unused",
        "-Ywarn-unused-import",
        "-Yinline",
        "-Yclosure-elim",
        "-Ydead-code"
      )
      case Some((2, scalaMajor)) if scalaMajor == 12 => Seq(
        "-Ywarn-unused",
        "-Ywarn-unused-import"
      )
      case _ => Seq()
    }
  }
)

// ===========================================================================
//  Modules
// ===========================================================================



// ===========================================================================
//  Release
// ===========================================================================

// run `sbt release`.
// The rest should be fairly straightforward.  Follow prompts for things like
// and eventually enter PGP pass phrase.

sonatypeProfileName := "com.eharmony"

pomExtra in Global := (
    <scm>
      <url>git@github.com:eharmony/aloha.git</url>
      <developerConnection>scm:git:git@github.com:eharmony/aloha.git</developerConnection>
      <connection>scm:git:git@github.com:eharmony/aloha.git</connection>
    </scm>
    <developers>
      <developer>
        <id>deaktator</id>
        <name>R M Deak</name>
        <url>https://deaktator.github.io</url>
        <roles>
            <role>creator</role>
            <role>developer</role>
        </roles>
        <timezone>-7</timezone>
      </developer>
    </developers>
  )


import ReleaseTransformations._

releaseCrossBuild := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
  pushChanges
)
