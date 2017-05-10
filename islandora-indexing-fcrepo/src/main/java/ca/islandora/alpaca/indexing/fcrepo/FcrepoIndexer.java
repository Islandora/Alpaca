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

        // Route for creating a Fedora resource from a Drupal entity
        from("{{create.input.stream}}")
                .routeId("IslandoraFcrepoIndexerCreate")
                .errorHandler(
                        deadLetterChannel("direct:create-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .onException(HttpOperationFailedException.class).onWhen(is409)
                        .useOriginalMessage()
                        .handled(true)
                                .log(WARN, LOGGER, "Received 409 from Milliner POST, skipping routing.")
                                .to("{{create.output.stream}}")
                .end()
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Create'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerCreate")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerCreate")
                .to("{{map.input.stream}}");

        from("direct:create-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{create.dead.stream}}");

        // Route for mapping a Fedora resource to a Drupal entity
        from("{{map.input.stream}}")
                .routeId("IslandoraFcrepoIndexerPathMapper")
                .errorHandler(
                        deadLetterChannel("direct:map-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiCreate")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{create.output.stream}}");

        from("direct:map-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{map.dead.stream}}");

        // Route for updating a Fedora resource from a Drupal entity
        from("{{update.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdate")
                .errorHandler(
                        deadLetterChannel("direct:update-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Update'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerUpdate")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{update.output.stream}}");

        from("direct:update-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{update.dead.stream}}");

        // Route for deleting a Fedora resource for a Drupal entity
        from("{{delete.input.stream}}")
                .routeId("IslandoraFcrepoIndexerDelete")
                .errorHandler(
                        deadLetterChannel("direct:delete-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .onException(HttpOperationFailedException.class).onWhen(is404)
                        .useOriginalMessage()
                        .handled(true)
                                .log(WARN, LOGGER, "Received 404 from Milliner DELETE, skipping routing.")
                                .to("{{delete.output.stream}}")
                .end()
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Delete'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerDelete")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerDelete")
                .to("{{unmap.input.stream}}");

        from("direct:delete-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{delete.dead.stream}}");

        // Route for unmapping a Fedora resource from a Drupal entity
        from("{{unmap.input.stream}}")
                .routeId("IslandoraFcrepoIndexerPathUnmapper")
                .errorHandler(
                        deadLetterChannel("direct:unmap-log-error")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiDelete")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{delete.output.stream}}");

        from("direct:unmap-log-error")
                .log(ERROR, LOGGER, "${exception.stacktrace}")
                .to("{{unmap.dead.stream}}");

        from("{{create.binary.input.stream}}")
                .routeId("IslandoraFcrepoIndexerCreateBinary")
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .filter(simple("${body.type} == 'Create'"))
                .to("bean:fcrepoIndexerBean?method=preprocessForBinaryGet");
    }
}
