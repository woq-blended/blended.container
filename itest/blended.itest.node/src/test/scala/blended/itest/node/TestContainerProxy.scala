package blended.itest.node

import akka.actor.{ActorSystem, Props}
import blended.itestsupport.condition.{Condition, SequentialComposedCondition}
import blended.itestsupport.jms.{JMSAvailableCondition, JMSConnectedCondition}
import blended.itestsupport.jolokia.{CamelContextExistsCondition, JolokiaAvailableCondition}
import blended.itestsupport.{ContainerUnderTest, DockerbasedTestconnectorSetupActor, TestConnector, TestConnectorSetup}
import blended.jms.utils.{IdAwareConnectionFactory, SimpleIdAwareConnectionFactory}
import blended.jolokia.{JolokiaAddress, JolokiaClient}
import org.apache.activemq.ActiveMQConnectionFactory

import scala.concurrent.duration._

class TestContainerProxy(timeout: FiniteDuration)
  extends DockerbasedTestconnectorSetupActor
  with TestConnectorSetup {

  private[this] val testHost : String = context.system.settings.config.getString("test.host")

  override def configure(cuts: Map[String, ContainerUnderTest]): Unit = {

    implicit val system : ActorSystem = context.system

    // The url for the internal connection factory
    val internal : String = cuts("node_0").url("internal", testHost, "tcp")

    // The url for the external connection factory
    val external : String = cuts("node_0").url("external", testHost, "tcp")

    // the url to jolokia for REST JMX queries
    val jmxRest : String = s"${cuts("node_0").url("http", testHost, "http")}/hawtio/jolokia"
    val jolokia : JolokiaClient = new JolokiaClient(
      JolokiaAddress(
        jolokiaUrl = jmxRest,
        user = Some("root"),
        password = Some("mysecret")
      )
    )

    // the url to the ldap server
    //val ldapUrl : String = cuts("apacheds_0").url("ldap", testHost, "ldap")

    val internalCf : IdAwareConnectionFactory = SimpleIdAwareConnectionFactory(
      vendor = "activemq",
      provider = "internal",
      clientId = "spec-internal",
      cf = new ActiveMQConnectionFactory(internal),
      minReconnect = 10.seconds
    )

    val externalCf : IdAwareConnectionFactory = SimpleIdAwareConnectionFactory(
      vendor = "activemq",
      provider = "external",
      clientId = "spec-external",
      cf = new ActiveMQConnectionFactory(external),
      minReconnect = 10.seconds
    )

    TestConnector.put("jmxRest", jmxRest)
    TestConnector.put("jolokia", jolokia)
    //TestConnector.put("ldap", ldapUrl)
    TestConnector.put("internalCf", internalCf)
    TestConnector.put("externalCf", externalCf)

    log.debug(s"Configured Test Connector [${TestConnector.properties.mkString(",")}]")
  }

  override def containerReady() : Condition = {

    import TestContainerProxy._

    implicit val system : ActorSystem = context.system

    SequentialComposedCondition(
      JMSAvailableCondition(internalCf, Some(timeout)),
      JMSAvailableCondition(externalCf, Some(timeout)),
      JolokiaAvailableCondition(jolokia, Some(timeout)),
      JMSConnectedCondition(jolokia, "activemq", "internal", Some(timeout))
    )
  }
}

object TestContainerProxy {

  def jolokia : JolokiaClient =
    TestConnector.property[JolokiaClient]("jolokia").get

  def internalCf : IdAwareConnectionFactory =
    TestConnector.property[IdAwareConnectionFactory]("internalCf").get

  def externalCf : IdAwareConnectionFactory =
    TestConnector.property[IdAwareConnectionFactory]("externalCf").get

  def jmxRest : String =
    TestConnector.property[String]("jmxRest").get

  def ldapUrl : String =
    TestConnector.property[String]("ldap").get

  def props(timeout: FiniteDuration) : Props = Props(new TestContainerProxy(timeout))
}
