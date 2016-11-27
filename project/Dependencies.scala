import sbt._
// import Process._
// import Keys._

object Dependencies {

  val springVersion = "3.1.4.RELEASE"
  val protobufVersion = "2.4.1"
  val alohaProtoVersion = "2.0.1"

  val scalazVersion = "7.0.6"
  val commonsCodecVersion = "1.10"
  val commonsIoVersion = "2.4"
  val slf4jVersion = "1.7.10"

  val vwJniVersion = "8.1.0"
  val h2oVersion = "3.8.2.3"

  val scalazCore = "org.scalaz" %% "scalaz-core" % scalazVersion
  val runtimeManifest = "com.github.deaktator" %% "scala-runtime-manifest" % "1.0.0"

  val alohaProto = "com.eharmony" % "aloha-proto" % alohaProtoVersion
  val protobuf = "com.google.protobuf" % "protobuf-java" % protobufVersion

  val classMate = "com.fasterxml" % "classmate" % "1.0.0"
  val scopt = "com.github.scopt" %% "scopt" % "3.3.0"
  val twitterUtilCore = "com.twitter" %% "util-core" % "6.27.0"
  val commonsCodec = "commons-codec" % "commons-codec" % commonsCodecVersion
  val commonsIo = "commons-io" % "commons-io" % commonsIoVersion
  val vfs1 = "commons-vfs" % "commons-vfs" % "1.0"  excludeAll(ExclusionRule("commons-logging", "commons-logging"))
  val vfs2 = "org.apache.commons" % "commons-vfs2" % "2.0" excludeAll(ExclusionRule("org.apache.maven.scm", "maven-scm-api"),
                                                                      ExclusionRule("org.apache.maven.scm", "maven-scm-provider-svnexe"))
  val sprayJson = "io.spray" %% "spray-json" % "1.3.1"

  val reflections = "org.reflections" % "reflections" % "0.9.9"
  val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jVersion
  val slf4jLog4j = "org.slf4j" % "slf4j-log4j12" % slf4jVersion


  val mwt = "com.github.multiworldtesting" % "explore-java" % "1.0.0"

  val junit = "junit" % "junit" % "4.11" % "test"
  val junitInterface = "com.novocode" % "junit-interface" % "0.11" % "test"

  val mallet = "cc.mallet" %  "mallet" % "2.0.7" excludeAll(ExclusionRule("junit", "junit"))

  // Just for testing.  Not an Aloha compile dep, so leave them as test scoped.
  val springBeans = "org.springframework" % "spring-beans" % springVersion % "test"
  val springCtxSupport = "org.springframework" % "spring-context-support" % springVersion % "test"
  val springCtx = "org.springframework" % "spring-context" % springVersion % "test"
  val springCore = "org.springframework" % "spring-core" % springVersion % "test"
  val springTest = "org.springframework" % "spring-test" % springVersion % "test"

  val vwJni = "com.github.johnlangford" % "vw-jni" % vwJniVersion

  val h2o = "ai.h2o" % "h2o-core" % h2oVersion excludeAll(ExclusionRule("log4j", "log4j"),
                                                          ExclusionRule("ai.h2o", "reflections"))
  val h2oGenModel = "ai.h2o" % "h2o-genmodel" % h2oVersion excludeAll(ExclusionRule("log4j", "log4j"))

  lazy val coreDeps = Seq(
    alohaProto, classMate, commonsCodec, commonsIo, mwt,
    protobuf, reflections, runtimeManifest, scalazCore, scopt,
    slf4jApi, sprayJson, twitterUtilCore, vfs1, vfs2,

    // Test Deps.
    junit, junitInterface, mallet % "test",
    springBeans, springCore, springCtx, springCtxSupport, springTest
  )

  lazy val vwJniDeps = Seq(
    vwJni, commonsCodec, scopt,
    junit, junitInterface, slf4jLog4j % "test"
  )

  lazy val h2oDeps = Seq(
    h2o, commonsCodec, h2oGenModel, scopt,
    junit, junitInterface, slf4jLog4j % "test"
  )

  // <dependency>
  //     <groupId>com.eharmony</groupId>
  //     <artifactId>aloha-h2o</artifactId>
  //     <version>${project.version}</version>
  //     <exclusions>
  //         <exclusion>
  //             <groupId>com.google.guava</groupId>
  //             <artifactId>guava</artifactId>
  //         </exclusion>
  //     </exclusions>
  // </dependency>

  lazy val cliDeps = Seq(
    slf4jLog4j,
    junit, junitInterface
  )
}
