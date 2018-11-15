package blended.itest.node

import blended.itestsupport.{BlendedTestContextManager, ContainerUnderTest, TestContextConfigurator}
import blended.itestsupport.condition.{Condition, SequentialComposedCondition}
import blended.itestsupport.jms.JMSAvailableCondition
import blended.itestsupport.jolokia.{CamelContextExistsCondition, JolokiaAvailableCondition}
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.camel.CamelContext
import org.apache.camel.component.jms.JmsComponent
import scala.concurrent.duration._

import akka.actor.Props


class TestContainerProxy(timeout: FiniteDuration) extends BlendedTestContextManager with TestContextConfigurator {

  import TestContainerProxy._
  
  implicit val dockerHost = context.system.settings.config.getString("docker.host")

  override def configure(cuts: Map[String, ContainerUnderTest], camelCtxt : CamelContext): CamelContext = {
    camelCtxt.addComponent("internal", JmsComponent.jmsComponent(new ActiveMQConnectionFactory(internal(cuts))))
    camelCtxt.addComponent("external", JmsComponent.jmsComponent(new ActiveMQConnectionFactory(external(cuts))))
    camelCtxt
  }
  
  override def containerReady(cuts: Map[String, ContainerUnderTest]) : Condition = {
    
    implicit val system = context.system

    SequentialComposedCondition(
      JMSAvailableCondition(new ActiveMQConnectionFactory(internal(cuts)), Some(timeout)),
      JMSAvailableCondition(new ActiveMQConnectionFactory(external(cuts)), Some(timeout)),
      JolokiaAvailableCondition(jmxRest(cuts), Some(timeout), Some("root"), Some("mysecret")),
      CamelContextExistsCondition(jmxRest(cuts), Some("root"), Some("mysecret"),  "BlendedSampleContext", Some(timeout))
    )
  }
}


object TestContainerProxy {
  def internal(cuts: Map[String, ContainerUnderTest])(implicit dockerHost: String) : String = cuts("node_0").url("internal", dockerHost, "tcp")
  def external(cuts: Map[String, ContainerUnderTest])(implicit dockerHost: String) : String = cuts("node_0").url("external", dockerHost, "tcp")
  def jmxRest(cuts: Map[String, ContainerUnderTest])(implicit dockerHost: String) : String = s"${cuts("node_0").url("http", dockerHost, "http")}/hawtio/jolokia"
  def ldapUrl(cuts: Map[String, ContainerUnderTest])(implicit dockerHost: String) : String = cuts("apacheds_0").url("ldap", dockerHost, "ldap")

  def props(timeout: FiniteDuration): Props = Props(new TestContainerProxy(timeout))
}