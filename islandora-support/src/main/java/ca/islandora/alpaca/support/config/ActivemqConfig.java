/*
 * Licensed to Islandora Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * The Islandora Foundation licenses this file to you under the MIT License.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.islandora.alpaca.support.config;

import static org.slf4j.LoggerFactory.getLogger;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ActiveMQ configuration class
 *
 * @author whikloj
 */
@Configuration
public class ActivemqConfig extends PropertyConfig {

  private static final Logger LOGGER = getLogger(ActivemqConfig.class);

  public static final String JMS_BROKER_URL = "jms.brokerUrl";
  public static final String JMS_USERNAME = "jms.username";
  public static final String JMS_PASSWORD = "jms.password";
  public static final String CONNECTIONS = "jms.connections";

  @Value("${" + JMS_BROKER_URL + ":tcp://localhost:61616}")
  private String jmsBrokerUrl;

  @Value("${" + JMS_USERNAME + ":#{null}}")
  private String jmsUsername;

  @Value("${" + JMS_PASSWORD + ":#{null}}")
  private String jmsPassword;

  @Value("${" + CONNECTIONS + ":10}")
  private int jmsConnections;

  /**
   * @return the jms broker url
   */
  public String getJmsBrokerUrl() {
    if (jmsBrokerUrl == null) {
      return "";
    }
    return jmsBrokerUrl;
  }

  /**
   * @return the jms broker username (if applicable).
   */
  public String getJmsUsername() {
    if (jmsUsername == null) {
      return "";
    }
    return jmsUsername;
  }

  /**
   * @return the jms broker password (if applicable).
   */
  public String getJmsPassword() {
    if (jmsPassword == null) {
      return "";
    }
    return jmsPassword;
  }

  /**
   * @return the size of the connection pool.
   */
  public int getJmsConnections() {
    return jmsConnections;
  }

  /**
   * @return JMS Connection factory bean.
   * @throws JMSException on failure to create new connection.
   */
  @Bean
  public ConnectionFactory jmsConnectionFactory() throws JMSException {
    final var factory = new ActiveMQConnectionFactory();
    LOGGER.debug("jmsConnectionFactory: brokerUrl is {}", getJmsBrokerUrl());
    if (!getJmsBrokerUrl().isBlank()) {
      factory.setBrokerURL(getJmsBrokerUrl());
      LOGGER.debug("jms username is {}", getJmsUsername());
      if (!getJmsUsername().isBlank() && !getJmsPassword().isBlank()) {
        factory.createConnection(getJmsUsername(), getJmsPassword());
      }
    }
    return factory;
  }

  /**
   * @param connectionFactory the JMS connection factory.
   * @return A pooled connection factory.
   */
  @Bean
  public PooledConnectionFactory pooledConnectionFactory(final ConnectionFactory connectionFactory) {
    final var pooledConnectionFactory = new PooledConnectionFactory();
    pooledConnectionFactory.setMaxConnections(getJmsConnections());
    pooledConnectionFactory.setConnectionFactory(connectionFactory);
    return pooledConnectionFactory;
  }

  /**
   * @param connectionFactory the pooled connection factory.
   * @return the JMS configuration
   */
  @Bean
  public JmsConfiguration jmsConfiguration(final PooledConnectionFactory connectionFactory) {
    final var configuration = new JmsConfiguration();
    configuration.setConnectionFactory(connectionFactory);
    return configuration;
  }

  /**
   * @param jmsConfiguration the JMS configuration
   * @return the ActiveMQ endpoint.
   */
  @Bean(JMS_ENDPOINT_NAME)
  public ActiveMQComponent activeMQComponent(final JmsConfiguration jmsConfiguration) {
    final var component = new ActiveMQComponent();
    component.setConfiguration(jmsConfiguration);
    return component;
  }

}
