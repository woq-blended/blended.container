import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`

import mill._
import mill.scalalib._

import ammonite.ops.Path
import coursier.Repository
import coursier.maven.MavenRepository
import coursier.core.Authentication
import mill.define.{Sources, Target}
import mill.modules.Jvm
import mill.scalalib.publish._
import mill.util.PrintLogger
import os.RelPath

import scala.util.Success

/** The versions of the blended mill plugin, the core and the mgmt ui */
val coreVersion : String = "3.2-alpha1-80-g675a35b7b"
val blendedUiVersion : String = "0.6"
val akkaBundleRevision : String = "1"

/** Project directory. */
val projectDir: os.Path = build.millSourcePath

import $ivy.`de.wayofquality.blended::blended-mill:0.4-SNAPSHOT`
import de.wayofquality.blended.mill.modules._
import de.wayofquality.blended.mill.feature._
import de.wayofquality.blended.mill.utils.{FilterUtil, ZipUtil}
import de.wayofquality.blended.mill.versioning.GitModule
import de.wayofquality.blended.mill.publish.BlendedPublishModule
import de.wayofquality.blended.mill.container.BlendedContainerModule

object GitSupport extends GitModule {
  override def millSourcePath: Path = projectDir
}

def projectVersion = T { GitSupport.publishVersion() }

def imageVersion = T { 
  val tag : String = T.env.get("GIT_TAG").getOrElse("").trim()
  if (tag.isEmpty()) {
    os.read(projectDir / "version.txt").trim()
  } else {
    tag
  }
}

/** Configure additional repositories. */
trait BlendedCoursierModule extends CoursierModule {
  private def zincWorker: ZincWorkerModule = mill.scalalib.ZincWorkerModule
  override def repositories: Seq[Repository] = zincWorker.repositories ++ Seq(
    MavenRepository("https://repo.spring.io/libs-release"),
    MavenRepository("http://repository.springsource.com/maven/bundles/release"),
    MavenRepository("http://repository.springsource.com/maven/bundles/external"),
    MavenRepository(
      s"https://u233308-sub2.your-storagebox.de/blended/$coreVersion",
      Some(Authentication("u233308-sub2", "px8Kumv98zIzSF7k"))
    ),
    MavenRepository(
      s"https://u233308-sub2.your-storagebox.de/mgmt-ui/$blendedUiVersion",
      Some(Authentication("u233308-sub2", "px8Kumv98zIzSF7k"))
    ),
    MavenRepository(
      s"https://u233308-sub2.your-storagebox.de/akka-osgi/$akkaBundleRevision",
      Some(Authentication("u233308-sub2", "px8Kumv98zIzSF7k"))
    )
  )
}

trait CtFeatureModule extends BlendedFeatureModule with BlendedCoursierModule {

  override def version : T[String] = T { projectVersion() }
  override def scalaVersion = deps.scalaVersion

  override type ProjectDeps = BlendedDependencies 
  override def deps : ProjectDeps = BlendedDependencies.Deps_2_13

  override def baseDir = projectDir

  def blendedDep : String => Dep = deps.blendedDep(coreVersion)
}

trait ContainerModule extends BlendedContainerModule with BlendedPublishModule with BlendedCoursierModule {
  override def scalaVersion : T[String] = T{ deps.scalaVersion }
  override def baseDir : Path = projectDir
  override def profileVersion : T[String] = T { projectVersion() }
  override def blendedCoreVersion : String = coreVersion

  override type ProjectDeps = BlendedDependencies
  override def deps : ProjectDeps = BlendedDependencies.Deps_2_13

  override def githubRepo = "blended.container"
  override def publishVersion : T[String] = T { projectVersion() }

  override def extraPublish : T[Seq[PublishInfo]] = T { super.extraPublish() ++ ctArtifacts() }
}

trait CtIntegrationTest extends BlendedBaseModule with BlendedCoursierModule {

  override type ProjectDeps = BlendedDependencies
  override def deps : ProjectDeps = BlendedDependencies.Deps_2_13
  override def baseDir : os.Path = projectDir

  override def scalaVersion : T[String] = T { deps.scalaVersion }

  def blendedDep : String => Dep = deps.blendedDep(coreVersion)

  trait CtTest extends super.BlendedTests {

    def dockerhost = T.input { T.env.getOrElse("DOCKERHOST", "unix:///var/run/docker.sock") }
    def dockerport = T.input { T.env.getOrElse("DOCKERPORT", "2375") }

    def logResources = T {
      val moduleSpec = toString()
      val dest = T.ctx().dest
      val logConfig =
        s"""<configuration>
          |
          |  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
          |    <file>${projectDir.toString()}/target/test-${moduleSpec}.log</file>
          |
          |    <encoder>
          |      <pattern>%d{yyyy-MM-dd-HH:mm.ss.SSS} | %8.8r | %-5level [%t] %logger : %msg%n</pattern>
          |    </encoder>
          |  </appender>
          |
          |  <logger name="blended" level="debug" />
          |  <logger name="domino" level="debug" />
          |  <logger name="App" level="debug" />
          |
          |  <root level="INFO">
          |    <appender-ref ref="FILE" />
          |  </root>
          |
          |</configuration>
          |""".stripMargin
      os.write(dest / "logback-test.xml", logConfig)
      PathRef(dest)
    }

    override def runClasspath = T { super.runClasspath() ++ Seq(logResources()) }

    override def sources: Sources = T.sources (
      millSourcePath / "src" / "test" / "scala",
      millSourcePath / "src" / "test" / "java"
    )

    override def resources = T.sources(
      millSourcePath / "src" / "test" / "resources"
    )

    override def forkArgs = T { super.forkArgs() ++ Seq(
      s"-DprojectTestOutput=${(millSourcePath / "src" / "test" / "resources").toIO.getAbsolutePath()}",
      s"-Ddocker.host=${dockerhost()}",
      s"-Ddocker.port=${dockerport()}",
      s"-Dimage.version=${imageVersion()}"
    )}

    override def ivyDeps : T[Agg[Dep]] = T { super.ivyDeps() ++ Agg(
      deps.activeMqClient,
      deps.scalatest,
      deps.slf4j,
      deps.akkaActor(akkaBundleRevision),
      deps.akkaStream(akkaBundleRevision),
      deps.akkaSlf4j(akkaBundleRevision),
      deps.logbackClassic,
      deps.logbackCore,
      deps.geronimoJ2eeMgmtSpec,
      deps.geronimoJms11Spec,

      deps.dockerJava,
      deps.akkaTestkit,

      blendedDep("util.logging"),
      blendedDep("jms.utils"),
      blendedDep("util"),
      blendedDep("akka"),
      blendedDep("security.ssl"),
      blendedDep("streams"),
      blendedDep("testsupport"),
      blendedDep("streams.testsupport"),

      blendedDep("itestsupport")
    )}

    /* This is just a workaround to see if it actually works */
    def itest() = T.command {

      val dir = T.dest

      Jvm.runSubprocess(
        mainClass = "org.scalatest.tools.Runner",
        classPath = runClasspath().map(_.path),
        mainArgs = Seq(
          "-R", compile().classes.path.toIO.getAbsolutePath(),
          "-o"
        ),
        jvmArgs = forkArgs()
      )

      PathRef(dir)
    }
  }
}

object blended extends Module {

  object launcher extends Module {
    object feature extends Module {

      object activemq extends CtFeatureModule {
        override def featureBundles = T { Seq(
          FeatureBundle(deps.ariesProxyApi),
          FeatureBundle(deps.ariesBlueprintApi),
          FeatureBundle(deps.ariesBlueprintCore),
          FeatureBundle(deps.geronimoAnnotation),
          FeatureBundle(deps.geronimoJms11Spec),
          FeatureBundle(deps.geronimoJ2eeMgmtSpec),
          FeatureBundle(deps.servicemixStaxApi),
          FeatureBundle(deps.activeMqOsgi),
          FeatureBundle(blendedDep("activemq.brokerstarter"), 4, true),
          FeatureBundle(blendedDep("jms.utils")),
          FeatureBundle(deps.springJms)
        )}
      }

      object akka extends Module {
        object http extends Module {
          object base extends CtFeatureModule {
            override def featureDeps = Seq(blended.launcher.feature.base.common)

            override def featureBundles = T { Seq(
              FeatureBundle(deps.akkaParsing(akkaBundleRevision)),
              FeatureBundle(deps.akkaHttp(akkaBundleRevision)),
              FeatureBundle(deps.akkaHttpCore(akkaBundleRevision)),
              FeatureBundle(blendedDep("akka.http"), 4, true),
              FeatureBundle(blendedDep("prickle.akka.http")),
              FeatureBundle(blendedDep("security.akka.http"))
            )}
          }

          object modules extends CtFeatureModule {
            override def featureDeps = Seq(
              blended.launcher.feature.akka.http.base,
              blended.launcher.feature.spring
            )

            override def featureBundles = T { Seq(
              FeatureBundle(blendedDep("akka.http.proxy")),
              FeatureBundle(blendedDep("akka.http.restjms")),
              FeatureBundle(blendedDep("akka.http.jmsqueue"))
            )}
          }
        }
      }

      object base extends Module {
        object felix extends CtFeatureModule {
          override def featureBundles = T { Seq(
            FeatureBundle(deps.felixFramework, 0, true),
            FeatureBundle(deps.orgOsgiCompendium, 1, true),
            FeatureBundle(deps.jline, 1, false),
            FeatureBundle(deps.jlineBuiltins, 1, false),
            FeatureBundle(deps.felixGogoJline, 1, true),
            FeatureBundle(deps.felixGogoRuntime, 1, true),
            FeatureBundle(deps.felixGogoShell, 1, true),
            FeatureBundle(deps.felixGogoCommand, 1, true),
            FeatureBundle(deps.felixShellRemote, 1, true)
          )}
        }

        object equinox extends CtFeatureModule {
          override def featureBundles = T { Seq(
            FeatureBundle(deps.eclipseOsgi, 0, true),
            FeatureBundle(deps.eclipseEquinoxConsole, 1, true),
            FeatureBundle(deps.jline, 1, false),
            FeatureBundle(deps.jlineBuiltins, 1, false),
            FeatureBundle(deps.felixGogoJline, 1, true),
            FeatureBundle(deps.felixGogoRuntime, 1, true),
            FeatureBundle(deps.felixGogoShell, 1, true),
            FeatureBundle(deps.felixGogoCommand, 1, true),
            FeatureBundle(deps.felixShellRemote, 1, true)
          )}
        }

        object common extends CtFeatureModule {
          override def featureBundles = T { Seq(
            FeatureBundle(blendedDep("security.boot")),
            FeatureBundle(deps.asmAll, 4, true),
            FeatureBundle(blendedDep("updater"), 4, true),
            FeatureBundle(blendedDep("updater.config"), 4, true),
            FeatureBundle(deps.scalaReflect(scalaVersion())),
            FeatureBundle(deps.scalaLibrary(scalaVersion())),
            FeatureBundle(deps.scalaXml),
            FeatureBundle(deps.scalaCompatJava8),
            FeatureBundle(deps.scalaParser),
            FeatureBundle(blendedDep("akka"), 4, true),
            FeatureBundle(blendedDep("util.logging")),
            FeatureBundle(blendedDep("util"), 4, true),
            FeatureBundle(deps.springCore),
            FeatureBundle(deps.springExpression),
            FeatureBundle(blendedDep("container.context.api")),
            FeatureBundle(blendedDep("container.context.impl"), 4, true),
            FeatureBundle(blendedDep("security.crypto")),
            FeatureBundle(deps.felixConfigAdmin, 4, true),
            FeatureBundle(deps.felixEventAdmin, 4, true),
            FeatureBundle(deps.felixFileinstall, 4, true),
            FeatureBundle(deps.felixMetatype, 4, true),
            FeatureBundle(deps.typesafeConfig),
            FeatureBundle(deps.typesafeSslConfigCore),
            FeatureBundle(deps.reactiveStreams),
            FeatureBundle(deps.akkaActor(akkaBundleRevision)),
            FeatureBundle(blendedDep("akka.logging")),
            FeatureBundle(deps.akkaProtobuf(akkaBundleRevision)),
            FeatureBundle(deps.akkaProtobufV3(akkaBundleRevision)),
            FeatureBundle(deps.akkaStream(akkaBundleRevision)),
            FeatureBundle(deps.domino),
            FeatureBundle(blendedDep("domino")),
            FeatureBundle(blendedDep("mgmt.base"), 4, true),
            FeatureBundle(blendedDep("prickle")),
            FeatureBundle(blendedDep("mgmt.service.jmx"))
          )}
        }
      }

      object commons extends CtFeatureModule {
        override def featureBundles = T { Seq(
          FeatureBundle(deps.ariesUtil),
          FeatureBundle(deps.ariesJmxApi),
          FeatureBundle(deps.ariesJmxCore, 4, true),
          FeatureBundle(blendedDep("jmx"), 4, true),
          FeatureBundle(deps.commonsCollections),
          FeatureBundle(deps.commonsDiscovery),
          FeatureBundle(deps.commonsLang3),
          FeatureBundle(deps.commonsPool2),
          FeatureBundle(deps.commonsNet),
          FeatureBundle(deps.commonsExec),
          FeatureBundle(deps.commonsIo),
          FeatureBundle(deps.commonsCodec),
          FeatureBundle(deps.commonsHttpclient),
          FeatureBundle(deps.commonsBeanUtils)
        )}
      }

      object jetty extends CtFeatureModule {

        override def featureDeps = Seq(base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(deps.activationApi),
          FeatureBundle(deps.javaxServlet31),
          FeatureBundle(deps.javaxMail),
          FeatureBundle(deps.geronimoAnnotation),
          FeatureBundle(deps.geronimoJaspic),
          FeatureBundle(deps.jettyUtil),
          FeatureBundle(deps.jettyHttp),
          FeatureBundle(deps.jettyIo),
          FeatureBundle(deps.jettyJmx),
          FeatureBundle(deps.jettySecurity),
          FeatureBundle(deps.jettyServlet),
          FeatureBundle(deps.jettyServer),
          FeatureBundle(deps.jettyWebapp),
          FeatureBundle(deps.jettyDeploy),
          FeatureBundle(deps.jettyXml),
          FeatureBundle(deps.equinoxServlet),
          FeatureBundle(deps.felixHttpApi),
          FeatureBundle(blendedDep("jetty.boot"), 4, true),
          FeatureBundle(deps.jettyHttpService, 4, true)
        )}
      }

      object hawtio extends CtFeatureModule {
        override def featureDeps = Seq(jetty)

        override def featureBundles = T { Seq(
          FeatureBundle(deps.hawtioWeb, 10, true),
          FeatureBundle(blendedDep("hawtio.login"))
        )}
      }

      object jolokia extends CtFeatureModule {
        override def featureDeps = Seq(jetty)

        override def featureBundles = T { Seq(
          FeatureBundle(deps.jolokiaOsgi, 4, true)
        )}
      }

      object login extends CtFeatureModule {
        override def featureDeps = Seq(
          blended.launcher.feature.base.common,
          blended.launcher.feature.akka.http.base,
          blended.launcher.feature.security,
          blended.launcher.feature.persistence,
          blended.launcher.feature.streams
        )

        override def featureBundles = T { Seq(
          // Required for Json Web Token
          FeatureBundle(deps.jacksonAnnotations),
          FeatureBundle(deps.jacksonCore),
          FeatureBundle(deps.jacksonBind),
          FeatureBundle(deps.jjwt),
          // Required for Login API
          FeatureBundle(blendedDep("security.login.api")),
          FeatureBundle(blendedDep("security.login.impl"), 4, true),
          FeatureBundle(blendedDep("websocket"), 4, true),
          FeatureBundle(blendedDep("security.login.rest"), 4, true)
        )}
      }

      object mgmt extends Module {
        object client extends CtFeatureModule {
          override def featureBundles = T { Seq(
            FeatureBundle(blendedDep("mgmt.agent"), 4, true)
          )}
        }

        object server extends CtFeatureModule  {
          override def featureDeps = Seq(
            blended.launcher.feature.base.common,
            blended.launcher.feature.akka.http.base,
            blended.launcher.feature.security,
            blended.launcher.feature.ssl,
            blended.launcher.feature.spring,
            blended.launcher.feature.persistence,
            blended.launcher.feature.login,
            blended.launcher.feature.streams
          )

          override def featureBundles = T { Seq(
            FeatureBundle(blendedDep("mgmt.rest"), 4, true),
            FeatureBundle(blendedDep("mgmt.repo"), 4, true),
            FeatureBundle(blendedDep("mgmt.repo.rest"), 4, true),
            FeatureBundle(deps.concurrentLinkedHashMapLru),
            FeatureBundle(deps.jsr305),
            FeatureBundle(ivy"${deps.blendedOrg}::blended.mgmt.ui.mgmtApp.webBundle:$blendedUiVersion", 4, true)
          )}
        }
      }

      object persistence extends CtFeatureModule {
        override def featureDeps = Seq(base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(blendedDep("persistence")),
          FeatureBundle(blendedDep("persistence.h2"), 4, true),
          // for Blended.persistenceH2
          FeatureBundle(deps.h2),
          // for Blended.persistenceH2
          FeatureBundle(deps.hikaricp),
          // for Blended.persistenceH2
          FeatureBundle(deps.liquibase),
          // for deps.liquibase
          FeatureBundle(deps.snakeyaml)
        )}
      }

      object samples extends CtFeatureModule {
        override def featureDeps = Seq(
          blended.launcher.feature.akka.http.base,
          blended.launcher.feature.activemq,
          blended.launcher.feature.streams
        )

        override def featureBundles = T { Seq(
          FeatureBundle(blendedDep("jms.bridge"), 4, true),
          FeatureBundle(blendedDep("streams.dispatcher"), 4, true),
          FeatureBundle(blendedDep("activemq.client"), 4, true),
          FeatureBundle(blendedDep("file")),
          FeatureBundle(blendedDep("akka.http.sample.helloworld"), 4, true)
        )}
      }

      object security extends CtFeatureModule {

        override def featureDeps = Seq(blended.launcher.feature.base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(blendedDep("security"), 4, true)
        )}
      }

      object spring extends CtFeatureModule {
        override def featureBundles = T { Seq(
          FeatureBundle(deps.aopAlliance),
          FeatureBundle(deps.springBeans),
          FeatureBundle(deps.springAop),
          FeatureBundle(deps.springContext),
          FeatureBundle(deps.springContextSupport),
          FeatureBundle(deps.springJdbc),
          FeatureBundle(deps.springTx)
        )}
      }

      object ssl extends CtFeatureModule {
        override def featureDeps = Seq(blended.launcher.feature.base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(deps.javaxServlet31),
          FeatureBundle(blendedDep("security.scep"), 4, true),
          FeatureBundle(blendedDep("security.ssl"), 4, true)
        )}
      }

      object streams extends CtFeatureModule {
        override def featureDeps = Seq(blended.launcher.feature.base.common)

        override def featureBundles = T { Seq(
          FeatureBundle(deps.geronimoJms11Spec),
          FeatureBundle(blendedDep("jms.utils")),
          FeatureBundle(blendedDep("streams"), 4, true)
        )}
      }
    }
  }

  object demo extends Module {
    object node extends ContainerModule {

      override def description : String = "A simple blended demo container"

      override def millSourcePath : os.Path = projectDir / "container" / "blended.demo.node"

      //override def debugTool = true

      override def featureModuleDeps = Seq(
        blended.launcher.feature.base.felix,
        //blended.launcher.feature.base.equinox,
        blended.launcher.feature.base.common,
        blended.launcher.feature.commons,
        blended.launcher.feature.spring,
        blended.launcher.feature.ssl,
        blended.launcher.feature.jetty,
        blended.launcher.feature.jolokia,
        blended.launcher.feature.hawtio,
        blended.launcher.feature.activemq,
        blended.launcher.feature.security,
        blended.launcher.feature.login,
        blended.launcher.feature.mgmt.client,
        blended.launcher.feature.akka.http.base,
        blended.launcher.feature.persistence,
        blended.launcher.feature.streams,
        blended.launcher.feature.samples
      )

      object docker extends Docker {
        override def dockerImage = T { s"atooni/blended-node:${imageVersion()}"}
        override def exposedPorts = Seq(1099, 1883, 1884, 1885, 1886, 8181, 8849, 9191, 9995, 9996)
      }
    }

    object mgmt extends ContainerModule {

      override def description : String = "A simple blended management container"

      override def millSourcePath : os.Path = projectDir / "container" / "blended.demo.mgmt"

      //override def debugTool = true

      override def featureModuleDeps = Seq(
        blended.launcher.feature.base.felix,
        //blended.launcher.feature.base.equinox,
        blended.launcher.feature.base.common,
        blended.launcher.feature.commons,
        blended.launcher.feature.jetty,
        blended.launcher.feature.hawtio,
        blended.launcher.feature.spring,
        blended.launcher.feature.ssl,
        blended.launcher.feature.security,
        blended.launcher.feature.login,
        blended.launcher.feature.mgmt.client,
        blended.launcher.feature.mgmt.server,
        blended.launcher.feature.akka.http.base,
        blended.launcher.feature.streams,
        blended.launcher.feature.persistence
      )

      object docker extends Docker {

        override def dockerImage = T { s"atooni/blended-mgmt:${imageVersion()}"}

        override def exposedPorts = Seq(1099, 1883, 9191, 8849, 9995, 9996)
      }
    }
  }

  object itest extends Module {

    object node extends CtIntegrationTest {

      object test extends super.CtTest {

        override def ivyDeps : T[Agg[Dep]] = T { 
          super.ivyDeps()
        }

        override def forkArgs: Target[Seq[String]] = T { super.forkArgs() ++ Seq(
          s"-DappFolder=${blended.demo.node.docker.appFolder()}"
        )}

        override def millSourcePath: Path = projectDir / "itest" / "blended.itest.node"
      }
    }

    object mgmt extends CtIntegrationTest {

      object test extends super.CtTest {

        override def ivyDeps : T[Agg[Dep]] = T { 

          super.ivyDeps() ++ Agg(
            deps.sttp,
            deps.sttpAkka,
            deps.microjson,
            deps.prickle,
            deps.lihaoyiPprint
          )
        }


        override def forkArgs = T { super.forkArgs() ++ Seq(
          s"-Ddeploymentpack=${blended.demo.node.deploymentpack().path.toIO.getAbsolutePath()}"
        )}

        override def millSourcePath: Path = projectDir / "itest" / "blended.itest.mgmt"

      }

    }
  }
}

