package blended.itest.node

import akka.actor.Props
import blended.itestsupport.condition.{Condition, SequentialComposedCondition}
import blended.itestsupport.jms.JMSAvailableCondition
import blended.itestsupport.jolokia.{CamelContextExistsCondition, JolokiaAvailableCondition}
import blended.itestsupport.{BlendedTestContextManager, ContainerUnderTest, TestContextConfigurator}
import blended.jms.utils.{IdAwareConnectionFactory, SimpleIdAwareConnectionFactory}
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.camel.CamelContext
import org.apache.camel.component.jms.JmsComponent

import scala.concurrent.duration._

class TestContainerProxy(timeout: FiniteDuration) extends BlendedTestContextManager with TestContextConfigurator {

  import TestContainerProxy._

  val dockerHost : String = context.system.settings.config.getString("docker.host")

  override def configure(cuts: Map[String, ContainerUnderTest], camelCtxt : CamelContext): CamelContext = {
    camelCtxt.addComponent("internal", JmsComponent.jmsComponent(internalCf(dockerHost)(cuts)))
    camelCtxt.addComponent("external", JmsComponent.jmsComponent(externalCf(dockerHost)(cuts)))
    camelCtxt
  }

  override def containerReady(cuts: Map[String, ContainerUnderTest]) : Condition = {
    
    implicit val system = context.system

    SequentialComposedCondition(
      JMSAvailableCondition(internalCf(dockerHost)(cuts), Some(timeout)),
      JMSAvailableCondition(externalCf(dockerHost)(cuts), Some(timeout)),
      JolokiaAvailableCondition(jmxRest(dockerHost)(cuts), Some(timeout), Some("root"), Some("mysecret")),
      CamelContextExistsCondition(jmxRest(dockerHost)(cuts), Some("root"), Some("mysecret"),  "BlendedSampleContext", Some(timeout))
    )
  }
}

object TestContainerProxy {

  val internalCf : String => Map[String, ContainerUnderTest] => IdAwareConnectionFactory =
    host => cuts => new SimpleIdAwareConnectionFactory(
      vendor = "activemq",
      provider = "internal",
      clientId = "internal",
      cf = new ActiveMQConnectionFactory(internal(host)(cuts))
    )

  val externalCf : String => Map[String, ContainerUnderTest] => IdAwareConnectionFactory =
    host => cuts => new SimpleIdAwareConnectionFactory(
      vendor = "activemq",
      provider = "external",
      clientId = "external",
      cf = new ActiveMQConnectionFactory(external(host)(cuts))
    )

  val internal : String => Map[String, ContainerUnderTest] => String =
    dockerHost => cuts => cuts("node_0").url("internal", dockerHost, "tcp")

  val external : String => Map[String, ContainerUnderTest] => String =
    dockerHost => cuts => cuts("node_0").url("external", dockerHost, "tcp")

  val jmxRest : String => Map[String, ContainerUnderTest] => String =
    dockerHost => cuts => s"${cuts("node_0").url("http", dockerHost, "http")}/hawtio/jolokia"

  val ldapUrl : String => Map[String, ContainerUnderTest] => String =
    dockerHost => cuts => cuts("apacheds_0").url("ldap", dockerHost, "ldap")

  def props(timeout: FiniteDuration): Props = Props(new TestContainerProxy(timeout))
}