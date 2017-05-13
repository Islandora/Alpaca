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

import java.net.URI;
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
    public void preprocessForMillinerCreateRdf(final Exchange exchange) throws Exception {
        preprocessForMillinerRdf(exchange);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");

        LOGGER.info("Creating Fedora resource for " + exchange.getProperty("DrupalPath"));
    }

    /**
     * Prepares message for Milliner PUT.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForMillinerUpdateRdf(final Exchange exchange) throws Exception {
        preprocessForMillinerRdf(exchange);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "PUT");

        LOGGER.info("Updating Fedora resource for " + exchange.getProperty("DrupalPath"));
    }

    private void preprocessForMillinerRdf(final Exchange exchange) throws Exception {

        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grab the event message
        final AS2Event event = exchange.getIn().getBody(AS2Event.class);

        // Grab the uri and break off the path
        final String uri = event.getObject();
        final String path = getPath(uri, drupalBaseUrl);

        // Cache the drupal path and its corresponding uri in Milliner.
        exchange.setProperty("DrupalRdfPath", path);
        exchange.setProperty("MillinerUri", addTrailingSlash(millinerBaseUrl) + "rdf/" + path);

        // Prepare the message for Milliner.
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setBody(null);
    }

    /**
     * Prepares message for Milliner DELETE.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForMillinerDeleteRdf(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grab the event message
        final AS2Event event = exchange.getIn().getBody(AS2Event.class);

        // Grab the uri and break off the path
        final String uri = event.getObject();
        final String path = getPath(uri, drupalBaseUrl);

        // Cache the drupal path and its corresponding uri in Milliner.
        exchange.setProperty("DrupalRdfPath", path);
        exchange.setProperty("MillinerUri", addTrailingSlash(millinerBaseUrl) + path);

        // Prepare the message for Milliner.
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setBody(null);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "DELETE");

        LOGGER.info("Deleting Fedora resource for " + path);
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
    public void postprocessForMillinerCreateRdf(final Exchange exchange) throws Exception {
        final String fcrepoPath = getPath(exchange.getIn().getHeader("Location", String.class), fcrepoBaseUrl);

        resetToOriginalMessage(exchange);
        exchange.getOut().setHeader("DrupalRdfPath", exchange.getProperty("DrupalRdfPath", String.class));
        exchange.getOut().setHeader("FcrepoRdfPath", fcrepoPath);
    }

    /**
     * Prepares message to get sent to queue for Gemini unmapping.
     *
     * @param exchange
     * @throws Exception
     */
    public void postprocessForMillinerDeleteRdf(final Exchange exchange) throws Exception {
        resetToOriginalMessage(exchange);
        exchange.getOut().setHeader("DrupalRdfPath", exchange.getProperty("DrupalRdfPath"));
    }

    /**
     * Prepares message for Gemini POST.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForGeminiCreateRdf(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grap the fcrepo and drupal paths
        final String fcrepoPath = exchange.getIn().getHeader("FcrepoRdfPath", String.class);
        final String drupalPath = exchange.getIn().getHeader("DrupalRdfPath", String.class);

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
    public void preprocessForGeminiDeleteRdf(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);
        final String path = exchange.getIn().getHeader("DrupalRdfPath", String.class);
        // Set the uri to call dynamically
        exchange.setProperty(
                "GeminiUri",
                addTrailingSlash(geminiBaseUrl) + "drupal/" + path
        );

        LOGGER.info("Unmapping " + path);

        // Prepare the message
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "DELETE");
        exchange.getIn().setBody(null);
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

    /**
     * Prepares message for Milliner POST on a binary.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForMillinerCreateBinary(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grab the event message
        final AS2Event event = exchange.getIn().getBody(AS2Event.class);

        // Grab the uri and break off the path
        final String rdfUri = event.getObject();
        final String rdfPath = getPath(rdfUri, drupalBaseUrl);
        final String fileUri = event.getAttachment().getUrl();
        final String filePath = getPath(fileUri, drupalBaseUrl);

        final String millinerUri = addTrailingSlash(millinerBaseUrl) + "binary/" + filePath;
        exchange.setProperty("DrupalRdfPath", rdfPath);
        exchange.setProperty("DrupalFilePath", filePath);
        exchange.setProperty("MillinerUri", millinerUri);

        LOGGER.info("Creating Fedora resource for " + filePath);

        // Prepare the message for Milliner
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setBody(null);
    }

    /**
     * Prepares message for Milliner POST on a binary.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForMillinerCreateBinaryUpdate(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        final String filePath = exchange.getIn().getHeader("DrupalFilePath", String.class);

        final String millinerUri = addTrailingSlash(millinerBaseUrl) + "binary/" + filePath;
        exchange.setProperty("MillinerUri", millinerUri);

        LOGGER.info("Creating Fedora resource for " + filePath);

        // Prepare the message for Milliner
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setBody(null);
    }

    /**
     * Prepares message to get sent to queue for Gemini binary mapping.
     *
     * @param exchange
     * @throws Exception
     */
    public void postprocessForMillinerCreateBinary(final Exchange exchange) throws Exception {
        final String fileUri = exchange.getIn().getHeader("Location", String.class);

        final String linkHeader = exchange.getIn().getHeader("Link", String.class);

        final String describedBy = extractLinkHeader(linkHeader, "describedby");

        final String rdfPath = getPath(describedBy, fcrepoBaseUrl);
        final String filePath = getPath(fileUri, fcrepoBaseUrl);

        resetToOriginalMessage(exchange);
        exchange.getOut().setHeader("DrupalRdfPath", exchange.getProperty("DrupalRdfPath", String.class));
        exchange.getOut().setHeader("DrupalFilePath", exchange.getProperty("DrupalFilePath", String.class));
        exchange.getOut().setHeader("FcrepoRdfPath", rdfPath);
        exchange.getOut().setHeader("FcrepoFilePath", filePath);
    }

    /**
     * Prepares message to get sent to queue for Gemini binary mapping during a Binary update.
     *
     * @param exchange
     * @throws Exception
     */
    public void postprocessForMillinerCreateBinaryUpdate(final Exchange exchange) throws Exception {
        final String fileUri = exchange.getIn().getHeader("Location", String.class);

        final String linkHeader = exchange.getIn().getHeader("Link", String.class);

        final String describedBy = extractLinkHeader(linkHeader, "describedby");

        final String rdfPath = getPath(describedBy, fcrepoBaseUrl);
        final String filePath = getPath(fileUri, fcrepoBaseUrl);

        resetToOriginalMessage(exchange);
        exchange.getOut().setHeader("FcrepoRdfPath", rdfPath);
        exchange.getOut().setHeader("FcrepoFilePath", filePath);
    }

    private String extractLinkHeader(final String headers, final String rel) throws RuntimeException {
        final String[] links = headers.split(",");

        for (String header : links) {
            if (header.contains(rel)) {
                final String[] parts = header.split(";");
                String url = parts[0];
                url = url.replaceAll("<", "");
                return url.replaceAll(">", "");
            }
        }

        throw new RuntimeException("Cannot parse link out of header with type " + rel);
    }

    /**
     * Prepares message for Gemini POST for binary.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForGeminiCreateBinary(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grap the fcrepo and drupal paths
        final String fcrepoPath = exchange.getIn().getHeader("FcrepoFilePath", String.class);
        final String drupalPath = exchange.getIn().getHeader("DrupalFilePath", String.class);

        LOGGER.info("Mapping file " + drupalPath + " to " + fcrepoPath);

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
     * Prepares message for Gemini GET on Rdf to locate File path.
     * @param exchange
     * @throws Exception
     */
    public void preprocessForGeminiGetRdf(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grab the event message
        final AS2Event event = exchange.getIn().getBody(AS2Event.class);

        // Grab the uri and break off the path
        final String rdfUri = event.getObject();
        final String rdfPath = getPath(rdfUri, drupalBaseUrl);
        final String fileUri = event.getAttachment().getUrl();
        final String filePath = getPath(fileUri, drupalBaseUrl);

        final String geminiUri = addTrailingSlash(geminiBaseUrl) + "drupal/" + rdfPath;
        exchange.setProperty("Authorization", token);
        exchange.setProperty("DrupalRdfPath", rdfPath);
        exchange.setProperty("DrupalFilePath", filePath);
        exchange.setProperty("GeminiUri", geminiUri);

        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        exchange.getIn().setBody(null);
    }

    /**
     * Prepares message for Fcrepo HEAD to get Link headers.
     * @param exchange
     * @throws Exception
     */
    public void preprocessForFcrepoHead(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getProperty("Authorization", String.class);

        final String fcrepoPath = exchange.getIn().getBody(String.class);

        exchange.setProperty("FcrepoRdfPath", fcrepoPath);
        exchange.setProperty("FcrepoUri", addTrailingSlash(fcrepoBaseUrl) + fcrepoPath);

        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "HEAD");
        exchange.getIn().setBody(null);
    }

    /**
     * Extracts link header for file path in Fedora.
     * @param exchange
     * @throws Exception
     */
    public void postprocessForFcrepoHead(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getProperty("Authorization", String.class);

        final String linkHeaders = exchange.getIn().getHeader("Link", String.class);
        final String describes = extractLinkHeader(linkHeaders, "describes");
        final String fcrepoPath = getPath(describes, fcrepoBaseUrl);

        exchange.setProperty("FcrepoFilePath", fcrepoPath);
    }

    /**
     * Prepares message for Gemini GET on Fedora file path to find Drupal path.
     * @param exchange
     * @throws Exception
     */
    public void preprocessForGeminiGetFile(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getProperty("Authorization", String.class);

        // Get the path to the File
        final String linkHeaders = exchange.getIn().getHeader("Link", String.class);
        final String describes = extractLinkHeader(linkHeaders, "describes");
        final String filePath = getPath(describes, fcrepoBaseUrl);

        final String geminiUri = addTrailingSlash(geminiBaseUrl) + "fedora/" + filePath;
        exchange.setProperty("GeminiUri", geminiUri);
        exchange.setProperty("FcrepoFilePath", filePath);

        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        exchange.getIn().setBody(null);
    }

    /**
     * Prepares message for rest of Update workflow.
     * @param exchange
     * @throws Exception
     */
    public void postprocessForGeminiGetFile(final Exchange exchange) throws Exception {
        final String body = exchange.getIn().getBody(String.class);
        final String combined = addTrailingSlash(drupalBaseUrl) + body;
        final URI uri = new URI("http", combined.substring("http:".length()), null);
        final String uriString = uri.toASCIIString();
        final String path = getPath(uriString, drupalBaseUrl);

        resetToOriginalMessage(exchange);
        exchange.getOut().setHeader("OriginalDrupalFilePath", path);
        exchange.getOut().setHeader("DrupalRdfPath", exchange.getProperty("DrupalRdfPath", String.class));
        exchange.getOut().setHeader("DrupalFilePath", exchange.getProperty("DrupalFilePath", String.class));
        exchange.getOut().setHeader("FcrepoRdfPath", exchange.getProperty("FcrepoRdfPath", String.class));
        exchange.getOut().setHeader("FcreopFilePath", exchange.getProperty("FcrepoFilePath", String.class));
    }

    /**
     * Prepares message for Milliner DELETE on a binary.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForMillinerDeleteBinary(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        // Grab the event message
        final AS2Event event = exchange.getIn().getBody(AS2Event.class);

        // Grab the uri and break off the path
        final String rdfUri = event.getObject();
        final String rdfPath = getPath(rdfUri, drupalBaseUrl);
        final String fileUri = event.getAttachment().getUrl();
        final String filePath = getPath(fileUri, drupalBaseUrl);

        final String millinerUri = addTrailingSlash(millinerBaseUrl) + filePath;
        exchange.setProperty("DrupalRdfPath", rdfPath);
        exchange.setProperty("DrupalFilePath", filePath);
        exchange.setProperty("MillinerUri", millinerUri);

        LOGGER.info("Deleting Fedora resource for " + filePath);

        // Prepare the message for Milliner
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "DELETE");
        exchange.getIn().setBody(null);
    }

    /**
     * Prepares message for Milliner DELETE on a binary during the update process.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForMillinerDeleteBinaryUpdate(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);

        final String filePath = exchange.getIn().getHeader("OriginalDrupalFilePath", String.class);

        final String millinerUri = addTrailingSlash(millinerBaseUrl) + filePath;
        exchange.setProperty("MillinerUri", millinerUri);

        LOGGER.info("Deleting Fedora resource for " + filePath);

        // Prepare the message for Milliner
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "DELETE");
        exchange.getIn().setBody(null);

    }

    /**
     * Prepares message to get sent to queue for Gemini binary unmapping.
     *
     * @param exchange
     * @throws Exception
     */
    public void postprocessForMillinerDeleteBinary(final Exchange exchange) throws Exception {
        resetToOriginalMessage(exchange);
        exchange.getOut().setHeader("DrupalRdfPath", exchange.getProperty("DrupalRdfPath", String.class));
        exchange.getOut().setHeader("DrupalFilePath", exchange.getProperty("DrupalFilePath", String.class));
    }

    /**
     * Prepares message for Gemini DELETE on a Binary.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForGeminiDeleteBinary(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);
        final String path = exchange.getIn().getHeader("DrupalFilePath", String.class);

        // Set the uri to call dynamically
        exchange.setProperty(
                "GeminiUri",
                addTrailingSlash(geminiBaseUrl) + "drupal/" + path
        );

        LOGGER.info("Unmapping " + path);

        // Prepare the message
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "DELETE");
        exchange.getIn().setBody(null);
    }

    /**
     * Prepares message for Gemini DELETE on a binary during an update.
     *
     * @param exchange
     * @throws Exception
     */
    public void preprocessForGeminiDeleteBinaryUpdate(final Exchange exchange) throws Exception {
        // Grab JWT token
        final String token = exchange.getIn().getHeader("Authorization", String.class);
        final String path = exchange.getIn().getHeader("OriginalDrupalFilePath", String.class);

        // Set the uri to call dynamically
        exchange.setProperty(
                "GeminiUri",
                addTrailingSlash(geminiBaseUrl) + "drupal/" + path
        );

        LOGGER.info("Unmapping " + path);

        // Prepare the message
        exchange.getIn().removeHeaders("*");
        exchange.getIn().setHeader("Authorization", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "DELETE");
        exchange.getIn().setBody(null);
    }

}
