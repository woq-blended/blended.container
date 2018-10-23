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
    camelCtxt.addComponent("jms", JmsComponent.jmsComponent(new ActiveMQConnectionFactory(amqUrl(cuts))))
    camelCtxt
  }
  
  override def containerReady(cuts: Map[String, ContainerUnderTest]) : Condition = {
    
    implicit val system = context.system

    SequentialComposedCondition(
      JMSAvailableCondition(new ActiveMQConnectionFactory(amqUrl(cuts)), Some(timeout)),
      JolokiaAvailableCondition(jmxRest(cuts), Some(timeout), Some("root"), Some("mysecret")),
      CamelContextExistsCondition(jmxRest(cuts), Some("root"), Some("mysecret"),  "BlendedSampleContext", Some(timeout))
    )
  }
}


object TestContainerProxy {
  def amqUrl(cuts: Map[String, ContainerUnderTest])(implicit dockerHost: String) : String = cuts("node_0").url("jms", dockerHost, "tcp")
  def jmxRest(cuts: Map[String, ContainerUnderTest])(implicit dockerHost: String) : String = s"${cuts("node_0").url("http", dockerHost, "http")}/hawtio/jolokia"
  def ldapUrl(cuts: Map[String, ContainerUnderTest])(implicit dockerHost: String) : String = cuts("apacheds_0").url("ldap", dockerHost, "ldap")

  def props(timeout: FiniteDuration): Props = Props(new TestContainerProxy(timeout))
}