import sbt._


object BlendedDockerDemoMgmt extends ProjectFactory {

  val helper = new BlendedDockerContainer(
    projectName = "blended.docker.demo.mgmt",
    imageTag = "atooni/blended_mgmt",
    publish = false,
    projectDir = Some("docker/blended.docker.demo.mgmt"),
    ports = List(1099,1883,9191,8849,9995,9996),
    folder = "mgmt",
    // TODO: no supported yet!
    overlays = List()
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      BlendedDockerContainer.containerImage := s"blended.demo.mgmt-${Blended.blendedVersion}" -> (BlendedDemoMgmt.project / BlendedContainer.packageFullNoJreTarGz).value
    )

  }

  override val project = helper.baseProject
}