package blended.itest.node

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.docker.protocol.GetContainerDirectoryResult
import blended.itestsupport.{BlendedIntegrationTestSupport, ContainerUnderTest}
import blended.testsupport.scalatest.LoggingFreeSpec
import blended.util.logging.Logger
import blended.testsupport.BlendedTestSupport
import blended.streams.jms.JmsStreamSupport

import scala.collection.immutable.IndexedSeq
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import org.scalatest.{BeforeAndAfterAll, Suite}

class BlendedDemoIntegrationSpec
  extends LoggingFreeSpec
  with BeforeAndAfterAll
  with BlendedIntegrationTestSupport
  with JmsStreamSupport {


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

  override def nestedSuites : IndexedSeq[Suite] = IndexedSeq(
    new BlendedDemoSpec()
  )

  override def beforeAll(): Unit = {
    log.info(s"Containers under test are : [${cuts.values.mkString(",")}]")
    log.info(s"Connection Factory [${TestContainerProxy.internalCf.id}] initialized")
    log.info(s"Connection Factory [${TestContainerProxy.externalCf.id}] initialized")
  }

  override def afterAll(): Unit = {
    log.info("Running afterAll...")

    val ctr = "node_0"
    val dir = "/opt/blended.demo.node_2.13/log"

    val logdir : String = System.getProperty("logDir", testOutput)

    val f : Future[GetContainerDirectoryResult] = readContainerDirectory(ctProxy, ctr, dir)

    f.onComplete {
      case Success(cdr) => cdr.result match {
        case Left(_) =>
        case Right(cd) =>
          val outputDir = s"$logdir"
          saveContainerDirectory(outputDir, cd)
          log.info(s"Saved container output to [$outputDir]")
      }
      case Failure(e) =>
        log.error(e)(s"Could not read container directory [$dir] of container [$ctr]")
    }

    Await.result(f, timeout.duration)
    stopContainers(ctProxy)(timeout, testkit)
  }
}
