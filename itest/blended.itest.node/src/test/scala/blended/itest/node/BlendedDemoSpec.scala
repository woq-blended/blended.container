package blended.itest.node

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.{BlendedIntegrationTestSupport, TestConnector}
import blended.jms.utils.{IdAwareConnectionFactory, JmsDestination, JmsQueue}
import blended.streams.jms.{JmsEnvelopeHeader, JmsProducerSettings, JmsStreamSupport}
import blended.streams.message.{FlowEnvelope, FlowMessage}
import blended.streams.testsupport._
import blended.streams.transaction.FlowHeaderConfig
import blended.testsupport.scalatest.LoggingFreeSpec
import blended.util.FileHelper
import blended.util.logging.Logger
import org.scalactic.source
import org.scalatest.{Assertions, DoNotDiscover, Matchers}

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
  implicit val materializer : Materializer = ActorMaterializer()
  implicit val timeOut : FiniteDuration = 10.seconds
  implicit val eCtxt : ExecutionContext = testKit.system.dispatcher

  private val intCf : IdAwareConnectionFactory = TestContainerProxy.internalCf
  private val extCf : IdAwareConnectionFactory = TestContainerProxy.externalCf

  private[this] val log = Logger[BlendedDemoSpec]

  "The demo container should" - {

    "Define the sample Camel Route from SampleIn to SampleOut" in {

      val testMessage = FlowEnvelope(
        FlowMessage("Hello Blended!")(FlowMessage.props("foo" -> "bar").get)
      )

      val pSettings : JmsProducerSettings = JmsProducerSettings(
        log = log,
        connectionFactory = intCf,
        jmsDestination = Some(JmsQueue("SampleIn"))
      )

      sendMessages(pSettings, log, testMessage)

      val outColl = receiveMessages(
        headerCfg = FlowHeaderConfig(prefix = "App"),
        cf = intCf,
        dest = JmsDestination.create("SampleOut").get,
        log = log
      )

      val errorsFut = outColl.result.map { msgs =>
        FlowMessageAssertion.checkAssertions(msgs:_*)(
          ExpectedMessageCount(1),
          ExpectedBodies(Some("Hello Blended!")),
          ExpectedHeaders("foo" -> "bar")
        )
      }

      Await.result(errorsFut, timeOut + 1.second) should be (empty)
    }

    "Define a dispatcher Route from DispatcherIn to DispatcherOut" in {

      val testMessage : FlowEnvelope = FlowEnvelope(
        FlowMessage("Hello Blended!")(FlowMessage.props("ResourceType" -> "SampleIn").get)
      )

      val pSettings : JmsProducerSettings = JmsProducerSettings(
        log = log,
        connectionFactory = extCf,
        jmsDestination = Some(JmsQueue("DispatcherIn"))
      )

      sendMessages(pSettings, log, testMessage)

      val outColl = receiveMessages(
        headerCfg = FlowHeaderConfig(prefix = "App"),
        cf = extCf,
        dest = JmsDestination.create("DispatcherOut").get,
        log = log
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
        log = log,
        connectionFactory = extCf,
        jmsDestination = Some(JmsQueue("DispatcherIn"))
      )

      sendMessages(pSettings, log, testMessage)

      val outColl = receiveMessages(
        headerCfg = FlowHeaderConfig(prefix = "App"),
        cf = extCf,
        dest = JmsDestination.create("response").get,
        log = log
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
        log = log,
        connectionFactory = intCf,
        jmsDestination = Some(JmsQueue("internal.data.in"))
      )

      sendMessages(pSettings, log, testMessage)

      val outColl = receiveMessages(
        headerCfg = FlowHeaderConfig(prefix = "App"),
        cf = intCf,
        dest = JmsDestination.create("response").get,
        log = log
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

      writeContainerDirectory(ctProxy, "node_0", "/opt/node", file).onComplete {
        case Failure(t) => fail(t.getMessage())
        case Success(r) => r.result match {
          case Left(t) => fail(t.getMessage())
          case Right(f) =>
            if (!f._2) fail("Error writing container directory")
            else {
              readContainerDirectory(ctProxy, "node_0", "/opt/node/data") onComplete {
                case Failure(t) => fail(t.getMessage())
                case Success(cdr) => cdr.result match {
                  case Left(t) => fail(t.getMessage())
                  case Right(cd) =>
                    cd.content.get("data/testFile.txt") match {
                      case None => fail("expected file [/opt/node/data/testFile.txt] not found in container")
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
        cmd = "ls -al /opt/node".split(" "): _*
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
  }
}
