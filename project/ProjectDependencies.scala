import phoenix.Utils
import sbt._

object ProjectDependencies extends blended.sbt.Dependencies {

  val activeMqOsgi = "org.apache.activemq" % "activemq-osgi" % activeMqVersion
  val activationApi = "org.apache.servicemix.specs" % "org.apache.servicemix.specs.activation-api-1.1" % "2.2.0"
  val akkaProtobuf = akka("protobuf")
  val aopAlliance = "org.apache.servicemix.bundles" % "org.apache.servicemix.bundles.aopalliance" % "1.0_6"
  val ariesBlueprintApi = "org.apache.aries.blueprint" % "org.apache.aries.blueprint.api" % "1.0.1"
  val ariesBlueprintCore = "org.apache.aries.blueprint" % "org.apache.aries.blueprint.core" % "1.4.3"
  val ariesJmxApi = "org.apache.aries.jmx" % "org.apache.aries.jmx.api" % "1.1.1"
  val ariesJmxCore = "org.apache.aries.jmx" % "org.apache.aries.jmx.core" % "1.1.1"
  val ariesProxyApi = "org.apache.aries.proxy" % "org.apache.aries.proxy.api" % "1.0.1"
  val ariesUtil = "org.apache.aries" % "org.apache.aries.util" % "1.1.0"
  val asmAll = "org.ow2.asm" % "asm-all" % "4.1"

  val camelSpring = "org.apache.camel" % "camel-spring" % camelVersion
  val commonsConfiguration2 = "org.apache.commons" % "commons-configuration2" % "2.2"
  val commonsCollections = "org.apache.commons" % "com.springsource.org.apache.commons.collections" % "3.2.1"
  val commonsDiscovery = "org.apache.commons" % "com.springsource.org.apache.commons.discovery" % "0.4.0"
  val commonsExec = "org.apache.commons" % "commons-exec" % "1.3"
  val commonsHttpclient = "org.apache.commons" % "com.springsource.org.apache.commons.httpclient" % "3.1.0"
  val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.7"
  val commonsLang = commonsLang3
  val commonsNet = "commons-net" % "commons-net" % "3.3"
  val commonsPool2 = "org.apache.commons" % "commons-pool2" % "2.6.0"

  // provides Equinox console commands to gogo shell
  val eclipseEquinoxConsole = "org.eclipse.platform" % "org.eclipse.equinox.console" % "1.1.300"
  val eclipseOsgi = "org.eclipse.platform" % "org.eclipse.osgi" % "3.12.50"
  val equinoxServlet = "org.eclipse.platform" % "org.eclipse.equinox.http.servlet" % "1.4.0"

  val felixConfigAdmin = "org.apache.felix" % "org.apache.felix.configadmin" % "1.8.6"
  val felixEventAdmin = "org.apache.felix" % "org.apache.felix.eventadmin" % "1.3.2"
  val felixHttpApi = "org.apache.felix" % "org.apache.felix.http.api" % "3.0.0"
  val felixMetatype = "org.apache.felix" % "org.apache.felix.metatype" % "1.0.12"

  val geronimoAnnotation = "org.apache.geronimo.specs" % "geronimo-annotation_1.1_spec" % "1.0.1"
  val geronimoJaspic = "org.apache.geronimo.specs" % "geronimo-jaspic_1.0_spec" % "1.1"
  val geronimoJ2eeMgmtSpec = "org.apache.geronimo.specs" % "geronimo-j2ee-management_1.1_spec" % "1.0.1"

  val hawtioWeb = "io.hawt" % "hawtio-web" % "1.5.11" withExplicitArtifacts (Vector(Artifact("hawtio-web", `type` = "war", extension = "war")))

  protected def jetty(n: String) = "org.eclipse.jetty" % s"jetty-${n}" % jettyVersion

  val jacksonCore = "com.fasterxml.jackson.core" % "jackson-core" % "2.9.3"
  val jacksonBind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.3"
  val jacksonAnnotations = "com.fasterxml.jackson.core" % "jackson-annotations" % "2.9.3"
  val javaxMail = "javax.mail" % "mail" % "1.4.5"
  val javaxServlet31 = "org.everit.osgi.bundles" % "org.everit.osgi.bundles.javax.servlet.api" % "3.1.0"
  val jettyDeploy = jetty("deploy")
  val jettyHttp = jetty("http")
  val jettyHttpService = jettyOsgi("httpservice")
  val jettyIo = jetty("io")
  val jettyJmx = jetty("jmx")
  val jettySecurity = jetty("security")
  val jettyServlet = jetty("servlet")
  val jettyServer = jetty("server")
  val jettyUtil = jetty("util")
  val jettyWebapp = jetty("webapp")
  val jettyXml = jetty("xml")

  val jline = "org.jline" % "jline" % "3.9.0"
  val jlineBuiltins = "org.jline" % "jline-builtins" % "3.9.0"
  val jsr305 = "com.google.code.findbugs" % "jsr305" % "3.0.1"

  val lihaoyiPprint = "com.lihaoyi" %% "pprint" % "0.5.3"

  val reactiveStreams = "org.reactivestreams" % "reactive-streams" % "1.0.0.final"

  val scalaCompatJava8 = "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  val servicemixStaxApi = "org.apache.servicemix.specs" % "org.apache.servicemix.specs.stax-api-1.0" % "2.4.0"
  val slf4jJcl = "org.slf4j" % "jcl-over-slf4j" % slf4jVersion
  val slf4jJul = "org.slf4j" % "jul-to-slf4j" % slf4jVersion

}

object Blended extends blended.sbt.Blended {

  val blendedOrganization = "de.wayofquality.blended"

  override def blendedVersion: String = Utils.readAsVersion(new File("version.txt"))

  val mgmtUi = blendedOrganization %% "blended.mgmt.ui.server" % "0.5-SNAPSHOT"

}
