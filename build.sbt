name := "aloha"
description := "Scala-based machine learning library with generic models and lazily created features."


// ===========================================================================
//  Build Settings
// ===========================================================================

lazy val commonSettings = Seq(
  organization := "com.eharmony",
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.10.5", "2.11.12"),
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
    "-Ywarn-dead-code",

    // Compile errors occur when omitted in 2.10 because of a scala language bug.
    "-Ycheck:jvm"
  ),

  // Set the dependency conflict resolution behavior.  For more info, see:
  //   http://www.scala-sbt.org/0.13/api/index.html#sbt.ConflictManager$
  //   https://ant.apache.org/ivy/history/latest-milestone/settings/conflict-managers.html
  conflictManager := ConflictManager.strict,

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

  // This is required to avoid Travis failures in sbt.ForkMain with message
  // "failed with exit code 137".  This is an issue others have when forking.
  javaOptions ++= Seq("-Xmx2g"),

  // Because 2.10 runtime reflection is not thread-safe, tests fail non-deterministically.
  // This is a hack to make tests pass by not allowing the tests to run in parallel.
  parallelExecution in Test := false
)

lazy val versionDependentSettings = Seq(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor == 10 => Seq(
        // "-Yinline",
        "-Yclosure-elim",
        "-Ydead-code",
        "-Ywarn-all"
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
//  Resource Filtering
// ===========================================================================

// Since `flatten` is a very generic name, Alias it to avoid conflicts with keys in other plugins.
import EditSourcePlugin.autoImport.{flatten => editFlatten}
import com.sun.java.swing.plaf.windows.WindowsInternalFrameTitlePane.ScalableIconUIResource

def editSourceSettings = Seq[Setting[_]](
  // This might be the only one that requires def instead of lazy val:
  targetDirectory in EditSource := (crossTarget.value / "filtered"),

  // These don't change per project and should be OK with a lazy val instead of def:
  editFlatten in EditSource := false,
  (sources in EditSource) ++= baseDirectory.map { d =>
    (d / "src" / "main" / "filtered_resources" / "" ** "*.*").get ++
    (d / "src" / "test" / "filtered_resources" / "" ** "*.*").get
  }.value,
  variables in EditSource += crossTarget {t => ("projectBuildDirectory", t.getCanonicalPath)}.value,
  variables in EditSource += (sourceDirectory in Test) {s => ("scalaTestSource", s.getCanonicalPath)}.value,
  variables in EditSource += version {s => ("projectVersion", s.toString)}.value,

  // Try doing this with adding to sourceManaged direcory instead of directly
  // adding to the class directory in the compile task.  This would most likely
  // be better because we could incorporate src/main/scala if we wanted.  This
  // could easily be accomplished if we rename the filtered_resources directory
  // and add an indicator in the file names (like __filtered__ or something).
  // For instance, do something like this:
  //
  //    mySourceGenerator in Compile := {
  //      generate( (sourceManaged in Compile).value / "some_directory")
  //    }
  //
  compile in Compile := {
    val c = (compile in Compile).value
    filteredTask.value
    c
  }
)

/**
 * This task moves the filtered files to the proper target directory. It is
 * based on specific EditSource settings, especially that filtered_resources
 * is the only directory in which EditSource searches.
 */
lazy val filteredTask = Def.task {
  val s = streams.value
  val files = (edit in EditSource).value

  // This directory is dependent on the target of EditSource.  It assumes that
  // it is crossTarget / "filtered".
  val slash = System.getProperty("file.separator", "/")

  val components = ("^(.*)/filtered/src/(main|test)/filtered_resources/(.*)$").r
  val mappings = files map { f =>
    // Replace in case this bombs in Windows.
    val newF = f.getCanonicalPath.replace(slash, "/") match {
      case components(prefix, srcType, suffix) =>
        // This could probably be more robust.
        val classDir = if (srcType == "test") "test-classes" else "classes"
        file(prefix) / classDir / suffix
    }
    (f, newF)
  }

  mappings foreach { case (f, t) =>
    val msg = s"""Moving "$f" -> "$t""""
    s.log.info(msg)
    val success = ((t.getParentFile.exists || t.getParentFile.mkdirs()) && f.renameTo(t))
    if (!success) s.log.error(s"Failure $msg")
  }
}


// ===========================================================================
//  Other Common Settings
// ===========================================================================

lazy val noPublishSettings = Seq(
  publish := { },
  publishM2 := { },
  publishLocal := { },
  publishArtifact := false
)

// ===========================================================================
//  Modules
// ===========================================================================

lazy val root = project.in(file("."))
  .aggregate(core, vwJni, h2o, cli, ioProto, avroScoreJava, ioAvro, docs)
  .dependsOn(core, vwJni, h2o, cli, ioProto, avroScoreJava, ioAvro, docs)
  .settings(commonSettings: _*)
  .settings(versionDependentSettings: _*)
  .settings(dependencyOverrides ++= Dependencies.overrideDeps)
  .settings(noPublishSettings)
//  .enablePlugins(ScalaUnidocPlugin)

lazy val core = project.in(file("aloha-core"))
  .settings(name := "aloha-core")
  .settings(commonSettings: _*)
  .settings(versionDependentSettings: _*)
  .settings(editSourceSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value
  ) ++ Dependencies.coreDeps)
  .settings(dependencyOverrides ++= Dependencies.overrideDeps)

lazy val vwJni = project.in(file("aloha-vw-jni"))
  .settings(name := "aloha-vw-jni")
  .dependsOn(core % "test->test;compile->compile")
  .settings(commonSettings: _*)
  .settings(versionDependentSettings: _*)
  .settings(editSourceSettings: _*)
  .settings(libraryDependencies ++= Dependencies.vwJniDeps)
  .settings(dependencyOverrides ++= Dependencies.overrideDeps)

lazy val h2o = project.in(file("aloha-h2o"))
  .settings(name := "aloha-h2o")
  .dependsOn(core % "test->test;compile->compile", ioProto % "test->test")
  .settings(commonSettings: _*)
  .settings(versionDependentSettings: _*)
  .settings(editSourceSettings: _*)
  .settings(libraryDependencies ++= Dependencies.h2oDeps)
  .settings(dependencyOverrides ++= Dependencies.overrideDeps)

lazy val ioProto = project.in(file("aloha-io-proto"))
  .settings(name := "aloha-io-proto")
  .dependsOn(core % "test->test;compile->compile")
  .settings(commonSettings: _*)
  .settings(versionDependentSettings: _*)
  .settings(editSourceSettings: _*)
  .settings(libraryDependencies ++= Dependencies.ioProtoDeps)
  .settings(dependencyOverrides ++= Dependencies.overrideDeps)

lazy val cli = project.in(file("aloha-cli"))
  .settings(name := "aloha-cli")
  .dependsOn(core % "test->test;compile->compile", vwJni, h2o, ioProto % "test->test;compile->compile")
  .settings(commonSettings: _*)
  .settings(versionDependentSettings: _*)
  .settings(editSourceSettings: _*)
  .settings(libraryDependencies ++= Dependencies.cliDeps)
  .settings(dependencyOverrides ++= Dependencies.overrideDeps)

// Avro definitions only
lazy val avroScoreJava = project.in(file("aloha-avro-score-java"))
  .settings(
    name := "aloha-avro-score-java",
    organization := "com.eharmony",
    crossPaths := false,
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
    ),
    libraryDependencies := Dependencies.avroScoreJavaDeps
  )

