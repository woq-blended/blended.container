import blended.sbt.container.BlendedContainerPlugin
import sbt._

class BlendedContainer(
  projectName: String,
  description: String,
  //  features: Seq[Feature] = Seq.empty,
  deps: Seq[ModuleID] = Seq.empty,
  publish: Boolean = true,
  projectDir: Option[String] = None
) extends ProjectSettings(
  projectName = projectName,
  description = description,
  //  features = features,
  deps = deps,
  osgi = false,
  osgiDefaultImports = false,
  publish = publish,
  adaptBundle = identity,
  projectDir = projectDir
) {

  override def plugins: Seq[AutoPlugin] = super.plugins ++ Seq(BlendedContainerPlugin)


}
