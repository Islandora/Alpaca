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
import static org.slf4j.LoggerFactory.getLogger;

import ca.islandora.alpaca.indexing.fcrepo.event.AS2Event;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;

/**
 * @author Danny Lamb
 */
public class FcrepoIndexer extends RouteBuilder {

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

    @PropertyInject("error.maxRedeliveries")
    private int maxRedeliveries;

    @PropertyInject("milliner.baseUrl")
    private String millinerBaseUrl;

    @PropertyInject("gemini.baseUrl")
    private String geminiBaseUrl;

    private static final Logger LOGGER = getLogger(FcrepoIndexer.class);

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
                .setProperty("drupal").simple("${exchangeProperty.event.object.url[0].href}")

                // Make a HEAD request against Drupal.
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("HEAD"))
            	.transform(simple("${null}"))
                .toD("${exchangeProperty.drupal}")

                // Extract the link header for the JSONLD representation.
                .process((Exchange ex) -> {
                    final String headers = ex.getIn().getHeader("Link", String.class);
                    final String[] links = headers.split(",");
                    for (String header : links) {
                        if (header.contains("rel=\"alternate\"; type=\"application/ld+json\"")) {
                            final String[] parts = header.split(";");
                            String url = parts[0];
                            url = url.replaceAll("<", "");
                            ex.setProperty("jsonldUrl", url.replaceAll(">", ""));
                            return;
                        }
                    }
                })

                // Prepare the message. 
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Location", simple("${exchangeProperty.jsonldUrl}"))

                // Pass it to milliner.
                .toD(getMillinerBaseUrl() + "node/${exchangeProperty.uuid}");

        from("{{node.delete.stream}}")
                .routeId("FcrepoIndexerDelete")
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

                // Prepare the message. 
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("DELETE"))
            	.transform(simple("${null}"))

                // Remove the file from Gemini.
                .toD(getMillinerBaseUrl() + "resource/${exchangeProperty.uuid}");

        from("{{media.stream}}")
                .routeId("FcrepoIndexerMedia")

                // Parse the event into a POJO.
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)

                // Extract relevant data from the event.
                .setProperty("event").simple("${body}")
                .setProperty("sourceField").simple("${exchangeProperty.event.attachment.content.sourceField}")
                .setProperty("drupal").simple("${exchangeProperty.event.object.url[0].href}")

                // Make a HEAD request against Drupal.
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("HEAD"))
            	.transform(simple("${null}"))
                .toD("${exchangeProperty.drupal}")

                // Extract the link header for the JSON representation.
                .process((Exchange ex) -> {
                    final String headers = ex.getIn().getHeader("Link", String.class);
                    final String[] links = headers.split(",");
                    for (String header : links) {
                        if (header.contains("rel=\"alternate\"; type=\"application/json\"")) {
                            final String[] parts = header.split(";");
                            String url = parts[0];
                            url = url.replaceAll("<", "");
                            ex.setProperty("jsonUrl", url.replaceAll(">", ""));
                            return;
                        }
                    }
                })

                // Prepare the message. 
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Location", simple("${exchangeProperty.jsonUrl}"))

                // Pass it to milliner.
                .toD(getMillinerBaseUrl() + "media/${exchangeProperty.sourceField}");

        from("{{file.stream}}")
                .routeId("FcrepoIndexerFile")

                // Parse the event into a POJO.
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)

                // Extract relevant data from the event.
                .setProperty("event").simple("${body}")
                .setProperty("uuid").simple("${exchangeProperty.event.object.id.replaceAll(\"urn:uuid:\",\"\")}")
                .setProperty("drupal").simple("${exchangeProperty.event.object.url[0].href}")
                .setProperty("fedora").simple("${exchangeProperty.event.attachment.content.fedoraUri}")

                // Prepare the message. 
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .transform(simple("{\"drupal\": \"${exchangeProperty.drupal}\", \"fedora\": \"${exchangeProperty.fedora}\"}"))

                // Index the file in Gemini.
                .toD(getGeminiBaseUrl() + "${exchangeProperty.uuid}");

        from("{{file.delete.stream}}")
                .routeId("FcrepoIndexerFileDelete")

                // Parse the event into a POJO.
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)

                // Extract relevant data from the event.
                .setProperty("event").simple("${body}")
                .setProperty("uuid").simple("${exchangeProperty.event.object.id.replaceAll(\"urn:uuid:\",\"\")}")

                // Prepare the message. 
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("DELETE"))
            	.transform(simple("${null}"))

                // Remove the file from Gemini.
                .toD(getGeminiBaseUrl() + "${exchangeProperty.uuid}");

    }
}
