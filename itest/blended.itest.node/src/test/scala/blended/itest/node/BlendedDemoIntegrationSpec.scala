package blended.itest.node

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.{BlendedIntegrationTestSupport, ContainerUnderTest}
import blended.testsupport.BlendedTestSupport
import blended.util.logging.Logger
import org.scalatest.refspec.RefSpec
import org.scalatest.{Args, BeforeAndAfterAll, Status, Suite}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

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

  /** Even when unused, this one triggers the container start. */
  private[this] val cuts : Map[String, ContainerUnderTest] = {
    log.info(s"Using testkit [$testkit]")
    log.info(s"Using project test out path [${BlendedTestSupport.projectTestOutput}]")
    startContainers(ctProxy)(timeout, testkit)
    Await.result(containerReady(ctProxy)(timeout, testkit), timeout.duration)
  }

  override def nestedSuites : IndexedSeq[Suite] = {
    val result = IndexedSeq(
      new BlendedDemoSpec()
    )
    log.info(s"Found sub specs [${result.map(_.suiteName)}]" )

    result
  }

  override def run(testName: Option[String], args: Args): Status = {
    log.info(s"Running tests with test name [$testName], args [$args]")
    super.run(testName, args)
  }

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
