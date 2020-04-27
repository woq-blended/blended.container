import mill.scalalib.Dep

object FeatureBundle {
  def apply(dependency : Dep) : FeatureBundle =
    FeatureBundle(dependency, startLevel = None, start = false)

  def apply(dependency : Dep, level : Int, start : Boolean) : FeatureBundle =
    FeatureBundle(dependency, startLevel = Some(level), start = start)

  implicit def rw : upickle.default.ReadWriter[FeatureBundle] = upickle.default.macroRW
}

case class FeatureBundle private (
  dependency: Dep,
  startLevel: Option[Int],
  start: Boolean
) {

  def formatConfig(scalaBinVersion : String): String = {

    val builder: StringBuilder = new StringBuilder("    { ")

    val classifier : String = dependency.dep.attributes.classifier.value

    builder.append("url=\"")
    builder.append("mvn:")

    builder.append(dependency.dep.module.orgName)
    if (dependency.cross.isBinary) {
      builder.append(s"_$scalaBinVersion")
    }

    builder.append(":")
    if (classifier.nonEmpty) {
      builder.append(":")
    }

    builder.append(dependency.dep.version)

    if (classifier.nonEmpty) {
      builder.append(s":$classifier")
    }

    builder.append("\"")

    startLevel.foreach { sl => builder.append(s", startLevel=${sl}") }
    if (start) builder.append(", start=true")

    builder.append(" }")

    builder.toString()
  }
}
