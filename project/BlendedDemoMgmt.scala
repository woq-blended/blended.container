import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Keys._
import sbt._
import BlendedContainer._

import blended.sbt.feature.BlendedFeaturePlugin.autoImport._

object BlendedDemoMgmt extends ProjectFactory {

  private[this] val helper = new BlendedContainer(
    projectName = "blended.demo.mgmt",
    description = "A sample blended management container",
    projectDir = Some("container/blended.demo.mgmt")
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      materializeDebug := false,
      materializeExtraDeps := {
        val file = (BlendedDemoMgmtResources.project / Universal / packageBin).value
        val artifact = (BlendedDemoMgmtResources.project / artifacts).value.filter(a => a.`type` == "zip" && a.extension == "zip").head
        val moduleId: ModuleID = (Blended.blendedOrganization %% (BlendedDemoMgmtResources.project / name).value % Blended.blendedVersion)
        Seq(moduleId.artifacts(artifact) -> file)
      },
      materializeExtraFeatures ++= {
        Seq(
          (BlendedLauncherFeatureBase.project / featureGenerate).value,
          (BlendedLauncherFeatureBaseEquinox.project / featureGenerate).value,
          (BlendedLauncherFeatureBaseFelix.project / featureGenerate).value,
          (BlendedLauncherFeatureCommons.project / featureGenerate).value,
          (BlendedLauncherFeatureJetty.project / featureGenerate).value,
          (BlendedLauncherFeatureSecurity.project / featureGenerate).value,
          (BlendedLauncherFeatureHawtio.project / featureGenerate).value,
          (BlendedLauncherFeatureSpring.project / featureGenerate).value,
          (BlendedLauncherFeatureActivemq.project / featureGenerate).value,
          (BlendedLauncherFeatureMgmtClient.project / featureGenerate).value,
          (BlendedLauncherFeatureMgmtServer.project / featureGenerate).value,
          (BlendedLauncherFeatureAkkaHttp.project / featureGenerate).value,
          (BlendedLauncherFeaturePersistence.project / featureGenerate).value,
          (BlendedLauncherFeatureStreams.project / featureGenerate).value,
          (BlendedLauncherFeatureSsl.project / featureGenerate).value
        )
      }
    )

  }

  override val project = helper.baseProject

}
