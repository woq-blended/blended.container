package blended.sbt.feature

import sbt._
import sbt.Keys._

object BlendedFeaturePlugin extends AutoPlugin {

  object autoImport {
    val featureConfig = settingKey[Feature]("The Feature Config")
    val featureArtifact = settingKey[Artifact]("The feature sbt artifact")

    val featureGenerate = taskKey[(Feature, File)]("Generate the Feature config file")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(

    // Write feature config files
    featureGenerate := {
      val feature = featureConfig.value
      val featureDir: File = new File(target.value, "features")
      val file = new File(featureDir, s"${feature.name}.conf")
      IO.write(file, feature.formatConfig(version.value))

      feature -> file
    },

    // Trigger file generation to compile step
    Compile / compile := {
      featureGenerate.value
      (Compile / compile).value
    },

    featureArtifact := {
      val config = featureConfig.value
      Artifact(
        name = moduleName.value,
        `type` = "conf",
        extension = "conf"
      )
    },

    packageBin := featureGenerate.value._2,
    Keys.`package` := packageBin.value,

    artifacts := Seq(featureArtifact.value),

    packagedArtifacts := {
      //      packagedArtifacts.value updated(featureArtifact.value, featureGenerate.value._2)
      Map(featureArtifact.value -> featureGenerate.value._2)
    }
  )

}
