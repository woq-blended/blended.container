package blended.itest.mgmt

import akka.testkit.TestKit
import akka.util.Timeout
import blended.testsupport.TestFile
import blended.testsupport.retry.Retry
import blended.testsupport.scalatest.LoggingFreeSpec
import blended.updater.config._
import blended.updater.config.json.PrickleProtocol._
import blended.util.logging.Logger
import sttp.client._

import scala.concurrent.duration.DurationInt
import org.scalatest.DoNotDiscover
import org.scalatest.matchers.should.Matchers
import prickle.Unpickle
import sttp.model.StatusCode

@DoNotDiscover
class BlendedDemoMgmtSpec()(implicit testKit: TestKit)
  extends LoggingFreeSpec
  with Matchers
  with TestFile {

  implicit val system = testKit.system
  implicit val timeOut = Timeout(30.seconds)
  implicit val eCtxt = testKit.system.dispatcher
  implicit val scheduler = testKit.system.scheduler

  private[this] val log = Logger[BlendedDemoMgmtSpec]

  implicit val backend = HttpURLConnectionBackend()

  def mgmtRequest(path: String) = {
    val uri = s"${TestMgmtContainerProxy.mgmtHttp}${path}"
    log.debug(s"Using uri: ${uri}")
    val request = basicRequest.get(uri"${uri}")
    val response = request.send()
    response
  }

  "Reference test: Report Blended mgmt version" in logException {
    val response = mgmtRequest("/mgmt/version")
    assert(response.code === StatusCode.Ok)
    assert(response.body.isRight)
  }

  "Mgmt container sees 2 node and 1 mgmt container" in logException {
    val rcs : Seq[RemoteContainerState] = Retry.unsafeRetry(delay = 2.seconds, retries = 20) {
      val response = mgmtRequest("/mgmt/container")
      val body = response.body
      val remoteContState : Seq[RemoteContainerState] = Unpickle[Seq[RemoteContainerState]].fromString(body.getOrElse("")).get
      log.info(s"remote container states: [${remoteContState.mkString("\n")}]")
      // we expect the mgmt container + 2 node containers
      assert(remoteContState.size >= 3)
      remoteContState
    }

    val ctProfiles : Map[String, Seq[String]] = rcs.map{ s => 
      s.containerInfo.containerId -> s.containerInfo.profiles.map(_.name)
    }.toMap

    assert(ctProfiles.count(_._2.exists(_.startsWith("blended.demo.node_"))) == 2)
    assert(ctProfiles.count(_._2.exists(_.startsWith("blended.demo.mgmt_"))) == 1)
  }
}