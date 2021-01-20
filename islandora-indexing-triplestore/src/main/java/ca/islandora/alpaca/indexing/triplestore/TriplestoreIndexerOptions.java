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
package ca.islandora.alpaca.indexing.triplestore;

import org.apache.camel.component.http.HttpComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.islandora.alpaca.support.config.ConditionOnPropertyTrue;
import ca.islandora.alpaca.support.config.PropertyConfig;

/**
 * Triplestore indexer configuration class.
 * @author whikloj
 */
@Configuration
@Conditional(TriplestoreIndexerOptions.TriplestoreIndexerEnabled.class)
public class TriplestoreIndexerOptions extends PropertyConfig {

  /**
   * Name of property to enable the triplestore indexer service.
   */
  private static final String TRIPLESTORE_INDEXER_ENABLED = "triplestore.indexer.enabled";

  private static final String BASE_URL_PROPERTY = "triplestore.baseUrl";
  private static final String TRIPLESTORE_INDEX_QUEUE = "triplestore.index.stream";
  private static final String TRIPLESTORE_DELETE_QUEUE = "triplestore.delete.stream";

  @Value("${" + TRIPLESTORE_INDEX_QUEUE + ":}")
  private String jmsIndexStream;

  @Value("${" + TRIPLESTORE_DELETE_QUEUE + ":}")
  private String jmsDeleteStream;

  @Value("${" + BASE_URL_PROPERTY + "}")
  private String triplestoreBaseUrl;

  /**
   * Defines that triplestore indexer is only enabled if the appropriate property is set to "true".
   */
  static class TriplestoreIndexerEnabled extends ConditionOnPropertyTrue {
    TriplestoreIndexerEnabled() {
      super(TriplestoreIndexerOptions.TRIPLESTORE_INDEXER_ENABLED, false);
    }
  }

  /**
   * @return the jms index stream endpoint.
   */
  public String getJmsIndexStream() {
    // Prepend the current broker name
    return JMS_ENDPOINT_NAME + ":" + jmsIndexStream;
  }

  /**
   * @return the jms delete stream endpoint.
   */
  public String getJmsDeleteStream() {
    // Prepend the current broker name
    return JMS_ENDPOINT_NAME + ":" + jmsDeleteStream;
  }

  /**
   * @return the triplestore base url.
   */
  public String getTriplestoreBaseUrl() {
    // Append "connectionClose=true" to force closing connections immediately
    return triplestoreBaseUrl + "?connectionClose=true";
  }

  /**
   * @return bean for the http endpoint.
   */
  @Bean(name = "http")
  public HttpComponent http() {
    return new HttpComponent();
  }

  /**
   * @return bean for the https endpoint.
   */
  @Bean(name = "https")
  public HttpComponent https() {
    return new HttpComponent();
  }

  /**
   * @return Triplestore indexer bean.
   */
  @Bean
  public TriplestoreIndexer triplestoreRoute() {
    return new TriplestoreIndexer();
  }
}
