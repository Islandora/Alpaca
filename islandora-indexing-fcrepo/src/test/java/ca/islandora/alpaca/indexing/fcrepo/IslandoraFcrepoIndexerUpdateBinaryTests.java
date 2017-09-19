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

import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.junit.Test;

/**
 * @author ajs6f
 *
 */
public class IslandoraFcrepoIndexerUpdateBinaryTests extends FcrepoIndexerTestFramework {

    private final static String routeName = "IslandoraFcrepoIndexerUpdateBinary";

    @Test
    public void testUpdateBinaryFiltersBadEvents() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();
            }
        });

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-delete").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);
        getMockEndpoint("mock:result").expectedMessageCount(0);

        // Send a delete event instead of an update.
        sendBodyAndHeaders("delete-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryRoutesToWorkflowIfDifferent() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http
                interceptSendToEndpoint("http://localhost:8000/gemini/drupal/media/1").skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setBody("bar.jpeg/fcr:metadata");
                        });

                interceptSendToEndpoint("http://localhost:8080/fcrepo/rest/bar.jpeg/fcr:metadata")
                        .skipSendToOriginalEndpoint().process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setHeader("Link",
                                    "<http://locahost:8080/fcrepo/rest/bar.jpeg>; rel=\"describes\"");
                        });

                interceptSendToEndpoint("http://localhost:8000/gemini/fedora/bar.jpeg").skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setBody("sites/default/files/image2.jpeg");
                        });

                mockEndpointsAndSkip("seda*");
            }
        });

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-delete").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);
        getMockEndpoint("mock:result").expectedMessageCount(0);

        sendBodyAndHeaders("update-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryRoutesToResultIfSame() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http
                interceptSendToEndpoint("http://localhost:8000/gemini/drupal/media/1").skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setBody("bar.jpeg/fcr:metadata");
                        });

                interceptSendToEndpoint("http://localhost:8080/fcrepo/rest/bar.jpeg/fcr:metadata")
                        .skipSendToOriginalEndpoint().process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setHeader("Link",
                                    "<http://locahost:8080/fcrepo/rest/bar.jpeg>; rel=\"describes\"");
                        });

                interceptSendToEndpoint("http://localhost:8000/gemini/fedora/bar.jpeg").skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setBody("sites/default/files/image.jpeg");
                        });

                mockEndpointsAndSkip("seda*");
            }
        });

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-delete").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);
        getMockEndpoint("mock:result").expectedMessageCount(1);

        sendBodyAndHeaders("update-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryRoutesToDLQOnException() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        sendBodyAndHeaders("update-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }
}
