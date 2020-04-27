import mill._
import scalalib._

object Deps {

  // Versions
  val activeMqVersion = "5.15.6"
  val akkaVersion = "2.5.26"
  val akkaHttpVersion = "10.1.11"
  val dominoVersion = "1.1.3"
  val jettyVersion = "9.4.28.v20200408"
  val jolokiaVersion = "1.6.0"
  val microJsonVersion = "1.4"
  val parboiledVersion = "1.1.6"
  val prickleVersion = "1.1.14"
  val scalaVersion = "2.12.11"
  val scalatestVersion = "3.0.5"
  val scalaCheckVersion = "1.14.0"
  val slf4jVersion = "1.7.25"
  val sprayVersion = "1.3.4"
  val springVersion = "4.3.12.RELEASE_1"

  val activationApi = ivy"org.apache.servicemix.specs:org.apache.servicemix.specs.activation-api-1.1:2.2.0"
  val activeMqOsgi = ivy"org.apache.activemq:activemq-osgi:$activeMqVersion"

  val aopAlliance = ivy"org.apache.servicemix.bundles:org.apache.servicemix.bundles.aopalliance:1.0_6"

  val ariesBlueprintApi = ivy"org.apache.aries.blueprint:org.apache.aries.blueprint.api:1.0.1"
  val ariesBlueprintCore = "org.apache.aries.blueprint:org.apache.aries.blueprint.core:1.4.3"
  val ariesJmxApi = ivy"org.apache.aries.jmx:org.apache.aries.jmx.api:1.1.1"
  val ariesJmxCore = ivy"org.apache.aries.jmx:org.apache.aries.jmx.core:1.1.1"
  val ariesProxyApi = ivy"org.apache.aries.proxy:org.apache.aries.proxy.api:1.0.1"
  val ariesUtil = ivy"org.apache.aries:org.apache.aries.util:1.1.0"

  //  val activeMqBroker = "org.apache.activemq" % "activemq-broker" % activeMqVersion
//  val activeMqClient = "org.apache.activemq" % "activemq-client" % activeMqVersion
//  val activeMqKahadbStore = "org.apache.activemq" % "activemq-kahadb-store" % activeMqVersion
//  val activeMqSpring = "org.apache.activemq" % "activemq-spring" % activeMqVersion
//
  protected def akka(m : String) : Dep = ivy"com.typesafe.akka::akka-$m::$akkaVersion"
  protected def akkaHttpModule(m : String) : Dep = ivy"com.typesafe.akka::akka-$m:$akkaHttpVersion"

  val akkaActor = akka("actor")
//  val akkaHttp = akkaHttpModule("http")
//  val akkaHttpCore = akkaHttpModule("http-core")
//  val akkaHttpTestkit = akkaHttpModule("http-testkit")
//  val akkaOsgi = akka("osgi")
//  val akkaParsing = akkaHttpModule("parsing")
//  val akkaPersistence = akka("persistence")
  val akkaProtobuf = akka("protobuf")
  val akkaStream = akka("stream")
//  val akkaStreamTestkit = akka("stream-testkit")
//  val akkaTestkit = akka("testkit")
//  val akkaSlf4j = akka("slf4j")
//
//  val asciiRender = "com.indvd00m.ascii.render" % "ascii-render" % "1.2.3"
  val asmAll = ivy"org.ow2.asm:asm-all:4.1"
//
//  val bouncyCastleBcprov = "org.bouncycastle" % "bcprov-jdk15on" % "1.60"
//  val bouncyCastlePkix = "org.bouncycastle" % "bcpkix-jdk15on" % "1.60"
//
//  val cmdOption = "de.tototec" % "de.tototec.cmdoption" % "0.6.0"

  val commonsBeanUtils = ivy"commons-beanutils:commons-beanutils:1.9.3"
  val commonsCodec = ivy"commons-codec:commons-codec:1.11"
  val commonsCollections = ivy"org.apache.commons:com.springsource.org.apache.commons.collections:3.2.1"
  val commonsDiscovery = ivy"org.apache.commons:com.springsource.org.apache.commons.discovery:0.4.0"
  val commonsExec = ivy"org.apache.commons:commons-exec:1.3"
//  val commonsDaemon = "commons-daemon" % "commons-daemon" % "1.0.15"
  val commonsHttpclient = ivy"org.apache.commons:com.springsource.org.apache.commons.httpclient:3.1.0"
  val commonsIo = ivy"commons-io:commons-io:2.6"
  val commonsLang3 = ivy"org.apache.commons:commons-lang3:3.7"
  val commonsNet = ivy"commons-net:commons-net:3.3"
  val commonsPool2 = ivy"org.apache.commons:commons-pool2:2.6.0"
//  val commonsLang2 = "commons-lang" % "commons-lang" % "2.6"
  val concurrentLinkedHashMapLru = ivy"com.googlecode.concurrentlinkedhashmap:concurrentlinkedhashmap-lru:1.4.2"

//  val dockerJava = "com.github.docker-java" % "docker-java" % "3.0.13"
  val domino = ivy"com.github.domino-osgi::domino:$dominoVersion"
//
  val eclipseEquinoxConsole = ivy"org.eclipse.platform:org.eclipse.equinox.console:1.4.0"
  val eclipseOsgi = ivy"org.eclipse.platform:org.eclipse.osgi:3.12.50"
  val equinoxServlet = ivy"org.eclipse.platform:org.eclipse.equinox.http.servlet:1.4.0"

