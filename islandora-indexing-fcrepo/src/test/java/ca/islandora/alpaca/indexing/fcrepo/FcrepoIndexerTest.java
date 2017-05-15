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
        final String route = "IslandoraFcrepoIndexerCreateRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateRoutesToMapOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreateRdf";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Mock milliner http endpoint and return canned response
                interceptSendToEndpoint("http://*")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                                    exchange.getIn().removeHeaders("*");
                                    exchange.getIn().setHeader("Location", "http://localhost:8080/fcrepo/rest/foo");
                                    exchange.getIn().setBody("http://localhost:8080/fcrepo/rest/foo");
                        });

                // Mock and skip final endpoint
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-map");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreatePassthruOn409() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreateRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateRoutesToDLQOnOtherExceptions() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreateRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMapRoutesToResultOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerMapRdf";
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
            exchange.getIn().setHeader("DrupalRdfPath", "fedora_resource/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMapRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerMapRdf";
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
            exchange.getIn().setHeader("DrupalRdfPath", "fedora_resource/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateFiltersBadEvents() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateRoutesToResultOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateRoutesToDLQOnException() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteFiltersBadEvents() throws Exception {
        final String route = "IslandoraFcrepoIndexerDeleteRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteRoutesToUnmapOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerDeleteRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeletePassthruOn404() throws Exception {
        final String route = "IslandoraFcrepoIndexerDeleteRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteRoutesToDLQOnOtherExceptions() throws Exception {
        final String route = "IslandoraFcrepoIndexerDeleteRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUnmapRoutesToResultOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUnmapRdf";
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
            exchange.getIn().setHeader("DrupalPath", "fedora_resource/1");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUnmapRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerUnmapRdf";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateBinaryFiltersBadEvents() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreateBinary";
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
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateBinaryRoutesToMapOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreateBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Mock milliner http endpoint and return canned response
                interceptSendToEndpoint("http://*")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setHeader("Location", "http://localhost:8080/fcrepo/rest/foo");
                            exchange.getIn().setHeader(
                                    "Link",
                                    "<http://localhost:8080/fcrepo/rest/foo/fcr:metadata>; rel=\"describedby\""
                            );
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

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateBinaryPassthruOn409() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreateBinary";
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreateBinaryRoutesToDLQOnOtherExceptions() throws Exception {
        final String route = "IslandoraFcrepoIndexerCreateBinary";
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMapBinaryRoutesToMapBinaryRdfOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerMapBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda*");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map-binary-rdf").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo.jpeg/fcr:metadata");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setHeader("FcrepoFilePath", "foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMapBinaryRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerMapBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda*");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-map-binary-rdf").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo.jpeg/fcr:metadata");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setHeader("FcrepoFilePath", "foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMapBinaryRdfRoutesToResultOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerMapBinaryRdf";
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
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo.jpeg/fcr:metadata");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setHeader("FcrepoFilePath", "foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMapBinaryRdfRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerMapBinaryRdf";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http
                mockEndpointsAndSkip("http*");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo.jpeg/fcr:metadata");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setHeader("FcrepoFilePath", "foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteBinaryFiltersBadEvents() throws Exception {
        final String route = "IslandoraFcrepoIndexerDeleteBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        // Send a create event instead of a delete.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("create-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteBinaryRoutesToUnmapBinaryOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerDeleteBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-unmap-binary");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteBinaryPassthruOn404() throws Exception {
        final String route = "IslandoraFcrepoIndexerDeleteBinary";
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteBinaryRoutesToDLQOnOtherExceptions() throws Exception {
        final String route = "IslandoraFcrepoIndexerDeleteBinary";
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUnmapBinaryRoutesToUnmapBinaryRdfOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUnmapBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-unmap-binary-rdf");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary-rdf").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUnmapBinaryRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerUnmapBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-unmap-binary-rdf");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-unmap-binary-rdf").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUnmapBinaryRdfRoutesToResultOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUnmapBinaryRdf";
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
            exchange.getIn().setHeader("DrupalRdfPath", "sites/default/files/foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUnmapBinaryRdfRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerUnmapBinaryRdf";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http
                mockEndpointsAndSkip("http*");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalRdfPath", "sites/default/files/foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryFiltersBadEvents() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpoints();
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-delete").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);
        getMockEndpoint("mock:result").expectedMessageCount(0);

        // Send a delete event instead of an update.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("delete-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryRoutesToWorkflowIfDifferent() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http
                interceptSendToEndpoint("http://localhost:8000/gemini/drupal/media/1")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setBody("bar.jpeg/fcr:metadata");
                        });

                interceptSendToEndpoint("http://localhost:8080/fcrepo/rest/bar.jpeg/fcr:metadata")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setHeader(
                                    "Link",
                                    "<http://locahost:8080/fcrepo/rest/bar.jpeg>; rel=\"describes\""
                            );
                        });

                interceptSendToEndpoint("http://localhost:8000/gemini/fedora/bar.jpeg")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setBody("sites/default/files/image2.jpeg");
                        });

                mockEndpointsAndSkip("seda*");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-delete").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);
        getMockEndpoint("mock:result").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryRoutesToResultIfSame() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinary";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http
                interceptSendToEndpoint("http://localhost:8000/gemini/drupal/media/1")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setBody("bar.jpeg/fcr:metadata");
                        });

                interceptSendToEndpoint("http://localhost:8080/fcrepo/rest/bar.jpeg/fcr:metadata")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setHeader(
                                    "Link",
                                    "<http://locahost:8080/fcrepo/rest/bar.jpeg>; rel=\"describes\""
                            );
                        });

                interceptSendToEndpoint("http://localhost:8000/gemini/fedora/bar.jpeg")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setBody("sites/default/files/image.jpeg");
                        });

                mockEndpointsAndSkip("seda*");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-delete").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);
        getMockEndpoint("mock:result").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

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
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryDeleteRoutesToUnmapBinaryOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryDelete";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-binary-update-unmap");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-unmap").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("OriginalDrupalFilePath", "sites/default/files/image2.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryDeletePassthruOn404() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryDelete";
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-unmap").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("OriginalDrupalFilePath", "sites/default/files/image2.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryDeleteRoutesToDLQOnOtherExceptions() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryDelete";
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-unmap").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("OriginalDrupalFilePath", "sites/default/files/image2.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryUnmapRoutesToUnmapBinaryRdfOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryUnmap";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
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

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("OriginalDrupalFilePath", "sites/default/files/image2.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryUnmapRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryUnmap";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
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

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("OriginalDrupalFilePath", "sites/default/files/image2.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryUnmapRdfRoutesToCreateBinaryOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryUnmapRdf";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-binary-update-create");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-create").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryUnmapRdfRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryUnmapRdf";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-binary-update-create");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-create").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryCreateRoutesToMapOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryCreate";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Mock milliner http endpoint and return canned response
                interceptSendToEndpoint("http://*")
                        .skipSendToOriginalEndpoint()
                        .process(exchange -> {
                            exchange.getIn().removeHeaders("*");
                            exchange.getIn().setHeader("Location", "http://localhost:8080/fcrepo/rest/foo.jpeg");
                            exchange.getIn().setHeader(
                                    "Link",
                                    "<http://localhost:8080/fcrepo/rest/foo.jpeg/fcr:metadata>; rel=\"describedby\""
                            );
                            exchange.getIn().setBody("http://localhost:8080/fcrepo/rest/foo");
                        });

                // Mock and skip final endpoint
                mockEndpointsAndSkip("seda:islandora-indexing-fcrepo-binary-update-map");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-map").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/image.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryCreatePassthruOn409() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryCreate";
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-map").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/image.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryCreateRoutesToDLQOnOtherExceptions() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryCreate";
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
        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-map").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/image.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryMapRoutesToMapBinaryRdfOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryMap";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda*");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-map-rdf").expectedMessageCount(1);
        getMockEndpoint("mock:dead").expectedMessageCount(0);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo.jpeg/fcr:metadata");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setHeader("FcrepoFilePath", "foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryMapRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryMap";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http or seda
                mockEndpointsAndSkip("http*");
                mockEndpointsAndSkip("seda*");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:seda:islandora-indexing-fcrepo-binary-update-map-rdf").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo.jepg/fcr:metadata");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setHeader("FcrepoFilePath", "foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryMapRdfRoutesToResultOnSuccess() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryMapRdf";
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
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo.jpeg/fcr:metadata");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setHeader("FcrepoFilePath", "foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateBinaryMapRdfRoutesToDLQOnError() throws Exception {
        final String route = "IslandoraFcrepoIndexerUpdateBinaryMapRdf";
        context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");

                // Don't actually send out messages using http
                mockEndpointsAndSkip("http*");

                // Jam in an exception right at the beginning
                weaveAddFirst().throwException(Exception.class, "Error Message");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:dead").expectedMessageCount(1);

        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "some_nifty_token");
            exchange.getIn().setHeader("DrupalRdfPath", "media/1");
            exchange.getIn().setHeader("FcrepoRdfPath", "foo.jpeg/fcr:metadata");
            exchange.getIn().setHeader("DrupalFilePath", "sites/default/files/foo.jpeg");
            exchange.getIn().setHeader("FcrepoFilePath", "foo.jpeg");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("update-binary-event.json"), "UTF-8"),
                    String.class
            );
        });

        assertMockEndpointsSatisfied();
    }
}
