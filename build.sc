import mill._
import mill.scalalib._
import $file.feature_support
import feature_support.FeatureBundle
import $file.build_deps
import build_deps.Deps
import mill.scalalib.PublishModule.ExtraPublish
import mill.scalalib.publish._

///** Project directory. */
val baseDir: os.Path = build.millSourcePath

import $file.blended_deps
import blended_deps.BlendedDeps

trait BlendedModule extends ScalaModule {
  def blendedModule: String = millModuleSegments.parts.mkString(".")
  override def artifactName: T[String] = blendedModule
  def scalaVersion = Deps.scalaVersion
  val scalaBinVersion = T {scalaVersion().split("[.]").take(2).mkString(".") }
}

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

trait BlendedFeatureModule extends BlendedModule with BlendedPublishModule {

  override def artifactName = T { blendedModule }

  override def extraPublish = T { super.extraPublish() ++ Seq(
    ExtraPublish(featureConf(), "confs", ".conf")
  )}

  def featureBundles : T[Seq[FeatureBundle]] = T { Seq.empty[FeatureBundle] }

  def featureConf : T[PathRef] = T {

    val bundleConf : String = featureBundles()
      .map(_.formatConfig(scalaBinVersion()))
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

    os.write(confDir / s"${artifactName()}.conf", content)

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
          override def featureBundles = T { Seq(
            FeatureBundle(Deps.felixFramework, 0, true),
            FeatureBundle(Deps.orgOsgiCompendium, 1, true),
            FeatureBundle(Deps.jline, 1, false),
            FeatureBundle(Deps.jlineBuiltins, 1, false),
            FeatureBundle(Deps.felixGogoJline, 1, true),
            FeatureBundle(Deps.felixGogoRuntime, 1, true),
            FeatureBundle(Deps.felixGogoShell, 1, true),
            FeatureBundle(Deps.felixGogoCommand, 1, true),
            FeatureBundle(Deps.felixShellRemote, 1, true)
          )}
        }

        object equinox extends BlendedFeatureModule {
          override def featureBundles = T { Seq(
            FeatureBundle(Deps.eclipseOsgi, 0, true),
            FeatureBundle(Deps.eclipseEquinoxConsole, 1, true),
            FeatureBundle(Deps.jline, 1, false),
            FeatureBundle(Deps.jlineBuiltins, 1, false),
            FeatureBundle(Deps.felixGogoJline, 1, true),
            FeatureBundle(Deps.felixGogoRuntime, 1, true),
            FeatureBundle(Deps.felixGogoShell, 1, true),
            FeatureBundle(Deps.felixGogoCommand, 1, true),
            FeatureBundle(Deps.felixShellRemote, 1, true)
          )}
        }

        object common extends BlendedFeatureModule {

          override def featureBundles = T { Seq(
            FeatureBundle(BlendedDeps.securityBoot(blended.version())),
            FeatureBundle(Deps.asmAll, 4, true),
            FeatureBundle(BlendedDeps.updater(blended.version()), 4, true),
            FeatureBundle(BlendedDeps.updaterConfig(blended.version()), 4, true),
            FeatureBundle(Deps.scalaReflect),
            FeatureBundle(Deps.scalaLibrary),
            FeatureBundle(Deps.scalaXml),
            FeatureBundle(Deps.scalaCompatJava8),
            FeatureBundle(Deps.scalaParser),
            FeatureBundle(BlendedDeps.akka(blended.version()), 4, true),
            FeatureBundle(BlendedDeps.utilLogging(blended.version())),
            FeatureBundle(BlendedDeps.util(blended.version()), 4, true),
            FeatureBundle(Deps.springCore),
            FeatureBundle(Deps.springExpression),
            FeatureBundle(BlendedDeps.containerContextApi(blended.version())),
            FeatureBundle(BlendedDeps.containerContextImpl(blended.version()), 4, true),
            FeatureBundle(BlendedDeps.securityCrypto(blended.version())),
            FeatureBundle(Deps.felixConfigAdmin, 4, true),
            FeatureBundle(Deps.felixEventAdmin, 4, true),
            FeatureBundle(Deps.felixFileinstall, 4, true),
            FeatureBundle(Deps.felixMetatype, 4, true),
            FeatureBundle(Deps.typesafeConfig),
            FeatureBundle(Deps.typesafeSslConfigCore),
            FeatureBundle(Deps.reactiveStreams),
            FeatureBundle(Deps.akkaActor),
            FeatureBundle(BlendedDeps.akkaLogging(blended.version())),
            FeatureBundle(Deps.akkaProtobuf),
            FeatureBundle(Deps.akkaStream),
            FeatureBundle(Deps.domino),
            FeatureBundle(BlendedDeps.domino(blended.version())),
            FeatureBundle(BlendedDeps.mgmtBase(blended.version()), 4, true),
            FeatureBundle(BlendedDeps.prickle(blended.version())),
            FeatureBundle(BlendedDeps.mgmtServiceJmx(blended.version()))
          )}
        }
      }
    }
  }
}

