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

import static org.apache.camel.LoggingLevel.OFF;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;

/**
 * @author dhlamb
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

        from("{{delete.input.stream}}")
                .routeId("IslandoraFcrepoIndexerDelete")
                .errorHandler(
                        deadLetterChannel("{{delete.dead.stream}}")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                                .logExhaustedMessageBody(true)
                                .logExhaustedMessageHistory(true)
                                .retryAttemptedLogLevel(OFF)
                )
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerDelete")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerDelete")
                .to("{{unmap.input.stream}}");

        from("{{create.input.stream}}")
                .routeId("IslandoraFcrepoIndexerCreate")
                .errorHandler(
                        deadLetterChannel("{{create.dead.stream}}")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                                .logExhaustedMessageBody(true)
                                .logExhaustedMessageHistory(true)
                                .retryAttemptedLogLevel(OFF)
                )
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerCreate")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerCreate")
                .to("{{map.input.stream}}");

        from("{{update.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdate")
                .errorHandler(
                        deadLetterChannel("{{update.dead.stream}}")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                                .logExhaustedMessageBody(true)
                                .logExhaustedMessageHistory(true)
                                .retryAttemptedLogLevel(OFF)
                )
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .to("bean:fcrepoIndexerBean?method=preprocessForMillinerUpdate")
                .toD("${exchangeProperty.MillinerUri}")
                .to("bean:fcrepoIndexerBean?method=postprocessForMillinerUpdate")
                .to("{{update.output.stream}}");

        from("{{map.input.stream}}")
                .routeId("IslandoraFcrepoIndexerPathMapper")
                .errorHandler(
                        deadLetterChannel("{{map.dead.stream}}")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                                .logExhaustedMessageBody(true)
                                .logExhaustedMessageHistory(true)
                                .retryAttemptedLogLevel(OFF)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiCreate")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{create.output.stream}}");

        from("{{unmap.input.stream}}")
                .routeId("IslandoraFcrepoIndexerPathUnmapper")
                .errorHandler(
                        deadLetterChannel("{{unmap.dead.stream}}")
                                .useOriginalMessage()
                                .maximumRedeliveries(maxRedeliveries)
                                .logExhaustedMessageBody(true)
                                .logExhaustedMessageHistory(true)
                                .retryAttemptedLogLevel(OFF)
                )
                .to("bean:fcrepoIndexerBean?method=preprocessForGeminiDelete")
                .toD("${exchangeProperty.GeminiUri}")
                .to("bean:fcrepoIndexerBean?method=resetToOriginalMessage")
                .to("{{delete.output.stream}}");
    }
}
