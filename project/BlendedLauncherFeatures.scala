import sbt._
import sbt.Keys._

object BlendedLauncherFeatures extends ProjectFactory {

  val generateFeatureConfigs = taskKey[Seq[(Feature, File)]]("Generate Feature config files")

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
        val generated = features.map { feature: Feature =>
          val file = new File(featureDir, s"${feature.name}.conf")
          IO.write(file, feature.formatConfig(version.value))
          feature -> file
        }
        generated
      },

      // Trigger file generation to compile step
      Compile / compile := {
        generateFeatureConfigs.value
        (Compile / compile).value
      }

    // Attach feature files as artifacts
    ) ++ (features.flatMap { feature: Feature =>

        val a = Artifact(
          name = projectName,
          `type` = "conf",
          extension = "conf",
          classifier = feature.name
        )

        Seq(
          artifacts += a,
          packagedArtifacts := {
            // trigger generator
            generateFeatureConfigs.value

            val featureDir: File = new File(target.value, "features")
            val file = new File(featureDir, s"${feature.name}.conf")

            packagedArtifacts.value updated (a, file)
          }
        )

      })

  }

  override val project = helper.baseProject

}
