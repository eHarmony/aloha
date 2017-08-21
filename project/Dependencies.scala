import sbt._

object Dependencies {

  val scalaToolsVersion = "1.0.4"
  val springVersion = "3.1.4.RELEASE"
  val protobufVersion = "2.4.1"
  val alohaProtoVersion = "2.0.1"

  val scalazVersion = "7.0.6"
  val commonsCodecVersion = "1.4"
  val commonsIoVersion = "2.4"
  val commonsLoggingVersion = "1.1.1"
  val slf4jVersion = "1.7.10"
  val log4jVersion = "1.2.17"

  val vwJniVersion = "8.4.1"
  val h2oVersion = "3.10.3.2"
  val guavaVersion = "16.0.1"
  val avroVersion = "1.8.1"

  val scalazCore = "org.scalaz" %% "scalaz-core" % scalazVersion
  val runtimeManifest = "com.github.deaktator" %% "scala-runtime-manifest" % "1.0.0"

  val alohaProto = "com.eharmony" % "aloha-proto" % alohaProtoVersion
  val protobuf = "com.google.protobuf" % "protobuf-java" % protobufVersion

  val classMate = "com.fasterxml" % "classmate" % "1.0.0"
  val scopt = "com.github.scopt" %% "scopt" % "3.3.0"
  val twitterUtilCore = "com.twitter" %% "util-core" % "6.27.0"
  val commonsCodec = "commons-codec" % "commons-codec" % commonsCodecVersion
  val commonsIo = "commons-io" % "commons-io" % commonsIoVersion
  val vfs1 = "commons-vfs" % "commons-vfs" % "1.0"
  val vfs2 = "org.apache.commons" % "commons-vfs2" % "2.0" excludeAll(ExclusionRule("org.apache.maven.scm", "maven-scm-api"),
                                                                      ExclusionRule("org.apache.maven.scm", "maven-scm-provider-svnexe"))
  val sprayJson = "io.spray" %% "spray-json" % "1.3.1"

  val reflections = "org.reflections" % "reflections" % "0.9.9"
  val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jVersion
  val slf4jLog4j = "org.slf4j" % "slf4j-log4j12" % slf4jVersion
  val commonsLogging = "commons-logging" % "commons-logging" % commonsLoggingVersion
  val log4j = "log4j" % "log4j" % log4jVersion

  val mwt = "com.github.multiworldtesting" % "explore-java" % "1.0.0"

  val vwJni = "com.github.johnlangford" % "vw-jni" % vwJniVersion

  val h2o = "ai.h2o" % "h2o-core" % h2oVersion excludeAll(ExclusionRule("ai.h2o", "reflections"))
  val h2oGenModel = "ai.h2o" % "h2o-genmodel" % h2oVersion

  val guava = "com.google.guava" % "guava"  % guavaVersion

  val parserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % scalaToolsVersion
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % scalaToolsVersion

  val junit = "junit" % "junit" % "4.11" % "test"
  val junitInterface = "com.novocode" % "junit-interface" % "0.11" % "test"

  val mallet = "cc.mallet" %  "mallet" % "2.0.7" excludeAll(ExclusionRule("junit", "junit"))

  // Just for testing.  Not an Aloha compile dep, so leave them as test scoped.
  val springBeans = "org.springframework" % "spring-beans" % springVersion % "test"
  val springCtxSupport = "org.springframework" % "spring-context-support" % springVersion % "test"
  val springCtx = "org.springframework" % "spring-context" % springVersion % "test"
  val springCore = "org.springframework" % "spring-core" % springVersion % "test"
  val springTest = "org.springframework" % "spring-test" % springVersion % "test"


  val avro = "org.apache.avro" % "avro" % avroVersion
  val avroNoSlf4j = avro excludeAll(ExclusionRule("org.slf4j", "slf4j-api"))

  lazy val coreDeps = Seq(
    classMate, commonsCodec, commonsIo, mwt, reflections,
    runtimeManifest, scalazCore, scopt, slf4jApi, sprayJson,
    twitterUtilCore, vfs1, vfs2,

    // Test Deps.
    junit, junitInterface, mallet % "test",
    springBeans, springCore, springCtx, springCtxSupport, springTest
  )

  lazy val ioAvroDeps = Seq(avroNoSlf4j, junit, slf4jApi)

  lazy val avroScoreJavaDeps = Seq(avro)

  lazy val vwJniDeps = Seq(
    vwJni, commonsCodec, scopt,
    junit, junitInterface, slf4jLog4j % "test"
  )

  lazy val h2oDeps = Seq(
    h2o, commonsCodec, h2oGenModel, scopt,
    junit, junitInterface, slf4jLog4j % "test"
  )

  lazy val cliDeps = Seq(
    slf4jLog4j,
    junit, junitInterface
  )

  lazy val ioProtoDeps = Seq(
    protobuf, alohaProto,
    junit, junitInterface
  )

  lazy val overrideDeps = Set(
    guava, parserCombinators, scalaXml, commonsLogging, log4j, slf4jApi
  )
}
