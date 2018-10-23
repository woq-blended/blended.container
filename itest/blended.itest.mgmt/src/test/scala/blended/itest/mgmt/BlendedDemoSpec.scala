package blended.itest.mgmt

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import akka.actor.ActorRef
import akka.actor.Scheduler
import akka.pattern._
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.BlendedIntegrationTestSupport
import blended.itestsupport.BlendedTestContextManager.ConfiguredContainers
import blended.itestsupport.BlendedTestContextManager.ConfiguredContainers_?
import blended.itestsupport.ContainerUnderTest
import blended.testsupport.scalatest.LoggingFreeSpec
import blended.updater.config.RemoteContainerState
import blended.updater.config.json.PrickleProtocol._
import blended.util.logging.Logger
import com.softwaremill.sttp.HttpURLConnectionBackend
import com.softwaremill.sttp.UriContext
import com.softwaremill.sttp.sttp
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers
import prickle.Unpickle

@DoNotDiscover
class BlendedDemoSpec(ctProxy: ActorRef)(implicit testKit: TestKit)
  extends LoggingFreeSpec
  with Matchers
  with BlendedIntegrationTestSupport {

  implicit val system = testKit.system
  implicit val timeOut = Timeout(30.seconds)
  implicit val eCtxt = testKit.system.dispatcher
  implicit val scheduler = testKit.system.scheduler
  //  override implicit val camelContext = CamelExtension.get(system).context

  //  private[this] val log = testKit.system.log
  private[this] val log = Logger[BlendedDemoSpec]

  private[this] val dockerHost = system.settings.config.getString("docker.host")

  def cuts: Map[String, ContainerUnderTest] =
    Await.result((ctProxy ? ConfiguredContainers_?).mapTo[ConfiguredContainers], timeOut.duration).cuts

  implicit val backend = HttpURLConnectionBackend()

  def mgmtRequest(path: String) = {
    val uri = s"${TestContainerProxy.mgmtHttp(cuts, dockerHost)}${path}"
    log.debug(s"Using uri: ${uri}")
    val request = sttp.get(uri"${uri}")
    val response = request.send()
    response
  }

  "Reference test: Report Blended mgmt version" in {
    val response = mgmtRequest("/mgmt/version")
    assert(response.code === 200)
    assert(response.body.isRight)
  }

  "Get a running mgmt container profile" in {
    val rcsFut = retry(delay = 2.seconds, retries = 20) {
      Future {
        val response = mgmtRequest("/mgmt/container")
        val body = response.body.right.get
        val remoteContState = Unpickle[Seq[RemoteContainerState]].fromString(body).get
        log.info(s"remote container states: [${remoteContState}]")
        // we expect the mgmt-container + 2 node containers
        assert(remoteContState.size >= 3)
        remoteContState
      }
    }
    val rcs = Await.result(rcsFut, 50.seconds)
    assert(rcs.filter(_.containerInfo.profiles.map(_.name) == "blended.demo.mgmt").size == 1)
    assert(rcs.filter(_.containerInfo.profiles.map(_.name) == "blended.demo.node").size == 2)

    // TODO: register a profile
    // TODO: register a overlay
    // TODO: schedule a profile+overlay update for a node container
    // TODO: restart node-container
    // TODO: check restarted node-container for new profile+overlay

  }

  /**
   * Given an operation that produces a T, returns a Future containing the result of T, unless an exception is thrown,
   * in which case the operation will be retried after _delay_ time, if there are more possible retries, which is configured through
   * the _retries_ parameter. If the operation does not succeed and there is no retries left, the resulting Future will contain the last failure.
   */
  def retry[T](
    delay: FiniteDuration,
    retries: Int,
    onRetry: (Int, Throwable) => Unit = (n, e) => log.debug(e)(s"Retrying after unfulfilled condition (${n} retries left)")
  )(
    op: => Future[T]
  )(implicit ec: ExecutionContext, s: Scheduler): Future[T] =
    Future { op } flatMap (x => x) recoverWith {
      case e: Throwable if retries > 0 => akka.pattern.after(delay, s)({
        onRetry(retries - 1, e)
        retry(delay, retries - 1, onRetry)(op)(ec, s)
      })
    }

}