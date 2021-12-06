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

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import ca.islandora.alpaca.support.config.ActivemqConfig;
import ca.islandora.alpaca.support.exceptions.MissingCanonicalUrlException;
import ca.islandora.alpaca.support.exceptions.MissingJsonldUrlException;

/**
 * @author dannylamb
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(CamelSpringRunner.class)
public class TriplestoreIndexerTest extends CamelSpringTestSupport {

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Test
    public void testParseUrl() throws Exception {
        final String route = "IslandoraTriplestoreIndexerParseUrl";

        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveAddLast().to(resultEndpoint);
        });
        context.start();

        resultEndpoint.expectedMessageCount(1);

        final Exchange exchange = template.send(xchange ->
                xchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"))
        );


        assertPredicate(
                exchangeProperty("jsonld_url").isEqualTo("http://localhost:8000/node/1?_format=jsonld"),
                exchange,
                true
        );
        assertPredicate(
                exchangeProperty("subject_url").isEqualTo("http://localhost:8000/node/1"),
                exchange,
                true
        );
        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testParseUrlDiesOnNoJsonld() throws Exception {
        final String route = "IslandoraTriplestoreIndexerParseUrl";

        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveAddLast().to(resultEndpoint);
        });
        context.start();

        resultEndpoint.expectedMessageCount(0);

        // Make sure it dies if you can't extract the jsonld url from the event.
        try {
            template.sendBody(
                    IOUtils.toString(loadResourceAsStream("AS2EventNoJsonldUrl.jsonld"), "UTF-8"));
        } catch (final CamelExecutionException e) {
            assertIsInstanceOf(MissingJsonldUrlException.class, e.getCause());
        }

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testParseUrlDiesOnNoCanonical() throws Exception {
        final String route = "IslandoraTriplestoreIndexerParseUrl";

        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveAddLast().to(resultEndpoint);
        });
        context.start();

        resultEndpoint.expectedMessageCount(0);

        // Make sure it dies if you can't extract the jsonld url from the event.
        try {
            template.sendBody(
                    IOUtils.toString(loadResourceAsStream("AS2EventNoCanonicalUrl.jsonld"), UTF_8));
        } catch (final CamelExecutionException e) {
            assertIsInstanceOf(MissingCanonicalUrlException.class, e.getCause());
        }

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testIndex() throws Exception {
        final String route = "IslandoraTriplestoreIndexer";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");

            // Rig Drupal REST endpoint to return canned jsonld
            a.interceptSendToEndpoint("http://localhost:8000/node/1?_format=jsonld&connectionClose=true" +
                            "&disableStreamCache=true")
                    .skipSendToOriginalEndpoint()
                    .process(exchange -> {
                        exchange.getIn().removeHeaders("*");
                        exchange.getIn().setHeader("Content-Type", "application/ld+json");
                        exchange.getIn().setBody(
                                IOUtils.toString(loadResourceAsStream("node.jsonld"), UTF_8),
                                String.class);
                    });

            a.mockEndpointsAndSkip(
                "http://localhost:8080/bigdata/namespace/islandora/sparql?connectionClose=true&disableStreamCache=true"
            );
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
        endpoint.allMessages().body().startsWith("update=" + encode(responsePrefix, UTF_8));
        endpoint.allMessages().body().endsWith(encode("\n}", UTF_8));
        for (final String triple : triples) {
            endpoint.expectedBodyReceived().body().contains(encode(triple, UTF_8));
        }

        template.send(exchange -> {
                exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), UTF_8));
        });

        endpoint.assertIsSatisfied();
    }

    @Test
    public void testDelete() throws Exception {
        final String route = "IslandoraTriplestoreIndexerDelete";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpoints("broker:*");
            a.mockEndpointsAndSkip("http://localhost:8080/bigdata/namespace/islandora/sparql?" +
                    "connectionClose=true&disableStreamCache=true");
        });
        context.start();

        final MockEndpoint endpoint = getMockEndpoint("mock:http:localhost:8080/bigdata/namespace/islandora/sparql");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.allMessages().body().startsWith(
                "update=" + encode("DELETE WHERE { <http://localhost:8000/node/1> ?p ?o }", UTF_8)
        );

        template.send(exchange -> {
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), UTF_8));
        });

        endpoint.assertIsSatisfied();
    }

    @BeforeClass
    public static void setProperties() {
        System.setProperty("error.maxRedeliveries", "1");
        System.setProperty("triplestore.indexer.enabled", "true");
        System.setProperty("triplestore.index.stream", "topic:islandora-indexing-triplestore-index");
        System.setProperty("triplestore.delete.stream", "topic:islandora-indexing-triplestore-delete");
        System.setProperty("triplestore.baseUrl", "http://localhost:8080/bigdata/namespace/islandora/sparql");
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        final var context = new AnnotationConfigApplicationContext();
        context.register(TriplestoreIndexerTest.ContextConfig.class);
        return context;
    }

    @Configuration
    @ComponentScan(basePackageClasses = {TriplestoreIndexerOptions.class, ActivemqConfig.class},
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {TriplestoreIndexerOptions.class, ActivemqConfig.class}))
    static class ContextConfig extends CamelConfiguration {

        @Bean
        public RouteBuilder route() {
            return new TriplestoreIndexer();
        }
    }
}
