package blended.itest.mgmt

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import akka.actor.ActorRef
import akka.pattern._
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.Timeout
import blended.itestsupport.BlendedIntegrationTestSupport
import blended.itestsupport.BlendedTestContextManager.ConfiguredContainers
import blended.itestsupport.BlendedTestContextManager.ConfiguredContainers_?
import blended.itestsupport.ContainerUnderTest
import blended.itestsupport.condition.ConditionActor
import blended.testsupport.scalatest.LoggingFreeSpec
import blended.updater.config.RuntimeConfig
import blended.updater.config.json.PrickleProtocol._
import blended.util.logging.Logger
import com.softwaremill.sttp.HttpURLConnectionBackend
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.UriContext
import com.softwaremill.sttp.sttp
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
  //  implicit val eCtxt = testKit.system.dispatcher
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
    val response = mgmtRequest("/mgmt/runtimeConfig")

    assert(response.code === 200)
    assert(response.body.isRight)

    val body = response.body.right.get
    Unpickle[Seq[RuntimeConfig]].fromString(body)
    log.info(s"runtime configs: [${body}]")

//    val request = sttp.get(uri"${url}")
//    val response = request.send()
//    log.debug(s"Response: [${response}]")
//    response.code

  }

}