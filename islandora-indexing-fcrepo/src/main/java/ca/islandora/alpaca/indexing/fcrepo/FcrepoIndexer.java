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

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

import ca.islandora.alpaca.support.event.AS2Event;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
// import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Danny Lamb
 */
// @JsonIgnoreProperties(ignoreUnknown = true)
public class FcrepoIndexer extends RouteBuilder {

    /**
     * Header to use to pass the Fedora Base URI on.
     */
    public final static String FEDORA_HEADER = "X-Islandora-Fedora-Endpoint";

    /**
     * Maximum attempts to deliver a message.
     */
    @PropertyInject("error.maxRedeliveries")
    private int maxRedeliveries;

    /**
     * Base URI of the milliner web service.
     */
    @PropertyInject("milliner.baseUrl")
    private String millinerBaseUrl;

    /**
     * Base URI of the Gemini web service.
     */
    @PropertyInject("gemini.baseUrl")
    private String geminiBaseUrl;

    /**
     * The Logger.
     */
    private static final Logger LOGGER = getLogger(FcrepoIndexer.class);

    /**
     * @return  Number of times to retry
     */
    public int getMaxRedeliveries() {
        return maxRedeliveries;
    }

    /**
     * @param   maxRedeliveries Number of times to retry
     */
    public void setMaxRedeliveries(final int maxRedeliveries) {
        this.maxRedeliveries = maxRedeliveries;
    }

    /**
     * @return  Milliner base url
     */
    public String getMillinerBaseUrl() {
        return enforceTrailingSlash(millinerBaseUrl);
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
        return enforceTrailingSlash(geminiBaseUrl);
    }

    /**
     * @param   geminiBaseUrl Gemini base url
     */
    public void setGeminiBaseUrl(final String geminiBaseUrl) {
        this.geminiBaseUrl = geminiBaseUrl;
    }

    private String enforceTrailingSlash(final String baseUrl) {
        final String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }


    @Override
    public void configure() {

        final Predicate is412 = PredicateBuilder.toPredicate(simple("${exception.statusCode} == 412"));
        final Predicate is404 = PredicateBuilder.toPredicate(simple("${exception.statusCode} == 404"));

        onException(HttpOperationFailedException.class)
                .onWhen(is412)
                .useOriginalMessage()
                .handled(true)
                .log(
                        INFO,
                        LOGGER,
                        "Received 412 from Milliner, skipping indexing."
                );

        onException(Exception.class)
                .maximumRedeliveries(maxRedeliveries)
                .log(
                        ERROR,
                        LOGGER,
                        "Error indexing resource in fcrepo: ${exception.message}\n\n${exception.stacktrace}"
                );
        from("{{node.stream}}")
                .routeId("FcrepoIndexerNode")

                // Parse the event into a POJO.
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)

                // Extract relevant data from the event.
                .setProperty("event").simple("${body}")
                .setProperty("uuid").simple("${exchangeProperty.event.object.id.replaceAll(\"urn:uuid:\",\"\")}")
                .setProperty("jsonldUrl").simple("${exchangeProperty.event.object.url[2].href}")
                .setProperty("fedoraBaseUrl").simple("${exchangeProperty.event.target}")
                .log(DEBUG, LOGGER, "Received Node event for UUID (${exchangeProperty.uuid}), jsonld URL (" +
                        "${exchangeProperty.jsonldUrl}), fedora base URL (${exchangeProperty.fedoraBaseUrl})")

                // Prepare the message.
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Location", simple("${exchangeProperty.jsonldUrl}"))
                .setHeader(FEDORA_HEADER, exchangeProperty("fedoraBaseUrl"))
                .setBody(simple("${null}"))
                .multicast().parallelProcessing()
                //pass it to milliner
                .toD(getMillinerBaseUrl() + "node/${exchangeProperty.uuid}?connectionClose=true")
                .choice()
                        .when()
                        .simple("${exchangeProperty.event.object.isNewVersion}")
                                //pass it to milliner
                                .toD(
                                        getMillinerBaseUrl() + "version/${exchangeProperty.uuid}?connectionClose=true"
                                    ).endChoice();



