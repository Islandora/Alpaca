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
import org.apache.camel.http.common.HttpOperationFailedException;
import org.junit.Test;

/**
 * @author ajs6f
 *
 */
public class IslandoraFcrepoIndexerUpdateBinaryCreateTests extends FcrepoIndexerTestFramework {

    private final static String routeName = "IslandoraFcrepoIndexerUpdateBinaryCreate";

    @Test
    public void testUpdateBinaryCreateRoutesToMapOnSuccess() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Mock milliner http endpoint and return canned response
                interceptSendToEndpoint("http://*").skipSendToOriginalEndpoint().process(exchange -> {
                    exchange.getIn().removeHeaders("*");
                    exchange.getIn().setHeader("Location", "http://localhost:8080/fcrepo/rest/foo.jpeg");
                    exchange.getIn().setHeader("Link",
                            "<http://localhost:8080/fcrepo/rest/foo.jpeg/fcr:metadata>; rel=\"describedby\"");
                    exchange.getIn().setBody("http://localhost:8080/fcrepo/rest/foo");
                });

                // Mock and skip final endpoint
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-binary-update-map");
            }
        });

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-map").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        sendBodyAndHeaders("update-binary-event.json", h("Authorization", "some_nifty_token"),
                h("DrupalFilePath", "sites/default/files/image.jpeg"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryCreatePassthruOn409() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();

                // Jam in a 409 right at the beginning
                weaveAddFirst().process(exchange -> {
                    throw new HttpOperationFailedException("http://test.com", 409, "Conflict", null, null,
                            "Error message");
                });
            }
        });

        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-map").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        sendBodyAndHeaders("update-binary-event.json", h("Authorization", "some_nifty_token"),
                h("DrupalFilePath", "sites/default/files/image.jpeg"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryCreateRoutesToDLQOnOtherExceptions() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();

                // Jam in some other type of exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-map").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        sendBodyAndHeaders("update-binary-event.json", h("Authorization", "some_nifty_token"),
                h("DrupalFilePath", "sites/default/files/image.jpeg"));

        assertMockEndpointsSatisfied();
    }
}
