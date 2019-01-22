import sbt._


object BlendedDockerDemoNode extends ProjectFactory {

  val helper = new BlendedDockerContainer(
    projectName = "blended.docker.demo.node",
    imageTag = "atooni/blended_node",
    publish = false,
    projectDir = Some("docker/blended.docker.demo.node"),
    ports = List(1099, 1883, 1884, 1885, 1886, 8181, 8849, 9191, 9995, 9996),
    folder = "node",
    // TODO: no supported yet!
    overlays = List()
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      BlendedDockerContainer.containerImage := s"blended.demo.node-${Blended.blendedVersion}" -> (BlendedDemoNode.project / BlendedContainer.packageFullNoJreTarGz).value
    )

  }

  override val project = helper.baseProject
}