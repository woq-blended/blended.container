import de.wayofquality.sbt.testlogconfig.TestLogConfig
import de.wayofquality.sbt.testlogconfig.TestLogConfig.autoImport._
import com.typesafe.sbt.osgi.SbtOsgi
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import sbt.internal.inc.Analysis
import sbt.librarymanagement.InclExclRule
import xerial.sbt.Sonatype
import xsbti.api.{AnalyzedClass, Projection}

import blended.sbt.feature._

trait ProjectFactory {
  def project: Project
}

trait ProjectCreator {

  /**
    * Override this method to customize the creation of this project.
    */
  def createProject(): Project = {
    val name = projectName.split("[._]").foldLeft("") {
      case ("", next) => next
      case (name, next) => name + next.capitalize
    }
    Project(name, file(projectDir.getOrElse(projectName)))
  }

  def projectName: String

  def projectDir: Option[String] = None

  def settings: Seq[sbt.Setting[_]] = CommonSettings() ++ Seq(
    Keys.name := projectName,
    Keys.moduleName := Keys.name.value
  )

  def plugins: Seq[AutoPlugin] = Seq()

  // creates the project and apply settings and plugins
  def baseProject: Project = createProject()
    .settings(settings)
    .enablePlugins(plugins: _*)

}

/**
  * Blended project settings.
  *
  * @param projectName The Project name, also used as Bundle-Name and prefix for package names.
  * @param description The project description, also used as Bundle-Description.
  * @param deps        The project classpath dependencies (exclusive of other blended projects).
  * @param osgi        If `true` this project is packaged as OSGi Bundle.
  * @param publish     If `true`, this projects package will be publish.
  * @param adaptBundle adapt the bundle configuration (used by sbt-osgi)
  * @param projectDir  Optional project directory (use this if not equal to project name)
  */
class ProjectSettings(
                       override val projectName: String,
                       val description: String,
                       val features: Seq[Feature] = Seq.empty,
                       deps: Seq[ModuleID] = Seq.empty,
                       osgi: Boolean = true,
                       osgiDefaultImports: Boolean = true,
                       publish: Boolean = true,
                       adaptBundle: BlendedBundle => BlendedBundle = identity,
                       override val projectDir: Option[String] = None
                     ) extends ProjectCreator {

  def libDeps: Seq[ModuleID] = deps ++ features.flatMap { f =>
    f.libDeps
  }.map(_.intransitive())

  def defaultBundle: BlendedBundle = BlendedBundle(
    bundleSymbolicName = projectName,
    exportPackage = Seq(projectName),
    privatePackage = Seq(s"${projectName}.internal.*"),
    defaultImports = osgiDefaultImports
  )

  def bundle: BlendedBundle = adaptBundle(defaultBundle)

  def sbtBundle: Option[BlendedBundle] = if (osgi) Some(bundle) else None

  private def hasForkAnnotation(clazz: AnalyzedClass): Boolean = {

    val c = clazz.api().classApi()

    c.annotations.exists { ann =>
      ann.base() match {
        case proj: Projection if proj.id() == "RequiresForkedJVM" => true
        case _ => false
      }
    }
  }

  def defaultSettings: Seq[Setting[_]] = super.settings ++ {

    val osgiSettings: Seq[Setting[_]] = sbtBundle.toSeq.flatMap(_.osgiSettings)

    Seq(
      Keys.name := projectName,
      Keys.moduleName := Keys.name.value,
      Keys.description := description,
      Keys.libraryDependencies ++= libDeps,
      Test / javaOptions += ("-DprojectTestOutput=" + (Test / classDirectory).value),

      javaOptions += s"-Ddocker.host=${System.getProperty("docker.host", "localhost")}",
      javaOptions += s"-Ddocker.port=${System.getProperty("docker.port", "4243")}",

      Test / fork := true,
      Test / parallelExecution := false,
      Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "binaryResources",
      Test / unmanagedResourceDirectories += baseDirectory.value / "src" / "test" / "binaryResources",

      Test / testlogDirectory := (Global / testlogDirectory).value,
      Test / testlogLogToConsole := false,
      Test / testlogLogToFile := true,

      Test / resourceGenerators += (Test / testlogCreateConfig).taskValue,

      // inspired by : https://chariotsolutions.com/blog/post/sbt-group-annotated-tests-run-forked-jvms 
      Test / testGrouping := {

        val log = streams.value.log

        val options = (Test / javaOptions).value.toVector

        val annotatedTestNames: Seq[String] = (Test / compile).value.asInstanceOf[Analysis]
          .apis.internal.values.filter(hasForkAnnotation).map(_.name()).toSeq

        val (forkedTests, otherTests) = (Test / definedTests).value.partition { t =>
          annotatedTestNames.contains(t.name)
        }

        val combined: Tests.Group = new Group(
          name = "Combined",
          tests = otherTests,
          runPolicy = SubProcess(config = ForkOptions.apply().withRunJVMOptions(options))
        )

        val forked: Seq[Tests.Group] = forkedTests.map { t =>
          new Group(
            name = t.name,
            tests = Seq(t),
            runPolicy = SubProcess(config = ForkOptions.apply().withRunJVMOptions(options))
          )
        }

        if (forkedTests.nonEmpty) {
          log.info(s"Forking extra JVM for test [${annotatedTestNames.mkString(",")}]")
        }

        forked ++ Seq(combined)
      }

    ) ++ osgiSettings ++ (
      if (publish) PublishConfig.doPublish else PublishConfig.noPublish
      )
  }

  override def settings: Seq[sbt.Setting[_]] = defaultSettings

  override def plugins: Seq[AutoPlugin] = super.plugins ++
    Seq(TestLogConfig) ++
    (if (publish) Seq(Sonatype) else Seq()) ++
    (if (osgi) Seq(SbtOsgi) else Seq())


}
