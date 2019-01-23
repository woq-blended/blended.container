import sbt.Keys._
import sbt._

object Dependencies extends blended.sbt.Dependencies {

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

  val dockerJava = "com.github.docker-java" % "docker-java" % "3.0.14"

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

  val hawtioWeb = "io.hawt" % "hawtio-web" % "1.5.8" withExplicitArtifacts(Vector(Artifact("hawtio-web", `type` = "war", extension = "war")))

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

  val reactiveStreams = "org.reactivestreams" % "reactive-streams" % "1.0.0.final"

  val scalaCompatJava8 = "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  val servicemixStaxApi = "org.apache.servicemix.specs" % "org.apache.servicemix.specs.stax-api-1.0" % "2.4.0"
  val slf4jJcl = "org.slf4j" % "jcl-over-slf4j" % slf4jVersion
  val slf4jJul = "org.slf4j" % "jul-to-slf4j" % slf4jVersion

}

object Blended extends blended.sbt.Blended {

  val blendedOrganization = "de.wayofquality.blended"

  override def blendedVersion: String = BuildHelper.readAsVersion(new File("version.txt"))

  val mgmtUi = blendedOrganization %% "blended.mgmt.ui.server" % "0.4-SNAPSHOT"

  //  def blended(name: String) = Def.setting(groupId %% name % version.value)
  //
  //  val activemqBrokerstarter = blended("blended.activemq.brokerstarter")
  //  val activemqClient = blended("blended.activemq.client")
  //  val activemqDefaultbroker = blended("blended.activemq.defaultbroker")
  //  val akka = blended("blended.akka")
  //  val akkaHttp = blended("blended.akka.http")
  //  val akkaHttpApi = blended("blended.akka.http.api")
  //  val akkaHttpJmsQueue = blended("blended.akka.http.jmsqueue")
  //  val akkaHttpProxy = blended("blended.akka.http.proxy")
  //  val akkaHttpRestJms = blended("blended.akka.http.restjms")
  //  val akkaHttpSampleHelloworld = blended("blended.akka.http.sample.helloworld")
  //  val camelUtils = blended("blended.camel.utils")
  //  val containerContextApi = blended("blended.container.context.api")
  //  val containerContextImpl = blended("blended.container.context.impl")
  //  val containerRegistry = blended("blended.container.registry")
  //  val demoReactor = blended("blended.demo.reactor")
  //  val demoMgmt = blended("blended.demo.mgmt")
  //  val demoMgmtResources = blended("blended.demo.mgmt.resources")
  //  val demoNode = blended("blended.demo.node")
  //  val demoNodeResources = blended("blended.demo.node.resources")
  //  val dockerDemoApacheDS = blended("blended.docker.demo.apacheds")
  //  val dockerReactor = blended("blended.docker.reactor")
  //  val dockerDemoNode = blended("blended.docker.demo.node")
  //  val dockerDemoMgmt = blended("blended.docker.demo.mgmt")
  //  val domino = blended("blended.domino")
  //  val file = blended("blended.file")
  //  val hawtioLogin = blended("blended.hawtio.login")
  //  val itestReactor = blended("blended.itest.reactor")
  //  val itestSupport = blended("blended.itestsupport")
  //  val itestMgmt = blended("blended.itest.mgmt")
  //  val itestNode = blended("blended.itest.node")
  //  val jettyBoot = blended("blended.jetty.boot")
  //  val jmsBridge = blended("blended.jms.bridge")
  //  val jmsSampler = blended("blended.jms.sampler")
  //  val jmsUtils = blended("blended.jms.utils")
  //  val jmx = blended("blended.jmx")
  //  val jolokia = blended("blended.jolokia")
  //  val launcher = blended("blended.launcher")
  //  val launcherFeatures = blended("blended.launcher.features")
  //  val mgmtAgent = blended("blended.mgmt.agent")
  //  val mgmtBase = blended("blended.mgmt.base")
  //  val mgmtRepo = blended("blended.mgmt.repo")
  //  val mgmtRepoRest = blended("blended.mgmt.repo.rest")
  //  val mgmtMock = blended("blended.mgmt.mock")
  //  val mgmtRest = blended("blended.mgmt.rest")
  //  val mgmtServiceJmx = blended("blended.mgmt.service.jmx")
  //  val mgmtWs = blended("blended.mgmt.ws")
  //  val persistence = blended("blended.persistence")
  //  val persistenceH2 = blended("blended.persistence.h2")
  //  val prickle = blended("blended.prickle")
  //  val prickleAkkaHttp = blended("blended.prickle.akka.http")
  //  val samplesReactor = blended("blended.samples.reactor")
  //  val samplesCamel = blended("blended.samples.camel")
  //  val samplesJms = blended("blended.samples.jms")
  //  val security = blended("blended.security")
  //  val securityAkkaHttp = blended("blended.security.akka.http")
  //  val securityBoot = blended("blended.security.boot")
  //  val securityScep = blended("blended.security.scep")
  //  val securityScepStandalone = blended("blended.security.scep.standalone")
  //  val securitySsl = blended("blended.security.ssl")
  //  val securityLoginApi = blended("blended.security.login.api")
  //  val securityLoginImpl = blended("blended.security.login.impl")
  //  val securityLoginRest = blended("blended.security.login.rest")
  //  val sslContext = blended("blended.sslcontext")
  //  val streams = blended("blended.streams")
  //  val streamsDispatcher = blended("blended.streams.dispatcher")
  //  val streamsTestsupport = blended("blended.streams.testsupport")
  //  val testSupport = blended("blended.testsupport")
  //  val testSupportPojosr = blended("blended.testsupport.pojosr")
  //  val updater = blended("blended.updater")
  //  val updaterConfig = blended("blended.updater.config")
  //  val updaterRemote = blended("blended.updater.remote")
  //  val updaterTools = blended("blended.updater.tools")
  //  val util = blended("blended.util")
  //  val utilLogging = blended("blended.util.logging")

}