  //  val felixConnect = "org.apache.felix" % "org.apache.felix.connect" % "0.1.0"
  val felixConfigAdmin = ivy"org.apache.felix:org.apache.felix.configadmin:1.8.6"
  val felixEventAdmin = ivy"org.apache.felix:org.apache.felix.eventadmin:1.3.2"
  val felixFileinstall = ivy"org.apache.felix:org.apache.felix.fileinstall:3.4.2"
  val felixFramework = ivy"org.apache.felix:org.apache.felix.framework:6.0.2"
  val felixGogoCommand = ivy"org.apache.felix:org.apache.felix.gogo.command:1.1.0"
  val felixGogoJline = ivy"org.apache.felix:org.apache.felix.gogo.jline:1.1.4"
  val felixGogoShell = ivy"org.apache.felix:org.apache.felix.gogo.shell:1.1.2"
  val felixGogoRuntime = ivy"org.apache.felix:org.apache.felix.gogo.runtime:1.1.2"
  val felixHttpApi = ivy"org.apache.felix:org.apache.felix.http.api:3.0.0"
  val felixMetatype = ivy"org.apache.felix:org.apache.felix.metatype:1.0.12"
  val felixShellRemote = ivy"org.apache.felix:org.apache.felix.shell.remote:1.2.0"

  val geronimoAnnotation = ivy"org.apache.geronimo.specs:geronimo-annotation_1.1_spec:1.0.1"
  val geronimoJaspic = ivy"org.apache.geronimo.specs:geronimo-jaspic_1.0_spec:1.1"
  val geronimoJ2eeMgmtSpec = ivy"org.apache.geronimo.specs:geronimo-j2ee-management_1.1_spec:1.0.1"
  val geronimoJms11Spec = ivy"org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.1.1"

  val hawtioWeb = ivy"io.hawt:hawtio-web:1.5.11;classifier=war"

  val h2 = ivy"com.h2database:h2:1.4.197"
  val hikaricp = ivy"com.zaxxer:HikariCP:3.1.0"

  val jacksonCore = ivy"com.fasterxml.jackson.core:jackson-core:2.9.3"
  val jacksonBind = ivy"com.fasterxml.jackson.core:jackson-databind:2.9.3"
  val jacksonAnnotations = ivy"com.fasterxml.jackson.core:jackson-annotations:2.9.3"

  val javaxMail = ivy"javax.mail:mail:1.4.5"
  val javaxServlet31 = ivy"org.everit.osgi.bundles:org.everit.osgi.bundles.javax.servlet.api:3.1.0"

