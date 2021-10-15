/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.islandora.alpaca.indexing.triplestore.processors;

import static ca.islandora.alpaca.indexing.triplestore.processors.FcrepoHeaders.FCREPO_NAMED_GRAPH;
import static ca.islandora.alpaca.indexing.triplestore.processors.ProcessorUtils.deleteWhere;
import static ca.islandora.alpaca.indexing.triplestore.processors.ProcessorUtils.getSubjectUri;
import static java.net.URLEncoder.encode;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.NoSuchHeaderException;
import org.apache.camel.Processor;

/**
 * Represents a message processor that deletes objects from an
 * external triplestore.
 *
 * @author Aaron Coburn
 *
 * @author whikloj
 *   Copied from fcrepo-camel and modified for use in Alpaca - 2021-10-13
 */
public class SparqlDeleteProcessor implements Processor {

    /**
     * Define how the message should be processed.
     *
     * @param exchange the current camel message exchange
     */
    @Override
    public void process(final Exchange exchange) throws IOException, NoSuchHeaderException {

        final Message in = exchange.getIn();
        final String namedGraph = in.getHeader(FCREPO_NAMED_GRAPH, "", String.class);
        final String subject = getSubjectUri(exchange);

        in.setBody("update=" + encode(deleteWhere(subject, namedGraph), "UTF-8"));
        in.setHeader(HTTP_METHOD, "POST");
        in.setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
    }

}
