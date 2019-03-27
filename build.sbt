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

lazy val blendedLauncherFeatureActivemq = BlendedLauncherFeatureActivemq.project
lazy val blendedLauncherFeatureAkkaHttp = BlendedLauncherFeatureAkkaHttp.project
lazy val blendedLauncherFeatureBase = BlendedLauncherFeatureBase.project
lazy val blendedLauncherFeatureBaseEquinox = BlendedLauncherFeatureBaseEquinox.project
lazy val blendedLauncherFeatureBaseFelix = BlendedLauncherFeatureBaseFelix.project
lazy val blendedLauncherFeatureCamel = BlendedLauncherFeatureCamel.project
lazy val blendedLauncherFeatureCommons= BlendedLauncherFeatureCommons.project
lazy val blendedLauncherFeatureHawtio = BlendedLauncherFeatureHawtio.project
lazy val blendedLauncherFeatureJetty = BlendedLauncherFeatureJetty.project
lazy val blendedLauncherFeatureMgmtClient = BlendedLauncherFeatureMgmtClient.project
lazy val blendedLauncherFeatureMgmtServer = BlendedLauncherFeatureMgmtServer.project
lazy val blendedLauncherFeaturePersistence = BlendedLauncherFeaturePersistence.project
lazy val blendedLauncherFeatureSamples = BlendedLauncherFeatureSamples.project
lazy val blendedLauncherFeatureSecurity = BlendedLauncherFeatureSecurity.project
lazy val blendedLauncherFeatureSpring = BlendedLauncherFeatureSpring.project
lazy val blendedLauncherFeatureSsl = BlendedLauncherFeatureSsl.project
lazy val blendedLauncherFeatureStreams = BlendedLauncherFeatureStreams.project

lazy val blendedLauncherFeature = project.settings(CommonSettings()).aggregate(
  blendedLauncherFeatureActivemq,
  blendedLauncherFeatureAkkaHttp,
  blendedLauncherFeatureBase,
  blendedLauncherFeatureBaseEquinox,
  blendedLauncherFeatureBaseFelix,
  blendedLauncherFeatureCamel,
  blendedLauncherFeatureHawtio,
  blendedLauncherFeatureJetty,
  blendedLauncherFeatureMgmtClient,
  blendedLauncherFeatureMgmtServer,
  blendedLauncherFeaturePersistence,
  blendedLauncherFeatureSamples,
  blendedLauncherFeatureSecurity,
  blendedLauncherFeatureSpring,
  blendedLauncherFeatureSsl,
  blendedLauncherFeatureStreams
)

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
      blendedLauncherFeature,
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