import org.sonatype.maven.polyglot.scala.model._
import scala.collection.immutable.Seq

//#include ../../blended.build/build-versions.scala
//#include ../../blended.build/build-dependencies.scala
//#include ../../blended.build/build-plugins.scala
//#include ../../blended.build/build-common.scala

val deploymentPackForTest = Dependency(
      gav = Blended.demoNode,
      scope = "test",
      classifier = "deploymentpack",
      `type` = "zip"
    )

BlendedModel(
  gav = Blended.itestMgmt,
  packaging = "jar",
  description = "A sample integration test using docker to fire up the container(s) under test, execute the test suite and shutdown the container(s) afterwards.",
  dependencies = Seq(
    Deps.scalaLib,
    Blended.utilLogging,
    Blended.dockerDemoNode % "provided",
    Blended.dockerDemoMgmt % "provided",
    Deps.activeMqClient % "test",
    Blended.itestSupport % "test",
    Deps.scalaTest % "test",
    Deps.slf4j % "test",
    Deps.akkaSlf4j % "test",
    Deps.logbackCore % "test",
    Deps.logbackClassic % "test",
    Deps.sttp % "test",
    // we use this in test cases as resource, but express it as dependency to force proper build order
    deploymentPackForTest % "test"
  ),
  plugins = Seq(
    scalaCompilerPlugin,
    scalatestMavenPlugin,
    Plugin(
      gav = Plugins.dependency,
      executions = Seq(
        Execution(
          id = "copy-deploymentpack",
          phase = "process-test-resources",
          goals = Seq("copy"),
          configuration = Config(
            artifactItems = Config(
              artifactItem = Config(
                groupId = deploymentPackForTest.gav.groupId.get,
                artifactId = deploymentPackForTest.gav.artifactId,
                version = deploymentPackForTest.gav.version.get,
                classifier = deploymentPackForTest.classifier,
                `type` = deploymentPackForTest.`type`,
                outputDirectory = "${project.build.testOutputDirectory}",
                destFileName = s"${deploymentPackForTest.gav.artifactId}${deploymentPackForTest.classifier.map(c => s"-${c}").getOrElse("")}.${deploymentPackForTest.`type`}"
              )
            )
          )
        )
      )
    )
  )
)
