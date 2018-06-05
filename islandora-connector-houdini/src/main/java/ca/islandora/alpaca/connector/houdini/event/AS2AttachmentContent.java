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

package ca.islandora.alpaca.connector.houdini.event;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for attachment content.  Part of a AS2Event.
 *
 * @author Danny Lamb
 */
public class AS2AttachmentContent {

    /**
     * @return  Source uri
     */
    @JsonProperty(value = "source_uri")
    public String getSourceUri() {
        return sourceUri;
    }

    /**
     * @param   sourceUri    Source uri
     */
    public void setSourceUri(final String sourceUri) {
        this.sourceUri = sourceUri;
    }

    /**
     * @return  Destination uri
     */
    @JsonProperty(value = "destination_uri")
    public String getDestinationUri() {
        return destinationUri;
    }

    /**
     * @param   destinationUri    Destination uri
     */
    public void setDestinationUri(final String destinationUri) {
        this.destinationUri = destinationUri;
    }

    /**
     * @return  Mimetype
     */
    public String getMimetype() {
        return mimetype;
    }

    /**
     * @param   mimetype    Mimetype
     */
    public void setMimetype(final String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * @return  Args
     */
    public String getArgs() {
        return args;
    }

    /**
     * @param   args    Args
     */
    public void setArgs(final String args) {
        this.args = args;
    }

    /**
     * @return  Filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param   filename    Filename
     */
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    private String sourceUri;
    private String destinationUri;
    private String mimetype;
    private String args;
    private String filename;

}
