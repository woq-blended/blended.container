package blended.itest.mgmt

import akka.actor.Props
import blended.itestsupport._
import blended.itestsupport.condition.{Condition, ParallelComposedCondition}
import blended.itestsupport.http.HttpAvailableCondition
import blended.util.logging.Logger

import scala.concurrent.duration._

class TestContainerProxy(timeout: FiniteDuration)
  extends DockerbasedTestconnectorSetup
  with TestConnectorSetup {

  private[this] val log : Logger = Logger[TestContainerProxy.type]
  private[this] val dockerHost = context.system.settings.config.getString("docker.host")

  override def configure(cuts: Map[String, ContainerUnderTest]): Unit = {

    val mgmtHttp : String = cuts("mgmt_0").url("http-akka", dockerHost, "http")
    val node_1_Http : String = cuts("node1_0").url("http-akka", dockerHost, "http")
    val node_2_Http : String = cuts("node2_0").url("http-akka", dockerHost, "http")

    TestConnector.put("ctProxy", self)
    TestConnector.put("mgmtHttp", mgmtHttp)
    TestConnector.put("node1Http", node_1_Http)
    TestConnector.put("node2Http", node_2_Http)

    log.debug(s"Configured TestConnector [${TestConnector.properties.mkString(",")}]")
  }

  override def containerReady(): Condition = {

    implicit val system = context.system
    import TestContainerProxy._

    ParallelComposedCondition(
      HttpAvailableCondition(mgmtHttp + "/mgmt/version", Some(timeout)),
      HttpAvailableCondition(node1Http + "/helloworld", Some(timeout)),
      HttpAvailableCondition(node2Http + "/helloworld", Some(timeout))
    )
  }
}

object TestContainerProxy {

  def mgmtHttp : String = TestConnector.property[String]("mgmtHttp").get
  def node1Http : String = TestConnector.property[String]("node1Http").get
  def node2Http : String = TestConnector.property[String]("node2Http").get

  def props(timeout: FiniteDuration): Props = Props(new TestContainerProxy(timeout))
}