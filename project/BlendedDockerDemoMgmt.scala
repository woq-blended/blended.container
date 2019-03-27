import sbt._
import sbt.Keys._
import blended.sbt.container.BlendedContainerPlugin.autoImport._
import phoenix.ProjectFactory

object BlendedDockerDemoMgmt extends ProjectFactory {

  object config extends BlendedDockerContainer {
    override val projectName = "blended.docker.demo.mgmt"
    override val imageTag = "atooni/blended_mgmt"
    override val publish = false
    override val projectDir = Some("docker/blended.docker.demo.mgmt")
    override val ports = List(1099, 1883, 9191, 8849, 9995, 9996)
    override val folder = "mgmt"
    // TODO: no supported yet!
    //    overlays = List()

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      BlendedDockerContainer.containerImage :=
        s"blended.demo.mgmt_${scalaBinaryVersion.value}-${Blended.blendedVersion}" ->
        (BlendedDemoMgmt.project / packageFullNoJreTarGz).value
    )

  }
}