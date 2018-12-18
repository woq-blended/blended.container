import sbt._

inThisBuild(BuildHelper.readVersion(file("version.txt")))

lazy val global = Def.settings(
  Global/scalariformAutoformat := false,
  Global/scalariformWithBaseDirectory := true,

  Global/testlogDirectory := target.value / "testlog",

  Global/useGpg := false,
  Global/pgpPublicRing := baseDirectory.value / "project" / ".gnupg" / "pubring.gpg",
  Global/pgpSecretRing := baseDirectory.value / "project" / ".gnupg" / "secring.gpg",
  Global/pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray)
)

lazy val blendedLauncherFeatures = BlendedLauncherFeatures.project
lazy val containerDemoNode = BlendedDemoNode.project

lazy val root = {
  project
    .in(file("."))
    .settings(global)
    .settings(PublishConfig.doPublish)
    .aggregate(
      blendedLauncherFeatures,
      containerDemoNode
    )

}