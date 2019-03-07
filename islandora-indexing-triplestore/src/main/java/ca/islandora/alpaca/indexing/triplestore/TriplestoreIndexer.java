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

package ca.islandora.alpaca.indexing.triplestore;

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import com.jayway.jsonpath.JsonPathException;

import net.minidev.json.JSONArray;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.slf4j.Logger;

import java.util.LinkedHashMap;

/**
 * @author dhlamb
 */
public class TriplestoreIndexer extends RouteBuilder {

    private static final Logger LOGGER = getLogger(TriplestoreIndexer.class);

    @Override
    public void configure() {
        // Global exception handler for the indexer.
        // Just logs after retrying X number of times.
        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log(
                ERROR,
                LOGGER,
                "Error indexing ${property.uri} in triplestore: ${exception.message}\n\n${exception.stacktrace}"
            );

        from("{{index.stream}}")
            .routeId("IslandoraTriplestoreIndexer")
              .to("direct:parse.url")
              .removeHeaders("*", "Authorization")
              .setHeader(Exchange.HTTP_METHOD, constant("GET"))
              .setBody(simple("${null}"))
              .toD("${exchangeProperty.url}&connectionClose=true")
              .setHeader(FCREPO_URI, simple("${exchangeProperty.url}"))
              .process(new SparqlUpdateProcessor())
              .log(INFO, LOGGER, "Indexing ${exchangeProperty.url} in triplestore")
              .to("{{triplestore.baseUrl}}?connectionClose=true");

        from("{{delete.stream}}")
            .routeId("IslandoraTriplestoreIndexerDelete")
              .to("direct:parse.url")
              .setHeader(FCREPO_URI, simple("${exchangeProperty.url}"))
              .process(new SparqlDeleteProcessor())
              .log(INFO, LOGGER, "Deleting ${exchangeProperty.url} in triplestore")
              .to("{{triplestore.baseUrl}}?connectionClose=true");

        // Extracts the JSONLD URL from the event message and stores it on the exchange.
        from("direct:parse.url")
            .routeId("IslandoraTriplestoreIndexerParseUrl")
              // Custom exception handlers.  Don't retry if event is malformed.
              .onException(JsonPathException.class)
                .maximumRedeliveries(0)
                .log(
                   ERROR,
                   LOGGER,
                   "Error extracting properties from event: ${exception.message}\n\n${exception.stacktrace}"
                )
                .end()
              .onException(RuntimeException.class)
                .maximumRedeliveries(0)
                .log(
                   ERROR,
                   LOGGER,
                   "Error extracting properties from event: ${exception.message}\n\n${exception.stacktrace}"
                )
                .end()
              .transform().jsonpath("$.object.url")
              .process(ex -> {
                  final LinkedHashMap url = ex.getIn().getBody(JSONArray.class).stream()
                          .map(LinkedHashMap.class::cast)
                          .filter(elem -> "application/ld+json".equals(elem.get("mediaType")))
                          .findFirst()
                          .orElseThrow(() -> new RuntimeException("Cannot find JSONLD URL in event message."));
                  ex.setProperty("url", url.get("href"));
              });
    }
}
