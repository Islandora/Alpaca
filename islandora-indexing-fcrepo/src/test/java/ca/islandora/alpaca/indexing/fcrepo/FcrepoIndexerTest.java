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

import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ToDynamicDefinition;
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

/**
 * @author dannylamb
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(CamelSpringRunner.class)
public class FcrepoIndexerTest extends CamelSpringTestSupport {

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
    public void testNode() throws Exception {
        final String route = "FcrepoIndexerNode";
        final String nodeSubRoute = "FcrepoIndexerNodeIndex";

        context.disableJMX();
        AdviceWithRouteBuilder.adviceWith(context, route, a ->
            a.replaceFromWith("direct:start")
        );
        AdviceWithRouteBuilder.adviceWith(context, nodeSubRoute, a ->
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000" +
                    "/milliner/node/${exchangeProperty.uuid}")
        );
        context.start();

        // Assert we POST to milliner with creds.
        final MockEndpoint milliner = getMockEndpoint(
            "mock:localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b1d"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/node/2?_format=jsonld");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/node");

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

        context.disableJMX();
        AdviceWithRouteBuilder.adviceWith(context, route, a ->
            a.replaceFromWith("direct:start")
        );
        AdviceWithRouteBuilder.adviceWith(context, versionSubRoute, a ->
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000" +
                    "/milliner/node/${exchangeProperty.uuid}/version")
        );
        context.start();

        // Assert we POST to milliner with creds.
        final MockEndpoint milliner = getMockEndpoint(
                "mock:localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b/version"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/node/2?_format=jsonld");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

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

        context.disableJMX();
        AdviceWithRouteBuilder.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000/milliner/node" +
                    "/${exchangeProperty.uuid}");
        });
        context.start();

        // Assert we DELETE to milliner with creds.
        final MockEndpoint milliner = getMockEndpoint(
            "mock:localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b1d"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "DELETE");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/node");

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

        context.disableJMX();
        AdviceWithRouteBuilder.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000/milliner/" +
                    "external/${exchangeProperty.uuid}");
        });
        context.start();

        // Assert we POST to Milliner with creds.
        final MockEndpoint milliner = getMockEndpoint(
            "mock:localhost:8000/milliner/external/148dfe8f-9711-4263-97e7-3ef3fb15864f"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(
            "Content-Location",
            "http://localhost:8000/sites/default/files/2018-08/Voltaire-Records1.jpg"
        );
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/externalFile");

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

        context.disableJMX();
        AdviceWithRouteBuilder.adviceWith(context, route, a ->
            a.replaceFromWith("direct:start")
        );
        AdviceWithRouteBuilder.adviceWith(context, mediaSubRoute, a->
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000/milliner/" +
                    "media/${exchangeProperty.sourceField}")
        );

        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = getMockEndpoint(
                "mock:localhost:8000/milliner/media/field_media_image"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/media/6?_format=json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/media");

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

        context.disableJMX();
        AdviceWithRouteBuilder.adviceWith(context, route, a -> a.replaceFromWith("direct:start"));
        AdviceWithRouteBuilder.adviceWith(context, versionSubRoute, a ->
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000/milliner/" +
                    "media/${exchangeProperty.sourceField}/version")
        );
        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = getMockEndpoint(
                "mock:localhost:8000/milliner/media/field_media_image/version"
        );
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/media/7?_format=json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

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

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        final var context = new AnnotationConfigApplicationContext();
        context.register(FcrepoIndexerTest.ContextConfig.class);
        return context;
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
