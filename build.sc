import mill._
import mill.scalalib._

import $file.feature_support
import feature_support.FeatureBundle

import $file.build_deps
import build_deps.Deps

import mill.scalalib.publish._

///** Project directory. */
val baseDir: os.Path = build.millSourcePath

import $file.blended_deps
import blended_deps.BlendedDependencies

trait BlendedPublishModule extends PublishModule {
  def description: String = "Blended module ${blendedModule}"
  override def publishVersion = T { blended.version() }
  override def pomSettings: T[PomSettings] = T {
    PomSettings(
      description = description,
      organization = "de.wayofquality.blended",
      url = "https://github.com/woq-blended/blended.container",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("woq-blended", "blended.container"),
      developers = Seq(
        Developer("atooni", "Andreas Gies", "https://github.com/atooni"),
        Developer("lefou", "Tobias Roeser", "https://github.com/lefou")
      )
    )
  }
}

trait BlendedFeatureModule extends BlendedPublishModule {

  def featureBundles : Seq[FeatureBundle] = Seq.empty

  def featureConf : T[PathRef] = T {

    val bundleConf : String = featureBundles
      .map(_.formatConfig)
      .mkString(",\n")

    val confDir = T.dest / "featureConf"
    os.makeDir.all(confDir)

    val content =
      s"""{
         |  name = "${artifactName()}"
         |  version = "${publishVersion()}"
         |  bundles = [
         |""".stripMargin + bundleConf +
         """
          |  ]
          |}""".stripMargin

    os.write(confDir / "feature.conf", content)

    PathRef(confDir)
  }
}

object blended extends Module {

  def version = T.input {
    os.read(baseDir / "version.txt").trim()
  }

  object launcher extends Module {
    object feature extends Module {
      object base extends Module {
        object felix extends BlendedFeatureModule {
          override def featureBundles = Seq(
            FeatureBundle(Deps.felixFramework, 0, true),
            FeatureBundle(Deps.orgOsgiCompendium, 1, true),
            FeatureBundle(Deps.jline, 1, false),
            FeatureBundle(Deps.jlineBuiltins, 1, false),
            FeatureBundle(Deps.felixGogoJline, 1, true),
            FeatureBundle(Deps.felixGogoRuntime, 1, true),
            FeatureBundle(Deps.felixGogoShell, 1, true),
            FeatureBundle(Deps.felixGogoCommand, 1, true),
            FeatureBundle(Deps.felixShellRemote, 1, true)
          )
        }
      }
    }
  }
}

