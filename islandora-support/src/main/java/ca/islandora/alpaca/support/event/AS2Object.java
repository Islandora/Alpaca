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

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.islandora.alpaca.support.exceptions.MissingCanonicalUrlException;
import ca.islandora.alpaca.support.exceptions.MissingDescribesUrlException;
import ca.islandora.alpaca.support.exceptions.MissingJsonUrlException;
import ca.islandora.alpaca.support.exceptions.MissingJsonldUrlException;
import ca.islandora.alpaca.support.exceptions.MissingPropertyException;

/**
 * POJO for a user performing an action.  Part of a AS2Event.
 *
 * @author Danny Lamb
 */
public class AS2Object {

    /**
     * The object type if applicable.
     */
    private String type;
    /**
     * The object UUID.
     */
    private String id;
    /**
     * The URLs passed with the event.
     */
    private AS2Url[] url;

    /**
     * Are we creating a new revision?
     */
    private Boolean isNewVersion;

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
        this.url = url.clone();
    }

    /**
     * @return true or false
     */
    @JsonProperty("isNewVersion")
    public Boolean getIsNewVersion() {
        return isNewVersion;
    }

    /**
     * @param isNewVersion true or false
     */
    public void setIsNewVersion(final Boolean isNewVersion) {
        this.isNewVersion = isNewVersion;
    }

    /**
     * @return the Json-ld Url
     * @throws MissingJsonldUrlException
     *   When there is no url with application/ld+json mimetype
     */
    public AS2Url getJsonldUrl() throws MissingPropertyException {
        return getObjectUrl("application/ld+json", null, new MissingJsonldUrlException());
    }

    /**
     * @return the Canonical Url
     * @throws MissingJsonUrlException
     *   When there is no url with rel = canonical and text/html mimetype
     */
    public AS2Url getJsonUrl() throws MissingPropertyException {
        return getObjectUrl("application/json", null, new MissingJsonUrlException());
    }

    /**
     * @return the Canonical Url
     * @throws MissingCanonicalUrlException
     *   When there is no url with rel = canonical and text/html mimetype
     */
    public AS2Url getCanonicalUrl() throws MissingPropertyException {
        return getObjectUrl(null, "canonical", new MissingCanonicalUrlException());
    }

    /**
     * @return the Canonical Url
     * @throws MissingDescribesUrlException
     *   When there is no url with rel = describes
     */
    public AS2Url getDescribesUrl() throws MissingPropertyException {
        return getObjectUrl(null, "describes", new MissingDescribesUrlException());
    }

    /**
     * Utility to filter AS2Urls, filters by mimetype or rel or both
     * @param mimetype
     *   The mimetype to filter on or null for none.
     * @param rel
     *   The rel to filter on or null for none.
     * @param e
     *   The exception to throw if we can't find a matching url
     * @return
     *   The first matching AS2Url.
     * @throws MissingPropertyException
     *   If no matching url can be found.
     */
    private AS2Url getObjectUrl(final String mimetype, final String rel, final MissingPropertyException e) throws
            MissingPropertyException {
        if (url == null) {
            throw e;
        }
        final var filterUrl = Arrays.stream(url).filter(a -> {
            if (mimetype != null && (a.getMediaType() == null || !a.getMediaType().equalsIgnoreCase(mimetype))) {
                return false;
            }
            if (rel != null) {
                return a.getRel() != null && a.getRel().equalsIgnoreCase(rel);
            }
            return true;
        }).findFirst().orElse(null);
        if (filterUrl == null) {
            throw e;
        }
        return filterUrl;
    }
}
