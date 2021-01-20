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

package ca.islandora.alpaca.support.event;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for attachment content.  Part of a AS2Event.
 *
 * @author Danny Lamb
 */
public class AS2AttachmentContent {

    /**
     * Path to resource in Fedora.
     */
    private String fedoraUri;
    /**
     * Source field.
     */
    private String sourceField;
    /**
     * Source URI, ie. http://localhost:8000/_flysystem/fedora/2019-10/wonderful.tiff.
     */
    private String sourceUri;
    /**
     * URL to post content back to, ie. http://localhost:8000/node/2/media/image/19.
     */
    private String destinationUri;
    /**
     * Attachment mime-type, image/jpeg.
     */
    private String mimetype;
    /**
     * Arguments to pass to derivative connector, ie. "-thumbnail 100x100".
     */
    private String args;
    /**
     * Path to attachment file, ie. public://2019-11/2-Thumbnail Image.jpg.
     */
    private String fileUploadUri;

    /**
     * @return Fedora uri
     */
    @JsonProperty("fedora_uri")
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
    @JsonProperty("source_field")
    public String getSourceField() {
        return sourceField;
    }

    /**
     * @param   sourceField   Source field
     */
    public void setSourceField(final String sourceField) {
        this.sourceField = sourceField;
    }

    /**
     * @return  Source uri
     */
    @JsonProperty("source_uri")
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
    @JsonProperty("destination_uri")
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
     * @return  File upload uri
     */
    @JsonProperty("file_upload_uri")
    public String getFileUploadUri() {
        return fileUploadUri;
    }

    /**
     * @param   fileUploadUri    File upload uri
     */
    public void setFileUploadUri(final String fileUploadUri) {
        this.fileUploadUri = fileUploadUri;
    }

}
