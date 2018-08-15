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

package ca.islandora.alpaca.connector.houdini;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

/**
 * @author dannylamb
 */
public class HoudiniConnectorTest extends CamelBlueprintTestSupport {

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

    @Test
    public void testHoudiniConnector() throws Exception {
        final String route = "IslandoraConnectorHoudini";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() throws Exception {
                    replaceFromWith("direct:start");

                    // Rig Drupal REST endpoint to return canned jsonld
                    interceptSendToEndpoint("http://example.org/houdini/convert")
                            .skipSendToOriginalEndpoint()
                            .process(exchange -> {
                                exchange.getIn().removeHeaders("*", "Authorization");
                                exchange.getIn().setHeader("Content-Type", "image/jpeg");
                                exchange.getIn().setBody("SOME DERIVATIVE", String.class);
                            });

                    mockEndpointsAndSkip("http://localhost:8000/node/2/media/image/3");
                }
        });
        context.start();

        final MockEndpoint endpoint = getMockEndpoint("mock:http:localhost:8000/node/2/media/image/3");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "PUT");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "image/jpeg");
        endpoint.expectedHeaderReceived("Content-Location", "public://2018-08/2-Service File.jpg");
        endpoint.expectedHeaderReceived("Authorization", "Bearer islandora");

        template.send(exchange -> {
                exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"));
                exchange.getIn().setHeader("Authorization", "Bearer islandora");
        });

        assertMockEndpointsSatisfied();
    }

}
