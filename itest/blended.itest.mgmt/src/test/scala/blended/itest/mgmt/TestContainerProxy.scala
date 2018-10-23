package blended.itest.mgmt

import scala.concurrent.duration._

import akka.actor.Props
import blended.itestsupport.BlendedTestContextManager
import blended.itestsupport.ContainerUnderTest
import blended.itestsupport.TestContextConfigurator
import blended.itestsupport.condition.Condition
import blended.itestsupport.condition.ParallelComposedCondition
import blended.itestsupport.http.HttpAvailableCondition
import blended.util.logging.Logger
import org.apache.camel.CamelContext

class TestContainerProxy(timeout: FiniteDuration) extends BlendedTestContextManager with TestContextConfigurator {

  import TestContainerProxy._

  private[this] val dockerHost = context.system.settings.config.getString("docker.host")

  override def configure(cuts: Map[String, ContainerUnderTest], camelCtx: CamelContext): CamelContext = camelCtx

  override def containerReady(cuts: Map[String, ContainerUnderTest]): Condition = {
    implicit val system = context.system

    Logger[TestContainerProxy].debug(s"Checking container ready with cuts [${cuts}]")

    ParallelComposedCondition(
      HttpAvailableCondition(mgmtHttp(cuts, dockerHost) + "/mgmt/version", Some(timeout)),
      HttpAvailableCondition(nodeHttp(1, cuts, dockerHost) + "/helloworld", Some(timeout)),
      HttpAvailableCondition(nodeHttp(2, cuts, dockerHost) + "/helloworld", Some(timeout))
    )
  }
}

object TestContainerProxy {

  def mgmtHttp(cuts: Map[String, ContainerUnderTest], dockerHost: String): String =
    cuts("mgmt_0").url("http-akka", dockerHost, "http")

  def nodeHttp(nr: Int, cuts: Map[String, ContainerUnderTest], host: String): String =
    cuts(s"node${nr}_0").url("http-akka", host, "http")

  def akkaHttpsTestUrl(cuts: Map[String, ContainerUnderTest], dockerHost: String): String =
    cuts("mgmt_0").url("https-akka", dockerHost, "https") + "/mgmt/version"

  def jettyHttpTestUrl(cuts: Map[String, ContainerUnderTest], dockerHost: String): String =
    cuts("mgmt_0").url("http-jetty", dockerHost, "http") + "/hawtio/dashboard"

  def props(timeout: FiniteDuration): Props = Props(new TestContainerProxy(timeout))

}