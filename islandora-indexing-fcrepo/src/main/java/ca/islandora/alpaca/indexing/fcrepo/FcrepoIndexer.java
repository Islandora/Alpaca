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
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import com.jayway.jsonpath.JsonPathException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author dhlamb
 */
public class FcrepoIndexer extends RouteBuilder {

    private static final Logger LOGGER = getLogger(FcrepoIndexer.class);

    @Override
    public void configure() {

        from("timer:foo?period=5000")
                .log(INFO, LOGGER, "HELLO");

        from("{{delete.input.stream")
            .routeId("IslandoraFcrepoIndexerDelete")
                .log(INFO, LOGGER, "DELETE EVENT")
                .to("{{delete.output.stream}}");

        from("{{create.input.stream}}")
            .routeId("IslandoraFcrepoIndexerCreate")
                .log(INFO, LOGGER, "CREATE")
                .to("{{create.output.stream}}");

        from("{{update.input.stream}}")
                .routeId("IslandoraFcrepoIndexerUpdate")
                .log(INFO, LOGGER, "UPDATE")
                .to("{{update.output.stream}}");
    }
}
