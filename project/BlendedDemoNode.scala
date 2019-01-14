import sbt._
import sbt.Keys._

object BlendedDemoNode extends ProjectFactory {

  private[this] val helper = new ProjectSettings(
    projectName = "blended.demo.node",
    description = "A sample container with some routes and Mgmt client functions",
    projectDir = Some("container/blended.demo.node"),
    features = Seq(
      Feature.blendedBaseFelix,
      Feature.blendedBaseEquinox,
      Feature.blendedBase,
      Feature.blendedCommons,
      Feature.blendedSsl,
      Feature.blendedJetty,
      Feature.blendedHawtio,
      Feature.blendedSpring,
      Feature.blendedActivemq,
      Feature.blendedCamel,
      Feature.blendedSecurity,
      Feature.blendedMgmtClient,
      Feature.blendedAkkaHttp,
      Feature.blendedPersistence,
      Feature.blendedStreams,
      Feature.blendedSamples
    ),
    deps = Seq(
      Dependencies.typesafeConfig,
      Dependencies.domino
    )
  )

  override val project = helper.baseProject

}
