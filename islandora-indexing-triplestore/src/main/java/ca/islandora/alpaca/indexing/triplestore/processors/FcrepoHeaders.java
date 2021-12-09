/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.islandora.alpaca.indexing.triplestore.processors;

/**
 * @author acoburn
 *
 * @author whikloj
 *   Copied from fcrepo-camel and modified for use in Alpaca - 2021-10-13
 */
public final class FcrepoHeaders {

    public static final String FCREPO_BASE_URL = "CamelFcrepoBaseUrl";

    public static final String FCREPO_IDENTIFIER = "CamelFcrepoIdentifier";

    public static final String FCREPO_NAMED_GRAPH = "CamelFcrepoNamedGraph";

    public static final String FCREPO_URI = "CamelFcrepoUri";

    private FcrepoHeaders() {
        // prevent instantiation
    }
}
