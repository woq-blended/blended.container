import sbt._
import sbt.Keys._

object BlendedLauncherFeatures extends ProjectFactory {

  val generateFeatureConfigs = taskKey[Seq[File]]("Generate Feature config files")

  private[this] val helper = new ProjectSettings(
    projectName = "blended.launcher.features",
    description = "The prepackaged features for blended.",
    features = Feature.allFeatures,
    osgi = false
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(

      // Write feature config files
      generateFeatureConfigs := {
        val featureDir: File = new File(target.value, "features")
        val files = features.map { feature: Feature =>
          val file = new File(featureDir, s"${feature.name}.conf")
          IO.write(file, feature.formatConfig(version.value))
          file
        }
        files
      }

    )


  }


  override val project = helper.baseProject

}
