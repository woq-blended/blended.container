import blended.sbt.container.BlendedContainerPlugin.autoImport._
import blended.sbt.feature.BlendedFeaturePlugin.autoImport._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import phoenix.ProjectFactory
import sbt.Keys._
import sbt._

object BlendedDemoNode extends ProjectFactory {

  val featureRepos = settingKey[Seq[Project]]("Projects holding feature defs")
  val featureRepoCoords = taskKey[Seq[Project]]("")

  object config extends BlendedContainer {

    override val projectName = "blended.demo.node"
    override val description = "A sample container with some routes and Mgmt client functions"
    override val projectDir = Some("container/blended.demo.node")

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
      blendedVersion := Blended.blendedVersion,
      materializeDebug := true,
      materializeExtraDeps += {
        val file = (BlendedDemoNodeResources.project / Universal / packageBin).value
        val artifact = (BlendedDemoNodeResources.project / artifacts).value.filter(a => a.`type` == "zip" && a.extension == "zip").head
        val moduleId: ModuleID = Blended.blendedOrganization %% (BlendedDemoNodeResources.project / name).value % Blended.blendedVersion
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
          (BlendedLauncherFeatureSecurity.project / featureGenerate).value,
          (BlendedLauncherFeatureMgmtClient.project / featureGenerate).value,
          (BlendedLauncherFeatureAkkaHttp.project / featureGenerate).value,
          (BlendedLauncherFeaturePersistence.project / featureGenerate).value,
          (BlendedLauncherFeatureStreams.project / featureGenerate).value,
          (BlendedLauncherFeatureSamples.project / featureGenerate).value,
          (BlendedLauncherFeatureLogin.project / featureGenerate).value
        )
      }
    )

  }
}
