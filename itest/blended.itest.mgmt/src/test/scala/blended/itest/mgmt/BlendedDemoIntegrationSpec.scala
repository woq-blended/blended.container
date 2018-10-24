package blended.itest.mgmt

import scala.collection.immutable.IndexedSeq
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.BlendedIntegrationTestSupport
import blended.util.logging.Logger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.refspec.RefSpec

/**
 * This Spec contains mostly test setup and teardown logic.
 * The real test cases are nested in [[nestedSuites]].
 */
class BlendedDemoIntegrationSpec
  extends RefSpec
  with BeforeAndAfterAll
  with BlendedIntegrationTestSupport {

  implicit val testkit = new TestKit(ActorSystem("Blended"))
  implicit val eCtxt = testkit.system.dispatcher

  private[this] val log = Logger[BlendedDemoIntegrationSpec]

  /**
   * Timeout in which we wait for container ready (before nested tests).
   * Also used when stopping containers.
   */
  private[this] implicit val timeout = Timeout(60.seconds)
  private[this] val ctProxy = testkit.system.actorOf(TestContainerProxy.props(timeout.duration))

  override def nestedSuites = IndexedSeq(new BlendedDemoSpec(ctProxy: ActorRef))

  override def beforeAll() {
    log.info(s"Using testkit [${testkit}]")
    testContext(ctProxy)(timeout, testkit)
    containerReady(ctProxy)(timeout, testkit)
    log.info("Container is ready: Starting tests...")
  }

  override def afterAll() {
    log.info("Running afterAll...")

    def writeLog(ctr: String, dir: String): Unit = {
      readContainerDirectory(ctProxy, ctr, dir).onComplete {
        case Success(cdr) => cdr.result match {
          case Left(_) =>
          case Right(cd) =>
            val outputDir = s"${testOutput}/testlog/${ctr}"
            saveContainerDirectory(outputDir, cd)
            log.info(s"Saved container output to [${outputDir}]")

        }
        case Failure(e) =>
          log.error(e)(s"Could not read containder directory [${dir}] of container [${ctr}]")
      }
    }

    writeLog("mgmt_0", "opt/mgmt/log")
    writeLog("node1_0", "opt/node/log")
    writeLog("node2_0", "opt/node/log")

    stopContainers(ctProxy)(timeout, testkit)
  }
}
