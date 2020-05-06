import mill._
import mill.scalalib._
import $file.feature_support
import feature_support.FeatureBundle
import $file.build_deps
import build_deps.Deps
import coursier.Repository
import coursier.core.{Extension, Publication}
import coursier.maven.MavenRepository
import mill.scalalib.PublishModule.ExtraPublish
import mill.scalalib.publish._

///** Project directory. */
val baseDir: os.Path = build.millSourcePath

import $file.build_util
import build_util.{FilterUtil, ZipUtil}

import $file.blended_deps
import blended_deps.BlendedDeps

/** Configure additional repositories. */
trait BlendedCoursierModule extends CoursierModule {
  private def zincWorker: ZincWorkerModule = mill.scalalib.ZincWorkerModule
  override def repositories: Seq[Repository] = zincWorker.repositories ++ Seq(
    MavenRepository("https://repo.spring.io/libs-release"),
    MavenRepository("http://repository.springsource.com/maven/bundles/release"),
    MavenRepository("http://repository.springsource.com/maven/bundles/external")
  )
}

trait BlendedModule extends BlendedCoursierModule {
  def blendedModule: String = millModuleSegments.parts.mkString(".")
  def blendedCoreVersion : T[String] = blended.version()
}

trait BlendedScalaModule extends ScalaModule with BlendedModule {
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

trait BlendedFeatureModule extends BlendedScalaModule with BlendedCoursierModule with BlendedPublishModule {

  override def artifactName = T { blendedModule }

  override def extraPublish = T { super.extraPublish() ++ Seq(
    ExtraPublish(featureConf(), "confs", ".conf")
  )}

  def featureDeps : Seq[BlendedFeatureModule] = Seq.empty

  override def moduleDeps = super.moduleDeps

  def featureBundles : T[Seq[FeatureBundle]] = T { Seq.empty[FeatureBundle] }

  override def ivyDeps = T { featureBundles().map(_.dependency) }

  def featureConf : T[PathRef] = T {

    val bundleConf : String = featureBundles()
      .map(_.formatConfig(scalaBinVersion()))
      .mkString(",\n")

    val featureConf : String = if (featureDeps.isEmpty) {
      ""
    } else {
      T.traverse(featureDeps)( fd =>
        T.task { s"""    { name = "${fd.artifactName()}", version = "${fd.publishVersion()}" }""" }
      )().mkString("  features = [\n", ",\n", "\n  ]")
    }

    val confDir = T.dest

    val content =
      s"""{
         |  name = "${artifactName()}"
         |  version = "${publishVersion()}"
         |""".stripMargin + featureConf +
         """
         |  bundles = [
         |""".stripMargin + bundleConf +
         """
          |  ]
          |}""".stripMargin

    os.write(confDir / s"${artifactName()}.conf", content)

    PathRef(confDir)
  }
}

trait BlendedContainer extends BlendedPublishModule with BlendedScalaModule {

  def blendedLauncherZip : T [Agg[Dep]] = T { Agg(
    ivy"${BlendedDeps.organization}::blended.launcher:${blendedCoreVersion()};classifier=dist".exclude("*" -> "*")
  )}

  def resolveLauncher : T[PathRef] = T {
    val resolved = resolveDeps(blendedLauncherZip)()
    resolved.items.next()
  }

  def unpackLauncher : T[PathRef] = T {
    ZipUtil.unpackZip(resolveLauncher().path, T.dest)
    PathRef(T.dest)
  }
}

object blended extends Module {

  def version = T.input {
    os.read(baseDir / "version.txt").trim()
  }

  object launcher extends Module {
    object feature extends Module {

