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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * This condition enables a bean/configuration when the specified property matches the expected value
 *
 * Implementations must provide a no-arg constructor.
 *
 * @author pwinckles
 */
public abstract class ConditionOnProperty<T> implements ConfigurationCondition {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConditionOnProperty.class);

  private final String name;
  private final T expected;
  private final T defaultValue;
  private final Class<T> clazz;

  /**
   * Basic constructor
   * @param name the property name
   * @param expected the expected value for the condition
   * @param defaultValue the default value
   * @param clazz the class to control.
   */
  public ConditionOnProperty(final String name, final T expected, final T defaultValue, final Class<T> clazz) {
    this.name = name;
    this.expected = expected;
    this.defaultValue = defaultValue;
    this.clazz = clazz;
  }

  @Override
  public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
    LOGGER.debug("Prop {}: {}", name, context.getEnvironment().getProperty(name));
    return Objects.equals(expected, context.getEnvironment().getProperty(name, clazz, defaultValue));
  }

  @Override
  public ConfigurationPhase getConfigurationPhase() {
    // This forces spring to not evaluate these conditions until after it has loaded other @Configuration classes,
    // ensuring that the properties have been loaded.
    return ConfigurationPhase.REGISTER_BEAN;
  }
}