  protected def jetty(n: String) = ivy"org.eclipse.jetty:jetty-${n}:$jettyVersion"
  protected def jettyOsgi(n : String) = ivy"org.eclipse.jetty.osgi:jetty-${n}:$jettyVersion"

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
//
//  val jcip = "net.jcip" % "jcip-annotations" % "1.0"
//  val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % slf4jVersion
//  val jettyOsgiBoot = jettyOsgi("osgi-boot")
  val jjwt = ivy"io.jsonwebtoken:jjwt:0.7.0"
  val jline = ivy"org.jline:jline:3.9.0"
  val jlineBuiltins = ivy"org.jline:jline-builtins:3.9.0"
//  val jms11Spec = "org.apache.geronimo.specs" % "geronimo-jms_1.1_spec" % "1.1.1"
  val jolokiaOsgi = ivy"org.jolokia:jolokia-osgi:$jolokiaVersion"
  //  val jolokiaJvm = "org.jolokia" % "jolokia-jvm" % jolokiaVersion
//  val jolokiaJvmAgent = jolokiaJvm.classifier("agent")
//  val jscep = "com.google.code.jscep" % "jscep" % "2.5.0"
//  val jsonLenses = "net.virtual-void" %% "json-lenses" % "0.6.2"
  val jsr305 = ivy"com.google.code.findbugs:jsr305:3.0.1"
//  val julToSlf4j = "org.slf4j" % "jul-to-slf4j" % slf4jVersion
//  val junit = "junit" % "junit" % "4.12"
//
//  val lambdaTest = "de.tototec" % "de.tobiasroeser.lambdatest" % "0.6.2"
//  val levelDbJava = "org.iq80.leveldb" % "leveldb" % "0.9"
//  val levelDbJni = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
  val liquibase = ivy"org.liquibase:liquibase-core:3.6.1"
//  /** Only for use in test that also runs in JS */
//  val log4s = "org.log4s" %% "log4s" % "1.6.1"
//  val logbackCore = "ch.qos.logback" % "logback-core" % "1.2.3"
//  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
//
//  val microjson = "com.github.benhutchison" %% "microjson" % microJsonVersion
//  val mimepull = "org.jvnet.mimepull" % "mimepull" % "1.9.5"
//  val mockitoAll = "org.mockito" % "mockito-all" % "1.9.5"
//
//  val orgOsgi = "org.osgi" % "org.osgi.core" % "6.0.0"
  val orgOsgiCompendium = ivy"org.osgi:org.osgi.compendium:5.0.0"
//  val osLib = "com.lihaoyi" %% "os-lib" % "0.4.2"
//
//  val parboiledCore = "org.parboiled" % "parboiled-core" % parboiledVersion
//  val parboiledScala = "org.parboiled" %% "parboiled-scala" % parboiledVersion
//  val prickle = "com.github.benhutchison" %% "prickle" % prickleVersion
//
  val reactiveStreams = ivy"org.reactivestreams:reactive-streams:1.0.0.final"
//  // SCALA
  val scalaCompatJava8 = ivy"org.scala-lang.modules::scala-java8-compat:0.8.0"
  val scalaLibrary = ivy"org.scala-lang:scala-library:$scalaVersion"
  val scalaReflect = ivy"org.scala-lang:scala-reflect:$scalaVersion"
  val scalaParser = ivy"org.scala-lang.modules::scala-parser-combinators:1.1.1"
  val scalaXml = ivy"org.scala-lang.modules::scala-xml:1.1.0"

  val servicemixStaxApi = ivy"org.apache.servicemix.specs:org.apache.servicemix.specs.stax-api-1.0:2.4.0"

  //
//  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.0"
//  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion
//  val shapeless = "com.chuusai" %% "shapeless" % "1.2.4"
//  val slf4j = "org.slf4j" % "slf4j-api" % slf4jVersion
//  val slf4jLog4j12 = "org.slf4j" % "slf4j-log4j12" % slf4jVersion
  val snakeyaml = ivy"org.yaml:snakeyaml:1.18"
//
//  // libs for splunk support via HEC
//  val splunkjava = "com.splunk.logging" % "splunk-library-javalogging" % "1.7.3"
//  val httpCore = "org.apache.httpcomponents" % "httpcore" % "4.4.9"
//  val httpCoreNio = "org.apache.httpcomponents" % "httpcore" % "4.4.6"
//  val httpComponents = "org.apache.httpcomponents" % "httpclient" % "4.5.5"
//  val httpAsync = "org.apache.httpcomponents" % "httpasyncclient" % "4.1.3"
//  val commonsLogging = "commons-logging" % "commons-logging" % "1.2"
//  val jsonSimple = "com.googlecode.json-simple" % "json-simple" % "1.1.1"
//  // -------------- end of splunk libs -----------------------------------------
//
//  val sprayJson = "io.spray" %% s"spray-json" % sprayVersion
//
//  //  protected def spring(n: String) = "org.springframework" % s"spring-${n}" % springVersion
  protected def spring(n : String): Dep = ivy"org.apache.servicemix.bundles:org.apache.servicemix.bundles.spring-${n}:$springVersion"

  val springBeans = spring("beans")
  val springAop = spring("aop")
  val springContext = spring("context")
  val springContextSupport = spring("context-support")
  val springExpression = spring("expression")
  val springCore = spring("core")
  val springJdbc = spring("jdbc")
  val springJms = spring("jms")
  val springTx = spring("tx")
//
//  val sttp = "com.softwaremill.sttp" %% "core" % "1.3.0"
//  val sttpAkka = "com.softwaremill.sttp" %% "akka-http-backend" % "1.3.0"
//
//  val travesty = "net.mikolak" %% "travesty" % s"0.9.1_2.5.17"
//
  val typesafeConfig = ivy"com.typesafe:config:1.3.3"
  val typesafeSslConfigCore = ivy"com.typesafe::ssl-config-core:0.3.6"

}
