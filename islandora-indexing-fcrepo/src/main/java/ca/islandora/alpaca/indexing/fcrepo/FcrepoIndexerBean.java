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

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.Exchange;
import org.apache.camel.PropertyInject;
import org.slf4j.Logger;

/**
 * Processing functions for FcrepoIndexer routes.
 *
 * @author Danny Lamb
 */
public class FcrepoIndexerBean {

    private static final Logger LOGGER = getLogger(FcrepoIndexerBean.class);

    /**
     * @return  Fedora base url
     */
    public String getFcrepoBaseUrl() {
        return fcrepoBaseUrl;
    }

    /**
     * @param   fcrepoBaseUrl   Fedora base url
     */
    public void setFcrepoBaseUrl(final String fcrepoBaseUrl) {
        this.fcrepoBaseUrl = fcrepoBaseUrl;
    }

    /**
     * @return  Drupal base url
     */
    public String getDrupalBaseUrl() {
        return drupalBaseUrl;
    }

    /**
     * @param   drupalBaseUrl   Drupal base url
     */
    public void setDrupalBaseUrl(final String drupalBaseUrl) {
        this.drupalBaseUrl = drupalBaseUrl;
    }

    /**
     * @return  Milliner base url
     */
    public String getMillinerBaseUrl() {
        return millinerBaseUrl;
    }

    /**
     * @param   millinerBaseUrl Milliner base url
     */
    public void setMillinerBaseUrl(final String millinerBaseUrl) {
        this.millinerBaseUrl = millinerBaseUrl;
    }

    /**
     * @return  Gemini base url
     */
    public String getGeminiBaseUrl() {
        return geminiBaseUrl;
    }

    /**
     * @param   geminiBaseUrl   Gemini base url
     */
    public void setGeminiBaseUrl(final String geminiBaseUrl) {
        this.geminiBaseUrl = geminiBaseUrl;
    }

    @PropertyInject("fcrepo.baseUrl")
    private String fcrepoBaseUrl;

    @PropertyInject("drupal.baseUrl")
    private String drupalBaseUrl;

    @PropertyInject("milliner.baseUrl")
    private String millinerBaseUrl;

    @PropertyInject("gemini.baseUrl")
    private String geminiBaseUrl;

    /**
     * Prepares message for Milliner POST.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForMillinerCreate(final Exchange exchange) throws Exception {
        preprocessForMilliner(exchange);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");

        LOGGER.info("Creating Fedora resource for " + exchange.getProperty("DrupalPath"));
    }

    /**
     * Prepares message for Milliner PUT.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForMillinerUpdate(final Exchange exchange) throws Exception {
        preprocessForMilliner(exchange);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "PUT");

        LOGGER.info("Updating Fedora resource for " + exchange.getProperty("DrupalPath"));
    }

    /**
     * Prepares message for Milliner DELETE.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForMillinerDelete(final Exchange exchange) throws Exception {
        preprocessForMilliner(exchange);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "DELETE");

        LOGGER.info("Deleting Fedora resource for " + exchange.getProperty("DrupalPath"));
    }

    private void preprocessForMilliner(final Exchange exchange) throws Exception {

        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grab the event message
        final AS2Event event = exchange.getIn().getBody(AS2Event.class);

        // Grab the uri and break off the path
        final String uri = event.getObject();
        final String path = getPath(uri, drupalBaseUrl);

        // Cache the drupal path and its corresponding uri in Milliner.
        exchange.setProperty("DrupalPath", path);
        exchange.setProperty("MillinerUri", addTrailingSlash(millinerBaseUrl) + path);

        // Prepare the message for Milliner.
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setBody(null);
    }

    /**
     * Resets message to how it was at the beginning of the route.
     *
     * @param exchange
     * @throws Exception
     */
    public void resetToOriginalMessage(final Exchange exchange) throws Exception {
        exchange.setOut(exchange.getUnitOfWork().getOriginalInMessage());
    }

    /**
     * Prepares message to get sent to queue for Gemini mapping.
     *
     * @param exchange
     * @throws Exception
     */
    public void postprocessForMillinerCreate(final Exchange exchange) throws Exception {
        resetToOriginalMessage(exchange);
        exchange.getOut().setHeader("DrupalPath", exchange.getProperty("DrupalPath"));
        exchange.getOut().setHeader("FcrepoUri", exchange.getIn().getBody(String.class));
    }

    /**
     * Prepares message to get sent to queue for Gemini unmapping.
     *
     * @param exchange
     * @throws Exception
     */
    public void postprocessForMillinerDelete(final Exchange exchange) throws Exception {
        resetToOriginalMessage(exchange);
        exchange.getOut().setHeader("DrupalPath", exchange.getProperty("DrupalPath"));
    }

    /**
     * Prepares message for Gemini POST.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForGeminiCreate(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grap the fcrepo and drupal paths
        final String fcrepoPath = getPath(exchange.getIn().getHeader("FcrepoUri", String.class), fcrepoBaseUrl);
        final String drupalPath = exchange.getIn().getHeader("DrupalPath", String.class);

        LOGGER.info("Mapping " + drupalPath + " to " + fcrepoPath);

        // Prepare the message
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setHeader("Content-Type", "application/json");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setBody("{\"drupal\": \"" + drupalPath + "\", \"fedora\": \"" + fcrepoPath + "\"}");

        // Set the uri to call dynamically
        exchange.setProperty("GeminiUri", addTrailingSlash(geminiBaseUrl));
    }

    /**
     * Prepares message for Gemini DELETE.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForGeminiDelete(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Set the uri to call dynamically
        exchange.setProperty(
                "GeminiUri",
                addTrailingSlash(geminiBaseUrl) + "drupal/" + exchange.getIn().getHeader("DrupalPath", String.class)
        );

        LOGGER.info("Unmapping " + exchange.getIn().getHeader("DrupalPath", String.class));

        // Prepare the message
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "DELETE");
    }

    private String addTrailingSlash(final String str) {
        final String trimmed = str.trim();
        if (!trimmed.endsWith("/")) {
            return trimmed + "/";
        }
        return trimmed;
    }

    private String getPath(final String uri, final String baseUrl) {
        final String sanitized = addTrailingSlash(baseUrl);
        return uri.substring(uri.lastIndexOf(sanitized) + sanitized.length());
    }

    public void preprocessForBinaryGet(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grab the event message
        final AS2Event event = exchange.getIn().getBody(AS2Event.class);

        // Grab the uri and break off the path
        final String uri = event.getObject();
        final String path = getPath(uri, drupalBaseUrl);

        // Cache the drupal path and its corresponding uri in Milliner.
        exchange.setProperty("DrupalUri", uri);
        exchange.setProperty("DrupalPath", path);
        exchange.setProperty("MillinerUri", addTrailingSlash(millinerBaseUrl) + path);

        LOGGER.info(uri);

        // Prepare the message for Drupal
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
    }
}
