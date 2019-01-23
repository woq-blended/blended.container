import sbt.Keys._
import sbt._

object BlendedItestNode extends ProjectFactory {

  private[this] val helper = new ProjectSettings(
    projectName = "blended.itest.node",
    description = "A sample integration test using docker to fire up the container(s) under test, execute the test suite and shutdown the container(s) afterwards",
    deps = Seq(
    Blended.utilLogging,
    Dependencies.activeMqClient % "test",
    Blended.itestSupport % "test",
    Blended.streams % "test",
    Blended.streamsTestsupport % "test",
    Blended.jmsUtils % "test",
    Blended.testSupport % "test",
    Blended.util % "test",
    Blended.akka % "test"
  ) //.map(_.intransitive())
      ++ Seq(
      Dependencies.scalatest % "test",
      Dependencies.akkaActor % "test",
      Dependencies.akkaStream % "test",
      Dependencies.slf4j % "test",
      Dependencies.akkaSlf4j % "test",
      Dependencies.logbackClassic % "test",
      Dependencies.akkaTestkit % "test",
      Dependencies.geronimoJms11Spec % "test",
      Dependencies.dockerJava % "test",
      Dependencies.geronimoJ2eeMgmtSpec % "test"
    ),
    osgi = false,
    projectDir = Some("itest/blended.itest.node")
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      Test / dependencyClasspath := {
        // trigger docker containers
        (BlendedDockerDemoNode.project / BlendedDockerContainer.createDockerImage).value
        (BlendedDockerDemoApacheds.project / BlendedDockerContainer.createDockerImage).value

        (Test / dependencyClasspath).value
      },
      javaOptions += s"-Ddocker.host=${System.getProperty("docker.host", "localhost")}",
      javaOptions += s"-Ddocker.port=${System.getProperty("docker.port", "4243")}"
    )

  }

  override val project = helper.baseProject

}
