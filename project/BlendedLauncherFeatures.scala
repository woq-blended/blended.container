import sbt._
import sbt.Keys._

import blended.sbt.feature._
import blended.sbt.feature.BlendedFeaturePlugin.autoImport._

class FeatureProjectCreator(
  feature: Feature
) extends ProjectCreator {
  override def projectName: String = feature.name
  override def projectDir: Option[String] = Some(s"blended.launcher.feature/${feature.name}")
  override def plugins: Seq[AutoPlugin] = super.plugins ++ Seq(BlendedFeaturePlugin)
  override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
    featureConfig := feature
  )
}

object BlendedLauncherFeatureActivemq extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedActivemq)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureAkkaHttp extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedAkkaHttp)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureAkkaHttpModule extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedAkkaHttpModules)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureBase extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedBase)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureBaseEquinox extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedBaseEquinox)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureBaseFelix extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedBaseFelix)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureCamel extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedCamel)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureCommons extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedCommons)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureHawtio extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedHawtio)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureJetty extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedJetty)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureMgmtClient extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedMgmtClient)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureMgmtServer extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedMgmtServer)
  override val project = helper.baseProject
}

object BlendedLauncherFeaturePersistence extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedPersistence)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureSamples extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedSamples)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureSecurity extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedSecurity)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureSpring extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedSpring)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureStreams extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedStreams)
  override val project = helper.baseProject
}

object BlendedLauncherFeatureSsl extends ProjectFactory {
  private[this] val helper = new FeatureProjectCreator(feature = BlendedFeatures.blendedSsl)
  override val project = helper.baseProject
}

//object BlendedLauncherFeatures extends ProjectFactory {
//
//  val generateFeatureConfigs = taskKey[Seq[(Feature, File)]]("Generate Feature config files")
//
//  private[this] val helper = new ProjectSettings(
//    projectName = "blended.launcher.features",
//    description = "The prepackaged features for blended.",
//    features = BlendedFeatures.allFeatures,
//    osgi = false
//  ) {
//
//
//    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
//
//      // Write feature config files
//      generateFeatureConfigs := {
//        val featureDir: File = new File(target.value, "features")
//        val generated = features.map { feature: Feature =>
//          val file = new File(featureDir, s"${feature.name}.conf")
//          IO.write(file, feature.formatConfig(version.value))
//          feature -> file
//        }
//        generated
//      },
//
//      // Trigger file generation to compile step
//      Compile / compile := {
//        generateFeatureConfigs.value
//        (Compile / compile).value
//      }
//
//      // Attach feature files as artifacts
//    ) ++ (features.flatMap { feature: Feature =>
//
//      val a = Artifact(
//        name = projectName,
//        `type` = "conf",
//        extension = "conf",
//        classifier = feature.name
//      )
//
//      Seq(
//        artifacts += a,
//        packagedArtifacts := {
//          // trigger generator
//          generateFeatureConfigs.value
//
//          val featureDir: File = new File(target.value, "features")
//          val file = new File(featureDir, s"${feature.name}.conf")
//
//          packagedArtifacts.value updated(a, file)
//        }
//      )
//
//    })
//
//  }
//
//  override val project = helper.baseProject
//
//}
