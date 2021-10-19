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
package ca.islandora.alpaca.indexing.fcrepo;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ca.islandora.alpaca.support.config.ConditionOnPropertyTrue;
import ca.islandora.alpaca.support.config.PropertyConfig;

/**
 * Property configuration class.
 * @author whikloj
 */
@Configuration
@Conditional(FcrepoIndexerOptions.FcrepoIndexerEnabled.class)
public class FcrepoIndexerOptions extends PropertyConfig {

  private static final String FCREPO_INDEXER_ENABLED = "fcrepo.indexer.enabled";
  private static final String FCREPO_INDEXER_NODE_INDEX = "fcrepo.indexer.node";
  private static final String FCREPO_INDEXER_NODE_DELETE = "fcrepo.indexer.delete";
  private static final String FCREPO_INDEXER_MEDIA_INDEX = "fcrepo.indexer.media";
  private static final String FCREPO_INDEXER_EXTERNAL_INDEX = "fcrepo.indexer.external";
  private static final String FCREPO_INDEXER_MILLINER = "fcrepo.indexer.milliner.baseUrl";
  private static final String FCREPO_BASE_URI_HEADER_PROPERTY = "fcrepo.indexer.fedoraHeader";

  @Value("${" + FCREPO_INDEXER_NODE_INDEX + ":}")
  private String fcrepoNodeIndex;

  @Value("${" + FCREPO_INDEXER_NODE_DELETE + ":}")
  private String fcrepoNodeDelete;

  @Value("${" + FCREPO_INDEXER_MEDIA_INDEX + ":}")
  private String fcrepoMediaIndex;

  @Value("${" + FCREPO_INDEXER_EXTERNAL_INDEX + ":}")
  private String fcrepoExternalIndex;

  @Value("${" + FCREPO_INDEXER_MILLINER + ":}")
  private String fcrepoMillinerBaseUrl;

  @Value("${" + FCREPO_BASE_URI_HEADER_PROPERTY + ":X-Islandora-Fedora-Endpoint}")
  private String fcrepoFedoraUriHeader;

  /**
   * Defines that Fedora indexer is only enabled if the appropriate property is set to "true".
   */
  static class FcrepoIndexerEnabled extends ConditionOnPropertyTrue {
    FcrepoIndexerEnabled() {
      super(FcrepoIndexerOptions.FCREPO_INDEXER_ENABLED, false);
    }
  }

  /**
   * @return the node index endpoint.
   */
  public String getNodeIndex() {
    return JMS_ENDPOINT_NAME + ":" + fcrepoNodeIndex;
  }

  /**
   * @return the node delete endpoint.
   */
  public String getNodeDelete() {
    return JMS_ENDPOINT_NAME + ":" + fcrepoNodeDelete;
  }

  /**
   * @return the media index endpoint.
   */
  public String getMediaIndex() {
    return JMS_ENDPOINT_NAME + ":" + fcrepoMediaIndex;
  }

  /**
   * @return the external content index endpoint.
   */
  public String getExternalIndex() {
    return JMS_ENDPOINT_NAME + ":" + fcrepoExternalIndex;
  }

  /**
   * @return the milliner base url.
   */
  public String getMillinerBaseUrl() {
    return fcrepoMillinerBaseUrl;
  }

  /**
   * @return the header containing the fedora base uri.
   */
  public String getFedoraUriHeader() {
    return fcrepoFedoraUriHeader;
  }

  /**
   * @return bean for the fcrepo indexer camel route.
   */
  @Bean
  public RouteBuilder fcrepoIndexer() {
    return new FcrepoIndexer();
  }
}