      object activemq extends BlendedFeatureModule {
        override def featureBundles = T { Seq(
          FeatureBundle(Deps.ariesProxyApi),
          FeatureBundle(Deps.ariesBlueprintApi),
          FeatureBundle(Deps.ariesBlueprintCore),
          FeatureBundle(Deps.geronimoAnnotation),
          FeatureBundle(Deps.geronimoJms11Spec),
          FeatureBundle(Deps.geronimoJ2eeMgmtSpec),
          FeatureBundle(Deps.servicemixStaxApi),
          FeatureBundle(Deps.activeMqOsgi),
          FeatureBundle(BlendedDeps.activemqBrokerstarter(blendedCoreVersion()), 4, true),
          FeatureBundle(BlendedDeps.jmsUtils(blendedCoreVersion())),
          FeatureBundle(Deps.springJms)
        )}
      }

      object akka extends Module {
        object http extends Module {
          object base extends BlendedFeatureModule {
            override def featureDeps = Seq(blended.launcher.feature.base.common)

            override def featureBundles = T { Seq(
              FeatureBundle(BlendedDeps.akkaHttpApi(blendedCoreVersion())),
              FeatureBundle(BlendedDeps.akkaHttp(blendedCoreVersion()), 4, true),
              FeatureBundle(BlendedDeps.prickleAkkaHttp(blendedCoreVersion())),
              FeatureBundle(BlendedDeps.securityAkkaHttp(blendedCoreVersion()))
            )}
          }

          object modules extends BlendedFeatureModule {
            override def featureDeps = Seq(
              blended.launcher.feature.akka.http.base,
              blended.launcher.feature.spring
            )

