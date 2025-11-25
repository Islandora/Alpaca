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

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;

import ca.islandora.alpaca.support.event.AS2Event;

/**
 * @author dhlamb
 */
public class DerivativeConnector extends RouteBuilder {

    /**
     * Local logger.
     */
    private static final Logger LOGGER = getLogger(DerivativeConnector.class);

    /**
     * Input source.
     */
    private final String inputStream;

    /**
     * Output target.
     */
    private final String outputStream;

    /**
     * The name of this connector instance
     */
    private final String connectorName;

    /**
     * The common derivative configuration.
     */
    private DerivativeOptions config;

    /**
     * Basic constructor
     *
     * @param name
     *   The derivative connector name.
     * @param inputSource
     *   The input stream name.
     * @param outputSource
     *   The output target name.
     * @param configuration
     *   The common configuration options.
     */
    public DerivativeConnector(final String name, final String inputSource, final String outputSource,
                               final DerivativeOptions configuration) {
        super();
        connectorName = name;
        inputStream = inputSource;
        outputStream = outputSource;
        config = configuration;
    }

    @Override
    public void configure() {
        LOGGER.info("DerivativeConnector (" + connectorName + ") routes starting");

        // Global exception handler for the indexer.
        // Just logs after retrying X number of times.
        onException(Exception.class)
            .maximumRedeliveries(config.getMaxRedeliveries())
            .log(
                ERROR,
                LOGGER,
                "(" + connectorName + ") Error connecting generating derivative with " + outputStream + ": " +
                "${exception.message}\n\n${exception.stacktrace}"
            );

        from(inputStream)
            .routeId("IslandoraConnectorDerivative-" + connectorName)

            .log(DEBUG, LOGGER, "Received message on IslandoraConnectorDerivative-" + connectorName)

            // Stash the original JSON body on the exchange.
            .setProperty("event").simple("${body}")

            // Parse the event into a POJO for property extraction.
            .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
            .setProperty("eventPojo").simple("${body}")

            // Make the Crayfish request.
            .removeHeaders("*", "Authorization")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .process(exchange -> {
            final String jsonEvent = exchange.getProperty("event", String.class);
                if (jsonEvent != null) {
                    final String b64 = java.util.Base64.getEncoder()
                    .encodeToString(jsonEvent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    exchange.getIn().setHeader("X-Islandora-Event", b64);
                }
            })
            .process(exchange -> {
            final AS2Event event = exchange.getProperty("eventPojo", AS2Event.class);
                if (event != null && event.getAttachment() != null
                    && event.getAttachment().getContent() != null) {

                    final var content = event.getAttachment().getContent();
                    if (content.getMimetype() != null) {
                        exchange.getIn().setHeader("Accept", content.getMimetype());
                    }
                    if (content.getArgs() != null) {
                        exchange.getIn().setHeader("X-Islandora-Args", content.getArgs());
                    }
                    if (content.getSourceUri() != null) {
                        exchange.getIn().setHeader("Apix-Ldp-Resource", content.getSourceUri());
                    }

                }
            })
            .setBody(simple("${null}"))
            .to(outputStream)

            // PUT the media.
            .removeHeaders("*", "Authorization", "Content-Type")
            .process(exchange -> {
                final AS2Event event = exchange.getProperty("eventPojo", AS2Event.class);
                final boolean shouldPut = event != null && event.getAttachment() != null
                    && event.getAttachment().getContent() != null;
                exchange.setProperty("shouldPutMedia", shouldPut);

                if (shouldPut) {
                    final var content = event.getAttachment().getContent();
                    if (content.getFileUploadUri() != null) {
                        exchange.getIn().setHeader("Content-Location", content.getFileUploadUri());
                    }
                    exchange.getIn().setHeader(Exchange.HTTP_METHOD, "PUT");
                }
            })
            .choice()
                .when(simple("${exchangeProperty.shouldPutMedia} == true"))
                    .toD(config.addHttpOptions(
                        "${exchangeProperty.eventPojo.attachment.content.destinationUri}"))
            .end();
    }

}
