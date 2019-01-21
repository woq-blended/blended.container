import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Keys._
import sbt._
import BlendedContainer._

object BlendedDemoMgmt extends ProjectFactory {

  private[this] val helper = new BlendedContainer(
    projectName = "blended.demo.mgmt",
    description = "A sample blended management container",
    projectDir = Some("container/blended.demo.mgmt"),
    features = Seq(
      Feature.blendedBaseFelix,
      Feature.blendedBaseEquinox,
      Feature.blendedBase,
      Feature.blendedCommons,
      Feature.blendedJetty,
      Feature.blendedSecurity,
      Feature.blendedHawtio,
      Feature.blendedSpring,
      Feature.blendedActivemq,
      Feature.blendedCamel,
      Feature.blendedSamples,
      Feature.blendedMgmtClient,
      Feature.blendedMgmtServer,
      Feature.blendedAkkaHttp,
      Feature.blendedPersistence,
      Feature.blendedAkkaHttpModules,
      Feature.blendedStreams,
      Feature.blendedSsl
    )
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      materializeDebug := false,
      materializeExtraDeps := {
        val file = (BlendedDemoMgmtResources.project / Universal / packageBin).value
        val artifact = (BlendedDemoMgmtResources.project / artifacts).value.filter(a => a.`type` == "zip" && a.extension == "zip").head
        val moduleId: ModuleID = (Blended.blendedOrganization %% (BlendedDemoMgmtResources.project / name).value % Blended.blendedVersion)
        Seq(moduleId.artifacts(artifact) -> file)
      }
    )

  }

  override val project = helper.baseProject

}
