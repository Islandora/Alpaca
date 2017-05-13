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
import static org.apache.camel.LoggingLevel.WARN;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.Predicate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;

/**
 * @author Danny Lamb
 */
public class FcrepoIndexer extends RouteBuilder {

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

    @PropertyInject("error.maxRedeliveries")
    private int maxRedeliveries;

    @Override
    public void configure() {

        // Predicates
        final Predicate is404 = PredicateBuilder.toPredicate(simple("${exception.statusCode} == 404"));
        final Predicate is409 = PredicateBuilder.toPredicate(simple("${exception.statusCode} == 409"));
        final Predicate isSameFile = PredicateBuilder.toPredicate(
                simple("${headers.OriginalDrupalFilePath} == ${headers.DrupalFilePath}")
        );

        // Routes for creating a Fedora resource from a Drupal entity
        from("{{create.rdf.input.stream}}")
                .routeId("IslandoraFcrepoIndexerCreateRdf")
                .errorHandler(
                        deadLetterChannel("direct:create-rdf-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .onException(HttpOperationFailedException.class).onWhen(is409)
                        .useOriginalMessage()
                        .handled(true)
                                .log(WARN, LOGGER, "Received 409 from Milliner POST, skipping routing.")
                                .to("{{create.rdf.output.stream}}")
                .end()
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Create'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerCreateRdf")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerCreateRdf")
                .to("{{map.rdf.input.stream}}");

        from("direct:create-rdf-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{create.rdf.dead.stream}}");

        // Routes for mapping a Fedora resource to a Drupal entity
        from("{{map.rdf.input.stream}}")
                .routeId("IslandoraFcrepoIndexerMapRdf")
                .errorHandler(
                        deadLetterChannel("direct:map-rdf-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiCreateRdf")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{create.rdf.output.stream}}");

        from("direct:map-rdf-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{map.rdf.dead.stream}}");

        // Routes for updating a Fedora resource from a Drupal entity
        from("{{update.rdf.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdateRdf")
                .errorHandler(
                        deadLetterChannel("direct:update-rdf-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Update'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerUpdateRdf")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{update.rdf.output.stream}}");

        from("direct:update-rdf-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{update.rdf.dead.stream}}");

        // Routes for deleting a Fedora resource for a Drupal entity
        from("{{delete.rdf.input.stream}}")
                .routeId("IslandoraFcrepoIndexerDeleteRdf")
                .errorHandler(
                        deadLetterChannel("direct:delete-rdf-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .onException(HttpOperationFailedException.class).onWhen(is404)
                        .useOriginalMessage()
                        .handled(true)
                                .log(WARN, LOGGER, "Received 404 from Milliner DELETE, skipping routing.")
                                .to("{{delete.rdf.output.stream}}")
                .end()
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Delete'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerDeleteRdf")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerDeleteRdf")
                .to("{{unmap.rdf.input.stream}}");

        from("direct:delete-rdf-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{delete.rdf.dead.stream}}");

        // Routes for unmapping a Fedora resource from a Drupal entity
        from("{{unmap.rdf.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUnmapRdf")
                .errorHandler(
                        deadLetterChannel("direct:unmap-rdf-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiDeleteRdf")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{delete.rdf.output.stream}}");

        from("direct:unmap-rdf-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{unmap.rdf.dead.stream}}");

        // Routes for creating a Fedora resource from a Drupal file
        from("{{create.binary.input.stream}}")
                .routeId("IslandoraFcrepoIndexerCreateBinary")
                .errorHandler(
                        deadLetterChannel("direct:create-binary-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .onException(HttpOperationFailedException.class).onWhen(is409)
                        .useOriginalMessage()
                        .handled(true)
                                .log(WARN, LOGGER, "Received 409 from Milliner POST, skipping routing.")
                                .to("{{create.binary.output.stream}}")
                .end()
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Create'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerCreateBinary")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerCreateBinary")
                .to("{{map.binary.input.stream}}");

        from("direct:create-binary-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{create.binary.dead.stream}}");

        // Routes for mapping a Fedora resource to a Drupal file
        from("{{map.binary.input.stream}}")
                .routeId("IslandoraFcrepoIndexerMapBinary")
                .errorHandler(
                        deadLetterChannel("direct:map-binary-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiCreateBinary")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{map.binary.rdf.input.stream}}");

        from("direct:map-binary-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{map.binary.dead.stream}}");

        // Routes for mapping a Fedora resource that describes a Drupal file
        from("{{map.binary.rdf.input.stream}}")
                .routeId("IslandoraFcrepoIndexerMapBinaryRdf")
                .errorHandler(
                        deadLetterChannel("direct:map-binary-rdf-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiCreateRdf")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{create.binary.output.stream}}");

        from("direct:map-binary-rdf-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{map.binary.rdf.dead.stream}}");

        // Routes for updating a Fedora resource from a Drupal file
        from("{{update.binary.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdateBinary")
                .errorHandler(
                        deadLetterChannel("direct:update-binary-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Update'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiGetRdf")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=preprocessForFcrepoHead")
                .toD("${exchangeProperty.FcrepoUri}")
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiGetFile")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForGeminiGetFile")
                .choice().when(isSameFile)
                        .process(exchange -> {
                            LOGGER.info("Binary has not been updated.  Skipping routing.");
                        })
                        .to("{{update.binary.output.stream}}")
                .otherwise()
                        // DELETE BINARY - UNMAP BINARY AND RDF - CREATE BINARY - MAP BINARY AND RDF
                        .process(exchange -> {
                            LOGGER.info("Binary has been updated.  Deleting old and creating new.");
                        })
                        .to("{{update.binary.delete.input.stream}}")
                .endChoice();

        from("direct:update-binary-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{update.binary.dead.stream}}");

        // Update routes for deleting a Fedora resource for a Drupal file
        from("{{update.binary.delete.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdateBinaryDelete")
                .errorHandler(
                        deadLetterChannel("direct:update-binary-delete-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .onException(HttpOperationFailedException.class).onWhen(is404)
                        .useOriginalMessage()
                        .handled(true)
                                .log(WARN, LOGGER, "Received 404 from Milliner DELETE, skipping routing.")
                                .to("{{update.binary.output.stream}}")
                .end()
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerDeleteBinaryUpdate")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{update.binary.unmap.input.stream}}");

        from("direct:update-binary-delete-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{update.binary.delete.dead.stream}}");

        // Update routes for unmapping a Fedora resource from a Drupal file
        from("{{update.binary.unmap.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdateBinaryUnmap")
                .errorHandler(
                        deadLetterChannel("direct:update-binary-unmap-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiDeleteBinaryUpdate")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{update.binary.unmap.rdf.input.stream}}");

        from("direct:update-binary-unmap-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{update.binary.unmap.dead.stream}}");

        // Update routes for unmapping a Fedora resource that describes a Drupal file
        from("{{update.binary.unmap.rdf.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdateBinaryUnmapRdf")
                .errorHandler(
                        deadLetterChannel("direct:update-binary-unmap-rdf-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiDeleteRdf")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{update.binary.create.input.stream}}");

        from("direct:update-binary-unmap-rdf-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{update.binary.unmap.rdf.dead.stream}}");

        // Update routes for creating a Fedora resource from a Drupal file
        from("{{update.binary.create.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdateBinaryCreate")
                .errorHandler(
                        deadLetterChannel("direct:update-binary-create-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .onException(HttpOperationFailedException.class).onWhen(is409)
                        .useOriginalMessage()
                        .handled(true)
                                .log(WARN, LOGGER, "Received 409 from Milliner POST, skipping routing.")
                                .to("{{create.binary.output.stream}}")
                .end()
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerCreateBinaryUpdate")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerCreateBinaryUpdate")
                .to("{{update.binary.map.input.stream}}");

        from("direct:update-binary-create-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{update.binary.create.dead.stream}}");

        // Update routes for mapping a Fedora resource to a Drupal file
        from("{{update.binary.map.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdateBinaryMap")
                .errorHandler(
                        deadLetterChannel("direct:update-binary-map-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiCreateBinary")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{update.binary.map.rdf.input.stream}}");

        from("direct:update-binary-map-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{map.binary.dead.stream}}");

        // Update routes for mapping a Fedora resource that describes a Drupal file
        from("{{update.binary.map.rdf.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdateBinaryMapRdf")
                .errorHandler(
                        deadLetterChannel("direct:update-binary-map-rdf-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiCreateRdf")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{update.binary.output.stream}}");

        from("direct:update-binary-map-rdf-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{update.binary.map.rdf.dead.stream}}");

        // Routes for deleting a Fedora resource for a Drupal file
        from("{{delete.binary.input.stream}}")
                .routeId("IslandoraFcrepoIndexerDeleteBinary")
                .errorHandler(
                        deadLetterChannel("direct:delete-binary-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .onException(HttpOperationFailedException.class).onWhen(is404)
                        .useOriginalMessage()
                        .handled(true)
                                .log(WARN, LOGGER, "Received 404 from Milliner DELETE, skipping routing.")
                                .to("{{delete.binary.output.stream}}")
                .end()
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Delete'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerDeleteBinary")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerDeleteBinary")
                .to("{{unmap.binary.input.stream}}");

        from("direct:delete-binary-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{delete.binary.dead.stream}}");

        // Routes for unmapping a Fedora resource from a Drupal file
        from("{{unmap.binary.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUnmapBinary")
                .errorHandler(
                        deadLetterChannel("direct:unmap-binary-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiDeleteBinary")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{unmap.binary.rdf.input.stream}}");

        from("direct:unmap-binary-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{unmap.binary.dead.stream}}");

        // Routes for unmapping a Fedora resource that describes a Drupal file
        from("{{unmap.binary.rdf.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUnmapBinaryRdf")
                .errorHandler(
                        deadLetterChannel("direct:unmap-binary-rdf-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiDeleteRdf")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{delete.binary.output.stream}}");

        from("direct:unmap-binary-rdf-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{unmap.binary.rdf.dead.stream}}");
    }
}
