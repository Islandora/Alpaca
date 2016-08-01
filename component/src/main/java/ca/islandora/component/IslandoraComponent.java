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

package ca.islandora.component;

import java.net.URLDecoder;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.util.ObjectHelper;

/**
 * Represents the component that manages {@link IslandoraEndpoint}. With the
 * component it is possible to execute system commands.
 */
public class IslandoraComponent extends UriEndpointComponent {

    private String executable;
    private String workingDir;
    private String islandoraScript;
    
    public IslandoraComponent(String workingDir) {
        super(IslandoraEndpoint.class);

        this.executable = "php";

        ObjectHelper.notEmpty(workingDir, "workingDir");
        this.workingDir = workingDir;

        this.islandoraScript = "islandora.php";
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        IslandoraEndpoint endpoint = new IslandoraEndpoint(uri, this);
        setProperties(endpoint, parameters);
        endpoint.setArgs(URLDecoder.decode(remaining, "UTF-8"));
        return endpoint;
    }
    
    public String getExecutable() {
        return this.executable;
    }
    
    public String getWorkingDir() {
        return this.workingDir;
    }
    
    public String getIslandoraScript() {
        return this.islandoraScript;
    }
}
