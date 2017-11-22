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

import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

/**
 * @author dannylamb
 */
public class FcrepoIndexerTest extends CamelBlueprintTestSupport {

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
    public void testContent() throws Exception {
        final String route = "FcrepoIndexerContent";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http://localhost:8000/milliner/content");
            }
        });
        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = getMockEndpoint("mock:http:localhost:8000/milliner/content");
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/ld+json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFile() throws Exception {
        final String route = "FcrepoIndexerFile";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http://localhost:8000/milliner/file");
            }
        });
        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = getMockEndpoint("mock:http:localhost:8000/milliner/file");
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/ld+json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMedia() throws Exception {
        final String route = "FcrepoIndexerMedia";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http://localhost:8000/milliner/media");
            }
        });
        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = getMockEndpoint("mock:http:localhost:8000/milliner/media");
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/ld+json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDelete() throws Exception {

        final String uuid = "9541c0c1-5bee-4973-a9d0-e55c1658bc81";

        final String route = "FcrepoIndexerDelete";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http://localhost:8000/milliner/resource/" + uuid);
            }
        });
        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = getMockEndpoint("mock:http:localhost:8000/milliner/resource/" + uuid);
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "DELETE");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

}