lazy val ioAvro = project.in(file("aloha-io-avro"))
  .settings(name := "aloha-io-avro")
  .dependsOn(core % "test->test;compile->compile", core, avroScoreJava)
  .settings(commonSettings: _*)
  .settings(versionDependentSettings: _*)
  .settings(editSourceSettings: _*)
  .settings(libraryDependencies ++= Dependencies.ioAvroDeps)
  .settings(dependencyOverrides ++= Dependencies.overrideDeps)



// ===========================================================================
//  Release
// ===========================================================================

// run `sbt release`.
// The rest should be fairly straightforward.  Follow prompts for things like
// and eventually enter PGP pass phrase.

sonatypeProfileName := "com.eharmony"

pomExtra in Global := (
    <url>https://github.com/eharmony/aloha</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>https://opensource.org/licenses/MIT</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>deaktator</id>
        <name>R M Deak</name>
        <url>https://deaktator.github.io</url>
        <roles>
            <role>creator</role>
            <role>developer</role>
        </roles>
        <timezone>-8</timezone>
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


// ===========================================================================
//  Site
// ===========================================================================

// enablePlugins(GhpagesPlugin)
git.remoteRepo := "git@github.com:eHarmony/aloha.git"

lazy val docsMappingsAPIDir = settingKey[String]("Name of subdirectory in site target directory for api docs")

lazy val docSettings = Seq(
  micrositeName := "Aloha",
  micrositeDescription := "Generic machine learning, lazy features",
  micrositeAuthor := "eHarmony",
  micrositeHighlightTheme := "atom-one-light",
  micrositeHomepage := "http://eharmony.github.io/aloha",
  micrositeOrganizationHomepage := "http://www.eharmony.com",
  micrositeBaseUrl := "aloha",
  micrositeDocumentationUrl := "docs",
  micrositeGithubOwner := "eharmony",
//  micrositeExtraMdFiles := Map(file("CONTRIBUTING.md") -> "contributing.md"),
  micrositeGithubRepo := "aloha",
  micrositePalette := Map(
    "brand-primary" -> "#5B5988",
    "brand-secondary" -> "#292E53",
    "brand-tertiary" -> "#222749",
    "gray-dark" -> "#49494B",
    "gray" -> "#7B7B7E",
    "gray-light" -> "#E5E5E6",
    "gray-lighter" -> "#F4F3F4",
    "white-color" -> "#FFFFFF"),
  autoAPIMappings := true,
  unidocProjectFilter in (ScalaUnidoc, unidoc) :=
    inProjects(core, vwJni, h2o, ioProto, avroScoreJava, ioAvro, cli),
  docsMappingsAPIDir := "docs/api",
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), docsMappingsAPIDir),
  ghpagesNoJekyll := false,
  fork in tut := true,
  fork in (ScalaUnidoc, unidoc) := true,
  // unidoc
  // Figure out a way to get this in.
//  scalacOptions in (ScalaUnidoc) ++= Seq(
//    "-Xfatal-warnings",
//    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
//    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
//    "-diagrams"
//  ),
  git.remoteRepo := "git@github.com:eharmony/aloha.git",
  includeFilter in makeSite := "**/*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "**/*.md"
)


// To see locally: docs/makeMicrosite
lazy val docs = project.in(file("docs"))
  .settings(name := "docs")
//  .settings(moduleName := "docs")
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(commonSettings: _*)
  .settings(noPublishSettings)
  .settings(GhpagesPlugin.globalSettings: _*)
  .settings(docSettings)
  .settings(scalacOptions in Tut := Seq("-usejavacp", "-J-Xmx2g"))
  .settings(dependencyOverrides ++= Dependencies.overrideDeps)
  .dependsOn(core, vwJni, h2o, ioProto % "compile->test;compile->compile", avroScoreJava, ioAvro, cli)
//  .settings(tutSettings)
//  .settings(tutScalacOptions ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code"))))
