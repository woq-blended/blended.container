package blended.itest.node

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.ssl.ContainerSslContextInfo
import blended.itestsupport.{BlendedIntegrationTestSupport, TestConnector}
import blended.jms.utils.{IdAwareConnectionFactory, JmsDestination, JmsQueue}
import blended.security.ssl.SslContextInfo
import blended.streams.jms.{JmsEnvelopeHeader, JmsProducerSettings, JmsStreamSupport}
import blended.streams.message._
import blended.streams.testsupport._
import blended.streams.FlowHeaderConfig
import blended.testsupport.scalatest.LoggingFreeSpec
import blended.util.FileHelper
import blended.util.logging.Logger
import org.scalactic.source
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertions, DoNotDiscover}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

@DoNotDiscover
class BlendedDemoSpec(implicit testKit: TestKit)
  extends LoggingFreeSpec
  with Matchers
  with BlendedIntegrationTestSupport
  with JmsStreamSupport
  with JmsEnvelopeHeader {

  implicit val system : ActorSystem = testKit.system
  implicit val timeOut : FiniteDuration = 30.seconds
  implicit val eCtxt : ExecutionContext = testKit.system.dispatcher

  private val intCf : IdAwareConnectionFactory = TestContainerProxy.internalCf
  private val extCf : IdAwareConnectionFactory = TestContainerProxy.externalCf

  private[this] val log = Logger[BlendedDemoSpec]
  private[this] val headerCfg : FlowHeaderConfig = FlowHeaderConfig.create("App")
  private[this] val envLogger : FlowEnvelopeLogger = FlowEnvelopeLogger.create(headerCfg, log)

  private[this] val appFolder : String = System.getProperty("appFolder", "node")

  "The demo container should" - {

    "Send a startup message once the dispatcher has started" in {

      val outColl = receiveMessages(
        headerCfg = FlowHeaderConfig.create(prefix = "App"),
        cf = extCf,
        dest = JmsQueue("startup"),
        log = envLogger,
        completeOn = Some(l => l.size == 1),
        timeout = None
      )

      val outFut = outColl.result.map { msgs =>
        FlowMessageAssertion.checkAssertions(msgs:_*)(
          ExpectedMessageCount(1),
          ExpectedHeaders("ResourceType" -> "DispatcherStarted"),
          ExpectedBodies(Some("de;blended"))
        )
      }

      Await.result(outFut, timeOut + 1.second) should be (empty)
    }

    "Define a dispatcher Route from DispatcherIn to DispatcherOut" in {

      val testMessage : FlowEnvelope = FlowEnvelope(
        FlowMessage("Hello Blended!")(FlowMessage.props("ResourceType" -> "SampleIn").get)
      )

      val pSettings : JmsProducerSettings = JmsProducerSettings(
        log = envLogger,
        headerCfg = headerCfg,
        connectionFactory = extCf,
        jmsDestination = Some(JmsQueue("DispatcherIn"))
      )

      sendMessages(pSettings, envLogger, testMessage)

      val outColl = receiveMessages(
        headerCfg = FlowHeaderConfig.create(prefix = "App"),
        cf = extCf,
        dest = JmsDestination.create("DispatcherOut").get,
        log = envLogger,
        completeOn = Some(l => l.size == 1),
        timeout = None
      )

      val errorsFut = outColl.result.map { msgs =>
        FlowMessageAssertion.checkAssertions(msgs:_*)(
          ExpectedMessageCount(1),
          ExpectedBodies(Some("Hello Blended!")),
          ExpectedHeaders("ResourceType" -> "SampleIn")
        )
      }

      Await.result(errorsFut, timeOut + 1.second) should be (empty)
    }

    "Define a replyTo Route Route (external)" in {

      val testMessage : FlowEnvelope = FlowEnvelope(
        FlowMessage("Hello Blended!")(FlowMessage.props(
          "ResourceType" -> "SampleRequest",
          replyToHeader("App") -> JmsDestination.create("response").get.asString
        ).get)
      )

      val pSettings : JmsProducerSettings = JmsProducerSettings(
        log = envLogger,
        headerCfg = headerCfg,
        connectionFactory = extCf,
        jmsDestination = Some(JmsQueue("DispatcherIn"))
      )

      sendMessages(pSettings, envLogger, testMessage)

      val outColl = receiveMessages(
        headerCfg = FlowHeaderConfig.create(prefix = "App"),
        cf = extCf,
        dest = JmsDestination.create("response").get,
        log = envLogger,
        completeOn = Some(l => l.size == 1),
        timeout = None
      )

      val errorsFut = outColl.result.map { msgs =>
        FlowMessageAssertion.checkAssertions(msgs:_*)(
          ExpectedMessageCount(1),
          ExpectedBodies(Some("Hello Blended!")),
          ExpectedHeaders("ResourceType" -> "SampleRequest")
        )
      }

      Await.result(errorsFut, timeOut + 1.second) should be (empty)
    }

    "Define a replyTo Route Route (internal)" in {

      val testMessage : FlowEnvelope = FlowEnvelope(
        FlowMessage("Hello Blended!")(FlowMessage.props(
          "ResourceType" -> "SampleRequest",
          replyToHeader("App") -> JmsDestination.create("response").get.asString
        ).get)
      )

      val pSettings : JmsProducerSettings = JmsProducerSettings(
        log = envLogger,
        headerCfg = headerCfg,
        connectionFactory = intCf,
        jmsDestination = Some(JmsQueue("internal.data.in"))
      )

      sendMessages(pSettings, envLogger, testMessage)

      val outColl = receiveMessages(
        headerCfg = FlowHeaderConfig.create(prefix = "App"),
        cf = intCf,
        dest = JmsDestination.create("response").get,
        log = envLogger,
        completeOn = Some(l => l.size == 1),
        timeout = None
      )

      val errorsFut = outColl.result.map { msgs =>
        FlowMessageAssertion.checkAssertions(msgs:_*)(
          ExpectedMessageCount(1),
          ExpectedBodies(Some("Hello Blended!")),
          ExpectedHeaders("ResourceType" -> "SampleRequest")
        )
      }

      Await.result(errorsFut, timeOut + 1.second) should be (empty)
    }

    "Allow to read and write directories via the docker API" in {

      val ctProxy : ActorRef = TestConnector.property[ActorRef]("ctProxy").get

      implicit val to : Timeout = Timeout(timeOut)
      import blended.testsupport.BlendedTestSupport.projectTestOutput

      val file = new File(s"$projectTestOutput/data")

      val test = Promise[Unit]()

      def fail(message: String)(implicit pos: source.Position): Unit = {
        test.complete(Try {
          Assertions.fail(message)(pos)
        })
      }

      writeContainerDirectory(ctProxy, "node_0", s"/opt/$appFolder", file).onComplete {
        case Failure(t) => fail(t.getMessage())
        case Success(r) => r.result match {
          case Left(t) => fail(t.getMessage())
          case Right(f) =>
            if (!f._2) fail("Error writing container directory")
            else {
              readContainerDirectory(ctProxy, "node_0", s"/opt/$appFolder/data") onComplete {
                case Failure(t) => fail(t.getMessage())
                case Success(cdr) => cdr.result match {
                  case Left(t) => fail(t.getMessage())
                  case Right(cd) =>
                    cd.content.get("data/testFile.txt") match {
                      case None => fail(s"expected file [/opt/$appFolder/data/testFile.txt] not found in container")
                      case Some(c) =>
                        test.complete(Try {
                          val fContent = FileHelper.readFile("data/testFile.txt")
                          c should equal(fContent)
                        })
                    }
                }
              }
            }
        }
      }

      Await.result(test.future, timeOut)
    }

    "Allow to execute an arbitrary command on the container" in {
      val ctProxy : ActorRef = TestConnector.property[ActorRef]("ctProxy").get

      implicit val to : Timeout = Timeout(timeOut)
      val test = Promise[Unit]()

      execContainerCommand(
        ctProxy = ctProxy,
        ctName = "node_0",
        cmdTimeout = 5.seconds,
        user = "blended",
        cmd = s"ls -al /opt/$appFolder".split(" ").toIndexedSeq: _*
      ) onComplete {
        case Failure(t) => test.failure(fail(t.getMessage()))
        case Success(r) =>
          r.result match {
            case Left(t) => test.failure(fail(t.getMessage()))
            case Right(er) =>
              test.complete(Try {
                log.info(s"Command output is [\n${new String(er._2.out)}\n]")
                er._2.rc should be(0)
              })
          }
      }

      Await.result(test.future, timeOut)
    }

    "Only support TLSv1.2" in {
      val info : SslContextInfo = ContainerSslContextInfo.sslContextInfo(
        client = TestContainerProxy.jolokia,
        domain = "blended",
        name = "server"
      ).get

      info.getEnabledProtocols() should be (Array("TLSv1.2"))
    }

    "Only support selected CypherSuites" in {

      val info : SslContextInfo = ContainerSslContextInfo.sslContextInfo(
        client = TestContainerProxy.jolokia,
        domain = "blended",
        name = "server"
      ).get

      info.getInvalidCypherSuites() should be (empty)
      info.getEnabledCypherSuites() should not be (empty)
    }
  }
}
