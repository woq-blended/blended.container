import mill._
import mill.scalalib._
import $file.feature_support
import feature_support.FeatureBundle
import $file.build_deps
import ammonite.ops.Path
import build_deps.Deps
import coursier.Repository
import coursier.maven.MavenRepository
import mill.api.Logger
import mill.define.{Sources, Task}
import mill.modules.Jvm
import mill.scalalib.publish._
import mill.util.PrintLogger

import scala.util.Success

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

trait BlendedScalaModule extends ScalaModule with SbtModule with BlendedModule {
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
    PublishInfo(featureConf(), ext = "conf", ivyType = "conf", ivyConfig = "compile")
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

    val conf = T.dest / s"${artifactName()}.conf"

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

    os.write(conf, content)

    PathRef(conf)
  }
}

/**
 * Define how blended containers are assembled.
 */
trait BlendedContainer extends BlendedPublishModule with BlendedScalaModule { ctModule =>

  def featureModuleDeps : Seq[BlendedFeatureModule] = Seq.empty
  def profileName : T[String]
  def profileVersion : T[String] = T { blended.version() }

  def debugTool : Boolean = false

  /**
   * The artifact map is a map from maven gavs to actual files. Typically all dependencies
   * that come from features are resolved by mill via coursier. In this list we keep track
   * of gav's to the absolute path of the associated file, so that we can pass this information
   * to blended's RuntimeConfigBuilder.
   */
  def artifactMap : T[Map[String, String]] = T {

    val bundles = T.traverse(featureModuleDeps)(fd =>
      T.task {

        fd.featureBundles().map{ fb =>
          val gav : String = fb.gav(scalaBinVersion())

          val singleDep : Seq[PathRef] = Lib.resolveDependencies(
            repositories,
            resolveCoursierDependency().apply(_),
            Agg(fb.dependency.exclude("*" -> "*")),
            false,
            mapDependencies = None,
            Some(implicitly[mill.util.Ctx.Log])
          ) match {
            case mill.api.Result.Success(r) =>
              if (r.isEmpty) {
                // TODO: This will appear in the log, but won't break the build. The RuntimeConfigBuilder
                // will try to download that file itself.
                // This happens i.e. when dependencies are not "jar"s
                T.log.error(s"No artifact found for [$gav]")
              }
              r.toSeq
            case mill.api.Result.Failure(m, _)  => sys.error(s"Failed to resolve [$gav] : $m")
          }

          singleDep.map(pr => (gav, pr.path.toIO.getAbsolutePath()))
        }
      }
    )()

    bundles.flatten.flatten.toMap
  }

  /**
   * The binaries packaged within the blended launcher. The content of this archive will be used as a starting
   * point for all blended containers.
   */
  def blendedLauncherZip : T [Agg[Dep]] = T { Agg(
    ivy"${BlendedDeps.organization}::blended.launcher:${blendedCoreVersion()};classifier=dist".exclude("*" -> "*")
  )}

  /**
   * The dependency to the blended updater tools. These contain the tools to actually generate the
   * profile configuration.
   */
  def blendedToolsDeps : T [Agg[Dep]] = T { Agg(
    BlendedDeps.updaterTools(blendedCoreVersion())
  )}

  /**
   * Resolve the launcher distribution file.
   */
  def resolveLauncher : T[PathRef] = T {
    val resolved = resolveDeps(blendedLauncherZip)()
    resolved.items.next()
  }

  /**
   * Unpack the content of the launcher distribution file.
   */
  def unpackLauncher : T[PathRef] = T {
    ZipUtil.unpackZip(resolveLauncher().path, T.dest)
    PathRef(T.dest)
  }

  /**
   * The resource path will be subject to filtering. We need to inject the profile name
   * and the profile version into the final profile.conf.
   */
  override def resources = T.sources { millSourcePath / "src"/ "profile" }

