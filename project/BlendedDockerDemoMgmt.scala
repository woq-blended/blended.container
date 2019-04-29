import sbt._
import sbt.Keys._
import phoenix.ProjectFactory
import blended.sbt.dockercontainer.BlendedDockerContainerPlugin.{autoImport => DC}
import blended.sbt.container.BlendedContainerPlugin.{autoImport => BC}

object BlendedDockerDemoMgmt extends ProjectFactory {

  object config extends BlendedDockerContainer {
    override val projectName = "blended.docker.demo.mgmt"
    override val imageTag = "atooni/blended_mgmt"
    override val publish = false
    override val projectDir = Some("docker/blended.docker.demo.mgmt")
    override val ports = List(1099, 1883, 9191, 8849, 9995, 9996)
    override val folder = "mgmt"

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      DC.containerImage :=
        s"blended.demo.mgmt_${scalaBinaryVersion.value}-${Blended.blendedVersion}" ->
        (BlendedDemoMgmt.project / BC.packageFullNoJreTarGz).value
    )

  }
}