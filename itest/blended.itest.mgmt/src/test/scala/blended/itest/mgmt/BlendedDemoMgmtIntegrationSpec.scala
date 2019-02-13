package blended.itest.mgmt

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
import org.scalatest.{BeforeAndAfterAll, TestSuite}

/**
 * This Spec contains mostly test setup and teardown logic.
 * The real test cases are nested in [[nestedSuites]].
 */
class BlendedDemoMgmtIntegrationSpec
  extends RefSpec
  with BeforeAndAfterAll
  with BlendedIntegrationTestSupport {

  private implicit val system : ActorSystem = ActorSystem("Blended")
  private implicit val testkit : TestKit = new TestKit(system)
  private implicit val eCtxt : ExecutionContext = testkit.system.dispatcher

  private[this] val log = Logger[BlendedDemoMgmtIntegrationSpec]

  /**
   * Timeout in which we wait for container ready (before nested tests).
   * Also used when stopping containers.
   */
  private[this] implicit val timeout : Timeout = Timeout(180.seconds)
  private[this] val ctProxy = system.actorOf(TestMgmtContainerProxy.props(timeout.duration))

  override def nestedSuites : IndexedSeq[TestSuite] = IndexedSeq(new BlendedDemoMgmtSpec())

  override def afterAll() {
    log.info("Running afterAll...")

    // wait a second, to give container logs time to flush
    Thread.sleep(1000)

    def writeLog(ctr: String, dir: String): Unit = {
      readContainerDirectory(ctProxy, ctr, dir).onComplete {
        case Success(cdr) => cdr.result match {
          case Left(_) =>
          case Right(cd) =>
            val outputDir = s"$testOutput/testlog/$ctr"
            saveContainerDirectory(outputDir, cd)
            log.info(s"Saved container output to [$outputDir]")

        }
        case Failure(e) =>
          log.error(e)(s"Could not read containder directory [$dir] of container [$ctr]")
      }
    }

    writeLog("mgmt_0", "opt/mgmt/log")
    writeLog("node1_0", "opt/node/log")
    writeLog("node2_0", "opt/node/log")

    stopContainers(ctProxy)(timeout, testkit)
  }
}
