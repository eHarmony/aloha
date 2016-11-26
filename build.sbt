import sbt.inc.IncOptions

// import sbtprotobuf.{ProtobufPlugin=>PB}
// PB.protobufSettings // move this later

// Currently, we have to do
// sbt
//  +test
//  run `mv_filtered` in another window in root project directory.
//  +test (again)

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
  crossScalaVersions := Seq("2.10.5", "2.11.8"),
  crossPaths := true,
  incOptions := incOptions.value.withNameHashing(true),
  javacOptions ++= Seq("-Xlint:unchecked"),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  scalacOptions ++= Seq(
    // "-verbose",
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
        // "-Yinline",
        "-Yclosure-elim",
        "-Ydead-code"
      )
      case Some((2, scalaMajor)) if scalaMajor == 11 => Seq(
        "-Ywarn-unused",
        "-Ywarn-unused-import",
        // "-Yinline",
        "-Yclosure-elim",
        "-Ydead-code" //,

        // "-Xlog-implicit-conversions",
        // "-Xlog-implicits"
      )
      case _ => Seq()
    }
  }
)

// ===========================================================================
//  Modules
// ===========================================================================

lazy val root = project.in(file("."))
  .aggregate(core)
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(versionDependentSettings: _*)

// PB.protobufConfig.version := "2.4.1"

// val pbconfig = inConfig(protobufConfig)(Seq[Setting[_]](
//   version := "2.4.1"
// ))

// lazy val protobufCompile = taskKey[Unit]("Generate java source from protocol buffers")
//
// protobufCompile := {
//   val s: TaskStreams = streams.value
//   val shell: Seq[String] = if (sys.props("os.name").contains("Windows")) Seq("cmd", "/c") else Seq("bash", "-c")
//   val pwd: Seq[String] = shell :+ "pwd"
//   val compile: Seq[String] = shell :+ "protoc --proto_path=src/test/proto --java_out=src/test/java-x"
//   val move: Seq[String] = shell :+    "ls"
//   s.log.info("compiling protos...")
//   if((pwd #&& compile #&& move !) == 0) {
//     s.log.success("protos compiled successful!")
//   } else {
//     throw new IllegalStateException("protos compilation failed!")
//   }
// }

// (run in Test) := (run in Test).dependsOn(protobufCompile)
// resources in Test in core ++= (edit in Test in EditSource).value // .data

// (copyResources in core in Test) <<= (copyResources in core in Test) map { _ =>
//   (edit in EditSource).value
// }
(copyResources in core in Test) := ((copyResources in core in Test) dependsOn (edit in EditSource)).value

def editSourceSettings = Seq[Setting[_]](
  // This might be the only one that requires def instead of lazy val:
  targetDirectory in EditSource := (crossTarget / "filtered").value,

  // These don't change per project and should be OK with a lazy val instead of def:
  flatten in EditSource := false,
  (sources in EditSource) <++= baseDirectory map { d =>
    (d / "src" / "main" / "filtered_resources" / "" ** "*.*").get ++
    (d / "src" / "test" / "filtered_resources" / "" ** "*.*").get
  },
  variables in EditSource <+= crossTarget {t => ("projectBuildDirectory", t.getCanonicalPath)},
  variables in EditSource <+= (sourceDirectory in Test) {s => ("scalaTestSource", s.getCanonicalPath)},
  variables in EditSource <+= version {s => ("projectVersion", s.toString)}
)

