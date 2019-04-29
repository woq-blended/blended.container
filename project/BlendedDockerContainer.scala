import java.io.File

import scala.sys.process.Process

import com.typesafe.sbt.packager.universal.{UniversalDeployPlugin, UniversalPlugin}
import de.wayofquality.sbt.filterresources.FilterResources
import sbt.Keys._
import sbt._
import blended.sbt.dockercontainer.BlendedDockerContainerPlugin
import blended.sbt.dockercontainer.BlendedDockerContainerPlugin.{autoImport => DC}
import blended.sbt.container.BlendedContainerPlugin.{autoImport => BC}

trait BlendedDockerContainer extends ProjectSettings {

  ///////

  def imageTag: String

  def folder: String

  def ports: List[Int] = List()

  /**
   * Filenames to overlays, relative to the project directory.
   */
  def overlays: Seq[String] = Seq()

  def env: Map[String, String] = Map()

  def profileName: Option[String] = None

  def maintainer: String = s"Blended Team version: ${Blended.blendedVersion}"

  override def plugins: Seq[AutoPlugin] = super.plugins ++ Seq(BlendedDockerContainerPlugin)

  //////

  override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
    Compile / packageBin / publishArtifact := false,
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false,

    BC.blendedVersion := Blended.blendedVersion,
    DC.appFolder := folder,
    DC.imageTag := imageTag,
    DC.ports := ports,
    DC.overlays := overlays.map(o => target.value / o),
    DC.env := env,
    DC.profile := profileName.map(n => n -> version.value),
    DC.maintainer := maintainer
  )

  def containerDep: Option[ModuleID] = None

  override def description = s"Docker container for container ${containerDep.getOrElse("")}"

  override def deps: Seq[sbt.ModuleID] = containerDep.toList

  override def osgi = false
  
}
