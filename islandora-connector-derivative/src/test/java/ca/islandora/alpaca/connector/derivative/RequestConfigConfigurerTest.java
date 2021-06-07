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

import org.apache.http.client.config.RequestConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies the behaviors and state of the RequestConfigConfigurer.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class RequestConfigConfigurerTest {

    /**
     * The default state of the RequestConfigConfigurer should provide the same timeout values as the default
     * RequestConfig.
     */
    @Test
    public void testDefaultValues() {
        RequestConfigConfigurer underTest = new RequestConfigConfigurer();

        assertEquals(RequestConfig.DEFAULT.getConnectionRequestTimeout(), underTest.getConnectionRequestTimeoutMs());
        assertEquals(RequestConfig.DEFAULT.getConnectTimeout(), underTest.getConnectTimeoutMs());
        assertEquals(RequestConfig.DEFAULT.getSocketTimeout(), underTest.getSocketTimeoutMs());
    }

    /**
     * Insure state is properly maintained by the RequestConfigConfigurer.
     */
    @Test
    public void testCustomValues() {
        RequestConfigConfigurer underTest = new RequestConfigConfigurer();
        underTest.setConnectionRequestTimeoutMs(12345);
        underTest.setConnectTimeoutMs(1111111);
        underTest.setSocketTimeoutMs(9999999);

        assertEquals(12345, underTest.getConnectionRequestTimeoutMs());
        assertEquals(1111111, underTest.getConnectTimeoutMs());
        assertEquals(9999999, underTest.getSocketTimeoutMs());
    }

    /**
     * Insure state from the RequestConfigConfigurer is properly communicated to the built RequestConfig.
     */
    @Test
    public void testBuild() {
        RequestConfigConfigurer underTest = new RequestConfigConfigurer();
        underTest.setConnectionRequestTimeoutMs(12345);
        underTest.setConnectTimeoutMs(1111111);
        underTest.setSocketTimeoutMs(9999999);

        underTest.buildConfig(RequestConfig.custom());

        assertNotNull(underTest.built);

        assertEquals(12345, underTest.built.getConnectionRequestTimeout());
        assertEquals(1111111, underTest.built.getConnectTimeout());
        assertEquals(9999999, underTest.built.getSocketTimeout());
    }
}