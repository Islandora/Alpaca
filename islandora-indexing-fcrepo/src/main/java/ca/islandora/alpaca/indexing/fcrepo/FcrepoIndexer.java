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

import static org.apache.camel.LoggingLevel.ERROR;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.Exchange;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * @author Danny Lamb
 */
public class FcrepoIndexer extends RouteBuilder {

    /**
     * @return  Number of times to retry
     */
    public int getMaxRedeliveries() {
        return maxRedeliveries;
    }

    /**
     * @param   maxRedeliveries Number of times to retry
     */
    public void setMaxRedeliveries(final int maxRedeliveries) {
        this.maxRedeliveries = maxRedeliveries;
    }

    /**
     * @return  Milliner base url
     */
    public String getMillinerBaseUrl() {
        return millinerBaseUrl;
    }

    /**
     * @param   millinerBaseUrl Milliner base url
     */
    public void setMillinerBaseUrl(final String millinerBaseUrl) {
        // Enforce trailing slash on the way in.
        final String trimmed = millinerBaseUrl.trim();
        if (trimmed.endsWith("/")) {
            this.millinerBaseUrl = trimmed;
        } else {
            this.millinerBaseUrl = trimmed + "/";
        }
    }

    @PropertyInject("error.maxRedeliveries")
    private int maxRedeliveries;

    @PropertyInject("milliner.baseUrl")
    private String millinerBaseUrl;

    private static final Logger LOGGER = getLogger(FcrepoIndexer.class);

    @Override
    public void configure() {

        onException(Exception.class)
                .maximumRedeliveries(maxRedeliveries)
                .log(
                        ERROR,
                        LOGGER,
                        "Error indexing resource in fcrepo: ${exception.message}\n\n${exception.stacktrace}"
                );

        from("{{content.input.stream}}")
                .routeId("FcrepoIndexerSaveContent")
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/ld+json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to(getMillinerBaseUrl() + "content")
                .process(exchange -> exchange.setOut(exchange.getUnitOfWork().getOriginalInMessage()));

        from("{{file.input.stream}}")
                .routeId("FcrepoIndexerSaveFile")
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/ld+json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to(getMillinerBaseUrl() + "file")
                .process(exchange -> exchange.setOut(exchange.getUnitOfWork().getOriginalInMessage()));

        from("{{media.input.stream}}")
                .routeId("FcrepoIndexerSaveMedia")
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/ld+json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to(getMillinerBaseUrl() + "media")
                .process(exchange -> exchange.setOut(exchange.getUnitOfWork().getOriginalInMessage()));

        from("{{delete.input.stream}}")
                .routeId("FcrepoIndexerDelete")
                .setProperty("urn").jsonpath("$.object.id")
                .setProperty("uuid").simple("${exchangeProperty.urn.replaceAll(\"urn:uuid:\",\"\"}")
                .removeHeaders("*", "Authorization")
                .setHeader(Exchange.HTTP_METHOD, constant("DELETE"))
                .toD(getMillinerBaseUrl() + "resource/${exchangeProperty.uuid}")
                .process(exchange -> exchange.setOut(exchange.getUnitOfWork().getOriginalInMessage()));
    }
}
