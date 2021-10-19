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

import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.camel.test.spring.UseAdviceWith;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ca.islandora.alpaca.support.config.ActivemqConfig;

/**
 * @author dannylamb
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@UseAdviceWith
@ContextConfiguration(classes = FcrepoIndexerTest.ContextConfig.class, loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class FcrepoIndexerTest {

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    CamelContext context;

    @Test
    public void testNode() throws Exception {
        final String route = "FcrepoIndexerNode";

        final String nodeSubRoute = "FcrepoIndexerNodeIndex";

        AdviceWithRouteBuilder.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("broker:*");
        });
        AdviceWithRouteBuilder.adviceWith(context, nodeSubRoute, a -> {
            a.mockEndpointsAndSkip("http://localhost?connectionClose=true");
        });
        context.start();

        // Assert we POST to milliner with creds.
        final MockEndpoint milliner = context.getEndpoint(
            "mock:http:localhost",
                MockEndpoint.class
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/node/2?_format=jsonld");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/node");
        milliner.expectedHeaderReceived(Exchange.HTTP_URI,
                "http://localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b1d");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("NodeAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testNodeVersion() throws Exception {
        final String route = "FcrepoIndexerNode";
        final String versionSubRoute = "FcrepoIndexerNodeVersion";
        AdviceWithRouteBuilder.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("broker:*");
        });
        AdviceWithRouteBuilder.adviceWith(context, versionSubRoute, a -> {
            a.mockEndpointsAndSkip("http://localhost?connectionClose=true");
        });
        context.start();

        // Assert we POST to milliner with creds.
        final MockEndpoint milliner = context.getEndpoint(
                "mock:http:localhost",
                MockEndpoint.class
        );
        milliner.expectedMessageCount(2);
        milliner.expectedHeaderValuesReceivedInAnyOrder("Authorization", List.of(
                "Bearer islandora", "Bearer islandora"));
        milliner.expectedHeaderValuesReceivedInAnyOrder("Content-Location", List.of(
                "http://localhost:8000/node/2?_format=jsonld", "http://localhost:8000/node/2?_format=jsonld"));
        milliner.expectedHeaderValuesReceivedInAnyOrder(Exchange.HTTP_METHOD, List.of(
                "POST", "POST"));
        milliner.expectedHeaderValuesReceivedInAnyOrder(Exchange.HTTP_URI, List.of(
                "http://localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b/version",
                "http://localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b"));

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                IOUtils.toString(loadResourceAsStream("VersionAS2Event.jsonld"), "UTF-8"),
                    String.class);
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testNodeDelete() throws Exception {
        final String route = "FcrepoIndexerDeleteNode";
        AdviceWithRouteBuilder.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip(
                "http://localhost?connectionClose=true"
            );
        });
        context.start();

        // Assert we DELETE to milliner with creds.
        final MockEndpoint milliner = (MockEndpoint) context.getEndpoint(
            "mock:http:localhost"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "DELETE");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/node");
        milliner.expectedHeaderReceived(Exchange.HTTP_URI, "http://localhost:8000/milliner/node/72358916-51e9-4712" +
                "-b756-4b0404c91b1d");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("NodeAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testExternalFile() throws Exception {
        final String route = "FcrepoIndexerExternalFile";
        AdviceWithRouteBuilder.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip(
                "http://localhost?connectionClose=true"
            );
        });
        context.start();

        // Assert we POST to Milliner with creds.
        final MockEndpoint milliner = (MockEndpoint) context.getEndpoint(
            "mock:http:localhost"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(
            "Content-Location",
            "http://localhost:8000/sites/default/files/2018-08/Voltaire-Records1.jpg"
        );
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/externalFile");
        milliner.expectedHeaderReceived(Exchange.HTTP_URI, "http://localhost:8000/milliner/external/148dfe8f-9711" +
                "-4263-97e7-3ef3fb15864f");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("ExternalFileAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testMedia() throws Exception {
        final String route = "FcrepoIndexerMedia";
        final String mediaSubRoute = "FcrepoIndexerMediaIndex";
        AdviceWithRouteBuilder.adviceWith(context, route, a -> a.replaceFromWith("direct:start"));
        AdviceWithRouteBuilder.adviceWith(context, mediaSubRoute,
                a -> a.mockEndpointsAndSkip("http://localhost?connectionClose=true"));

        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = context.getEndpoint("mock:http:localhost", MockEndpoint.class);
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/media/6?_format=json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/media");
        milliner.expectedHeaderReceived(Exchange.HTTP_URI, "http://localhost:8000/milliner/media/field_media_image");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("MediaAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testVersionMedia() throws Exception {
        final String route = "FcrepoIndexerMedia";
        final String versionSubRoute = "FcrepoIndexerMediaIndexVersion";
        AdviceWithRouteBuilder.adviceWith(context, route, a -> a.replaceFromWith("direct:start"));
        AdviceWithRouteBuilder.adviceWith(context, versionSubRoute,
                a -> a.mockEndpointsAndSkip("http://localhost?connectionClose=true"));

        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = context.getEndpoint("mock:http:localhost", MockEndpoint.class);
        // Expect 2 messages as we send the normal and version messages to the same "endpoint" but different
        // Exchange.HTTP_URIs
        milliner.expectedMessageCount(2);
        milliner.expectedHeaderValuesReceivedInAnyOrder("Authorization", List.of(
                "Bearer islandora", "Bearer islandora"
        ));
        milliner.expectedHeaderValuesReceivedInAnyOrder("Content-Location", List.of(
                "http://localhost:8000/media/7?_format=json",
                "http://localhost:8000/media/7?_format=json"
        ));
        milliner.expectedHeaderValuesReceivedInAnyOrder(Exchange.HTTP_METHOD, List.of(
                "POST", "POST"
        ));
        milliner.expectedHeaderValuesReceivedInAnyOrder(Exchange.HTTP_URI, List.of(
                "http://localhost:8000/milliner/media/field_media_image/version",
                "http://localhost:8000/milliner/media/field_media_image"
        ));

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("MediaVersionAS2Event.jsonld"), "UTF-8"),
                    String.class);
        });

        milliner.assertIsSatisfied();
    }

    @BeforeClass
    public static void setProperties() {
        System.setProperty("error.maxRedeliveries", "1");
        System.setProperty("fcrepo.indexer.enabled", "true");
        System.setProperty("fcrepo.indexer.node", "topic:islandora-indexing-fcrepo-content");
        System.setProperty("fcrepo.indexer.delete", "topic:islandora-indexing-fcrepo-delete");
        System.setProperty("fcrepo.indexer.external", "topic:islandora-indexing-fcrepo-file-external");
        System.setProperty("fcrepo.indexer.media", "topic:islandora-indexing-fcrepo-media");
        System.setProperty("fcrepo.indexer.milliner.baseUrl", "http://localhost:8000/milliner/");
        System.setProperty("fcrepo.indexer.fedoraHeader", "X-ISLANDORA-FEDORA-HEADER");
    }

    @Configuration
    @ComponentScan(basePackageClasses = {FcrepoIndexerOptions.class, ActivemqConfig.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {FcrepoIndexerOptions.class, ActivemqConfig.class}))
    static class ContextConfig extends CamelConfiguration {

        @Bean
        public RouteBuilder route() {
            return new FcrepoIndexer();
        }
    }
}
