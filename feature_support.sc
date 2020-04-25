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

    builder.append("url=\"")
    builder.append("mvn:")

    builder.append(dependency.dep.module.orgName)
    if (dependency.cross.isBinary) {
      builder.append(s"_$scalaBinVersion")
    }

    builder.append(":")

    builder.append(dependency.dep.version)

    builder.append("\"")

    startLevel.foreach { sl => builder.append(s", startLevel=${sl}") }
    if (start) builder.append(", start=true")

    builder.append(" }")

    builder.toString()
  }
}

//object FeatureBundle {
//  /**
//   *
//   * @param moduleID
//   * @param scalaBinVersion We hard-code the default, to avoid to make this def a sbt setting.
//   * @return
//   */
//  def artifactNameSuffix(moduleID: Dependency, scalaBinVersion: String = "2.12"): String = moduleID.crossVersion match {
//    case b: Binary => s"_${b.prefix}${scalaBinVersion}${b.suffix}"
//    case c: Constant => s"_${c.value}"
//    case _ => ""
//  }
//}

case class Feature(name: String, features: Seq[Feature] = Seq(), bundles: Seq[FeatureBundle], featureRefs: Seq[FeatureRef] = Seq()) {

  def libDeps: Seq[Dep] = (features.flatMap(_.libDeps) ++ bundles.map(_.dependency)).distinct

  // This is the content of the feature file
  def formatConfig(version: String): String = {
    val prefix =
      s"""name="${name}"
         |version="${version}"
         |""".stripMargin

    val bundlesList = bundles.map(_.toString).mkString(
      "bundles = [\n", ",\n", "\n]\n"
    )

    val fRefs = featureRefs ++ features.map(f => FeatureRef(f.name))

    val fRefString =
      if (fRefs.isEmpty) ""
      else fRefs.map(f => s"""{ name="${f.name}", version="${version}" }""").mkString(
        "features = [\n", ",\n", "\n]\n"
      )

    prefix + fRefString + bundlesList
  }
}

case class FeatureRef(name: String)