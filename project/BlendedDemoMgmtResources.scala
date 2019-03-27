import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.universal.{UniversalDeployPlugin, UniversalPlugin}
import phoenix.ProjectFactory
import sbt.Keys._
import sbt._

object BlendedDemoMgmtResources extends ProjectFactory {

  object config extends ProjectSettings {

    override val projectName = "blended.demo.mgmt.resources"
    override val description = "Resources for blended.demo.mgmt container"
    override val projectDir = Some("container/blended.demo.mgmt/blended.demo.mgmt.resources")

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
          packagedArtifacts.value updated (artifact, (Universal / packageBin).value)
        }
      )
    }

  }

}
