addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

// Trying to get test scoped protobuf compilation working but
// https://github.com/sbt/sbt-protobuf/issues/39 shows there are
// problems compiling protos in src/test/proto.  It adds them to
// the jar.  This isn't really OK.  Instead, we might have to
// just do it will shell scripts.  See here:
//
// http://stackoverflow.com/a/39621471
//
// addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.5.3")

// This doesn't need to be there by default.  Just for testing.
//
// addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// To do resource filtering.
addSbtPlugin("org.clapper" % "sbt-editsource" % "0.7.0")

// For microsite creation.
// NOTE: up to and including, v0.7.2, sbt-microsites appears to use
// org.tpolecat:tut-plugin:0.5.3 that contains a State monad implementation
// that is not stack-safe.  0.5.5 is stack-safe but trying to include it
// with an older version of sbt-microsites causes sbt-microsites to have
// ClassNotFound errors.  I think that newer versions of sbt-microsites
// use 0.6.1, but that is only with SBT 1.0+.  See
//
//   https://github.com/47deg/sbt-microsites/blob/f91a939a63e802080960ebbcc07dc6f14e70de02/project/ProjectPlugin.scala#L45
//
// Until Aloha is ready for SBT 1.0+, we roll the dice and bump -Xss to a
// high number (8M) and the build will likely fail intermittently.
addSbtPlugin("com.47deg"  % "sbt-microsites" % "0.5.3")

// For unified scaladoc
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.0")
