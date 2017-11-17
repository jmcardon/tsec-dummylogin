val Http4sVersion = "0.18.0-M5"
val Specs2Version = "4.0.0"
val LogbackVersion = "1.2.3"
val tsecV = "0.0.1-M5"

lazy val root = (project in file("."))
  .settings(
    organization := "io.github.jmcardon",
    name := "tsec-login",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.4",
    resolvers += "jmcardon at bintray" at "https://dl.bintray.com/jmcardon/tsec",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.specs2" %% "specs2-core" % Specs2Version % "test",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "io.github.jmcardon" %% "tsec-http4s" % tsecV
    ),
    scalacOptions := Seq(
      "-unchecked",
      "-feature",
      "-deprecation",
      "-encoding",
      "utf8",
      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
      "-Ypartial-unification",
      "-language:higherKinds",
      "-language:implicitConversions"
    )
  )
