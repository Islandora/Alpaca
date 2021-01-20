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
}