lazy val core = project.in(file("aloha-core"))
  .settings(commonSettings: _*)
  .settings(versionDependentSettings: _*)
  .settings(coreDependencies: _*)
  .settings(editSourceSettings: _*)
  // .settings(PB.protobufSettings : _*)
  // .settings(
  //   version in PB.protobufConfig := "2.4.1",
  //   sourceDirectory in PB.protobufConfig := new java.io.File("src/test/protobuf"),
  //   javaSource in PB.protobufConfig := new java.io.File("$sourceManaged/generated-test-sources")  // $sourceManaged/compiled_protobuf
  // ) // .settings(pbconfig: _*)
  .settings(
    name := "aloha-core",

    // See: http://www.scala-sbt.org/release/docs/Running-Project-Code.html
    // fork := true is needed; otherwise we see error:
    //
    // Test com.eharmony.aloha.models.CategoricalDistibutionModelTest.testSerialization
    // failed: java.lang.ClassCastException: cannot assign instance of
    // scala.collection.immutable.List$SerializationProxy to field
    // com.eharmony.aloha.models.CategoricalDistibutionModel.features of type
    // scala.collection.Seq in instance of
    // com.eharmony.aloha.models.CategoricalDistibutionModel
    fork := true,

    // Because 2.10 runtime reflection is not thread-safe, tests fail non-deterministically.
    // This is a hack to make tests pass by not allowing the tests to run in parallel.
    parallelExecution in Test := true
  )

lazy val coreDependencies: Seq[Setting[_]] = Seq(
  libraryDependencies := Seq(
    "org.scala-lang" % "scala-library" % scalaVersion.value,
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    // "org.scalaz" %% "scalaz-core" % "7.0.0", // % scalaVersion.value,
    "org.scalaz" %% "scalaz-core" % "7.0.6", // % scalaVersion.value, // 7.0.6 is the first cross built for 2.10 and 2.11
    "com.github.deaktator" %% "scala-runtime-manifest" % "1.0.0",

    "com.eharmony" % "aloha-proto" % "2.0.1",
    "com.google.protobuf" % "protobuf-java" % "2.4.1",

    "com.fasterxml" % "classmate" % "1.0.0",
    "com.github.scopt" %% "scopt" % "3.3.0",
    "com.twitter" %% "util-core" % "6.27.0",
    "commons-codec" % "commons-codec" % "1.10",
    "commons-io" % "commons-io" % "2.4",
    "commons-vfs" % "commons-vfs" % "1.0"  excludeAll(ExclusionRule("commons-logging", "commons-logging")),
    "org.apache.commons" % "commons-vfs2" % "2.0" excludeAll(ExclusionRule("org.apache.maven.scm", "maven-scm-api"),
                                                             ExclusionRule("org.apache.maven.scm", "maven-scm-provider-svnexe")),
    "io.spray" %% "spray-json" % "1.3.1",

    "org.reflections" % "reflections" % "0.9.9",
    "org.slf4j" % "slf4j-api" % "1.7.10",

    "com.github.multiworldtesting" % "explore-java" % "1.0.0",

    "junit" % "junit" % "4.11" % "test",
    "cc.mallet" %  "mallet" % "2.0.7" % "test" excludeAll(ExclusionRule("junit", "junit")),

    "org.springframework" % "spring-beans" % "3.1.4.RELEASE" % "test",
    "org.springframework" % "spring-context-support" % "3.1.4.RELEASE" % "test",
    "org.springframework" % "spring-context" % "3.1.4.RELEASE" % "test",
    "org.springframework" % "spring-core" % "3.1.4.RELEASE" % "test",
    "org.springframework" % "spring-test" % "3.1.4.RELEASE" % "test",

    "com.novocode" % "junit-interface" % "0.11" % "test"




//      "org.slf4j" % "slf4j-api" % "1.7.10",


/*
    "org.apache.commons" % "commons-vfs2" % "2.0" excludeAll(ExclusionRule("org.apache.maven.scm", "maven-scm-api"),
                                                             ExclusionRule("org.apache.maven.scm", "maven-scm-provider-svnexe")) ,
    "org.apache.commons" % "commons-lang3" % "3.2",
    "commons-logging" % "commons-logging" % "1.1.1",

    "org.slf4j" % "slf4j-api" % "1.7.10",
    "org.slf4j" % "slf4j-log4j12" % "1.7.10",
    "commons-logging" % "commons-logging" % "1.1.1",
    "commons-logging" % "commons-logging" % "1.1.1",
    "commons-logging" % "commons-logging" % "1.1.1",
*/


//      "org.scalatest" %% "scalatest" % "2.2.5" % "test",
//      "org.slf4j" % "slf4j-log4j12" % "1.7.10" % "test"
  )
)



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
