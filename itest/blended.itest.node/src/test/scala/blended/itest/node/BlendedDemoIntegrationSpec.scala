package blended.itest.node

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.{BlendedIntegrationTestSupport, ContainerUnderTest}
import blended.jms.utils.{IdAwareConnectionFactory, JmsDestination, JmsQueue}
import blended.streams.FlowHeaderConfig
import blended.streams.jms.{JmsProducerSettings, JmsStreamSupport}
import blended.streams.message.{FlowEnvelope, FlowEnvelopeLogger, FlowMessage}
import blended.streams.testsupport.{ExpectedBodies, ExpectedHeaders, ExpectedMessageCount, FlowMessageAssertion}
import blended.testsupport.BlendedTestSupport
import blended.testsupport.scalatest.LoggingFreeSpec
import blended.util.logging.Logger
import org.scalactic.Requirements.requireNonNull
import org.scalatest.events.{InfoProvided, NameInfo}
import org.scalatest.{Args, BeforeAndAfterAll, CompositeStatus, Resources, Status, SucceededStatus, Suite, SuiteHelpers}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class BlendedDemoIntegrationSpec
  extends LoggingFreeSpec
  with BeforeAndAfterAll
  with BlendedIntegrationTestSupport
  with JmsStreamSupport {

  private implicit val system : ActorSystem = ActorSystem("Blended")
  private implicit val materializer : Materializer = ActorMaterializer()
  private implicit val testkit : TestKit = new TestKit(system)
  private implicit val eCtxt : ExecutionContext = system.dispatcher

  private[this] val log = Logger[BlendedDemoIntegrationSpec]

  private[this] implicit val timeout : Timeout = Timeout(300.seconds)
  private[this] val ctProxy = system.actorOf(TestContainerProxy.props(timeout.duration))

  private[this] val headerCfg : FlowHeaderConfig = FlowHeaderConfig.create("App")
  private[this] val envLogger : FlowEnvelopeLogger = FlowEnvelopeLogger.create(headerCfg, log)

  /** Even when unused, this one triggers the container start. */
  private[this] val cuts : Map[String, ContainerUnderTest] = {
    log.info(s"Using testkit [$testkit]")
    log.info(s"Using project test out path [${BlendedTestSupport.projectTestOutput}]")
    startContainers(ctProxy)(timeout, testkit)
    Await.result(containerReady(ctProxy)(timeout, testkit), timeout.duration)
  }

  private val intCf : IdAwareConnectionFactory = TestContainerProxy.internalCf
  private val extCf : IdAwareConnectionFactory = TestContainerProxy.externalCf

  override def nestedSuites : IndexedSeq[Suite] = IndexedSeq(
    new BlendedDemoSpec()
  )

  override def beforeAll(): Unit = {}

  override def afterAll(): Unit = {
    log.info("Running afterAll...")

    val ctr = "node_0"
    val dir = "/opt/node/log"

    readContainerDirectory(ctProxy, ctr, dir).onComplete {
      case Success(cdr) => cdr.result match {
        case Left(_) =>
        case Right(cd) =>
          val outputDir = s"$testOutput/testlog"
          saveContainerDirectory(outputDir, cd)
          log.info(s"Saved container output to [$outputDir]")

      }
      case Failure(e) =>
        log.error(e)(s"Could not read container directory [$dir] of container [$ctr]")
    }

    //stopContainers(ctProxy)(timeout, testkit)
  }
}
