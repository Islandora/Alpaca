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
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import com.jayway.jsonpath.JsonPathException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author dhlamb
 */
public class FcrepoIndexer extends RouteBuilder {

    private static final Logger LOGGER = getLogger(FcrepoIndexer.class);

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
                "Error indexing ${property.uri} in fcrepo: ${exception.message}\n\n${exception.stacktrace}"
            );

        // Main router.
        from("{{input.stream}}")
            .routeId("IslandoraFcrepoIndexerRouter")
              .to("direct:parse.event")
              .choice()
                .when(exchangeProperty("action").isEqualTo("Delete"))
                  .to("direct:fcrepo.delete")
                .otherwise()
                  .to("direct:retrieve.resource")
                  .choice()
                    .when(exchangeProperty("action").isEqualTo("Create"))
                      .to("direct:fcrepo.create")
                  .endChoice()
                .endChoice();
                /*
                  .otherwise()
                    .to("direct:fcrepo.update");
                    */

        // Extracts info using jsonpath and stores it as properties on the exchange.
        from("direct:parse.event")
            .routeId("IslandoraFcrepoIndexerParseEvent")
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

        // Retrieves the resource from Drupal.
        from("direct:retrieve.resource")
            .routeId("IslandoraFcrepoIndexerRetrieveResource")
            .log(INFO, LOGGER, "GETTING RESOURCE")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .toD("${property.uri}?_format=jsonld&authUsername={{drupal.username}}" +
                    "&authPassword={{drupal.password}}"
            )
            .unmarshal().json(Jackson);
        /*
            .setBody().jsonpath("$.['@graph']")
            .unmarshal().json(Jackson, List.class)
            .split().simple("${body}")
                .log(INFO, LOGGER, "ID: ${body[@id]}");
                */


        /*
        from("direct:retrive.id")
                .routeId("IslandoraFcrepoIndexerRetrieveId")
                .process(exchange -> {
                    String regex = "fedora_resource\\/(\\d)";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(exchange.getProperty("uri", String.class));
                    if (matcher.find()) {
                        exchange.setProperty("id", matcher.group());
                    }
                    else {
                        throw new RuntimeException("OH NOES");
                    }
                })
                .log(INFO, LOGGER, "Got an ID: ${property.id}");
                //.setHeader(Exchange.HTTP_METHOD, constant("GET"))
                //.toD("{{idiomatic.baseUrl}}/${property.id}");
           */
        // POSTs a SPARQL delete query for all triples with subject == uri.
        from("direct:fcrepo.delete")
            .routeId("IslandoraFcrepoIndexerDelete")
                .log(INFO, LOGGER, "DELETE EVENT");
        /*
              .setHeader(FCREPO_URI, simple("${property.uri}?_format=jsonld"))
              .process(new SparqlDeleteProcessor())
              .log(INFO, LOGGER, "Deleting ${property.uri} in triplestore")
              .to("{{triplestore.baseUrl}}");
              */

        from("direct:fcrepo.create")
            .routeId("IslandoraFcrepoIndexerCreate")
                .log(INFO, LOGGER, "CREATE")
                .removeHeaders("*")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/ld+json"))
                //.to("fcrepo:{{fcrepo.baseUrl}}")
                .log(INFO, LOGGER, "${body}");


        from("direct:fcrepo.update")
                .routeId("IslandoraFcrepoIndexerUpdate")
                .log(INFO, LOGGER, "UPDATE");
        /*
              .setHeader(FCREPO_URI, simple("${property.uri}?_format=jsonld"))
              .process(new SparqlUpdateProcessor())
              .log(INFO, LOGGER, "Indexing ${property.uri} in triplestore")
              .to("{{triplestore.baseUrl}}");
              */
    }
}
