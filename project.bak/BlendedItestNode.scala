import sbt.Keys._
import sbt._
import phoenix.ProjectFactory
import blended.sbt.dockercontainer.BlendedDockerContainerPlugin.{autoImport => DC}
import blended.sbt.container.BlendedContainerPlugin.{autoImport => BC}
import de.wayofquality.sbt.testlogconfig.TestLogConfig.{autoImport => TL}

object BlendedItestNode extends ProjectFactory {

  import ProjectDependencies._

  object config extends ProjectSettings {
    override val projectName = "blended.itest.node"
    override val description = "A sample integration test using docker to fire up the container(s) under test, execute the test suite and shutdown the container(s) afterwards"
    override val deps = Seq(
      Blended.utilLogging,
      activeMqClient % "test",
      Blended.itestSupport % "test",
      Blended.streams % "test",
      Blended.streamsTestsupport % "test",
      Blended.jmsUtils % "test",
      Blended.testSupport % "test",
      Blended.util % "test",
      Blended.akka % "test",
      Blended.securitySsl % "test",
      scalatest % "test",
      akkaActor % "test",
      akkaStream % "test",
      slf4j % "test",
      akkaSlf4j % "test",
      logbackClassic % "test",
      akkaTestkit % "test",
      geronimoJms11Spec % "test",
      dockerJava % "test",
      geronimoJ2eeMgmtSpec % "test"
    )

    override val osgi = false
    override val projectDir = Some("itest/blended.itest.node")

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(

      Test / TL.testlogDefaultLevel := "debug",

      Test / resourceGenerators += Def.task {
        // trigger docker containers
        (BlendedDockerDemoNode.project / DC.createDockerImage).value
        //(BlendedDockerDemoApacheds.project / DC.createDockerImage).value
        Seq()
      }
    )

  }

}
