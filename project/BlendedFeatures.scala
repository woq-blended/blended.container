import sbt._
import sbt.librarymanagement.{Constant, ModuleID}

import blended.sbt.feature._

object BlendedFeatures {

  import Dependencies._

  lazy val blendedBaseFelix = Feature(
    "blended.launcher.feature.base.felix",
    bundles = Seq(
      FeatureBundle(dependency = Dependencies.felixFramework, startLevel = Some(0), start = true),
      FeatureBundle(dependency = Dependencies.jline, startLevel = Some(1)),
      FeatureBundle(dependency = Dependencies.jlineBuiltins, startLevel = Some(1)),
      FeatureBundle(dependency = Dependencies.felixGogoJline, startLevel = Some(1), start = true),
      FeatureBundle(dependency = Dependencies.felixGogoRuntime, startLevel = Some(1), start = true),
      FeatureBundle(dependency = Dependencies.felixGogoShell, startLevel = Some(1), start = true),
      FeatureBundle(dependency = Dependencies.felixGogoCommand, startLevel = Some(1), start = true)
    )
  )

  lazy val blendedBaseEquinox = Feature("blended.launcher.feature.base.equinox", bundles = Seq(
    FeatureBundle(dependency = eclipseOsgi, startLevel = Option(0), start = true),
    FeatureBundle(dependency = eclipseEquinoxConsole, startLevel = Option(1), start = true)
  ))

  lazy val blendedBase = Feature("blended.launcher.feature.base", bundles = Seq(
    FeatureBundle(dependency = Blended.securityBoot),
    FeatureBundle(dependency = asmAll, start = true),
    FeatureBundle(dependency = Blended.updater, start = true),
    FeatureBundle(dependency = Blended.updaterConfig, start = true),
    FeatureBundle(dependency = scalaReflect),
    FeatureBundle(dependency = scalaLibrary),
    FeatureBundle(dependency = scalaXml),
    FeatureBundle(dependency = scalaCompatJava8),
    FeatureBundle(dependency = scalaParser),
    FeatureBundle(dependency = Blended.akka, start = true),
    FeatureBundle(dependency = Blended.utilLogging),
    FeatureBundle(dependency = Blended.util, start = true),
    FeatureBundle(dependency = springCore),
    FeatureBundle(dependency = springExpression),
    FeatureBundle(dependency = Blended.containerContextApi),
    FeatureBundle(dependency = Blended.containerContextImpl, start = true),
    FeatureBundle(dependency = felixConfigAdmin, start = true),
    FeatureBundle(dependency = felixEventAdmin, start = true),
    FeatureBundle(dependency = felixFileinstall, start = true),
    FeatureBundle(dependency = slf4jJcl),
    FeatureBundle(dependency = slf4jJul),
    FeatureBundle(dependency = slf4j),
    FeatureBundle(dependency = logbackCore),
    FeatureBundle(dependency = logbackClassic),
    FeatureBundle(dependency = felixMetatype, start = true),
    FeatureBundle(dependency = typesafeConfig),
    FeatureBundle(dependency = typesafeConfigSSL),
    FeatureBundle(dependency = reactiveStreams),
    FeatureBundle(dependency = akkaActor),
    FeatureBundle(dependency = akkaSlf4j),
    FeatureBundle(dependency = akkaProtobuf),
    FeatureBundle(dependency = akkaStream),
    FeatureBundle(dependency = domino),
    FeatureBundle(dependency = Blended.domino),
    FeatureBundle(dependency = Blended.mgmtBase, start = true),
    FeatureBundle(dependency = Blended.prickle),
    FeatureBundle(dependency = Blended.mgmtServiceJmx, start = true)
  ))

  lazy val blendedActivemq = Feature(
    "blended.launcher.feature.activemq",
    bundles = Seq(
      FeatureBundle(dependency = ariesProxyApi),
      FeatureBundle(dependency = ariesBlueprintApi),
      FeatureBundle(dependency = ariesBlueprintCore),
      FeatureBundle(dependency = geronimoAnnotation),
      FeatureBundle(dependency = geronimoJms11Spec),
      FeatureBundle(dependency = geronimoJ2eeMgmtSpec),
      FeatureBundle(dependency = servicemixStaxApi),
      FeatureBundle(dependency = activeMqOsgi),
      FeatureBundle(dependency = Blended.activemqBrokerstarter, start = true),
      FeatureBundle(dependency = Blended.jmsUtils),
      FeatureBundle(dependency = springJms)
    )
  )

