//import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`

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
val coreVersion : String = "3.2-alpha1-99-g7d81b11f0"
val blendedUiVersion : String = "0.6"
val akkaBundleRevision : String = "1"

/** Project directory. */
val projectDir: os.Path = build.millSourcePath

import $ivy.`de.wayofquality.blended::blended-mill:0.3-18-g293d5a1`
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

trait ContainerModule extends BlendedContainerModule with BlendedPublishModule with BlendedCoursierModule {
  override def scalaVersion : T[String] = T{ deps.scalaVersion }
  override def baseDir : Path = projectDir
  override def profileVersion : T[String] = T { projectVersion() }
  override def blendedCoreVersion : String = coreVersion

  override type ProjectDeps = BlendedDependencies
  override def deps : ProjectDeps = BlendedDependencies.Deps_2_13

  def blendedDep : String => Dep = deps.blendedDep(coreVersion)

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
          |    <file>${projectDir.toString()}/out/testlog/test-${moduleSpec}.log</file>
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
      s"-DlogDir=${(baseDir / "out" / "testlog" / "ctLogs"/ blendedModule).toIO.getAbsolutePath()}",
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

  object demo extends Module {
    object node extends ContainerModule {

      override def description = "A simple blended demo container"

      //override def debugTool : Boolean = true

      override def millSourcePath : os.Path = projectDir / "container" / "blended.demo.node"

      override def features : T[Seq[FeatureRef]] = T { Seq(
        FeatureRef(
          dependency = blendedDep("features"),
          names = List(
            "blended.features.base.felix",
            //"blended.features.base.equinox",
            "blended.features.base.common",
            "blended.features.commons",
            "blended.features.spring",
            "blended.features.ssl",
            "blended.features.jetty",
            "blended.features.jolokia",
            "blended.features.hawtio",
            "blended.features.activemq",
            "blended.features.security",
            "blended.features.login",
            "blended.features.mgmt.client",
            "blended.features.akka.http.base",
            "blended.features.persistence",
            "blended.features.streams",
            "blended.features.samples"
          )
        )
      )}

      object docker extends Docker {
        override def dockerImage = T { s"blended/blended-node:${imageVersion()}"}
        override def exposedPorts = Seq(1099, 1883, 1884, 1885, 1886, 8181, 8849, 9191, 9995, 9996)
      }
    }

    object mgmt extends ContainerModule {

      override def description = "A simple blended management container"

      override def millSourcePath : os.Path = projectDir / "container" / "blended.demo.mgmt"

      //override def debugTool = true

      override def features : T[Seq[FeatureRef]] = T { Seq(
        FeatureRef(
          dependency = blendedDep("features"),
          names = List(
            "blended.features.base.felix",
            //"blended.features.base.equinox",
            "blended.features.base.common",
            "blended.features.commons",
            "blended.features.jetty",
            "blended.features.hawtio",
            "blended.features.spring",
            "blended.features.ssl",
            "blended.features.security",
            "blended.features.login",
            "blended.features.mgmt.client",
            "blended.features.mgmt.server",
            "blended.features.akka.http.base",
            "blended.features.streams",
            "blended.features.persistence"
          )
        )
      )}

      object docker extends Docker {

        override def dockerImage = T { s"blended/blended-mgmt:${imageVersion()}"}
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

