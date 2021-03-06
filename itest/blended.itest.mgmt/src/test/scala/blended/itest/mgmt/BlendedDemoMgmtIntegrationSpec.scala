package blended.itest.mgmt

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.docker.protocol.GetContainerDirectoryResult
import blended.itestsupport.{BlendedIntegrationTestSupport, ContainerUnderTest}
import blended.util.logging.Logger
import org.scalatest.{BeforeAndAfterAll, TestSuite}
import org.scalatest.refspec.RefSpec

import scala.collection.immutable.IndexedSeq
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

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

  /** Even when unused, this one triggers the container start. */
  private[this] val cuts : Map[String, ContainerUnderTest] = {
    log.info(s"Using testkit [$testkit]")
    startContainers(ctProxy)(timeout, testkit)
    Await.result(containerReady(ctProxy)(timeout, testkit), timeout.duration)
  }

  override def nestedSuites : IndexedSeq[TestSuite] = {
    IndexedSeq(new BlendedDemoMgmtSpec())
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    log.info(s"Container under test are : [${cuts.values.mkString(",")}]")
  }

  override def afterAll() : Unit = {
    log.info("Running afterAll...")

    // wait a second, to give container logs time to flush
    Thread.sleep(1000)

    def writeLog(ctr: String, dir: String): Future[GetContainerDirectoryResult] = {
      val result : Future[GetContainerDirectoryResult] = readContainerDirectory(ctProxy, ctr, dir)

      val logdir : String = System.getProperty("logDir", testOutput)

      result.onComplete {
        case Success(cdr) => cdr.result match {
          case Left(_) =>
          case Right(cd) =>
            val outputDir = s"$logdir/$ctr"
            saveContainerDirectory(outputDir, cd)
            log.info(s"Saved container output to [$outputDir]")

        }
        case Failure(e) =>
          log.error(e)(s"Could not read containder directory [$dir] of container [$ctr]")
      }

      result
    }

    val getDirs = Future.sequence(Seq(
      writeLog("mgmt_0", "opt/blended.demo.mgmt_2.13/log"),
      writeLog("node1_0", "opt/blended.demo.node_2.13/log"),
      writeLog("node2_0", "opt/blended.demo.node_2.13/log")
    ))

    Await.result(getDirs, timeout.duration)
    stopContainers(ctProxy)(timeout, testkit)
  }
}