  lazy val blendedCamel = Feature(
    "blended.launcher.feature.camel",
    features = Seq(
      blendedSpring
    ),
    bundles = Seq(
      FeatureBundle(dependency = geronimoJms11Spec),
      FeatureBundle(dependency = camelCore),
      FeatureBundle(dependency = camelSpring),
      FeatureBundle(dependency = camelJms),
      FeatureBundle(dependency = Blended.camelUtils)
    )
  )

  lazy val blendedCommons = Feature(
    "blended.launcher.feature.commons",
    bundles = Seq(
      FeatureBundle(dependency = ariesUtil),
      FeatureBundle(dependency = ariesJmxApi),
      FeatureBundle(dependency = ariesJmxCore, start = true),
      FeatureBundle(dependency = Blended.jmx, start = true),
      FeatureBundle(dependency = commonsCollections),
      FeatureBundle(dependency = commonsDiscovery),
      FeatureBundle(dependency = commonsLang),
      FeatureBundle(dependency = commonsPool2),
      FeatureBundle(dependency = commonsNet),
      FeatureBundle(dependency = commonsExec),
      FeatureBundle(dependency = commonsIo),
      FeatureBundle(dependency = commonsCodec),
      FeatureBundle(dependency = commonsHttpclient),
      FeatureBundle(dependency = commonsBeanUtils),
      FeatureBundle(dependency = commonsConfiguration2)
    )
  )

  lazy val blendedHawtio = Feature(
    "blended.launcher.feature.hawtio",
    features = Seq(
      blendedJetty
    ),
    bundles = Seq(
      FeatureBundle(dependency = hawtioWeb, start = true),
      FeatureBundle(dependency = Blended.hawtioLogin)
    )
  )

  lazy val blendedMgmtClient = Feature(
    "blended.launcher.feature.mgmt.client",
    bundles = Seq(
      FeatureBundle(dependency = Blended.mgmtAgent, start = true)
    )
  )

  lazy val blendedPersistence = Feature(
    "blended.launcher.feature.persistence",
    features = Seq(
      blendedBase
    ),
    bundles = Seq(
      FeatureBundle(dependency = Blended.persistence),
      FeatureBundle(dependency = Blended.persistenceH2, start = true),
      // for Blended.persistenceH2
      FeatureBundle(dependency = h2),
      // for Blended.persistenceH2
      FeatureBundle(dependency = hikaricp),
      // for Blended.persistenceH2
      FeatureBundle(dependency = liquibase),
      // for Deps.liquibase
      FeatureBundle(dependency = snakeyaml)
    )
  )

  lazy val blendedMgmtServer = Feature(
    "blended.launcher.feature.mgmt.server",
    features = Seq(
      blendedBase,
      blendedAkkaHttp,
      blendedSecurity,
      blendedSsl,
      blendedSpring,
      blendedPersistence
    ),
    bundles = Seq(
      FeatureBundle(dependency = Blended.mgmtRest, start = true),
      FeatureBundle(dependency = Blended.mgmtRepo, start = true),
      FeatureBundle(dependency = Blended.mgmtRepoRest, start = true),
      FeatureBundle(dependency = Blended.updaterRemote, start = true),
      FeatureBundle(dependency = concurrentLinkedHashMapLru),
      FeatureBundle(dependency = jsr305),
      FeatureBundle(dependency = jacksonAnnotations),
      FeatureBundle(dependency = jacksonCore),
      FeatureBundle(dependency = jacksonBind),
      FeatureBundle(dependency = jjwt),
      FeatureBundle(dependency = Blended.securityLoginApi),
      FeatureBundle(dependency = Blended.securityLoginImpl, start = true),
      FeatureBundle(dependency = Blended.mgmtWs, start = true),
      FeatureBundle(dependency = Blended.securityLoginRest, start = true),
      FeatureBundle(dependency = Blended.mgmtUi, start = true)
    )
  )

