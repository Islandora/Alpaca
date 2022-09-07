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
package ca.islandora.alpaca.connector.derivative;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.UseAdviceWith;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Test of connector with the additional http options added.
 * @author whikloj
 * @since 2.2.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@UseAdviceWith
@ContextConfiguration(classes = DerivativeConnectorTest.ContextConfig.class,
        loader = AnnotationConfigContextLoader.class)
@TestPropertySource("/test3.properties")
@RunWith(SpringJUnit4ClassRunner.class)
public class ConnectorWithHttpOptionsTest2 {

    private static final Logger LOGGER = getLogger(DerivativeConnectorTest.class);

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    CamelContext camelContext;

    @Test
    public void testDerivativeConnectorWithMultipleOptions() throws Exception {
        final String route = "IslandoraConnectorDerivative-testRoutesWithMultipleOptions";

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");

            // Rig Drupal REST endpoint to return canned jsonld
            a.interceptSendToEndpoint("http://example.org/derivative/other?connectionClose=true&" +
                            "disableStreamCache=true&specialProp=true&otherProp=91")
                    .skipSendToOriginalEndpoint()
                    .process(exchange -> {
                        exchange.getIn().removeHeaders("*", "Authorization");
                        exchange.getIn().setHeader("Content-Type", "image/jpeg");
                        exchange.getIn().setBody("SOME DERIVATIVE", String.class);
                    });

            a.mockEndpointsAndSkip("http://localhost:8000/node/2/media/image/3?connectionClose=true&" +
                    "disableStreamCache=true&specialProp=true&otherProp=91");
        });
        context.start();

        final MockEndpoint endpoint = (MockEndpoint) context
                .getEndpoint("mock:http:localhost:8000/node/2/media/image/3");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "PUT");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "image/jpeg");
        endpoint.expectedHeaderReceived("Content-Location", "public://2018-08/2-Service File.jpg");
        endpoint.expectedHeaderReceived("Authorization", "Bearer islandora");

        template.send(exchange -> {
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("AS2Event.jsonld"), "UTF-8"));
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
        });

        endpoint.assertIsSatisfied();
    }
}
