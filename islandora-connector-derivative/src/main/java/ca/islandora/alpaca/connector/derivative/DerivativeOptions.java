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
package ca.islandora.alpaca.connector.derivative;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import ca.islandora.alpaca.support.config.PropertyConfig;

/**
 * Base derivative configuration class.
 * @author whikloj
 */
@Configuration
public class DerivativeOptions extends PropertyConfig implements ApplicationContextAware {

  private static final Logger LOGGER = getLogger(DerivativeOptions.class);

  private static final String DERIVATIVE_LIST_PROPERTY = "derivative.systems.installed";
  private static final String DERIVATIVE_PREFIX = "derivative";
  private static final String DERIVATIVE_ENABLED_PROPERTY = "enabled";
  private static final String DERIVATIVE_INPUT_PROPERTY = "in.stream";
  private static final String DERIVATIVE_OUTPUT_PROPERTY = "service.url";

  @Autowired
  private Environment environment;

  @Value("${" + DERIVATIVE_LIST_PROPERTY + ":#{null}}")
  private String derivativeSystems;

  /**
   * Register additional beans for derivative routes.
   *
   * @param camelContext
   *   The camel context to add derivative service routes to.
   */
  private void processAllServices(final CamelContext camelContext) {
    if (derivativeSystems != null && !derivativeSystems.isBlank()) {
      final var systemNames = derivativeSystems.contains(",") ?
              Arrays.stream(derivativeSystems.split(",")).filter(o -> !o.isBlank()).map(String::trim)
                      .collect(Collectors.toSet()) : Set.of(derivativeSystems);
      for (final var system : systemNames) {
        final var enabled =
                environment.getProperty(enabledProperty(system), Boolean.class, false);
        if (enabled) {
          startDerivativeService(camelContext, system);
        } else {
          LOGGER.debug("Derivative connector (" + system + ") is disabled, skipping");
        }
      }
    }
  }

  /**
   * Attempt to start the routes and add them to the camel context.
   *
   * @param camelContext
   *   The current camel context.
   * @param serviceName
   *   The derivative service name.
   */
  private void startDerivativeService(final CamelContext camelContext, final String serviceName) {
      final var input = environment.getProperty(inputProperty(serviceName), "");
      final var output = environment.getProperty(outputProperty(serviceName), "");
      if (!input.isBlank() && !output.isBlank()) {
        try {
          camelContext.addRoutes(new DerivativeConnector(serviceName, addBrokerName(input), output,
                  this));
        } catch (final Exception e) {
          e.printStackTrace();
        }
      } else {
        final StringBuilder message = new StringBuilder();
        if (input.isBlank()) {
          message.append(inputProperty(serviceName)).append(" is blank");
        }
        if (output.isBlank()) {
          if (message.length() > 0) {
            message.append(" and ");
          }
          message.append(outputProperty(serviceName)).append(" is blank");
        }
        message.append(", skipping");
        LOGGER.debug(message.toString());
      }

  }

  /**
   * Just adds the JMS broker name to the provided queue/topic.
   * @param queueName the provided queue/topic.
   * @return the full endpoint including the broker name.
   */
  private String addBrokerName(final String queueName) {
    return JMS_ENDPOINT_NAME + ":" + queueName;
  }

  /**
   * Return the expected enabled property
   * @param systemName the derivative system name
   * @return the property
   */
  private String enabledProperty(final String systemName) {
    return DERIVATIVE_PREFIX + "." + systemName + "." + DERIVATIVE_ENABLED_PROPERTY;
  }

  /**
   * Return the expected input topic/queue property
   * @param systemName the derivative system name
   * @return the property
   */
  private String inputProperty(final String systemName) {
    return DERIVATIVE_PREFIX + "." + systemName + "." + DERIVATIVE_INPUT_PROPERTY;
  }

  /**
   * Return the expected output service url property
   * @param systemName the derivative system name
   * @return the property
   */
  private String outputProperty(final String systemName) {
    return DERIVATIVE_PREFIX + "." + systemName + "." + DERIVATIVE_OUTPUT_PROPERTY;
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    final String[] names = applicationContext.getBeanNamesForType(CamelContext.class);
    if (names.length == 1) {
      final CamelContext camelContext = applicationContext.getBean(names[0], CamelContext.class);
      processAllServices(camelContext);
    } else {
      LOGGER.error("No CamelContext found, unable to start derivative routes.");
    }
  }
}
