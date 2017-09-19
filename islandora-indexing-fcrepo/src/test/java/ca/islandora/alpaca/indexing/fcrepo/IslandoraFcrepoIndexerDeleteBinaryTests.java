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
public class IslandoraFcrepoIndexerDeleteBinaryTests extends FcrepoIndexerTestFramework {

    private final static String routeName = "IslandoraFcrepoIndexerDeleteBinary";

    @Test
    public void testDeleteBinaryFiltersBadEvents() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();
            }
        });

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        // Send a create event instead of a delete.
        sendBodyAndHeaders("create-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteBinaryRoutesToUnmapBinaryOnSuccess() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-unmap-binary");
            }
        });

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        sendBodyAndHeaders("delete-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteBinaryPassthruOn404() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();

                // Jam in a 404 right at the beginning
                weaveAddFirst().process(exchange -> {
                    throw new HttpOperationFailedException("http://test.com", 404, "Not Found", null, null,
                            "Error message");
                });
            }
        });

        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        sendBodyAndHeaders("delete-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteBinaryRoutesToDLQOnOtherExceptions() throws Exception {
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        sendBodyAndHeaders("delete-binary-event.json", h("Authorization", "some_nifty_token"));

        assertMockEndpointsSatisfied();
    }
}
