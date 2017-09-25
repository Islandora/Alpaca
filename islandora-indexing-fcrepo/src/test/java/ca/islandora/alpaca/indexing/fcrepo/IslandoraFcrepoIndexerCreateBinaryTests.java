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
import org.junit.runner.RunWith;

import com.googlecode.junittoolbox.ParallelRunner;

/**
 * @author ajs6f
 *
 */
@RunWith(ParallelRunner.class)
public class IslandoraFcrepoIndexerCreateBinaryTests extends FcrepoIndexerTestFramework {

    private final static String routeName = "IslandoraFcrepoIndexerCreateBinary";

    @Test
    public void testCreateBinaryFiltersBadEvents() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();
            }
        });
        context.start();
        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        // Send an update event instead of a create.
        sendBodyAndHeaders("update-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateBinaryRoutesToMapOnSuccess() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Mock milliner http endpoint and return canned response
                interceptSendToEndpoint("http://*").skipSendToOriginalEndpoint().process(exchange -> {
                    exchange.getIn().removeHeaders("*");
                    exchange.getIn().setHeader("Location", "http://localhost:8080/fcrepo/rest/foo");
                    exchange.getIn().setHeader("Link",
                            "<http://localhost:8080/fcrepo/rest/foo/fcr:metadata>; rel=\"describedby\"");
                    exchange.getIn().setBody("http://localhost:8080/fcrepo/rest/foo");
                });

                // Mock and skip final endpoint
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-map-binary");
            }
        });
        context.start();
        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map-binary").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        sendBodyAndHeaders("create-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateBinaryPassthruOn409() throws Exception {
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
        context.start();
        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        sendBodyAndHeaders("create-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateBinaryRoutesToDLQOnOtherExceptions() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();

                // Jam in some other type of exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();
        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        sendBodyAndHeaders("create-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }
}
