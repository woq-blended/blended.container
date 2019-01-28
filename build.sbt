import sbt._

inThisBuild(BuildHelper.readVersion(file("version.txt")))
inThisBuild(sourcesInBase := false)

lazy val global = Def.settings(
  Global / scalariformAutoformat := false,
  Global / scalariformWithBaseDirectory := true,

  Global / testlogDirectory := target.value / "testlog",

  Global / useGpg := false,
  Global / pgpPublicRing := baseDirectory.value / "project" / ".gnupg" / "pubring.gpg",
  Global / pgpSecretRing := baseDirectory.value / "project" / ".gnupg" / "secring.gpg",
  Global / pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray),
  Global / resolvers += "SpringBundles" at "http://repository.springsource.com/maven/bundles/release",
  Global / resolvers += "SpringExternal" at "http://repository.springsource.com/maven/bundles/external"
)

lazy val blendedLauncherFeatures = BlendedLauncherFeatures.project
lazy val blendedDemoNodeResources = BlendedDemoNodeResources.project
lazy val blendedDemoMgmtResources = BlendedDemoMgmtResources.project
lazy val blendedDemoNode = BlendedDemoNode.project
lazy val blendedDemoMgmt = BlendedDemoMgmt.project
lazy val blendedDockerDemoNode = BlendedDockerDemoNode.project
lazy val blendedDockerDemoMgmt = BlendedDockerDemoMgmt.project
lazy val blendedDockerDemoApacheds = BlendedDockerDemoApacheds.project
lazy val blendedItestNode = BlendedItestNode.project
lazy val blendedItestMgmt = BlendedItestMgmt.project

lazy val root = {
  project
    .in(file("."))
    .settings(global)
    .settings(PublishConfig.doPublish)
    .aggregate(
      blendedLauncherFeatures,
      blendedDemoNodeResources,
      blendedDemoMgmtResources,
      blendedDemoNode,
      blendedDemoMgmt,
      blendedDockerDemoNode,
      blendedDockerDemoMgmt,
      blendedDockerDemoApacheds,
      blendedItestNode,
      blendedItestMgmt
    )

}