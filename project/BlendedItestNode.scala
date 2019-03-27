import sbt.Keys._
import sbt._
import de.wayofquality.sbt.testlogconfig.TestLogConfig.autoImport._
import phoenix.ProjectFactory

object BlendedItestNode extends ProjectFactory {

  object config extends ProjectSettings {
    override val projectName = "blended.itest.node"
    override val description = "A sample integration test using docker to fire up the container(s) under test, execute the test suite and shutdown the container(s) afterwards"
    override val deps = Seq(
      Blended.utilLogging,
      Dependencies.activeMqClient % "test",
      Blended.itestSupport % "test",
      Blended.streams % "test",
      Blended.streamsTestsupport % "test",
      Blended.jmsUtils % "test",
      Blended.testSupport % "test",
      Blended.util % "test",
      Blended.akka % "test",
      Blended.securitySsl % "test",
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
    )

    override val osgi = false
    override val projectDir = Some("itest/blended.itest.node")

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(

      Test / testlogDefaultLevel := "debug",

      Test / resourceGenerators += Def.task {
        // trigger docker containers
        (BlendedDockerDemoNode.project / BlendedDockerContainer.createDockerImage).value
        (BlendedDockerDemoApacheds.project / BlendedDockerContainer.createDockerImage).value
        Seq()
      }
    )

  }

}
