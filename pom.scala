import org.sonatype.maven.polyglot.scala.model._
import scala.collection.immutable.Seq

//#include blended.build/build-versions.scala
//#include blended.build/build-dependencies.scala
//#include blended.build/build-plugins.scala
//#include blended.build/build-common.scala

BlendedModel(
  gav = Blended.blended("blended.container.reactor"),
  packaging = "pom",
  description = "A collection of bundles to develop OSGi application on top of Scala and Akka and Camel.",

  modules = Seq(
    "container"
  ),
  profiles = Seq(
    Profile(
      id = "itest",
      modules = Seq(
        "itest"
      )
    ),
    Profile(
      id = "docker",
      modules = Seq(
        "docker"
      )
    )
  )
)
