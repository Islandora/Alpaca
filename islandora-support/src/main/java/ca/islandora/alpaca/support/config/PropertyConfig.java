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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * Abstract class of common properties
 *
 * @author whikloj
 */
@PropertySources({
  @PropertySource(value = PropertyConfig.ALPACA_DEFAULT_CONFIG_FILE, ignoreResourceNotFound = true),
  @PropertySource(value = PropertyConfig.ALPACA_CONFIG_FILE, ignoreResourceNotFound = true)
})
public abstract class PropertyConfig {

  public static final String ALPACA_CONFIG_PROPERTY = "alpaca.config";
  public static final String ALPACA_HOME_PROPERTY = "alpaca.home";
  public static final String ALPACA_DEFAULT_HOME = "alpaca-home-directory";
  public static final String ALPACA_DEFAULT_CONFIG_FILE = "file:${" +
    ALPACA_HOME_PROPERTY + ":" + ALPACA_DEFAULT_HOME + "}/config/alpaca.properties";
  public static final String ALPACA_CONFIG_FILE = "file:${" + ALPACA_CONFIG_PROPERTY + "}";
  // static endpoint name for activemq connection
  protected static final String JMS_ENDPOINT_NAME = "broker";
  protected static final String MAX_REDELIVERIES_PROPERTY = "error.maxRedeliveries";

  @Value("${" + MAX_REDELIVERIES_PROPERTY + ":5}")
  private int maxRedeliveries;

  /**
   * @return the error.maxRedeliveries amount.
   */
  public int getMaxRedeliveries() {
    return maxRedeliveries;
  }

  /**
   * Utility function to append various JMS options like concurrentConsumer variables.
   * @param queueString
   *   The original topic/queue string
   * @param concurrentConsumers
   *   The number of concurrent consumers. -1 means no setting.
   * @param maxConcurrentConsumers
   *   The max number of concurrent consumers. -1 means no setting.
   * @param asyncConsumers
   *   Indicate if the queue should be processed strictly queue-wise (false;
   *   more for dealing with overhead?); otherwise, allow multiple items to be
   *   processed at the same time.
   * @return
   *   The modified topic/queue string.
   */
  public static String addJmsOptions(final String queueString, final int concurrentConsumers,
                              final int maxConcurrentConsumers, final boolean asyncConsumers) {
    final StringBuilder builder = new StringBuilder();
    if (concurrentConsumers > 0) {
      builder.append("concurrentConsumers=");
      builder.append(concurrentConsumers);
    }
    if (maxConcurrentConsumers > 0) {
      if (builder.length() > 0) {
        builder.append('&');
      }
      builder.append("maxConcurrentConsumers=");
      builder.append(maxConcurrentConsumers);
    }
    if (asyncConsumers) {
      if (builder.length() > 0) {
        builder.append('&');
      }
      builder.append("asyncConsumer=")
        .append(asyncConsumers);
    }
    if (builder.length() > 0) {
      return queueString + (queueString.contains("?") ? '&' : '?') + builder;
    }
    return queueString;
  }

  /**
   * Utility to add common endpoint options to HTTP endpoints.
   * @param httpEndpoint
   *   The http endpoint string.
   * @param forceAmpersand
   *   If you want to use this function with a dynamic endpoint, this is whether to force an ampersand to start.
   * @return
   *   The modified http endpoint string.
   */
  public String addHttpOptions(final String httpEndpoint, final boolean forceAmpersand) {
    final String commonElements = "connectionClose=true&disableStreamCache=true";
    final int bestGuessAtFinalLength = httpEndpoint.length() + commonElements.length() + 1;
    final StringBuilder builder = new StringBuilder(bestGuessAtFinalLength);
    builder.append(httpEndpoint);
    // Only append ? or & if there is an endpoint.
    if (builder.length() > 0) {
      if (httpEndpoint.contains("?") || forceAmpersand) {
        builder.append('&');
      } else {
        builder.append('?');
      }
    }
    // Append the common elements.
    builder.append(commonElements);
    return builder.toString();
  }

  /**
   * Assumes not to forceAmpersand
   * @param httpEndpoint
   *   The http endpoint string.
   * @return
   *   The modified http endpoint string.
   */
  public String addHttpOptions(final String httpEndpoint) {
    return addHttpOptions(httpEndpoint, false);
  }
}
