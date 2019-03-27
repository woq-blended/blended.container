import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.universal.{UniversalDeployPlugin, UniversalPlugin}

object BlendedDemoNodeResources extends ProjectFactory {

  private[this] val helper = new ProjectSettings(
    projectName = "blended.demo.node.resources",
    description = "Resources for blended.demo.node container",
    projectDir = Some("container/blended.demo.node/blended.demo.node.resources"),
  ) {

    override def plugins: Seq[AutoPlugin] = super.plugins ++ Seq(
      UniversalPlugin,
      UniversalDeployPlugin
    )

    override def settings: Seq[sbt.Setting[_]] = {

      val artifact = Artifact(
        name = projectName,
        `type` = "zip",
        extension = "zip")


      super.settings ++ Seq(

        Universal / topLevelDirectory := None,

        // Use default mapping (from JAR) for ZIP
        Universal / packageBin / mappings := (Compile / packageBin / mappings).value,

        // trigger zip file packaging
        Compile / packageBin := (Universal / packageBin).value,

        // disable publishing the main jar produced by `package`
        Compile / packageBin / publishArtifact := false,

        // Attach the zip file as main artifact
        artifacts += artifact,
        packagedArtifacts := {
          packagedArtifacts.value updated(artifact, (Universal /packageBin).value)
        }
      )
    }

  }

  override val project = helper.baseProject

}
