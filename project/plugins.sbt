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
// It's important to use 0.7.3+ because it includes org.tpolecat:tut-plugin:0.5.5 which
// introduced a stack-safe version of the State monad.  This means that tut should no
// longer stack overflow on long files.  Consequently, no -Xss magic is needed anymore.
addSbtPlugin("com.47deg"  % "sbt-microsites" % "0.7.3")

// For unified scaladoc
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.0")
