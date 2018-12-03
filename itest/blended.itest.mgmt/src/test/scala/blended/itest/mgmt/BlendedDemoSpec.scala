package blended.itest.mgmt

import java.io.File
import java.util.UUID

import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt

import akka.actor.ActorRef
import akka.testkit.TestKit
import akka.util.Timeout
import blended.itestsupport.BlendedIntegrationTestSupport
import blended.itestsupport.ContainerUnderTest
import blended.testsupport.BlendedTestSupport
import blended.testsupport.TestFile
import blended.testsupport.retry.Retry
import blended.testsupport.scalatest.LoggingFreeSpec
import blended.updater.config._
import blended.updater.config.json.PrickleProtocol._
import blended.util.logging.Logger
import com.softwaremill.sttp
import com.softwaremill.sttp.HttpURLConnectionBackend
import com.softwaremill.sttp.UriContext
import com.typesafe.config.ConfigFactory
import prickle.Pickle
//import com.softwaremill.sttp.sttp
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers
import prickle.Unpickle

@DoNotDiscover
class BlendedDemoSpec(
  cuts: Map[String, ContainerUnderTest],
  ctProxy: ActorRef
)(implicit testKit: TestKit)
  extends LoggingFreeSpec
  with Matchers
  with BlendedIntegrationTestSupport
  with TestFile {

  implicit val system = testKit.system
  implicit val timeOut = Timeout(30.seconds)
  implicit val eCtxt = testKit.system.dispatcher
  implicit val scheduler = testKit.system.scheduler

  private[this] val log = Logger[BlendedDemoSpec]

  private[this] val dockerHost = system.settings.config.getString("docker.host")

  implicit val backend = HttpURLConnectionBackend()

  def mgmtRequest(path: String) = {
    val uri = s"${TestContainerProxy.mgmtHttp(cuts, dockerHost)}${path}"
    log.debug(s"Using uri: ${uri}")
    val request = sttp.sttp.get(uri"${uri}")
    val response = request.send()
    response
  }

  // Tests written here are expected in order, means, they depend on each other and in the same ordering.

  "Reference test: Report Blended mgmt version" in logException {
    val response = mgmtRequest("/mgmt/version")
    assert(response.code === 200)
    assert(response.body.isRight)
  }

  "Mgmt container sees node containers" in logException {
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

  "Upload a deployment pack to mgmt node" in logException {
    val packFile = new File(BlendedTestSupport.projectTestOutput, "blended.demo.node_2.12-deploymentpack.zip")
    assert(packFile.exists() === true)

    val uploadUrl = s"${TestContainerProxy.mgmtHttp(cuts, dockerHost)}/mgmt/profile/upload/deploymentpack/artifacts"
    log.debug(s"Uploading to: ${uploadUrl}")
    val uploadResponse = sttp.sttp.
      post(uri"${uploadUrl}").
      auth.basic("itest", "secret").
      multipartBody(sttp.multipartFile("file", packFile)).
      send()

    assert(uploadResponse.code === 200)

    val rcsJson = mgmtRequest("/mgmt/runtimeConfig").body.right.get
    val rcs = Unpickle[Seq[RuntimeConfig]].fromString(rcsJson).get
    assert(rcs.size >= 1)
    assert(rcs.find(_.name == "blended.demo.node_2.12").isDefined)
  }

  "Upload two overlays to mgmt node" in logException {

    val o1 =
      """name = "jvm-medium"
        |version = "1"
        |properties = {
        |  "blended.launcher.jvm.xms" = "768M"
        |  "blended.launcher.jvm.xmx" = "768M"
        |  "amq.systemMemoryLimit" = "500m"
        |}
        |""".stripMargin
    val overlayConfig1 = OverlayConfigCompanion.read(ConfigFactory.parseString(o1)).get

    val o2 =
      """name = "jvm-large"
        |version = "1"
        |properties = {
        |  "blended.launcher.jvm.xms" = "1024M"
        |  "blended.launcher.jvm.xmx" = "1024M"
        |  "amq.systemMemoryLimit" = "600m"
        |}
        |""".stripMargin
    val overlayConfig2 = OverlayConfigCompanion.read(ConfigFactory.parseString(o2)).get

    val uploadUrl = s"${TestContainerProxy.mgmtHttp(cuts, dockerHost)}/mgmt/overlayConfig"
    val uploadResponse1 = sttp.sttp.
      body(Pickle.intoString(overlayConfig1)).
      header(sttp.HeaderNames.ContentType, sttp.MediaTypes.Json).
      auth.basic("itest", "secret").
      post(uri"${uploadUrl}").
      send()
    assert(uploadResponse1.code === 200)

    val uploadResponse2 = sttp.sttp.
      body(Pickle.intoString(overlayConfig2)).
      header(sttp.HeaderNames.ContentType, sttp.MediaTypes.Json).
      auth.basic("itest", "secret").
      post(uri"${uploadUrl}").
      send()
    assert(uploadResponse2.code === 200)

    val ocsJson = mgmtRequest("/mgmt/overlayConfig").body.right.get
    val ocs = Unpickle[Seq[OverlayConfig]].fromString(ocsJson).get
    assert(ocs.size >= 2)
    assert(ocs.find(_.name == "jvm-medium").isDefined)
    assert(ocs.find(_.name == "jvm-large").isDefined)
  }

  case class RolloutCtx(profileName: String, profileVersion: String, overlayName: String, overlayVersion: String, containerId: String)

  val rolloutCtx = Promise[RolloutCtx]()

  "Rollout a profile+overlay for one node" in logException {
    // wait until all containers are present
    val containers = Retry.unsafeRetry(delay = 2.seconds, retries = 20) {
      val response = mgmtRequest("/mgmt/container")
      val body = response.body.right.get
      val remoteContState = Unpickle[Seq[RemoteContainerState]].fromString(body).get
      log.info(s"remote container states: [${remoteContState}]")
      // we expect the mgmt container + 2 node containers
      assert(remoteContState.size >= 3)
      remoteContState
    }

    // we need 2 node containers
    val nodeRcs = containers.filter(_.containerInfo.profiles.map(_.name).contains("blended.demo.node_2.12"))
    val containerIds = nodeRcs.map(_.containerInfo.containerId)
    assert(containerIds.size === 2)
    log.info("All node containers: " + dumpProfiles(nodeRcs.map(_.containerInfo)))

    // pick the first node container
    val id1 = containerIds.head
    val overlayName = "jvm-large"

    val rcsJson = mgmtRequest("/mgmt/runtimeConfig").body.right.get
    val rcs = Unpickle[Seq[RuntimeConfig]].fromString(rcsJson).get
    assert(rcs.size >= 1)

    // the new profile to apply
    val profile = rcs.find(_.name == "blended.demo.node_2.12").get

    val ocsJson = mgmtRequest("/mgmt/overlayConfig").body.right.get
    val ocs = Unpickle[Seq[OverlayConfig]].fromString(ocsJson).get
    val overlay = ocs.find(_.name == overlayName).get

    rolloutCtx.success(RolloutCtx(profile.name, profile.version, overlayName, overlay.version, id1))

    log.debug(s"Schedule a profile and overlay [${overlay}] update for node ${id1}")

    val rollout = RolloutProfile(
      profile.name, profile.version,
      overlays = Set(overlay.overlayRef),
      containerIds = List(id1)
    )

    val rolloutUrl = s"${TestContainerProxy.mgmtHttp(cuts, dockerHost)}/mgmt/rollout/profile"
    log.info(s"Using rollout uri [${rolloutUrl}] with body [${pp(rollout)}]")
    val response = sttp.sttp
      .post(uri"${rolloutUrl}")
      .auth.basic("itest", "secret")
      .header(sttp.HeaderNames.ContentType, sttp.MediaTypes.Json)
      .body(Pickle.intoString(rollout))
      .send()
    log.debug(s"Rollout request response: ${pp(response)}")
    assert(response.code === 200)

    log.info("Now wait for node container to report new profile")
    Retry.unsafeRetry(5.seconds, retries = 60) {
      log.info(s"We expect the updated node container with ID [${id1}] to appear after some time with the new profile")

      val response = mgmtRequest("/mgmt/container")
      val body = response.body.right.get
      val rcs = Unpickle[Seq[RemoteContainerState]].fromString(body).get
      // log.info(s"remote container states: [${pp(rcs)}]")

      val updatedNode = rcs.find(_.containerInfo.containerId == id1)
      assert(updatedNode.isDefined)

      val profiles = updatedNode.get.containerInfo.profiles
      log.debug(s"profiles of node under test: [${pp(profiles)}]")

      val updatedProfile = profiles.find(p =>
        p.name == "blended.demo.node_2.12" &&
          p.overlays.exists(o => o.name == overlayName))
      assert(updatedProfile.isDefined)
    }
  }

  "Rolled-out profile gets staged (valid)" in logException {
    // we depend on a part-result of previous test case
    assert(rolloutCtx.isCompleted, "Rollout test must complete before this test can run")
    val rCtx = rolloutCtx.future.value.get.get

    Retry.unsafeRetry(5.seconds, retries = 60) {
      val response = mgmtRequest("/mgmt/container")
      val body = response.body.right.get
      val rcs = Unpickle[Seq[RemoteContainerState]].fromString(body).get
      // log.info(s"remote container states: [${pp(rcs)}]")

      val updatedNode = rcs.find(_.containerInfo.containerId == rCtx.containerId)
      assert(updatedNode.isDefined)
      val profiles = updatedNode.get.containerInfo.profiles
      log.debug(s"profiles of node under test: [${pp(profiles)}]")
      val updatedProfile = profiles.find(p =>
        p.name == rCtx.profileName && p.state == OverlayState.Valid && p.overlays.exists(o => o.name == rCtx.overlayName))
      assert(updatedProfile.isDefined)
    }
  }

  "Container re-starts with newly rolled-out profile" in logException {
    // we depend on a part-result of previous test case
    assert(rolloutCtx.isCompleted, "Rollout test must complete before this test can run")
    val rCtx = rolloutCtx.future.value.get.get

    val activateProfile = ActivateProfile(
      id = UUID.randomUUID().toString(),
      profileName = rCtx.profileName,
      profileVersion = rCtx.profileVersion,
      overlays = Set(OverlayRef(name = rCtx.overlayName, version = rCtx.overlayVersion))
    )

    val activateUrl = s"${TestContainerProxy.mgmtHttp(cuts, dockerHost)}/mgmt/container/${rCtx.containerId}/update"
    log.info(s"Using activate uri [${activateUrl}] with body [${pp(activateProfile)}]")
    val response = sttp.sttp
      .post(uri"${activateUrl}")
      .auth.basic("itest", "secret")
      .header(sttp.HeaderNames.ContentType, sttp.MediaTypes.Json)
      .body(Pickle.intoString[UpdateAction](activateProfile))
      .send()
    log.debug(s"Activate request response: ${pp(response)}")
    assert(response.code === 200)

    Retry.unsafeRetry(5.seconds, retries = 60) {
      val response = mgmtRequest("/mgmt/container")
      val body = response.body.right.get
      val rcs = Unpickle[Seq[RemoteContainerState]].fromString(body).get
      // log.info(s"remote container states: [${pp(rcs)}]")

      val updatedNode = rcs.find(_.containerInfo.containerId == rCtx.containerId)
      assert(updatedNode.isDefined)
      val profiles = updatedNode.get.containerInfo.profiles
      log.debug(s"profiles of node under test: [${pp(profiles)}]")
      val updatedProfile = profiles.find(p =>
        p.name == rCtx.profileName && p.state == OverlayState.Active && p.overlays.exists(o => o.name == rCtx.overlayName))
      assert(updatedProfile.isDefined)
    }
  }

  def dumpProfiles(cis: Seq[ContainerInfo]): Seq[String] = {
    cis.map { ci =>
      s"Container [${pp(ci.containerId)}] has profiles [${pp(ci.profiles)}]"
    }
  }

  def pp(x: Any) = pprint.apply(x, width = 80, height = 1000000)

  // TODO: restart node-container
  // TODO: check restarted node-container for new profile+overlay

}