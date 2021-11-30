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

import static org.apache.camel.language.constant.ConstantLanguage.constant;
import static org.apache.camel.language.property.ExchangePropertyLanguage.exchangeProperty;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import ca.islandora.alpaca.support.event.AS2Event;

/**
 * A processor to perform some common actions on the Exchange
 * @author whikloj
 */
public class CommonProcessor implements Processor {

    private FcrepoIndexerOptions config;

    /**
     * Basic constructor.
     * @param fcrepoConfig
     *   The FcrepoIndexerOptions we are using.
     */
    public CommonProcessor(final FcrepoIndexerOptions fcrepoConfig) {
        config = fcrepoConfig;
    }

    @Override
    public void process(final Exchange exchange) {
        final var msg = exchange.getIn();
        exchange.setProperty("event", msg.getBody());
        final AS2Event json = msg.getBody(AS2Event.class);
        exchange.setProperty("fedoraBaseUrl", json.getTarget());
        msg.removeHeaders("*", "Authorization");
        msg.setHeader(config.getFedoraUriHeader(), exchangeProperty("fedoraBaseUrl"));
        msg.setBody(constant(null));
        exchange.setMessage(msg);
    }
}
