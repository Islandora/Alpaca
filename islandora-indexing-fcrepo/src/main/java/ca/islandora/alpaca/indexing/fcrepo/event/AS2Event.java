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
 * POJO for events emitted by Islandora.
 *
 * @author Danny Lamb
 */
public class AS2Event {

    /**
     * @return  Event type (Create, Update, Delete, etc...)
     */
    public String getType() {
        return type;
    }

    /**
     * @param   type    Event type (Create, Update, Delete, etc...)
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * @return  Summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * @param   summary Summary
     */
    public void setSummary(final String summary) {
        this.summary = summary;
    }
    /**
     * @return  Resource acted upon
     */
    public AS2Object getObject() {
        return object;
    }

    /**
     * @param   object  Resource acted upon
     */
    public void setObject(final AS2Object object) {
        this.object = object;
    }

    /**
     * @return  User performing the action
     */
    public AS2Actor getActor() {
        return actor;
    }

    /**
     * @param   actor   User performing the action
     */
    public void setActor(final AS2Actor actor) {
        this.actor = actor;
    }

    /**
     * @return  Configuration as an attachment
     */
    public AS2Attachment getAttachment() {
        return attachment;
    }

    /**
     * @param   attachment    Configuration as an attachment
     */
    public void setAttachment(final AS2Attachment attachment) {
        this.attachment = attachment;
    }

    /**
     * @return  JSON-LD Context
     */
    @JsonProperty(value = "@context")
    public String getContext() {
        return context;
    }

    /**
     * @param   context JSON-LD Context
     */
    public void setContext(final String context) {
        this.context = context;
    }

    private String context;
    private String type;
    private String summary;
    private AS2Object object;
    private AS2Actor actor;
    private AS2Attachment attachment;

}
