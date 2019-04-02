import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.universal.{UniversalDeployPlugin, UniversalPlugin}
import phoenix.ProjectFactory

object BlendedDemoNodeResources extends ProjectFactory {

  object config extends ProjectSettings {
    override val projectName = "blended.demo.node.resources"
    override val description = "Resources for blended.demo.node container"
    override val projectDir = Some("container/blended.demo.node/blended.demo.node.resources")

    override def plugins: Seq[AutoPlugin] = super.plugins ++ Seq(
      UniversalPlugin,
      UniversalDeployPlugin
    )

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ {

      val artifact = Artifact(
        name = projectName,
        `type` = "zip",
        extension = "zip"
      )

      Seq(

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
          packagedArtifacts.value updated (artifact, (Universal / packageBin).value)
        }
      )
    }

  }
}
