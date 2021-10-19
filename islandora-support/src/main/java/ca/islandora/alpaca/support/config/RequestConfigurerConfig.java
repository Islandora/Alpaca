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
package ca.islandora.alpaca.support.config;

import org.apache.camel.component.http.HttpComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Class to enable the HTTP client configurer.
 *
 * @author whikloj
 */
@Configuration
public class RequestConfigurerConfig {

    public static final String REQUEST_CONFIGURER_ENABLED_PROPERTY = "request.configurer.enabled";
    public static final String REQUEST_TIMEOUT_PROPERTY = "request.timeout";
    public static final String CONNECT_TIMEOUT_PROPERTY = "connection.timeout";
    public static final String SOCKET_TIMEOUT_PROPERTY = "socket.timeout";

    @Value("${" + REQUEST_CONFIGURER_ENABLED_PROPERTY + ":false}")
    private boolean enabled;

    @Value("${" + REQUEST_TIMEOUT_PROPERTY + ":1000}")
    private int requestTimeout;

    @Value("${" + CONNECT_TIMEOUT_PROPERTY + ":1000}")
    private int connectTimeout;

    @Value("${" + SOCKET_TIMEOUT_PROPERTY + ":1000}")
    private int socketTimeout;

    /**
     * Customize the connection setting if necessary.
     * @return the http component
     */
    private HttpComponent configComponent(final HttpComponent component) {
        if (enabled) {
            component.setConnectionRequestTimeout(requestTimeout);
            component.setConnectTimeout(connectTimeout);
            component.setSocketTimeout(socketTimeout);
        }
        return component;
    }

    /**
     * @return bean for the http endpoint.
     */
    @Bean(name = "http")
    public HttpComponent http() {
        return configComponent(new HttpComponent());
    }

    /**
     * @return bean for the https endpoint.
     */
    @Bean(name = "https")
    public HttpComponent https() {
        return configComponent(new HttpComponent());
    }
}
