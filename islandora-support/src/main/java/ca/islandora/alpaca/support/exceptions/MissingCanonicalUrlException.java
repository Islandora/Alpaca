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
package ca.islandora.alpaca.support.exceptions;

/**
 * Exception if there is no canonical URL in the message.
 * @author whikloj
 */
public class MissingCanonicalUrlException extends MissingPropertyException {
    /**
     * Basic constructor
     */
    public MissingCanonicalUrlException() {
        super("Cannot find canonical URL in event message.");
    }
}
