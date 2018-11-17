package blended.itest.node

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.{BlendedIntegrationTestSupport, ContainerUnderTest}
import blended.util.logging.Logger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.refspec.RefSpec

import scala.collection.immutable.IndexedSeq
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class BlendedDemoIntegrationSpec
  extends RefSpec
  with BeforeAndAfterAll
  with BlendedIntegrationTestSupport {

  implicit val testkit : TestKit = new TestKit(ActorSystem("Blended"))
  implicit val eCtxt : ExecutionContext = testkit.system.dispatcher

  private[this] val log = Logger[BlendedDemoIntegrationSpec]

  private[this] implicit val timeout : Timeout = Timeout(180.seconds)
  private[this] val ctProxy = testkit.system.actorOf(TestContainerProxy.props(timeout.duration))

  private[this] val cuts : Map[String, ContainerUnderTest] = {
    log.info(s"Using testkit [$testkit]")
    testContext(ctProxy)(timeout, testkit)
    Await.result(containerReady(ctProxy)(timeout, testkit), timeout.duration)
  }

  override def nestedSuites = IndexedSeq(
    new BlendedDemoSpec(cuts, ctProxy)
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
        log.error(e)(s"Could not read containder directory [${dir}] of container [${ctr}]")
    }

    stopContainers(ctProxy)(timeout, testkit)
  }
}
