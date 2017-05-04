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

/**
 * POJO for a user performing an action.  Part of an AS2Event.
 *
 * @author Danny Lamb
 */
public class AS2Actor {

    /**
     * @return  Type of user
     */
    public String getType() {
        return type;
    }

    /**
     * @param   type    Type of user
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * @return  URI of user
     */
    public String getId() {
        return id;
    }

    /**
     * @param   id  URI of user
     */
    public void setId(final String id) {
        this.id = id;
    }

    private String type;
    private String id;

}
