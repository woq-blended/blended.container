import java.io.File

import blended.updater.tools.configbuilder.RuntimeConfigBuilder
import de.wayofquality.sbt.filterresources.FilterResources
import de.wayofquality.sbt.filterresources.FilterResources.autoImport._
import sbt._
import sbt.Keys._
import sbt.librarymanagement.{InclExclRule, UnresolvedWarning}

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

    val unpackLauncherZip = taskKey[Unit]("Unpack the launcher ZIP")
    val materializeProfile = taskKey[Unit]("Materialize the profile")
    val materializeSourceProfile = settingKey[File]("Source profile")
    val materializeTargetDir = settingKey[File]("Target directory")
    val materializeDebug = settingKey[Boolean]("Enable debug mode")

    override def extraPlugins: Seq[AutoPlugin] = super.extraPlugins ++ Seq(
      FilterResources
    )

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ {

      // Unpack launcher zip
      // declare resources zip as dep
      // declare deps from features

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

          val launcher = Blended.launcher // .artifacts(Artifact(name = Blended.launcher.name, `type` = "zip", extension = "zip"))
          println("launcher: " + launcher + ", explicit artifacts: " + launcher.explicitArtifacts)

          val files = BuildHelper.resolveModuleFile(
            launcher,
            //            scalaModuleInfo.value,
            target.value
          ).distinct
          println("resolved: " + files)
          val destDir = target.value / "launcher"
          files.foreach { f =>
            IO.unzip(f, destDir)
          }
        },

        materializeDebug := false,

        materializeTargetDir := target.value / "profile",

        materializeSourceProfile := (Compile / filterTargetDir).value / "profile" / "profile.conf",

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
          log.info(s"feature args: ${featureArgs}")


          // experimental: trigger dep resolution
          val depCp = (Compile / dependencyClasspath).value
          log.info(s"Dependency classpath: [${depCp}]")
          val depRes = (Compile / dependencyResolution).value

          // We need to declare all bundles as libraryDependencies!
          val libDeps: Seq[ModuleID] = (Compile / libraryDependencies).value
          val artifactArgs = libDeps.flatMap { dep: ModuleID =>
            val optType = dep.explicitArtifacts.headOption.map { a => a.`type` }
            val optClassifier = dep.explicitArtifacts.headOption.flatMap(a => a.classifier).filterNot(_ == "jar")

            val gav = s"${dep.organization}:${dep.name}:${optClassifier.getOrElse("")}:${dep.revision}:${optType.getOrElse("")}"
            val resolved: Either[UnresolvedWarning, Vector[File]] = // BuildHelper.resolveModuleFile(
              depRes.retrieve(
                dep, // .withExclusions(Vector(InclExclRule())),
                scalaModuleInfo.value,
                target.value,
                log
              )
            val files = resolved match {
              case Right(files) => files.distinct
              case Left(w) => throw w.resolveException
            }
            files.headOption match {
              case Some(file) =>
                log.info(s"Associated file with dependency: ${dep} => ${gav} --> ${file.getAbsolutePath()}")
                Seq("--maven-artifact", gav, file.getAbsolutePath())
              case None =>
                log.warn(s"No file associated with dependency: ${dep} => ${gav}")
                Seq()
            }

          }
          log.info(s"artifact args: ${artifactArgs}")

          //          val repoArgs =
          ////            if (resolveFromDependencies) {
          //            (Compile / libraryDependencies).value
          //          project.getArtifacts.asScala.toArray.flatMap { a =>
          //            Array(
          //                "--maven-artifact",
          //                s"${a.getGroupId}:${a.getArtifactId}:${Option(a.getClassifier).filter(_ != "jar").getOrElse("")}:${a.getVersion}:${Option(a.getType).getOrElse("")}",
          //                a.getFile.getAbsolutePath
          //          )
          //        }
          //                } else {
          ////            Array("--maven-url", localRepoUrl) ++ remoteRepoUrls.toArray.flatMap(u => Array("--maven-url", u))
          //        }
          //          getLog.debug("repo args: " + repoArgs.mkString("Array(", ", ", ")"))
          //
          //          val explodeResourcesArgs = if (explodeResources) Array("--explode-resources") else Array[String]()
          //
          //          val debugArgs = if (debug) Array("--debug") else Array[String]()
          //
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
          //          val launchConfArgs = Option(createLaunchConfig).toList.flatMap(cf => Seq("--create-launch-config", cf.getPath())).toArray
          //
          //          val profileArgs = Array(
          //            "-f", srcProfile.getAbsolutePath,
          //            "-o", targetProfile.getAbsolutePath,
          //            "--download-missing",
          //            "--update-checksums",
          //            "--write-overlays-config"
          //          ) ++ debugArgs ++ featureArgs ++ repoArgs ++ explodeResourcesArgs ++ overlayArgs ++ launchConfArgs
          //

          val debugArgs = if (materializeDebug.value) Seq("--debug") else Nil

          val profileArgs = Seq(
            Seq("-f", srcProfile.getAbsolutePath(),
              "-o", targetProfile.getAbsolutePath(),
              "--download-missing",
              "--update-checksums",
              "--write-overlays-config"
            ),
            debugArgs,
            featureArgs,
            artifactArgs
          ).flatten

          // ++ repoArgs ++ explodeResourcesArgs ++ overlayArgs ++ launchConfArgs

          log.info("About to run RuntimeConfigBuilder.run with args: " + profileArgs)

          RuntimeConfigBuilder.run(
            args = profileArgs.toArray,
            debugLog = Some(msg => log.debug(msg)),
            infoLog = msg => log.info(msg),
            errorLog = msg => log.error(msg)
          )

        }

      )
    }

  }

  override val project = helper.baseProject.dependsOn(
    BlendedDemoNodeResources.project,
    BlendedLauncherFeatures.project
  )

}
