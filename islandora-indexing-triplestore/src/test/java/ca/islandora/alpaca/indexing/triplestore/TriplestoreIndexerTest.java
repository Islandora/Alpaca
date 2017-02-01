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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static java.net.URLEncoder.encode;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

/**
 * Created by daniel on 31/01/17.
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

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final Properties props = new Properties();
        props.put("input.stream", "seda:foo");
        return props;
    }

    @Test
    public void testRouterWithDeleteEvent() throws Exception {
        context.getRouteDefinition("IslandoraTriplestoreIndexerRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("direct:triplestore.*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:triplestore.delete").expectedMessageCount(1);
        getMockEndpoint("mock:direct:triplestore.index").expectedMessageCount(0);

        template.sendBody(
                IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRouterWithNonDeleteEvent() throws Exception {
        context.getRouteDefinition("IslandoraTriplestoreIndexerRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("direct:triplestore.*");
                mockEndpointsAndSkip("direct:retrieve.resource");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:triplestore.delete").expectedMessageCount(0);
        getMockEndpoint("mock:direct:triplestore.index").expectedMessageCount(1);

        template.sendBody(
                IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8")
        );

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testParseEvent() throws Exception {
        context.getRouteDefinition("IslandoraTriplestoreIndexerParseEvent").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        Exchange exchange = template.send(xchange ->
            xchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"))
        );

        this.assertPredicate(
            exchangeProperty("uri").isEqualTo("http://localhost:8000/fedora_resource/1"),
            exchange,
            true
        );
        this.assertPredicate(
            exchangeProperty("action").isEqualTo("Create"),
            exchange,
            true
        );
    }

    @Test
    public void testTriplestoreDelete() throws Exception {
        context.getRouteDefinition("IslandoraTriplestoreIndexerDelete").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http*");
            }
        });
        context.start();

        final String uri = "http://localhost:8000/fedora_resource/1";

        final MockEndpoint endpoint = getMockEndpoint("mock:http:localhost:8080/bigdata/namespace/kb/sparql");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.allMessages().body().startsWith(
                "update=" + encode("DELETE WHERE { <" + uri + "?_format=jsonld> ?p ?o }", "UTF-8")
        );

        template.send(exchange -> {
            exchange.setProperty("uri", uri);
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"));
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRetrieveResource() throws Exception {
        context.getRouteDefinition("IslandoraTriplestoreIndexerRetrieveResource").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http*");
            }
        });
        context.start();

        final MockEndpoint endpoint = getMockEndpoint("mock:http:localhost:8000/fedora_resource/1");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");

        template.send(exchange -> {
            exchange.setProperty("uri", "http://localhost:8000/fedora_resource/1");
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testTriplestoreIndex() throws Exception {
        context.getRouteDefinition("IslandoraTriplestoreIndexerIndex").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http*");
            }
        });
        context.start();

        final String uri = "http://localhost:8000/fedora_resource/1";
        final String subject = "<" + uri + "?_format=jsonld>";
        final String responsePrefix =
                "DELETE WHERE { " + subject + " ?p ?o };\n" +
                "INSERT DATA { ";
        final List<String> triples = Arrays.asList(
           subject + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/ldp#Container> .",
           subject + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/ldp#RDFSource> .",
           subject + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://schema.org/Thing> .",
           subject + " <http://schema.org/dateCreated> \"2017-01-30T04:36:07+00:00\" .",
           subject + " <http://schema.org/dateModified> \"2017-01-30T14:35:57+00:00\" .",
           subject + " <http://islandora.ca/CLAW/vclock> 5 ."
        );

        final MockEndpoint endpoint = getMockEndpoint("mock:http:localhost:8080/bigdata/namespace/kb/sparql");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.allMessages().body().startsWith("update=" + encode(responsePrefix, "UTF-8"));
        endpoint.allMessages().body().endsWith(encode("\n}", "UTF-8"));
        for (final String triple : triples) {
            endpoint.expectedBodyReceived().body().contains(encode(triple, "UTF-8"));
        }

        template.send(exchange -> {
            exchange.setProperty("uri", uri);
            exchange.getIn().setHeader(CONTENT_TYPE, "application/ld+json");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("resource.rdf"), "UTF-8"));
        });

        assertMockEndpointsSatisfied();
    }
}
