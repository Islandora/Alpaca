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

package ca.islandora.alpaca.http.client;

import static ca.islandora.alpaca.http.client.StaticTokenRequestInterceptor.AUTH_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;


/**
 * @author ajs6f
 *
 */
public class StaticTokenRequestInterceptorTest {

    @Test
    public void shouldInjectHeaderWhenNoAuthHeadersPresent() {
        final StaticTokenRequestInterceptor testInterceptor = new StaticTokenRequestInterceptor("testToken");
        final HttpRequest request = new HttpGet();
        testInterceptor.process(request, null);
        final Header[] authHeaders = request.getHeaders(AUTH_HEADER);
        assertEquals(1, authHeaders.length,"Should only be one auth header!");
        assertEquals( "Bearer testToken", authHeaders[0].getValue(), "Wrong value for header!");
    }

    @Test
    public void shouldNotInjectHeaderWhenAuthHeadersPresent() {
        final StaticTokenRequestInterceptor testInterceptor = new StaticTokenRequestInterceptor();
        testInterceptor.setToken("testToken");
        final HttpRequest request = new HttpGet();
        request.addHeader(AUTH_HEADER, "fake header");
        testInterceptor.process(request, null);
        final Header[] authHeaders = request.getHeaders(AUTH_HEADER);
        assertEquals(1, authHeaders.length, "Should only be one auth header!");
        assertEquals( "fake header", authHeaders[0].getValue(), "Wrong value for header!");
    }
}
