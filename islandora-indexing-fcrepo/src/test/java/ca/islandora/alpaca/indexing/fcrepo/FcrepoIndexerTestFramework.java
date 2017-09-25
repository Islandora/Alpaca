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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import java.io.IOException;
import java.util.HashMap;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.IOUtils;

/**
 * @author dannylamb
 * @author ajs6f
 */

public abstract class FcrepoIndexerTestFramework extends CamelBlueprintTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    protected void sendBodyAndHeaders(final String bodyResourceName, final Header... headers)
            throws CamelExecutionException, IOException {
        final HashMap<String, Object> headerMap = new HashMap<>(headers.length / 2);
        for (Header h : headers) {
            headerMap.put(h.key, h.value); }
        template.sendBodyAndHeaders(loadResource(bodyResourceName), headerMap);
    }

    protected static class Header {
        final String key;
        final Object value;

        public Header(final String k, final Object v) {
            this.key = k;
            this.value = v;
        }
    }

    protected static Header h(final String key, final Object value) {
        return new Header(key, value);
    }

    protected String loadResource(final String resourceName) throws IOException {
        return IOUtils.toString(loadResourceAsStream(resourceName), UTF_8);
    }
}