  lazy val blendedJetty = Feature(
    "blended.launcher.feature.jetty",
    features = Seq(
      blendedBase
    ),
    bundles = Seq(
      FeatureBundle(dependency = activationApi),
      FeatureBundle(dependency = javaxServlet31),
      FeatureBundle(dependency = javaxMail),
      FeatureBundle(dependency = geronimoAnnotation),
      FeatureBundle(dependency = geronimoJaspic),
      FeatureBundle(dependency = jettyUtil),
      FeatureBundle(dependency = jettyHttp),
      FeatureBundle(dependency = jettyIo),
      FeatureBundle(dependency = jettyJmx),
      FeatureBundle(dependency = jettySecurity),
      FeatureBundle(dependency = jettyServlet),
      FeatureBundle(dependency = jettyServer),
      FeatureBundle(dependency = jettyWebapp),
      FeatureBundle(dependency = jettyDeploy),
      FeatureBundle(dependency = jettyXml),
      FeatureBundle(dependency = equinoxServlet),
      FeatureBundle(dependency = felixHttpApi),
      FeatureBundle(dependency = Blended.jettyBoot, start = true),
      FeatureBundle(dependency = jettyHttpService, start = true)
    )
  )

  lazy val blendedSecurity = Feature(
    "blended.launcher.feature.security",
    features = Seq(
      blendedBase
    ),
    bundles = Seq(
      FeatureBundle(dependency = Blended.security, start = true)
    )
  )

  lazy val blendedSsl = Feature(
    "blended.launcher.feature.ssl",
    features = Seq(
      blendedBase
    ),
    bundles = Seq(
      FeatureBundle(dependency = Blended.securityScep, start = true),
      FeatureBundle(dependency = Blended.securitySsl, start = true)
    )
  )

  lazy val blendedAkkaHttp = Feature(
    "blended.launcher.feature.akka.http",
    features = Seq(
      blendedBase
    ),
    bundles = Seq(
      FeatureBundle(dependency = Blended.akkaHttpApi),
      FeatureBundle(dependency = Blended.akkaHttp, start = true),
      FeatureBundle(dependency = Blended.prickleAkkaHttp),
      FeatureBundle(dependency = Blended.securityAkkaHttp)
    )
  )

  lazy val blendedAkkaHttpModules = Feature(
    "blended.launcher.feature.akka.http.modules",
    features = Seq(
      blendedSpring,
      blendedCamel,
      blendedAkkaHttp
    ),
    bundles = Seq(
      FeatureBundle(dependency = Blended.akkaHttpProxy),
      FeatureBundle(dependency = Blended.akkaHttpRestJms),
      FeatureBundle(dependency = Blended.akkaHttpJmsQueue)
    )
  )

  lazy val blendedSpring = Feature(
    "blended.launcher.feature.spring", bundles = Seq(
    FeatureBundle(dependency = aopAlliance),
    FeatureBundle(dependency = springBeans),
    FeatureBundle(dependency = springAop),
    FeatureBundle(dependency = springContext),
    FeatureBundle(dependency = springContextSupport),
    FeatureBundle(dependency = springJdbc),
    FeatureBundle(dependency = springTx)
  ))

  lazy val blendedStreams = Feature(
    "blended.launcher.feature.streams",
    features = Seq(
      blendedBase,
      blendedPersistence
    ),
    bundles = Seq(
      FeatureBundle(dependency = Blended.streams)
    )
  )

  lazy val blendedSamples = Feature(
    "blended.launcher.feature.samples",
    features = Seq(
      blendedAkkaHttp,
      blendedActivemq,
      blendedCamel,
      blendedStreams
    ),
    bundles = Seq(
      FeatureBundle(dependency = Blended.jmsBridge, start = true),
      FeatureBundle(dependency = Blended.streamsDispatcher, start = true),
      FeatureBundle(dependency = Blended.activemqClient, start = true),
      FeatureBundle(dependency = Blended.samplesCamel, start = true),
      FeatureBundle(dependency = Blended.samplesJms, start = true),
      FeatureBundle(dependency = Blended.file),
      FeatureBundle(dependency = Blended.akkaHttpSampleHelloworld, start = true)
    )
  )

}