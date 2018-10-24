package blended.itest.mgmt

import java.io.File

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import akka.actor.ActorRef
import akka.pattern._
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.BlendedIntegrationTestSupport
import blended.itestsupport.BlendedTestContextManager.ConfiguredContainers
import blended.itestsupport.BlendedTestContextManager.ConfiguredContainers_?
import blended.itestsupport.ContainerUnderTest
import blended.testsupport.BlendedTestSupport
import blended.testsupport.retry.Retry
import blended.testsupport.scalatest.LoggingFreeSpec
import blended.updater.config.RemoteContainerState
import blended.updater.config.RuntimeConfig
import blended.updater.config.json.PrickleProtocol._
import blended.util.logging.Logger
import com.softwaremill.sttp
import com.softwaremill.sttp.HttpURLConnectionBackend
import com.softwaremill.sttp.UriContext
//import com.softwaremill.sttp.sttp
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

  private[this] val log = Logger[BlendedDemoSpec]

  private[this] val dockerHost = system.settings.config.getString("docker.host")

  def cuts: Map[String, ContainerUnderTest] =
    Await.result((ctProxy ? ConfiguredContainers_?).mapTo[ConfiguredContainers], timeOut.duration).cuts

  implicit val backend = HttpURLConnectionBackend()

  def mgmtRequest(path: String) = {
    val uri = s"${TestContainerProxy.mgmtHttp(cuts, dockerHost)}${path}"
    log.debug(s"Using uri: ${uri}")
    val request = sttp.sttp.get(uri"${uri}")
    val response = request.send()
    response
  }

  "Reference test: Report Blended mgmt version" in {
    val response = mgmtRequest("/mgmt/version")
    assert(response.code === 200)
    assert(response.body.isRight)
  }

  "Mgmt container sees node containers" in {
    val rcs = Retry.unsafeRetry(delay = 2.seconds, retries = 20) {
      val response = mgmtRequest("/mgmt/container")
      val body = response.body.right.get
      val remoteContState = Unpickle[Seq[RemoteContainerState]].fromString(body).get
      log.info(s"remote container states: [${remoteContState}]")
      // we expect the mgmt container + 2 node containers
      assert(remoteContState.size >= 3)
      remoteContState
    }
    assert(rcs.filter(_.containerInfo.profiles.map(_.name).contains("blended.demo.mgmt_2.12")).size == 1)
    assert(rcs.filter(_.containerInfo.profiles.map(_.name).contains("blended.demo.node_2.12")).size == 2)
  }

  "Upload a deployment pack to mgmt node" in {
    val packFile = new File(BlendedTestSupport.projectTestOutput, "blended.demo.node_2.12-deploymentpack.zip")
    assert(packFile.exists() === true)

    val uploadUrl = s"${TestContainerProxy.mgmtHttp(cuts, dockerHost)}/mgmt/profile/upload/deploymentpack/artifacts"
    val uploadResponse = sttp.sttp.multipartBody(sttp.multipartFile("file", packFile)).
      post(uri"${uploadUrl}").
      send()

    assert(uploadResponse.code === 200)

    val rcsJson = mgmtRequest("/mgmt/runtimeConfig").body.right.get
    val rcs = Unpickle[Seq[RuntimeConfig]].fromString(rcsJson).get
    assert(rcs.size >= 1)
    assert(rcs.find(_.name == "blended.demo.node_2.12").isDefined)
  }

  // TODO: register a profile
  // TODO: register a overlay
  // TODO: schedule a profile+overlay update for a node container
  // TODO: restart node-container
  // TODO: check restarted node-container for new profile+overlay

}