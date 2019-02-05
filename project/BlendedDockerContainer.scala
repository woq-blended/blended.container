import java.io.File

import scala.sys.process.Process

import com.typesafe.sbt.packager.universal.{UniversalDeployPlugin, UniversalPlugin}
import de.wayofquality.sbt.filterresources.FilterResources
import sbt.Keys._
import sbt._

class BlendedDockerContainer(
  projectName: String,
  description: String = null,
  containerDep: Option[ModuleID] = None,
  imageTag: String,
  publish: Boolean = true,
  projectDir: Option[String] = None,
  val ports: List[Int] = List(),
  val folder: String,
  overlays: List[String] = List()
) extends ProjectSettings(
  projectName = projectName,
  description = Option(description).getOrElse(s"Docker container for container ${containerDep.getOrElse("")}"),
  features = Seq(),
  deps = containerDep.toList,
  osgi = false,
  osgiDefaultImports = false,
  publish = publish,
  adaptBundle = identity,
  projectDir = projectDir
) {

  import BlendedDockerContainer._

  override def extraPlugins: Seq[AutoPlugin] = super.extraPlugins ++ Seq(
    FilterResources,
    UniversalPlugin,
    UniversalDeployPlugin
  )

  override def settings: Seq[sbt.Setting[_]] = super.settings ++ {

    Seq(
      Compile / packageBin / publishArtifact := false,
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false,

      //      containerImageTgz := {
      //        val log = streams.value.log
      //
      //        // by default, download the dependencies given with containerDep
      //        containerDep.map { container =>
      //          container.artifacts(Artifact(name = container.name, `type` = "tag.gz", extension = "tar.gz"))
      //          log.info("Container: " + container + ", explicit artifacts: " + container.explicitArtifacts)
      //
      //          val depRes = (Compile / dependencyResolution).value
      //          val files = depRes.retrieve(
      //            container.intransitive(),
      //            scalaModuleInfo.value,
      //            target.value / "dependencies",
      //          ).toOption.get.distinct
      //          log.info("resolved: " + files)
      //
      //          files.head
      //        }.get
      //      },

      dockerDir := target.value / "docker",

      //      unpackContainer := {
      //        val log = streams.value.log
      //
      //        val file = containerImage.value._2
      //        val destDir = dockerDir.value / "image"
      //        BuildHelper.unpackTarGz(file, destDir)
      //        destDir
      //      },

      generateDockerfile := {
        val log = streams.value.log

        // make Dockerfile

        val dockerfile = dockerDir.value / "Dockerfile"

        val dockerconf = Seq(
          "FROM atooni/blended-base:latest",
          s"MAINTAINER Blended Team version: ${Blended.blendedVersion}",
          s"ADD ${containerImage.value._2.getName()} /opt",
          s"RUN ln -s /opt/${containerImage.value._1} /opt/${folder}",
          s"RUN chown -R blended.blended /opt/${containerImage.value._1}",
          s"RUN chown -R blended.blended /opt/${folder}",
          "USER blended",
          "ENV JAVA_HOME /opt/java",
          "ENV PATH ${PATH}:${JAVA_HOME}/bin",
          s"""ENTRYPOINT ["/bin/sh", "/opt/${folder}/bin/blended.sh"]"""
        ) ++
          ports.map(p => s"EXPOSE ${p}")

        IO.write(dockerfile, dockerconf.mkString("\n"))

        dockerfile
      },

      createDockerImage := {
        val log = streams.value.log

        // trigger dockerfile generator
        generateDockerfile.value

        // copy container pack into docker dir
        IO.copyFile(containerImage.value._2, dockerDir.value / containerImage.value._2.getName())

        log.info(s"Creating docker image ${imageTag}")
        Process(
          command = List("docker", "build", "-t", imageTag, "."),
          cwd = Some(dockerDir.value)
        ) ! log
        log.info(s"Created docker image ${imageTag}")

      }
    )

  }

  def moduleIdToGav(dep: ModuleID): String = {
    val optExt = dep.explicitArtifacts.headOption.map { a => a.extension }
    val optClassifier = dep.explicitArtifacts.headOption.flatMap(a => a.classifier).filter(_ != "jar")
    s"${dep.organization}:${dep.name}:${optClassifier.getOrElse("")}:${dep.revision}:${optExt.getOrElse("jar")}"
  }

  def validateMapping(mapping: Seq[(File, String)], log: Logger): Unit = {
    // security measure
    var entries = Set[String]()
    mapping.foreach {
      case (k, v) =>
        if (k.isFile()) {
          if (entries.contains(v)) {
            log.warn(s"The resulting mapping will contain colliding entry [${v}] from: [${mapping.filter(_._2 == v).map(_._1)}]")
          } else {
            entries += v
          }
        }
    }
  }

}

object BlendedDockerContainer {

  val dockerDir = settingKey[File]("The base directory for the docker image content")
  val containerImage = taskKey[(String, File)]("The container image and it's folder name")
  val generateDockerfile = taskKey[File]("Generate to dockerfile")
  //  val unpackContainer = taskKey[File]("Unpack the container archive")
  val createDockerImage = taskKey[Unit]("Create the docker image")
}