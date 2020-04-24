import sbt._
import sbt.Keys._
import phoenix.ProjectFactory
import blended.sbt.container.BlendedContainerPlugin.{autoImport => BC}
import blended.sbt.dockercontainer.BlendedDockerContainerPlugin.{autoImport => DC}

object BlendedDockerDemoNode extends ProjectFactory {

  object config extends BlendedDockerContainer {
    override val projectName = "blended.docker.demo.node"
    override val imageTag = "atooni/blended_node"
    override val publish = false
    override val projectDir = Some("docker/blended.docker.demo.node")
    override val ports = List(1099, 1883, 1884, 1885, 1886, 8181, 8849, 9191, 9995, 9996)
    override val folder = "node"

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      DC.baseImage := "atooni/zulu-8-alpine:1.0.1",

      DC.containerImage :=
        s"blended.demo.node_${scalaBinaryVersion.value}-${Blended.blendedVersion}" ->
        (BlendedDemoNode.project / BC.packageFullNoJreTarGz).value
    )
  }
}