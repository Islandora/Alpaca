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

import static ca.islandora.alpaca.indexing.triplestore.processors.FcrepoHeaders.FCREPO_URI;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.TRACE;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPathException;

import ca.islandora.alpaca.indexing.triplestore.processors.SparqlDeleteProcessor;
import ca.islandora.alpaca.indexing.triplestore.processors.SparqlUpdateProcessor;
import ca.islandora.alpaca.support.event.AS2Event;
import ca.islandora.alpaca.support.event.AS2Url;
import ca.islandora.alpaca.support.exceptions.MissingDescribesUrlException;
import ca.islandora.alpaca.support.exceptions.MissingJsonldUrlException;
import ca.islandora.alpaca.support.exceptions.MissingPropertyException;

/**
 * @author dhlamb
 */
public class TriplestoreIndexer extends RouteBuilder {

    /**
     * The logger.
     */
    private static final Logger LOGGER = getLogger(TriplestoreIndexer.class);

    @Autowired
    private TriplestoreIndexerOptions config;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure() {
        LOGGER.info("TriplestoreIndexer routes starting");
        // Global exception handler for the indexer.
        // Just logs after retrying X number of times.
        onException(Exception.class)
            .maximumRedeliveries(config.getMaxRedeliveries())
            .log(
                ERROR,
                LOGGER,
                "Error indexing ${exchangeProperty.uri} in triplestore: ${exception.message}\n\n${exception.stacktrace}"
            );

        from(config.getJmsIndexStream())
            .routeId("IslandoraTriplestoreIndexer")
                .log(TRACE, LOGGER, "Received message on IslandoraTriplestoreIndexer")
              .to("direct:parse.url")
              .removeHeaders("*", "Authorization")
              .setHeader(Exchange.HTTP_METHOD, constant("GET"))
              .setBody(simple("${null}"))
              .toD(config.addHttpOptions("${exchangeProperty.jsonld_url}", true))
              .setHeader(FCREPO_URI, simple("${exchangeProperty.subject_url}"))
              .process(new SparqlUpdateProcessor())
              .log(INFO, LOGGER, "Indexing ${exchangeProperty.subject_url} in triplestore")
              .to(config.getTriplestoreBaseUrl());

        from(config.getJmsDeleteStream())
            .routeId("IslandoraTriplestoreIndexerDelete")
              .to("direct:parse.url")
              .setHeader(FCREPO_URI, simple("${exchangeProperty.subject_url}"))
              .process(new SparqlDeleteProcessor())
              .log(INFO, LOGGER, "Deleting ${exchangeProperty.subject_url} in triplestore")
              .to(config.getTriplestoreBaseUrl());

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
              .onException(MissingPropertyException.class)
                .maximumRedeliveries(0)
                .log(
                   ERROR,
                   LOGGER,
                   "Error extracting properties from event: ${exception.message}\n\n${exception.stacktrace}"
                )
                .end()
              .onException(MissingJsonldUrlException.class)
                .maximumRedeliveries(0)
                .log(
                        INFO,
                        LOGGER,
                        "Unable to find JsonLD Url, this is an error or happens when a file is pre-uploaded to Drupal"
                )
                .end()
              .process(ex -> {
                  // Parse the event message.
                  final String message = ex.getIn().getBody(String.class);

                  LOGGER.trace("Triplestore ParseUrl incoming message is \n{}", message);

                  final AS2Event object = objectMapper.readValue(message, AS2Event.class);

                  final AS2Url jsonldUrl = object.getObject().getJsonldUrl();

                  ex.setProperty("jsonld_url", jsonldUrl.getHref());

                  // Attempt to get the 'describes' url first, but if it fails, fall back to the canonical.
                  AS2Url subjectUrl;
                  try {
                      subjectUrl = object.getObject().getDescribesUrl();
                  } catch (final MissingDescribesUrlException e) {
                      subjectUrl = object.getObject().getCanonicalUrl();
                  }
                  ex.setProperty("subject_url", subjectUrl.getHref());
              }).transform().jsonpath("$.object.url");
    }
}
