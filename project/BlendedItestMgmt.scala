import sbt._
import sbt.Keys._
import de.wayofquality.sbt.testlogconfig.TestLogConfig.autoImport._
import blended.sbt.container.BlendedContainerPlugin.autoImport._
import phoenix.ProjectFactory

object BlendedItestMgmt extends ProjectFactory {

  object config extends ProjectSettings {
    override val projectName = "blended.itest.mgmt"
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
      Dependencies.activeMqClient % "test",
      Dependencies.scalatest % "test",
      Dependencies.akkaActor % "test",
      Dependencies.akkaStream % "test",
      Dependencies.akkaStreamTestkit % "test",
      Dependencies.slf4j % "test",
      Dependencies.akkaSlf4j % "test",
      Dependencies.logbackClassic % "test",
      Dependencies.akkaTestkit % "test",
      Dependencies.geronimoJms11Spec % "test",
      Dependencies.dockerJava % "test",
      Dependencies.geronimoJ2eeMgmtSpec % "test",
      Dependencies.sttp % "test",
      Dependencies.lihaoyiPprint % "test"
    )
    override val osgi = false
    override val projectDir = Some("itest/blended.itest.mgmt")

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(

      Test / testlogDefaultLevel := "debug",

      Test / resourceGenerators += Def.task {
        // trigger docker container creation
        (BlendedDockerDemoNode.project / BlendedDockerContainer.createDockerImage).value
        (BlendedDockerDemoMgmt.project / BlendedDockerContainer.createDockerImage).value

        // copy deploymentpack used in a test case
        val packFile = (BlendedDemoNode.project / packageDeploymentPack).value

        // As we want to rename it, we must copy it, BUT
        // it is essential, that we don't copy under the final resources directory,
        // otherwise we end up with an empty dir instead of the zip file
        val resDir = target.value
        resDir.mkdirs()
        val newFileName = {
          val idx = packFile.getName().indexOf((BlendedDemoNode.project / version).value)
          packFile.getName().substring(0, idx) + "deploymentpack.zip"
        }
        val targetFile = resDir / newFileName
        if (targetFile.exists()) {
          IO.delete(targetFile)
        }
        IO.copyFile(packFile, targetFile)
        if (!targetFile.isFile()) sys.error("Copied package is missing: " + targetFile)
        Seq(targetFile)
      }
    )

  }

}
