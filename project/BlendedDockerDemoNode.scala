import sbt._
import sbt.Keys._
import blended.sbt.container.BlendedContainerPlugin.autoImport._
import phoenix.ProjectFactory

object BlendedDockerDemoNode extends ProjectFactory {

  object config extends BlendedDockerContainer {
    override val projectName = "blended.docker.demo.node"
    override val imageTag = "atooni/blended_node"
    override val publish = false
    override val projectDir = Some("docker/blended.docker.demo.node")
    override val ports = List(1099, 1883, 1884, 1885, 1886, 8181, 8849, 9191, 9995, 9996)
    override val folder = "node"
    // TODO: no supported yet!
    //    overlays = List()

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      BlendedDockerContainer.containerImage :=
        s"blended.demo.node_${scalaBinaryVersion.value}-${Blended.blendedVersion}" ->
        (BlendedDemoNode.project / packageFullNoJreTarGz).value
    )

  }
}