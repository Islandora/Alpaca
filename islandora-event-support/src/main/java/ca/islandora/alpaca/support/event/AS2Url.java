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
 * POJO for a URL.  Part of a AS2Event.
 *
 * @author Danny Lamb
 */
public class AS2Url {

    /**
     *  The URL name, ie. JSON, Canonical, JSONLD
     */
    private String name;
    /**
     * The URL type, ie. Link
     */
    private String type;
    /**
     * The URL address.
     */
    private String href;
    /**
     * The URL media type, ie. text/xml, application/ld+json
     */
    private String mediaType;
    /**
     * The URL rel attribute, ie. alternate, canonical
     */
    private String rel;

    /**
     * @return  Name
     */
    public String getName() {
        return name;
    }

    /**
     * @param   name    Name
     */
    public void setName(final String name) {
        this.name = name;
    }

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
     * @return  Url as string
     */
    public String getHref() {
        return href;
    }

    /**
     * @param   href  Url as string
     */
    public void setHref(final String href) {
        this.href = href;
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
     * @return  Link relation
     */
    public String getRel() {
        return rel;
    }

    /**
     * @param   rel  Link relation
     */
    public void setRel(final String rel) {
        this.rel = rel;
    }

}


