import blended.sbt.container.BlendedContainerPlugin
import sbt._

trait BlendedContainer extends ProjectSettings {
  override def osgi = false
  override def osgiDefaultImports = false
  override def plugins: Seq[AutoPlugin] = super.plugins ++ Seq(BlendedContainerPlugin)

}
