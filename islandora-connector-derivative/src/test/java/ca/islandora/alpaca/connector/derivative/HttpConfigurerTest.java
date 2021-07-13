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

import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;

/**
 * Insures that the Camel HTTP component as configured by Blueprint is properly configured.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class HttpConfigurerTest extends CamelBlueprintTestSupport {

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-httpconfigurer-test.xml";
    }

    /**
     * Insure that the default RequestConfig for the HttpComponent carries the timeout values specified in the
     * blueprint xml.
     *
     * Note that the RequestConfig and RequestConfigConfigurer are difficult to test with mocking frameworks such as
     * Mockito due to the presence of final methods in the relevant HttpClient classes.
     *
     * @throws Exception
     */
    @Test
    public void testRequestConfig() throws Exception {
        context.start();
        final HttpComponent http = (HttpComponent) context.getComponent("http");

        final RequestConfigConfigurer configurer = (RequestConfigConfigurer) http.getHttpClientConfigurer();

        assertEquals(10000, configurer.built.getSocketTimeout());
        assertEquals(10000, configurer.built.getConnectTimeout());
        assertEquals(10000, configurer.built.getConnectionRequestTimeout());
    }

}
