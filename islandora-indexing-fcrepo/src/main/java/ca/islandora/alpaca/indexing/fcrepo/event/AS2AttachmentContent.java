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

package ca.islandora.alpaca.indexing.fcrepo.event;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for attachment content.  Part of a AS2Event.
 *
 * @author Danny Lamb
 */
public class AS2AttachmentContent {

    /**
     * @return Fedora uri 
     */
    @JsonProperty(value = "fedora_uri")
    public String getFedoraUri() {
        return fedoraUri;
    }

    /**
     * @param   fedoraUri    Fedora uri
     */
    public void setFedoraUri(final String fedoraUri) {
        this.fedoraUri = fedoraUri;
    }

    /**
     * @return Source field 
     */
    @JsonProperty(value = "source_field")
    public String getSourceField() {
        return sourceField;
    }

    /**
     * @param   sourceField   Source field 
     */
    public void setSourceField(final String sourceField) {
        this.sourceField = sourceField;
    }

    private String fedoraUri;
    private String sourceField;

}