        from("{{node.delete.stream}}")
                .routeId("FcrepoIndexerDeleteNode")
                .onException(HttpOperationFailedException.class)
                        .onWhen(is404)
                        .useOriginalMessage()
                        .handled(true)
                        .log(
                                INFO,
                                LOGGER,
                                "Received 404 from Milliner, skipping de-indexing."
                        )
                        .end()
                // Parse the event into a POJO.
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)

                // Extract relevant data from the event.
                .setProperty("event").simple("${body}")
                .setProperty("uuid").simple("${exchangeProperty.event.object.id.replaceAll(\"urn:uuid:\",\"\")}")
                .setProperty("fedoraBaseUrl").simple("${exchangeProperty.event.target}")
                .log(DEBUG, LOGGER, "Received Node delete event for UUID (${exchangeProperty.uuid}), fedora base URL" +
                        " (${exchangeProperty.fedoraBaseUrl})")

                // Prepare the message.
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("DELETE"))
                .setHeader(FEDORA_HEADER, exchangeProperty("fedoraBaseUrl"))
                .setBody(simple("${null}"))

                // Remove the file from Gemini.
                .toD(getMillinerBaseUrl() + "node/${exchangeProperty.uuid}?connectionClose=true");

        from("{{media.stream}}")
                .routeId("FcrepoIndexerMedia")

                // Parse the event into a POJO.
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)

                // Extract relevant data from the event.
                .setProperty("event").simple("${body}")
                .setProperty("sourceField").simple("${exchangeProperty.event.attachment.content.sourceField}")
                .setProperty("jsonUrl").simple("${exchangeProperty.event.object.url[1].href}")
                .setProperty("fedoraBaseUrl").simple("${exchangeProperty.event.target}")
                .log(DEBUG, LOGGER, "Received Media event for sourceField (${exchangeProperty.sourceField}), jsonld" +
                        " URL (${exchangeProperty.jsonUrl}), fedora Base URL (${exchangeProperty.fedoraBaseUrl})")

                // Prepare the message.
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Location", simple("${exchangeProperty.jsonUrl}"))
                .setHeader(FEDORA_HEADER, exchangeProperty("fedoraBaseUrl"))
                .setBody(simple("${null}"))

                // Pass it to milliner.
                .toD(getMillinerBaseUrl() + "media/${exchangeProperty.sourceField}?connectionClose=true");

        from("{{file.stream}}")
                .routeId("FcrepoIndexerFile")

                // Parse the event into a POJO.
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)

                // Extract relevant data from the event.
                .setProperty("event").simple("${body}")
                .setProperty("uuid").simple("${exchangeProperty.event.object.id.replaceAll(\"urn:uuid:\",\"\")}")
                .setProperty("drupal").simple("${exchangeProperty.event.object.url[0].href}")
                .setProperty("fedora").simple("${exchangeProperty.event.attachment.content.fedoraUri}")
                .setProperty("fedoraBaseUrl").simple("${exchangeProperty.event.target}")
                .log(DEBUG, LOGGER, "Received File event for UUID (${exchangeProperty.uuid}), drupal URL (" +
                        "${exchangeProperty.drupal}), fedoraURL (${exchangeProperty.fedora}), fedora base URL " +
                        "(${exchangeProperty.fedoraBaseUrl})")

                // Prepare the message.
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader(FEDORA_HEADER, exchangeProperty("fedoraBaseUrl"))
                .setBody(simple(
                    "{\"drupal\": \"${exchangeProperty.drupal}\", \"fedora\": \"${exchangeProperty.fedora}\"}")
                )

                // Index the file in Gemini.
                .toD(getGeminiBaseUrl() + "${exchangeProperty.uuid}?connectionClose=true");

        from("{{file.external.stream}}")
                .routeId("FcrepoIndexerExternalFile")

                // Parse the event into a POJO.
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)

                // Extract relevant data from the event.
                .setProperty("event").simple("${body}")
                .setProperty("uuid").simple("${exchangeProperty.event.object.id.replaceAll(\"urn:uuid:\",\"\")}")
                .setProperty("drupal").simple("${exchangeProperty.event.object.url[0].href}")
                .setProperty("fedoraBaseUrl").simple("${exchangeProperty.event.target}")
                .log(DEBUG, LOGGER, "Received File external event for UUID (${exchangeProperty.uuid}), drupal URL " +
                        "(${exchangeProperty.drupal}), fedora base URL (${exchangeProperty.fedoraBaseUrl})")

                // Prepare the message.
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Location", simple("${exchangeProperty.drupal}"))
                .setHeader(FEDORA_HEADER, exchangeProperty("fedoraBaseUrl"))
                .setBody(simple("${null}"))

                // Pass it to milliner.
                .toD(getMillinerBaseUrl() + "external/${exchangeProperty.uuid}?connectionClose=true");

    }
}
