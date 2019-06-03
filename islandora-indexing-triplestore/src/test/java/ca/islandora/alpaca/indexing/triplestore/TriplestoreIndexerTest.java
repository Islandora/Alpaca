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

import java.util.Arrays;
import java.util.List;

import com.jayway.jsonpath.JsonPathException;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static java.net.URLEncoder.encode;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

/**
 * @author dannylamb
 */
public class TriplestoreIndexerTest extends CamelBlueprintTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Test
    public void testParseUrl() throws Exception {
        final String route = "IslandoraTriplestoreIndexerParseUrl";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                weaveAddLast().to(resultEndpoint);
            }
        });
        context.start();

        resultEndpoint.expectedMessageCount(1);

        final Exchange exchange = template.send(xchange ->
                xchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"))
        );

        this.assertPredicate(
                exchangeProperty("jsonld_url").isEqualTo("http://localhost:8000/node/1?_format=jsonld"),
                exchange,
                true
        );
        this.assertPredicate(
                exchangeProperty("subject_url").isEqualTo("http://localhost:8000/node/1"),
                exchange,
                true
        );
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testParseUrlDiesOnMalformed() throws Exception {
        final String route = "IslandoraTriplestoreIndexerParseUrl";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                weaveAddLast().to(resultEndpoint);
            }
        });
        context.start();

        resultEndpoint.expectedMessageCount(0);

        // Make sure it dies if the jsonpath fails.
        try {
            template.sendBody(
                    IOUtils.toString(loadResourceAsStream("AS2EventNoUrls.jsonld"), "UTF-8"));
        } catch (CamelExecutionException e) {
            assertIsInstanceOf(JsonPathException.class, e.getCause().getCause());
        }

        // Make sure it dies if you can't extract the jsonld url from the event.
        try {
            template.sendBody(
                    IOUtils.toString(loadResourceAsStream("AS2EventNoJsonldUrl.jsonld"), "UTF-8"));
        } catch (CamelExecutionException e) {
            assertIsInstanceOf(RuntimeException.class, e.getCause());
        }

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testIndex() throws Exception {
        final String route = "IslandoraTriplestoreIndexer";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() throws Exception {
                    replaceFromWith("direct:start");

                    // Rig Drupal REST endpoint to return canned jsonld
                    interceptSendToEndpoint("http://localhost:8000/node/1?_format=jsonld&connectionClose=true")
                            .skipSendToOriginalEndpoint()
                            .process(exchange -> {
                                exchange.getIn().removeHeaders("*");
                                exchange.getIn().setHeader("Content-Type", "application/ld+json");
                                exchange.getIn().setBody(
                                        IOUtils.toString(loadResourceAsStream("node.jsonld"), "UTF-8"),
                                        String.class);
                            });

                    mockEndpointsAndSkip(
                        "http://localhost:8080/bigdata/namespace/islandora/sparql?connectionClose=true"
                    );
                }
        });
        context.start();

        final String subject = "<http://localhost:8000/node/1>";
        final String responsePrefix =
                "DELETE WHERE { " + subject + " ?p ?o };\n" +
                        "INSERT DATA { ";
        final List<String> triples = Arrays.asList(
                subject + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://schema.org/Thing> .",
                subject + " <http://schema.org/dateCreated> \"2017-01-30T04:36:07+00:00\" .",
                subject + " <http://schema.org/dateModified> \"2017-01-30T14:35:57+00:00\" ."
        );

        final MockEndpoint endpoint = getMockEndpoint("mock:http:localhost:8080/bigdata/namespace/islandora/sparql");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.allMessages().body().startsWith("update=" + encode(responsePrefix, "UTF-8"));
        endpoint.allMessages().body().endsWith(encode("\n}", "UTF-8"));
        for (final String triple : triples) {
            endpoint.expectedBodyReceived().body().contains(encode(triple, "UTF-8"));
        }

        template.send(exchange -> {
                exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"));
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDelete() throws Exception {
        final String route = "IslandoraTriplestoreIndexerDelete";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http://localhost:8080/bigdata/namespace/islandora/sparql?connectionClose=true");
            }
        });
        context.start();

        final MockEndpoint endpoint = getMockEndpoint("mock:http:localhost:8080/bigdata/namespace/islandora/sparql");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.allMessages().body().startsWith(
                "update=" + encode("DELETE WHERE { <http://localhost:8000/node/1> ?p ?o }", "UTF-8")
        );

        template.send(exchange -> {
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"));
        });

        assertMockEndpointsSatisfied();
    }

}
