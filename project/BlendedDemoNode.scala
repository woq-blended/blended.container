import sbt._

object BlendedDemoNode extends ProjectFactory {

  private[this] val helper = new ProjectSettings(
    projectName = "blended.demo.node",
    description = "A sample container with some routes and Mgmt client functions",
    projectDir = Some("container/blended.demo.node"),
    features = Seq(
      Features.blendedBaseFelix,
      Features.blendedBaseEquinox,
      Features.blendedBase,
      Features.blendedCommons,
      Features.blendedSsl,
      Features.blendedJetty,
      Features.blendedHawtio,
      Features.blendedSpring,
      Features.blendedActivemq,
      Features.blendedCamel,
      Features.blendedSecurity,
      Features.blendedMgmtClient,
      Features.blendedAkkaHttp,
      Features.blendedPersistence,
      Features.blendedStreams,
      Features.blendedSamples
    ),
    deps = Seq(
      Dependencies.typesafeConfig,
      Dependencies.domino
    )
  )

  override val project = helper.baseProject.dependsOn()

}