            override def featureBundles = T { Seq(
              FeatureBundle(BlendedDeps.akkaHttpProxy(blendedCoreVersion())),
              FeatureBundle(BlendedDeps.akkaHttpRestJms(blendedCoreVersion())),
              FeatureBundle(BlendedDeps.akkaHttpJmsQueue(blendedCoreVersion()))
            )}
          }
        }
      }

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
            FeatureBundle(BlendedDeps.securityBoot(blendedCoreVersion())),
            FeatureBundle(Deps.asmAll, 4, true),
            FeatureBundle(BlendedDeps.updater(blendedCoreVersion()), 4, true),
            FeatureBundle(BlendedDeps.updaterConfig(blendedCoreVersion()), 4, true),
            FeatureBundle(Deps.scalaReflect),
            FeatureBundle(Deps.scalaLibrary),
            FeatureBundle(Deps.scalaXml),
            FeatureBundle(Deps.scalaCompatJava8),
            FeatureBundle(Deps.scalaParser),
            FeatureBundle(BlendedDeps.akka(blendedCoreVersion()), 4, true),
            FeatureBundle(BlendedDeps.utilLogging(blendedCoreVersion())),
            FeatureBundle(BlendedDeps.util(blendedCoreVersion()), 4, true),
            FeatureBundle(Deps.springCore),
            FeatureBundle(Deps.springExpression),
            FeatureBundle(BlendedDeps.containerContextApi(blendedCoreVersion())),
            FeatureBundle(BlendedDeps.containerContextImpl(blendedCoreVersion()), 4, true),
            FeatureBundle(BlendedDeps.securityCrypto(blendedCoreVersion())),
            FeatureBundle(Deps.felixConfigAdmin, 4, true),
            FeatureBundle(Deps.felixEventAdmin, 4, true),
            FeatureBundle(Deps.felixFileinstall, 4, true),
            FeatureBundle(Deps.felixMetatype, 4, true),
            FeatureBundle(Deps.typesafeConfig),
            FeatureBundle(Deps.typesafeSslConfigCore),
            FeatureBundle(Deps.reactiveStreams),
            FeatureBundle(Deps.akkaActor),
            FeatureBundle(BlendedDeps.akkaLogging(blendedCoreVersion())),
            FeatureBundle(Deps.akkaProtobuf),
            FeatureBundle(Deps.akkaStream),
            FeatureBundle(Deps.domino),
            FeatureBundle(BlendedDeps.domino(blendedCoreVersion())),
            FeatureBundle(BlendedDeps.mgmtBase(blendedCoreVersion()), 4, true),
            FeatureBundle(BlendedDeps.prickle(blendedCoreVersion())),
            FeatureBundle(BlendedDeps.mgmtServiceJmx(blendedCoreVersion()))
          )}
        }
      }

      object commons extends BlendedFeatureModule {
        override def featureBundles = T { Seq(
          FeatureBundle(Deps.ariesUtil),
          FeatureBundle(Deps.ariesJmxApi),
          FeatureBundle(Deps.ariesJmxCore, 4, true),
          FeatureBundle(BlendedDeps.jmx(blendedCoreVersion()), 4, true),
          FeatureBundle(Deps.commonsCollections),
          FeatureBundle(Deps.commonsDiscovery),
          FeatureBundle(Deps.commonsLang3),
          FeatureBundle(Deps.commonsPool2),
          FeatureBundle(Deps.commonsNet),
          FeatureBundle(Deps.commonsExec),
          FeatureBundle(Deps.commonsIo),
          FeatureBundle(Deps.commonsCodec),
          FeatureBundle(Deps.commonsHttpclient),
          FeatureBundle(Deps.commonsBeanUtils)
        )}
      }

      object jetty extends BlendedFeatureModule {

        override def featureDeps = Seq(base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(Deps.activationApi),
          FeatureBundle(Deps.javaxServlet31),
          FeatureBundle(Deps.javaxMail),
          FeatureBundle(Deps.geronimoAnnotation),
          FeatureBundle(Deps.geronimoJaspic),
          FeatureBundle(Deps.jettyUtil),
          FeatureBundle(Deps.jettyHttp),
          FeatureBundle(Deps.jettyIo),
          FeatureBundle(Deps.jettyJmx),
          FeatureBundle(Deps.jettySecurity),
          FeatureBundle(Deps.jettyServlet),
          FeatureBundle(Deps.jettyServer),
          FeatureBundle(Deps.jettyWebapp),
          FeatureBundle(Deps.jettyDeploy),
          FeatureBundle(Deps.jettyXml),
          FeatureBundle(Deps.equinoxServlet),
          FeatureBundle(Deps.felixHttpApi),
          FeatureBundle(BlendedDeps.jettyBoot(blendedCoreVersion()), 4, true),
          FeatureBundle(Deps.jettyHttpService, 4, true)
        )}
      }

      object hawtio extends BlendedFeatureModule {
        override def featureDeps = Seq(jetty)

        override def featureBundles = T { Seq(
          FeatureBundle(Deps.hawtioWeb, 10, true),
          FeatureBundle(BlendedDeps.hawtioLogin(blendedCoreVersion()))
        )}
      }

      object jolokia extends BlendedFeatureModule {
        override def featureDeps = Seq(jetty)

        override def featureBundles = T { Seq(
          FeatureBundle(Deps.jolokiaOsgi, 4, true)
        )}
      }

      object login extends BlendedFeatureModule {
        override def featureDeps = Seq(
          blended.launcher.feature.base.common,
          blended.launcher.feature.akka.http.base,
          blended.launcher.feature.security,
          blended.launcher.feature.persistence
        )

        override def featureBundles = T { Seq(
          // Required for Json Web Token
          FeatureBundle(Deps.jacksonAnnotations),
          FeatureBundle(Deps.jacksonCore),
          FeatureBundle(Deps.jacksonBind),
          FeatureBundle(Deps.jjwt),
          // Required for Login API
          FeatureBundle(BlendedDeps.securityLoginApi(blendedCoreVersion())),
          FeatureBundle(BlendedDeps.securityLoginImpl(blendedCoreVersion()), 4, true),
          FeatureBundle(BlendedDeps.webSocket(blendedCoreVersion()), 4, true),
          FeatureBundle(BlendedDeps.securityLoginRest(blendedCoreVersion()), 4, true)
        )}
      }

      object mgmt extends Module {
        object client extends BlendedFeatureModule {
          override def featureBundles = T { Seq(
            FeatureBundle(BlendedDeps.mgmtAgent(blendedCoreVersion()), 4, true)
          )}
        }

        object server extends BlendedFeatureModule  {
          override def featureDeps = Seq(
            blended.launcher.feature.base.common,
            blended.launcher.feature.akka.http.base,
            blended.launcher.feature.security,
            blended.launcher.feature.ssl,
            blended.launcher.feature.spring,
            blended.launcher.feature.persistence,
            blended.launcher.feature.login
          )

          override def featureBundles = T { Seq(
            FeatureBundle(BlendedDeps.mgmtRest(blendedCoreVersion()), 4, true),
            FeatureBundle(BlendedDeps.mgmtRepo(blendedCoreVersion()), 4, true),
            FeatureBundle(BlendedDeps.mgmtRepoRest(blendedCoreVersion()), 4, true),
            FeatureBundle(BlendedDeps.updaterRemote(blendedCoreVersion()), 4, true),
            FeatureBundle(Deps.concurrentLinkedHashMapLru),
            FeatureBundle(Deps.jsr305),
            FeatureBundle(BlendedDeps.mgmtUi, 4, true)
          )}
        }
      }

      object persistence extends BlendedFeatureModule {
        override def featureDeps = Seq(base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(BlendedDeps.persistence(blendedCoreVersion())),
          FeatureBundle(BlendedDeps.persistenceH2(blendedCoreVersion()), 4, true),
          // for Blended.persistenceH2
          FeatureBundle(Deps.h2),
          // for Blended.persistenceH2
          FeatureBundle(Deps.hikaricp),
          // for Blended.persistenceH2
          FeatureBundle(Deps.liquibase),
          // for Deps.liquibase
          FeatureBundle(Deps.snakeyaml)
        )}
      }

      object samples extends BlendedFeatureModule {
        override def featureDeps = Seq(
          blended.launcher.feature.akka.http.base,
          blended.launcher.feature.activemq,
          blended.launcher.feature.streams
        )

        override def featureBundles = T { Seq(
          FeatureBundle(BlendedDeps.jmsBridge(blendedCoreVersion()), 4, true),
          FeatureBundle(BlendedDeps.streamsDispatcher(blendedCoreVersion()), 4, true),
          FeatureBundle(BlendedDeps.activemqClient(blendedCoreVersion()), 4, true),
          FeatureBundle(BlendedDeps.file(blendedCoreVersion())),
          FeatureBundle(BlendedDeps.akkaHttpSampleHelloworld(blendedCoreVersion()), 4, true)
        )}
      }

      object security extends BlendedFeatureModule {

        override def featureDeps = Seq(blended.launcher.feature.base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(BlendedDeps.security(blendedCoreVersion()), 4, true)
        )}
      }

      object spring extends BlendedFeatureModule {
        override def featureBundles = T { Seq(
          FeatureBundle(Deps.aopAlliance),
          FeatureBundle(Deps.springBeans),
          FeatureBundle(Deps.springAop),
          FeatureBundle(Deps.springContext),
          FeatureBundle(Deps.springContextSupport),
          FeatureBundle(Deps.springJdbc),
          FeatureBundle(Deps.springTx)
        )}
      }

      object ssl extends BlendedFeatureModule {
        override def featureDeps = Seq(blended.launcher.feature.base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(Deps.javaxServlet31),
          FeatureBundle(BlendedDeps.securityScep(blendedCoreVersion()), 4, true),
          FeatureBundle(BlendedDeps.securitySsl(blendedCoreVersion()), 4, true)
        )}
      }

      object streams extends BlendedFeatureModule {
        override def featureDeps = Seq(blended.launcher.feature.base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(BlendedDeps.streams(blendedCoreVersion()), 4, true)
        )}
      }
    }
  }

  object demo extends Module {
    object node extends BlendedContainer {

    }
  }
}

