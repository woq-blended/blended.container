import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Keys._
import sbt._
import BlendedContainer._

import blended.sbt.feature.BlendedFeaturePlugin.autoImport._

object BlendedDemoNode extends ProjectFactory {

  val featureRepos = settingKey[Seq[Project]]("Projects holding feature defs")
  val featureRepoCoords = taskKey[Seq[Project]]("")

  private[this] val helper = new BlendedContainer(
    projectName = "blended.demo.node",
    description = "A sample container with some routes and Mgmt client functions",
    projectDir = Some("container/blended.demo.node")
//    features = Seq(
//      BlendedFeatures.blendedBaseFelix,
//      BlendedFeatures.blendedBaseEquinox,
//      BlendedFeatures.blendedBase,
//      BlendedFeatures.blendedCommons,
//      BlendedFeatures.blendedSsl,
//      BlendedFeatures.blendedJetty,
//      BlendedFeatures.blendedHawtio,
//      BlendedFeatures.blendedSpring,
//      BlendedFeatures.blendedActivemq,
//      BlendedFeatures.blendedCamel,
//      BlendedFeatures.blendedSecurity,
//      BlendedFeatures.blendedMgmtClient,
//      BlendedFeatures.blendedAkkaHttp,
//      BlendedFeatures.blendedPersistence,
//      BlendedFeatures.blendedStreams,
//      BlendedFeatures.blendedSamples
//    )
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      materializeDebug := true,
      materializeExtraDeps += {
        val file = (BlendedDemoNodeResources.project / Universal / packageBin).value
        val artifact = (BlendedDemoNodeResources.project / artifacts).value.filter(a => a.`type` == "zip" && a.extension == "zip").head
        val moduleId: ModuleID = (Blended.blendedOrganization %% (BlendedDemoNodeResources.project / name).value % Blended.blendedVersion)
        moduleId.artifacts(artifact) -> file
      },
      materializeExtraFeatures ++= {
        Seq(
          (BlendedLauncherFeatureBase.project / featureGenerate).value,
          (BlendedLauncherFeatureBaseEquinox.project / featureGenerate).value,
          (BlendedLauncherFeatureBaseFelix.project / featureGenerate).value,
          (BlendedLauncherFeatureCommons.project / featureGenerate).value,
          (BlendedLauncherFeatureSsl.project / featureGenerate).value,
          (BlendedLauncherFeatureJetty.project / featureGenerate).value,
          (BlendedLauncherFeatureHawtio.project / featureGenerate).value,
          (BlendedLauncherFeatureSpring.project / featureGenerate).value,
          (BlendedLauncherFeatureActivemq.project / featureGenerate).value,
          (BlendedLauncherFeatureCamel.project / featureGenerate).value,
          (BlendedLauncherFeatureSecurity.project / featureGenerate).value,
          (BlendedLauncherFeatureMgmtClient.project / featureGenerate).value,
          (BlendedLauncherFeatureAkkaHttp.project / featureGenerate).value,
          (BlendedLauncherFeaturePersistence.project / featureGenerate).value,
          (BlendedLauncherFeatureStreams.project / featureGenerate).value,
          (BlendedLauncherFeatureSamples.project / featureGenerate).value
        )
      }
    )

  }

  override val project = helper.baseProject

}
