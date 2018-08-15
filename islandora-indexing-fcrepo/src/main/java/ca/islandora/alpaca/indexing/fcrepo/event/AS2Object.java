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

/**
 * POJO for a user performing an action.  Part of a AS2Event.
 *
 * @author Danny Lamb
 */
public class AS2Object {

    /**
     * @return  Type of object
     */
    public String getType() {
        return type;
    }

    /**
     * @param   type    Type of object
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * @return  URN of object
     */
    public String getId() {
        return id;
    }

    /**
     * @param   id  URN of object
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * @return  URLs for object
     */
    public AS2Url[] getUrl() {
        return url;
    }

    /**
     * @param   url  URLs for object
     */
    public void setUrl(final AS2Url[] url) {
        this.url = url;
    }

    private String type;
    private String id;
    private AS2Url[] url;

}
