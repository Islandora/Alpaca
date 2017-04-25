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

import static org.apache.camel.LoggingLevel.INFO;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;


/**
 * @author dhlamb
 */
public class FcrepoIndexer extends RouteBuilder {

    private static final Logger LOGGER = getLogger(FcrepoIndexer.class);

    @Override
    public void configure() {

        from("{{delete.input.stream}}")
                .routeId("IslandoraFcrepoIndexerDelete")
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .log(INFO, LOGGER, "DELETE EVENT")
                .to("{{delete.output.stream}}");

        from("{{create.input.stream}}")
                .routeId("IslandoraFcrepoIndexerCreate")
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .to("pathProcessor")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .toD("${exchangeProperty.destination}")
                .log(INFO, LOGGER, "GOT ${body} BACK FROM MILLINER")
                .to("{{create.output.stream}}");

        from("{{update.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdate")
                .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)
                .log(INFO, LOGGER, "UPDATE EVENT")
                .to("{{update.output.stream}}");
    }
}
