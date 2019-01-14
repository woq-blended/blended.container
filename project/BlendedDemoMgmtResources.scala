import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.universal.{UniversalDeployPlugin, UniversalPlugin}
import sbt.Keys._
import sbt._

object BlendedDemoMgmtResources extends ProjectFactory {

  private[this] val helper = new ProjectSettings(
    projectName = "blended.demo.mgmt.resources",
    description = "Resources for blended.demo.mgmt container",
    projectDir = Some("container/blended.demo.mgmt/blended.demo.mgmt.resources"),
  ) {

    override def extraPlugins: Seq[AutoPlugin] = Seq(
      UniversalPlugin,
      UniversalDeployPlugin
    )

    override def settings: Seq[sbt.Setting[_]] = {

      val a = Artifact(
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
        artifacts += a,
        packagedArtifacts := {
          packagedArtifacts.value updated(a, (Universal /packageBin).value)
        }
      )
    }

  }

  override val project = helper.baseProject

}
