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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.slf4j.Logger;

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
            .handled(true)
            .log(
                ERROR,
                LOGGER,
                "Error indexing ${property.uri} in triplestore: ${exception.message}\n\n${exception.stacktrace}"
            );

        // Main router.
        from("{{input.stream}}")
            .routeId("IslandoraTriplestoreIndexerRouter")
              .to("direct:parse.event")
              .choice()
                .when(exchangeProperty("action").isEqualTo("Delete"))
                  .to("direct:triplestore.delete")
                .otherwise()
                  .to("direct:retrieve.resource")
                  .to("direct:triplestore.index");

        // Extracts info using jsonpath and stores it as properties on the exchange.
        from("direct:parse.event")
            .routeId("IslandoraTriplestoreIndexerParseEvent")
              // Custom exception handler.  Doesn't retry if event is malformed.
              .onException(JsonPathException.class)
                .maximumRedeliveries(0)
                .handled(true)
                .log(
                   ERROR,
                   LOGGER,
                   "Error extracting properties from event: ${exception.message}\n\n${exception.stacktrace}"
                )
                .end()
              .setProperty("action").jsonpath("$.type")
              .setProperty("uri").jsonpath("$.object");

        // POSTs a SPARQL delete query for all triples with subject == uri.
        from("direct:triplestore.delete")
            .routeId("IslandoraTriplestoreIndexerDelete")
              .setHeader(FCREPO_URI, simple("${property.uri}?_format=jsonld"))
              .process(new SparqlDeleteProcessor())
              .log(INFO, LOGGER, "Deleting ${property.uri} in triplestore")
              .to("{{triplestore.baseUrl}}");

        // Retrieves the resource from Drupal.
        from("direct:retrieve.resource")
            .routeId("IslandoraTriplestoreIndexerRetrieveResource")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("Authentication", simple("${headers['Authentication']}"))
                .toD("${property.uri}?_format=jsonld");

        // Converts the resource to a SPARQL update query, POSTing it to the triplestore.
        from("direct:triplestore.index")
            .routeId("IslandoraTriplestoreIndexerIndex")
              .setHeader(FCREPO_URI, simple("${property.uri}?_format=jsonld"))
              .process(new SparqlUpdateProcessor())
              .log(INFO, LOGGER, "Indexing ${property.uri} in triplestore")
              .to("{{triplestore.baseUrl}}");

    }
}
