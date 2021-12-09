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

import static java.util.Objects.requireNonNull;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.springframework.stereotype.Component;

/**
 * Adds a single authentication header to any request that does not
 * already have at least one authentication header.
 * 
 * @author ajs6f
 *
 */
@Component
public class StaticTokenRequestInterceptor implements HttpRequestInterceptor {

    /**
     * Authorization HTTP header.
     */
    public static final String AUTH_HEADER = "Authorization";

    /**
     * The header to inject.
     */
    private Header header;

    /**
     * Default constructor
     */
    public StaticTokenRequestInterceptor() {
        // This constructor is intentionally blank.
    }

    /**
     * @param token the authentication token to use
     */
    public StaticTokenRequestInterceptor(final String token) {
        this.header = makeHeader(token);
    }

    /**
     * @param token the authentication token to use
     */
    public void setToken(final String token) {
        this.header = makeHeader(token);
    }

    private static Header makeHeader(final String token) {
        return new BasicHeader(AUTH_HEADER, "Bearer " + requireNonNull(token, "Token must not be null!"));
    }

    @Override
    public void process(final HttpRequest request, final HttpContext context) {
        // we do not inject if auth headers present
        if (request.getFirstHeader(AUTH_HEADER) == null) {
            request.addHeader(header);
        }
    }

    /**
     * Convenience factory method.
     * 
     * @param interceptor the interceptor to use, presumably an instance of {@link StaticTokenRequestInterceptor}
     * @return a default-configuration {@link HttpClient} that is wrapped with this interceptor
     */
    public static HttpClient defaultClient(final StaticTokenRequestInterceptor interceptor) {
        final PoolingHttpClientConnectionManager connMan = new PoolingHttpClientConnectionManager();
        return HttpClientBuilder.create()
                .setConnectionManager(connMan).setConnectionManagerShared(true)
                .addInterceptorFirst(interceptor).build();
    }
}
