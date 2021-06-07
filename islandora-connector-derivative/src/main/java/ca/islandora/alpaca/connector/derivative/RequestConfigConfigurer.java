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

import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Provides a default HttpClient {@link RequestConfig} with custom values for connection request, connect, and socket
 * timeout values.  Custom values must be set on this class prior to invoking
 * {@link #configureHttpClient(HttpClientBuilder)}.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class RequestConfigConfigurer implements HttpClientConfigurer {

    /**
     * The RequestConfig instance that is built by this configurer; exposed for testing purposes.
     */
    RequestConfig built;

    private int connectionRequestTimeoutMs = RequestConfig.DEFAULT.getConnectionRequestTimeout();

    private int connectTimeoutMs = RequestConfig.DEFAULT.getConnectTimeout();

    private int socketTimeoutMs = RequestConfig.DEFAULT.getSocketTimeout();

    /**
     * Creates a {@link RequestConfig} using custom values for {@code connectionRequestTimeout}, {@code connectTimeout},
     * and {@code socketTimeout}.  If custom values are not provided by this class, then the default values from
     * {@link RequestConfig#DEFAULT} are used.
     *
     * @param clientBuilder the HttpClientBuilder
     * @see #setConnectionRequestTimeoutMs(int)
     * @see #setConnectTimeoutMs(int)
     * @see #setSocketTimeoutMs(int)
     */
    @Override
    public void configureHttpClient(HttpClientBuilder clientBuilder) {
        RequestConfig.Builder builder = RequestConfig.copy(RequestConfig.DEFAULT);
        RequestConfig config = buildConfig(builder);
        clientBuilder.setDefaultRequestConfig(config);
    }

    /**
     * Package-private to support testing.
     *
     * @param builder the RequestConfig builder
     * @return the RequestConfig
     */
    RequestConfig buildConfig(RequestConfig.Builder builder) {
        builder.setConnectionRequestTimeout(connectionRequestTimeoutMs)
                .setSocketTimeout(socketTimeoutMs)
                .setConnectTimeout(connectTimeoutMs);
        built = builder.build();
        return built;
    }

    public int getConnectionRequestTimeoutMs() {
        return connectionRequestTimeoutMs;
    }

    public void setConnectionRequestTimeoutMs(int connectionRequestTimeoutMs) {
        this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public void setSocketTimeoutMs(int socketTimeoutMs) {
        this.socketTimeoutMs = socketTimeoutMs;
    }
}
