import org.sonatype.maven.polyglot.scala.model._
import scala.collection.immutable.Seq

//#include ../../blended.build/build-versions.scala
//#include ../../blended.build/build-dependencies.scala
//#include ../../blended.build/build-plugins.scala
//#include ../../blended.build/build-common.scala

BlendedDockerContainer(
  gav = Blended.dockerDemoNode,
  image = Dependency(
    gav = Blended.demoNode,
    `type` = "tar.gz",
    classifier = "full-nojre",
    scope = "provided"
  ),
  folder = "node",
  ports = List(1099,1883,1884,1885,1886,8181,8849,9191,9995,9996)
)
