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
public class IslandoraFcrepoIndexerUpdateBinaryUnmapTests extends FcrepoIndexerTestFramework {

    private final static String routeName = "IslandoraFcrepoIndexerUpdateBinaryUnmap";

    @Test
    public void testUpdateBinaryUnmapRoutesToUnmapBinaryRdfOnSuccess() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-binary-update-unmap-rdf");
            }
        });
        context.start();
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-unmap-rdf").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        sendBodyAndHeaders("update-binary-event.json", h("Authorization", "some_nifty_token"),
                h("OriginalDrupalFilePath", "sites/default/files/image2.jpeg"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryUnmapRoutesToDLQOnError() throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-binary-update-unmap-rdf");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-unmap-rdf").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        sendBodyAndHeaders("update-binary-event.json", h("Authorization", "some_nifty_token"),
                h("OriginalDrupalFilePath", "sites/default/files/image2.jpeg"));

        assertMockEndpointsSatisfied();
    }
}