  /**
   * Run resource filtering across all <code>resources()</code>.
   */
  def filterResources : T[PathRef] = T {

    FilterUtil.filterDirs(
      unfilteredResourcesDirs = resources().map(_.path),
      pattern = """\$\{(.+?)\}""",
      filterTargetDir = T.dest,
      props = Map(
        "profile.name" -> profileName(),
        "profile.version" -> profileVersion(),
        "blended.version" -> publishVersion()
      ),
      failOnMiss = false
    )

    PathRef(T.dest)
  }

  /**
   * Generate a profile conf that can be fed into RuntimeConfigBuilder. This will take the
   * filtered profile.conf from the sourcce files and append the configuration for the
   * container resource archive and the feature definitions.
   */
  def enhanceProfileConf : T[PathRef] = T {

    val content : String = os.read(filterResources().path / "profile.conf")

    val ctResArtifact = ctResources.artifactMetadata()

    val resources : String =
      s"""
         |resources = [
         |  { url="mvn:${ctResources.mvnGav()}" }
         |]
         |""".stripMargin

    val features : Seq[String] = T.traverse(featureModuleDeps)(fd =>
      T.task { s"""  { name=${fd.artifactName()}, version="${fd.publishVersion()}" }""" }
    )()

    val generated = content + resources + features.mkString("features = [\n", ",\n", "\n]\n") + "bundles = []\n"

    os.write(T.dest / "profile.conf", generated)
    PathRef(T.dest / "profile.conf")
  }

  /**
   * Return a sequence of all feature files used in this container. These files will be handed over
   * to the RuntimeConfigBuilder.
   */
  def featureFiles : T[Seq[String]] = T.traverse(featureModuleDeps)(fd =>
    T.task { fd.featureConf() }
  )().map(_.path.toIO.getAbsolutePath)

  /**
   * The class we need to run to materialze the profile.
   */
  def runtimeConfigBuilderClass : String = "blended.updater.tools.configbuilder.RuntimeConfigBuilder"

  /**
   * Materialize the container profile.
   */
  def materializeProfile : T[PathRef] = T {

    val toolsCp : Agg[Path] = resolveDeps(blendedToolsDeps)().map(_.path)

    // First do required replacements in the source profile file
    val profileSource : Path = filterResources().path / "profile" / "profile.conf"

    // This is the target profile file
    val profileDir : Path = T.dest

    // TODO: use other repo types ?
    val repoUrls : Seq[String] = repositories
      .filter(_.isInstanceOf[MavenRepository])
      .map(_.asInstanceOf[MavenRepository].root)

    // TODO: How do I determine if mill is started in debug mode
    val debugArgs : Seq[String] = if (debugTool) {
      Seq("--debug")
    } else {
      Seq.empty
    }

    // Assemble the command line parameters
    val toolArgs : Seq[String] = Seq(
      "-f", enhanceProfileConf().path.toIO.getAbsolutePath(),
      "-o", (profileDir / "profile.conf").toIO.getAbsolutePath(),
      "--create-launch-config", (profileDir / "launch.conf").toIO.getAbsolutePath(),
      "--download-missing",
      "--update-checksums",
      "--write-overlays-config",
      "--explode-resources",
      "--maven-artifact", ctResources.mvnGav(), ctResources.jar().path.toIO.getAbsolutePath()
    ) ++
      debugArgs ++
      featureFiles().flatMap(f => Seq[String]("--feature-repo", f)) ++
      artifactMap().flatMap{ case(k,v) => Seq[String]("--maven-artifact", k, v) } ++
      repoUrls.flatMap(r => Seq("--maven-url", r))

    T.log.debug(s"Calling $runtimeConfigBuilderClass with arguments : ${toolArgs.mkString("\n", "\n", "\n")}")

    Jvm.runSubprocess(
      mainClass = runtimeConfigBuilderClass,
      classPath = toolsCp,
      mainArgs = toolArgs
    )

    os.remove.all(profileDir / "META-INF")

    T.log.info(s"Materialized profile in [${profileDir.toIO.getAbsolutePath()}]")
    // Voila - the final profile configs
    PathRef(profileDir)
  }

