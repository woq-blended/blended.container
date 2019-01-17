import java.io.File
import java.net.URLClassLoader

import com.typesafe.sbt.packager.MappingsHelper
import de.wayofquality.sbt.filterresources.FilterResources
import de.wayofquality.sbt.filterresources.FilterResources.autoImport._
import sbt._
import sbt.Keys._
import sbt.librarymanagement.{UnresolvedWarning, UnresolvedWarningConfiguration, UpdateConfiguration}
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.universal.{UniversalDeployPlugin, UniversalPlugin}

object BlendedDemoNode extends ProjectFactory {

  private[this] val helper = new ProjectSettings(
    projectName = "blended.demo.node",
    description = "A sample container with some routes and Mgmt client functions",
    projectDir = Some("container/blended.demo.node"),
    features = Seq(
      Feature.blendedBaseFelix,
      Feature.blendedBaseEquinox,
      Feature.blendedBase,
      Feature.blendedCommons,
      Feature.blendedSsl,
      Feature.blendedJetty,
      Feature.blendedHawtio,
      Feature.blendedSpring,
      Feature.blendedActivemq,
      Feature.blendedCamel,
      Feature.blendedSecurity,
      Feature.blendedMgmtClient,
      Feature.blendedAkkaHttp,
      Feature.blendedPersistence,
      Feature.blendedStreams,
      Feature.blendedSamples
    ),
    deps = Seq(
      Dependencies.typesafeConfig,
      Dependencies.domino,
      Blended.launcher //.artifacts(Artifact(name = Blended.launcher.name, `type` = "zip", extension = "zip"))
    )
  ) {

    override def libDeps: Seq[sbt.ModuleID] = {
      println("libdeps: " + super.libDeps.mkString(" ,\n"))
      super.libDeps
    }

    val unpackLauncherZip = taskKey[File]("Unpack the launcher ZIP")

    val materializeDebug = settingKey[Boolean]("Enable debug mode")
    val materializeSourceProfile = settingKey[File]("Source profile")
    val materializeTargetDir = settingKey[File]("Target directory")
    val materializeToolsDeps = settingKey[Seq[ModuleID]]("Dependencies needed as tools classpath for the RuntimeConfigBuilder / Materializer")
    val materializeExplodeResources = settingKey[Boolean]("Should resources already be exploded")
    val materializeLaunchConf = settingKey[Option[File]]("The name of the optional created launch.conf file")

    val materializeProfile = taskKey[Unit]("Materialize the profile")
    val materializeExtraDeps = taskKey[Seq[(ModuleID, File)]]("Extra dependencies, which can't be expressed as libraryDependencies, e.g. other sub-projects for resources")
    val materializeToolsCp = taskKey[Seq[File]]("Tools Classpath for the RuntimeConfigBuilder / Materializer")

    val materializeOverlays = taskKey[Seq[(ModuleID, File)]]("Additional overlays that should be applied to the materialized profile")

    override def extraPlugins: Seq[AutoPlugin] = super.extraPlugins ++ Seq(
      FilterResources,
      UniversalPlugin,
      UniversalDeployPlugin
    )

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ {

      // [x] Unpack launcher zip
      // [x] declare resources zip as dep
      // [x] declare deps from features
      // [x] Materialize profile
      // [x] Build launch.conf
      // [ ] Build assembly (full-nojre)
      // [ ] Build deploymentpack

      Seq(
        Compile / filterSources := Seq(baseDirectory.value / "src" / "main" / "resources"),
        Compile / filterTargetDir := target.value / "filteredResources",
        Compile / filterProperties := Map(
          "profile.version" -> version.value,
          "profile.name" -> projectName,
          "blended.version" -> Blended.blendedVersion
        ),
        Compile / filterResourcesFailOnMissingMatch := false,

        unpackLauncherZip := {
          val log = streams.value.log

          val launcher = Blended.launcher.artifacts(Artifact(name = Blended.launcher.name, `type` = "zip", extension = "zip"))
          println("launcher: " + launcher + ", explicit artifacts: " + launcher.explicitArtifacts)

          val depRes = (Compile / dependencyResolution).value
          val files = depRes.retrieve(
            launcher.intransitive(), // .withExclusions(Vector(InclExclRule())),
            scalaModuleInfo.value,
            target.value / "dependencies",
            log
          ).toOption.get.distinct
          println("resolved: " + files)

          val destDir = target.value / "launcher"
          files.foreach { f =>
            IO.unzip(f, destDir)
          }

          destDir
        },

        materializeDebug := false,

        materializeTargetDir := target.value / "profile",

        materializeLaunchConf := Some(target.value / "container" / "launch.conf"),

        materializeSourceProfile := (Compile / filterTargetDir).value / "profile" / "profile.conf",

        materializeExtraDeps := {
          val file = (BlendedDemoNodeResources.project / Universal / packageBin).value
          //          val moduleId = Blended.blendedOrganization %% (BlendedDemoNodeResources.project / name).value % Blended.blendedVersion
          val artifact = (BlendedDemoNodeResources.project / artifacts).value.filter(a => a.`type` == "zip" && a.extension == "zip").head
          val moduleId: ModuleID = (Blended.blendedOrganization %% (BlendedDemoNodeResources.project / name).value % Blended.blendedVersion)

          Seq(moduleId.artifacts(artifact) -> file)
        },

        materializeToolsDeps := Seq(
          Blended.updaterTools
        ),

        materializeToolsCp := {
          val log = streams.value.log
          val depRes = (Compile / dependencyResolution).value

          materializeToolsDeps.value.flatMap { dep =>

            val resolved: Either[UnresolvedWarning, UpdateReport] =
              depRes.update(
                depRes.wrapDependencyInModule(dep, scalaModuleInfo.value),
                UpdateConfiguration(),
                UnresolvedWarningConfiguration(),
                log
              )

            val files = resolved match {
              case Right(report) =>
                val files: Seq[(ConfigRef, ModuleID, Artifact, File)] = report.toSeq
                files.map(_._4)
              case Left(w) => throw w.resolveException
            }

            files
          }
        },

        materializeExplodeResources := false,

        materializeProfile := {
          val log = streams.value.log

          // depend on filtered resources
          (Compile / filterResources).value

          log.debug("Running Mojo materialize-profile")

          val targetProfile = materializeTargetDir.value / "profile.conf"

          val srcProfile = materializeSourceProfile.value

          // Trigger feature file generation and add them to cmdline
          val featuresWithFile = (BlendedLauncherFeatures.project / BlendedLauncherFeatures.generateFeatureConfigs).value
          val featureFiles = featuresWithFile.map(_._2)
          log.debug(s"Feature files: ${featureFiles}")
          val featureArgs = featureFiles.flatMap { f =>
            Seq("--feature-repo", f.getAbsolutePath())
          }
          log.debug(s"feature args: ${featureArgs}")

          // experimental: trigger dep resolution
          val depCp = (Compile / dependencyClasspath).value
          log.debug(s"Dependency classpath: [${depCp}]")
          val depRes = (Compile / dependencyResolution).value

          // We need to declare all bundles as libraryDependencies!
          val libDeps: Seq[ModuleID] = (Compile / libraryDependencies).value
          val artifactArgs = libDeps.flatMap { dep: ModuleID =>
            val gav = moduleIdToGav(dep)

            val updateConfiguration = UpdateConfiguration()
            val unresolvedWarningConfiguration = UnresolvedWarningConfiguration()

            val resolved: Either[UnresolvedWarning, UpdateReport] = // BuildHelper.resolveModuleFile(
            //              depRes.retrieve(
            //                dep,
            //                scalaModuleInfo.value,
            //                target.value / "dependencies",
            //                log
            //              )
              depRes.update(
                depRes.wrapDependencyInModule(dep.intransitive(), scalaModuleInfo.value),
                updateConfiguration,
                unresolvedWarningConfiguration,
                log
              )
            val files = resolved match {
              case Right(report) =>
                val files: Seq[(ConfigRef, ModuleID, Artifact, File)] = report.toSeq
                files.map(_._4)
              case Left(w) => throw w.resolveException
            }
            files.headOption match {
              case Some(file) =>
                log.debug(s"Associated file with dependency: ${dep} => ${gav} --> ${file.getAbsolutePath()}")
                if (!file.exists()) {
                  log.error(s"Resolved file does not exist: ${file}")
                }
                Seq("--maven-artifact", gav, file.getAbsolutePath())
              case None =>
                log.warn(s"No file associated with dependency: ${dep} => ${gav}")
                Seq()
            }

          }
          log.debug(s"artifact args: ${artifactArgs}")

          val extraArtifactArgs = materializeExtraDeps.value.flatMap {
            case (moduleId, file) =>
              Seq("--maven-artifact", moduleIdToGav(moduleId), file.getAbsolutePath())
          }
          log.debug(s"extra artifact args: ${extraArtifactArgs}")

          val explodeResourcesArgs = if (materializeExplodeResources.value) Seq("--explode-resources") else Nil

          //          val overlayArgs =
          //          // prepend base dir if set
          //            Option(overlays).getOrElse(ju.Collections.emptyList()).asScala.map { o =>
          //              Option(overlaysDir) match {
          //                case None => o
          //                case _ if o.isAbsolute() => o
          //                case Some(f) => new File(f, o.getPath())
          //              }
          //            }.
          //              // create args
          //              flatMap(o => Seq("--add-overlay-file", o.getAbsolutePath())).toArray
          //

          val launchConfArgs = materializeLaunchConf.value.toList.flatMap { cf =>
            Option(cf.getParentFile()).foreach(_.mkdirs())
            Seq("--create-launch-config", cf.getAbsolutePath())
          }

          val debugArgs = if (materializeDebug.value) Seq("--debug") else Nil

          val profileArgs = Seq(
            Seq("-f", srcProfile.getAbsolutePath(),
              "-o", targetProfile.getAbsolutePath(),
              "--download-missing",
              "--update-checksums",
              "--write-overlays-config"),
            debugArgs,
            featureArgs,
            artifactArgs,
            extraArtifactArgs,
            explodeResourcesArgs,
            launchConfArgs
          ).flatten

          // ++ repoArgs ++ explodeResourcesArgs ++ overlayArgs ++ launchConfArgs

          log.debug("About to run RuntimeConfigBuilder.run with args: " + profileArgs.mkString("\n    "))

          //          try {

          // We use a separate Classloader with NULL as parent
          // to avoid classes with incompatible older versions of typesafe config in sbt
          val cl = new URLClassLoader(materializeToolsCp.value.map(_.toURI().toURL()).toArray, null)

          val builder = cl.loadClass("blended.updater.tools.configbuilder.RuntimeConfigBuilder")
          val runMethod = builder.getMethod("run", Seq(classOf[Array[String]]): _*)

          runMethod.invoke(null, profileArgs.toArray)

          // We can't call the tool directly, as some sbt deps (typesafe-config) collide
          //            RuntimeConfigBuilder.run(
          //              args = profileArgs.toArray,
          //              debugLog = Some(msg => log.debug(msg)),
          //              infoLog = msg => log.info(msg),
          //              errorLog = msg => log.error(msg)
          //            )

        },

        Universal / topLevelDirectory := None,

        Universal / packageBin / mappings := {
          // trigger
          (Compile / filterResources).value
          materializeProfile.value

          val launcherDir = unpackLauncherZip.value
          val containerResources = (Compile / filterTargetDir).value / "container"
          val profileDir = materializeTargetDir.value

          // mapping
          PathFinder(launcherDir).allPaths.pair(MappingsHelper.relativeTo(launcherDir)) ++
            PathFinder(containerResources).allPaths.pair(MappingsHelper.relativeTo(containerResources)) ++
            PathFinder(profileDir).allPaths.pair(
              f => IO.relativize(profileDir, f).map(p => s"profiles/${projectName}/${version.value}/${p}")
            ) ++
            Seq(profileDir -> s"profiles/${projectName}/${version.value}") ++
            materializeLaunchConf.value.toList.map(f => f -> f.getName())

        },

        artifacts ++= (Universal / packageBin / artifacts).value,

        packagedArtifacts := {
          // trigger
          (Universal / packageBin).value
          packagedArtifacts.value ++ (Universal / packageBin / packagedArtifacts).value
        }

      )
    }

  }

  def moduleIdToGav(dep: ModuleID): String = {
    val optExt = dep.explicitArtifacts.headOption.map { a => a.extension }
    val optClassifier = dep.explicitArtifacts.headOption.flatMap(a => a.classifier).filter(_ != "jar")
    s"${dep.organization}:${dep.name}:${optClassifier.getOrElse("")}:${dep.revision}:${optExt.getOrElse("jar")}"
  }

  override val project = helper.baseProject.dependsOn(
    BlendedDemoNodeResources.project,
    BlendedLauncherFeatures.project
  )

}
