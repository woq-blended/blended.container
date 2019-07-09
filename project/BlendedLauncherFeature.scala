import sbt._
import sbt.Keys._
import blended.sbt.feature._
import blended.sbt.feature.BlendedFeaturePlugin.autoImport._
import phoenix.ProjectFactory

object BlendedLauncherFeature extends ProjectFactory {
  object config extends CommonSettings {
    override val projectName = "blended.launcher.feature"
    override val aggregate: Seq[ProjectReference] = Seq(
      BlendedLauncherFeatureActivemq.project,
      BlendedLauncherFeatureAkkaHttp.project,
      BlendedLauncherFeatureAkkaHttpModules.project,
      BlendedLauncherFeatureBase.project,
      BlendedLauncherFeatureBaseEquinox.project,
      BlendedLauncherFeatureBaseFelix.project,
      BlendedLauncherFeatureCamel.project,
      BlendedLauncherFeatureCommons.project,
      BlendedLauncherFeatureHawtio.project,
      BlendedLauncherFeatureJetty.project,
      BlendedLauncherFeatureJolokia.project,
      BlendedLauncherFeatureMgmtClient.project,
      BlendedLauncherFeatureMgmtServer.project,
      BlendedLauncherFeaturePersistence.project,
      BlendedLauncherFeatureSamples.project,
      BlendedLauncherFeatureSecurity.project,
      BlendedLauncherFeatureSpring.project,
      BlendedLauncherFeatureSsl.project,
      BlendedLauncherFeatureStreams.project
    )
  }
}

trait FeatureProjectConfig extends CommonSettings {
  def feature: Feature
  override def projectName: String = feature.name
  override def projectDir: Option[String] = Some(s"blended.launcher.feature/${feature.name}")
  override def plugins: Seq[AutoPlugin] = super.plugins ++ Seq(BlendedFeaturePlugin)
  override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
    featureConfig := feature
  )
}

object BlendedLauncherFeatureActivemq extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedActivemq
  }
}

object BlendedLauncherFeatureAkkaHttp extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedAkkaHttp
  }
}

object BlendedLauncherFeatureAkkaHttpModules extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedAkkaHttpModules
  }
}

object BlendedLauncherFeatureBase extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedBase
  }
}

object BlendedLauncherFeatureBaseEquinox extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedBaseEquinox
  }
}

object BlendedLauncherFeatureBaseFelix extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedBaseFelix
  }
}

object BlendedLauncherFeatureCamel extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedCamel
  }
}

object BlendedLauncherFeatureCommons extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedCommons
  }
}

object BlendedLauncherFeatureHawtio extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedHawtio
  }
}

object BlendedLauncherFeatureJetty extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedJetty
  }
}

object BlendedLauncherFeatureJolokia extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedJolokia
  }
}

object BlendedLauncherFeatureMgmtClient extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedMgmtClient
  }
}

object BlendedLauncherFeatureMgmtServer extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedMgmtServer
  }
}

object BlendedLauncherFeaturePersistence extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedPersistence
  }
}

object BlendedLauncherFeatureSamples extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedSamples
  }
}

object BlendedLauncherFeatureSecurity extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedSecurity
  }
}

object BlendedLauncherFeatureSpring extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedSpring
  }
}

object BlendedLauncherFeatureStreams extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedStreams
  }
}

object BlendedLauncherFeatureSsl extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedSsl
  }
}

object BlendedLauncherFeatureLogin extends ProjectFactory {
  object config extends FeatureProjectConfig {
    override val feature = BlendedFeatures.blendedLogin
  }
}
