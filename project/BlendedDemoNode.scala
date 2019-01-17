import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Keys._
import sbt._

object BlendedDemoNode extends ProjectFactory {

  private[this] val helper = new BlendedContainer(
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
    )
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      materializeDebug := false,
      materializeExtraDeps := {
        val file = (BlendedDemoNodeResources.project / Universal / packageBin).value
        val artifact = (BlendedDemoNodeResources.project / artifacts).value.filter(a => a.`type` == "zip" && a.extension == "zip").head
        val moduleId: ModuleID = (Blended.blendedOrganization %% (BlendedDemoNodeResources.project / name).value % Blended.blendedVersion)
        Seq(moduleId.artifacts(artifact) -> file)
      }
    )

  }

  override val project = helper.baseProject

}
