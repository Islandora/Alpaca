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

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

/**
 * @author dannylamb
 */
public class FcrepoIndexerTest extends CamelBlueprintTestSupport {

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
    public void testCreateFiltersBadEvents() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreate";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
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
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("update-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateRoutesToMapOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreate";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-map");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreatePassthruOn409() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreate";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();

                // Jam in a 409 right at the beginning
                weaveAddFirst()
                        .process(exchange -> {
                            throw new HttpOperationFailedException(
                                "http://test.com",
                                409,
                                "Conflict",
                                null,
                                null,
                                "Error message"
                            );
                        });
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateRoutesToDLQOnOtherExceptions() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreate";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMapRoutesToResultOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerPathMapper";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalPath", "fedora_resource/1");
            exchange.getIn().setHeader("FcrepoUri", "http://localhost:8080/fcrepo/rest/foo");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMapRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerPathMapper";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalPath", "fedora_resource/1");
            exchange.getIn().setHeader("FcrepoUri", "http://localhost:8080/fcrepo/rest/foo");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateFiltersBadEvents() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdate";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        // Send a delete event instead of an update.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateRoutesToResultOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdate";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http
                mockEndpointsAndSkip("http*");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("update-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateRoutesToDLQOnException() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdate";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("update-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteFiltersBadEvents() throws Exception {
        final String route = "IslandoraFcrepoIndexerDelete";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        // Send a create event instead of a delete.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteRoutesToUnmapOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerDelete";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-unmap");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeletePassthruOn404() throws Exception {
        final String route = "IslandoraFcrepoIndexerDelete";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();

                // Jam in a 404 right at the beginning
                weaveAddFirst()
                        .process(exchange -> {
                            throw new HttpOperationFailedException(
                                    "http://test.com",
                                    404,
                                    "Not Found",
                                    null,
                                    null,
                                    "Error message"
                            );
                        });
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteRoutesToDLQOnOtherExceptions() throws Exception {
        final String route = "IslandoraFcrepoIndexerDelete";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUnmapRoutesToResultOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerPathUnmapper";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalPath", "fedora_resource/1");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUnmapRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerPathUnmapper";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalPath", "fedora_resource/1");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"), String.class);
        });

        assertMockEndpointsSatisfied();
    }
}
