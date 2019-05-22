import sbt.Keys._
import sbt._
import blended.sbt.phoenix.docker.DockerContainer

trait BlendedDockerContainer extends ProjectSettings with DockerContainer {

  override def blendedVersion = Blended.blendedVersion

  override def maintainer: String = s"Blended Team version: ${Blended.blendedVersion}"

  override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(
    Compile / packageBin / publishArtifact := false,
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false
  )

  def containerDep: Option[ModuleID] = None

  override def description = s"Docker container for container ${containerDep.getOrElse("")}"

  override def deps: Seq[sbt.ModuleID] = containerDep.toList

  override def osgi = false

}
