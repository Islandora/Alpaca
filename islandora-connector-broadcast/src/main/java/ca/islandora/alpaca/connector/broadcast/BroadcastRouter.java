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
package ca.islandora.alpaca.connector.broadcast;

import static org.apache.camel.LoggingLevel.INFO;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A content router distributing messages to multiple endpoints.
 *
 * @author Daniel Lamb
 */
public class BroadcastRouter extends RouteBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastRouter.class);

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        // Distribute message based on configured header.
        from("{{input.stream}}")
                .routeId("MessageBroadcaster")
                .description("Broadcast messages from one queue/topic to other specified queues/topics.")
                .log(INFO, LOGGER,
                        "Distributing message: ${headers[JMSMessageID]} with timestamp ${headers[JMSTimestamp]}")
                .recipientList(simple("${headers[{{recipients.header}}]}"))
                .ignoreInvalidEndpoints();
    }
}