  def containerExtraFiles  = T.sources { millSourcePath / "src"/ "package" / "container" }
  def profileExtraFiles = T.sources { millSourcePath / "src" / "package" / "profile" }

  /**
   * Create the runnable container by copying all resources into the right place.
   */
  def container : T [PathRef] = T {

    /**
     * Helper to copy the content of a given directory into a destination directory.
     * The given directory may not be present (i.e. no extra files are required).
     * Files within the destination folder will be overwritten by the content of the
     * given directory.
     */
    def copyOver(src: Path, dest: Path) : Unit = {
      if (src.toIO.exists()) {
        os.walk(src).foreach { p =>
          if (p.toIO.isFile()) {
            os.copy(p, dest / p.relativeTo(src), replaceExisting = true, createFolders = true)
          }
        }
      }
    }

    val ctDir = T.dest

    val launcher : Path = unpackLauncher().path
    val profile : Path = materializeProfile().path

    val profileDir = ctDir / "profiles" / profileName() / profileVersion()

    os.list(launcher).iterator.foreach { p => os.copy.into(p, ctDir) }
    os.remove.all(ctDir / "META-INF")

    os.copy.into(profile / "launch.conf", ctDir)
    os.copy(profile, profileDir, createFolders = true)

    containerExtraFiles().map(_.path).foreach(copyOver(_, ctDir) )
    profileExtraFiles().map(_.path).foreach(copyOver(_, profileDir))

    os.remove(ctDir / "profiles" / profileName() / profileVersion() / "launch.conf")

    PathRef(ctDir)
  }

  /**
   * Package the runnable container into a zip archive.
   */
  def dist = T {
    val zip = T.dest / "container.zip"
    ZipUtil.createZip(
      outputPath = zip,
      inputPaths = Seq(container().path),
      prefix = s"${artifactId()}-${artifactMetadata().version}/"
    )

    PathRef(zip)
  }

  /**
   * Make sure the container zips are also published.
   */
  override def extraPublish = T  { super.extraPublish() ++ Seq(
    PublishInfo(file = dist(), classifier = Some("full-nojre"), ext = "zip", ivyConfig = "compile", ivyType = "dist")
  )}

  // TODO: Apply magic to turn ctResources to magic overridable val (i.e. as in ScoverageData)
  // per default package downloadable resources in a separate jar
  object ctResources extends BlendedScalaModule with BlendedPublishModule {
    override def artifactName : T[String] = T { ctModule.artifactName() + ".resources" }

    def mvnGav : T [String] = T {
      s"${artifactMetadata().group}:${artifactMetadata().id}:${artifactMetadata().version}"
    }
  }

  /**
   * Docker definitions if we want to create a docker image from the container.
   */
  trait Docker extends BlendedModule {

    /**
     * The maintainer as it should appear in the docker file.
     */
    def maintainer : String = "Blended Team"

    /**
     * The base image that shall be used for the generated docker image.
     */
    def baseImage : String = "atooni/blended-base:latest"

    /**
     * The ports exposed from the docker image
     */
    def exposedPorts : Seq[Int] = Seq.empty

    /**
     * The folder under /opt where the application shall be installed.
     */
    def appFolder : T[String] = T { ctModule.profileName() }

    /**
     * The user who owns the application folder
     */
    def appUser : String = "blended"

    def dockerImage : T[String] = T { s"atooni/blended-${appFolder()}:${ctModule.profileVersion()}" }

