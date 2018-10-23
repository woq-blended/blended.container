import org.sonatype.maven.polyglot.scala.model._
import scala.collection.immutable.Seq

//#include ../../blended.build/build-versions.scala
//#include ../../blended.build/build-dependencies.scala
//#include ../../blended.build/build-plugins.scala
//#include ../../blended.build/build-common.scala

BlendedModel(
  gav = Blended.itestMgmt,
  packaging = "jar",
  description = "A sample integration test using docker to fire up the container(s) under test, execute the test suite and shutdown the container(s) afterwards.",
  dependencies = Seq(
    Deps.scalaLib,
    Blended.utilLogging,
    Blended.dockerDemoNode % "provided",
    Blended.dockerDemoMgmt % "provided",
    Deps.activeMqClient % "test",
    Blended.itestSupport % "test",
    Deps.scalaTest % "test",
    Deps.slf4j % "test",
    Deps.akkaSlf4j % "test",
    Deps.logbackCore % "test",
    Deps.logbackClassic % "test",
    Deps.sttp % "test"
  ),
  plugins = Seq(
    scalaCompilerPlugin,
    scalatestMavenPlugin
  )
)
