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

/**
 * POJO for an attachment.  Part of a AS2Event.
 *
 * @author Danny Lamb
 */
public class AS2Attachment {

    /**
     * Attachment type, ie. Object.
     */
    private String type;
    /**
     * Attachment mime-type, ie. application/json
     */
    private String mediaType;
    /**
     * Attachment content.
     */
    private AS2AttachmentContent content;

    /**
     * @return  Type
     */
    public String getType() {
        return type;
    }

    /**
     * @param   type    Type
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * @return  Mimetype
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * @param   mediaType  Mimetype
     */
    public void setMediaType(final String mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * @return  Content
     */
    public AS2AttachmentContent getContent() {
        return content;
    }

    /**
     * @param   content  Content
     */
    public void setContent(final AS2AttachmentContent content) {
        this.content = content;
    }

}