    def dockerconfig : T[PathRef] = T {

      val dir = T.dest

      os.copy(ctModule.container().path, dir / "files" / "container" / appFolder(), createFolders = true)

      val content : String =
        s"""FROM $baseImage
           |LABEL maintainer="$maintainer"
           |LABEL version="${ctModule.profileVersion()}"
           |ADD files/container /opt
           |RUN chown -R $appUser.$appUser /opt/${appFolder()}
           |USER $appUser
           |ENV JAVA_HOME /opt/java
           |ENV PATH $${PATH}:$${JAVA_HOME}/bin
           |ENTRYPOINT ["/bin/sh", "/opt/${appFolder()}/bin/blended.sh"]
           |""".stripMargin ++ exposedPorts.map(p => s"EXPOSE $p").mkString("\n", "\n", "\n")

      os.write(dir / "Dockerfile", content)

      PathRef(dir)
    }

    def dockerbuild()  = T.command {
      val process = Jvm.spawnSubprocess(commandArgs = Seq(
        "docker", "build", "-t", dockerImage(), "."
      ), envArgs = Map.empty, workingDir = dockerconfig().path)

      process.join()
      process.exitCode()
    }
  }
}

trait BlendedIntegrationTest extends TestModule with BlendedScalaModule {

  def dockerhost = "localhost"
  def dockerport = 2375

  override def testFrameworks = Seq("org.scalatest.tools.Framework")

  def logResources = T {
    val moduleSpec = toString()
    val dest = T.ctx().dest
    val logConfig =
      s"""<configuration>
         |
         |  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
         |    <file>${baseDir.toString()}/target/test-${moduleSpec}.log</file>
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
    s"-Ddocker.host=$dockerhost",
    s"-Ddocker.port=$dockerport"
  )}

  override def ivyDeps : T[Agg[Dep]] = T { super.ivyDeps() ++ Agg(
    Deps.activeMqClient,
    Deps.scalatest,
    Deps.slf4j,
    Deps.akkaActor,
    Deps.akkaStream,
    Deps.akkaSlf4j,
    Deps.logbackClassic,
    Deps.logbackCore,
    Deps.geronimoJ2eeMgmtSpec,
    Deps.geronimoJms11Spec,

    Deps.dockerJava,
    Deps.akkaTestkit,

    BlendedDeps.utilLogging(blendedCoreVersion()),
    BlendedDeps.jmsUtils(blendedCoreVersion()),
    BlendedDeps.util(blendedCoreVersion()),
    BlendedDeps.akka(blendedCoreVersion()),
    BlendedDeps.securitySsl(blendedCoreVersion()),
    BlendedDeps.streams(blendedCoreVersion()),

    BlendedDeps.testSupport(blendedCoreVersion()),
    BlendedDeps.streamsTestsupport(blendedCoreVersion()),
    BlendedDeps.itestSupport(blendedCoreVersion())
  )}
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

      override def profileName : T[String] = T { "node" }
      override def millSourcePath : os.Path = baseDir / "container" / "blended.demo.node"

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
        override def exposedPorts = Seq(1099, 1883, 1884, 1885, 1886, 8181, 8849, 9191, 9995, 9996)
      }
    }

    object mgmt extends BlendedContainer {

      override def profileName : T[String] = T { "mgmt" }
      override def millSourcePath : os.Path = baseDir / "container" / "blended.demo.mgmt"

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
        blended.launcher.feature.persistence
      )

      object docker extends Docker {
        override def exposedPorts = Seq(1099, 1883, 9191, 8849, 9995, 9996)
      }
    }
  }

  object itest extends Module {

    object node extends BlendedIntegrationTest {
      override def millSourcePath: Path = baseDir / "itest" / "blended.itest.node"
    }

    object mgmt extends BlendedIntegrationTest {


      override def ivyDeps : T[Agg[Dep]] = T { super.ivyDeps() ++ Agg(
        Deps.sttp,
        Deps.sttpAkka,
        Deps.microjson,
        Deps.prickle,
        Deps.lihaoyiPprint
      )}

      override def millSourcePath: Path = baseDir / "itest" / "blended.itest.mgmt"
    }
  }
}

