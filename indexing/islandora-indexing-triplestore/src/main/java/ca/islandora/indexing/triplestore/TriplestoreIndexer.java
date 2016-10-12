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

package ca.islandora.indexing.triplestore;

import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;

import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;

/**
 * @author dhlamb
 */
public class TriplestoreIndexer extends RouteBuilder {

    @Override
    public void configure() {

        final Predicate isTriples = header("Content-Type").isEqualTo("application/n-triples");
        final Predicate hasBaseUrl = header(FCREPO_BASE_URL).isNotNull();
        final Predicate hasIdentifier = header(FCREPO_IDENTIFIER).isNotNull();
        final Predicate hasFcrepoCamelHeaders = and(hasBaseUrl, hasIdentifier);
        final Predicate hasAction = or(header("action").isEqualTo("delete"), header("action").isEqualTo("upsert"));
        final Predicate isValid = and(isTriples, hasFcrepoCamelHeaders, hasAction);

        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log(ERROR, "Error Indexing in Triplestore: ${routeId}");

        from("{{input.stream}}")
            .routeId("IslandoraTriplestoreIndexerRouter")
            .filter(isValid)
                .choice()
                    .when(header("action").isEqualTo("delete"))
                        .to("direct:triplestoreDelete")
                    .otherwise()
                        .to("direct:triplestoreUpsert");

        from("direct:triplestoreUpsert")
            .routeId("islandoraTripelstoreIndexerUpsert")
            .process(new SparqlUpdateProcessor())
            .to("{{triplestore.baseUrl}}");

        from("direct:triplestoreDelete")
            .routeId("islandoraTripelstoreIndexerDelete")
            .process(new SparqlDeleteProcessor())
            .to("{{triplestore.baseUrl}}");

    }
}
