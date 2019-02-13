package blended.itest.node

import scala.collection.immutable.IndexedSeq
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.BlendedIntegrationTestSupport
import blended.util.logging.Logger
import org.scalatest.refspec.RefSpec
import org.scalatest.{BeforeAndAfterAll, Suite}

class BlendedDemoIntegrationSpec
  extends RefSpec
  with BeforeAndAfterAll
  with BlendedIntegrationTestSupport {

  private implicit val system : ActorSystem = ActorSystem("Blended")
  private implicit val testkit : TestKit = new TestKit(system)
  private implicit val eCtxt : ExecutionContext = system.dispatcher

  private[this] val log = Logger[BlendedDemoIntegrationSpec]

  private[this] implicit val timeout : Timeout = Timeout(300.seconds)
  private[this] val ctProxy = system.actorOf(TestContainerProxy.props(timeout.duration))

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

    stopContainers(ctProxy)(timeout, testkit)
  }
}
