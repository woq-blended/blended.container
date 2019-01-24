import sbt.Keys._
import sbt._

object BlendedItestMgmt extends ProjectFactory {

  private[this] val helper = new ProjectSettings(
    projectName = "blended.itest.mgmt",
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
      Dependencies.activeMqClient % "test",
      Dependencies.scalatest % "test",
      Dependencies.akkaActor % "test",
      Dependencies.akkaStream % "test",
      Dependencies.slf4j % "test",
      Dependencies.akkaSlf4j % "test",
      Dependencies.logbackClassic % "test",
      Dependencies.akkaTestkit % "test",
      Dependencies.geronimoJms11Spec % "test",
      Dependencies.dockerJava % "test",
      Dependencies.geronimoJ2eeMgmtSpec % "test",
      Dependencies.sttp % "test",
      Dependencies.lihaoyiPprint % "test"
    ),
    osgi = false,
    projectDir = Some("itest/blended.itest.mgmt")
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(

      Test / resourceGenerators += Def.task {
        // trigger docker container creation
        (BlendedDockerDemoNode.project / BlendedDockerContainer.createDockerImage).value
        (BlendedDockerDemoMgmt.project / BlendedDockerContainer.createDockerImage).value

        // copy deploymentpack used in a test case
        val packFile = (BlendedDemoNode.project / BlendedContainer.packageDeploymentPack).value
        val resDir = (Test / classDirectory).value
        resDir.mkdirs()
        val target = resDir / packFile.getName()
        if(target.exists()) {
          target.delete()
        }
        IO.copyFile(packFile, target)
        Seq(target)
      }
    )

  }

  override val project = helper.baseProject

}
