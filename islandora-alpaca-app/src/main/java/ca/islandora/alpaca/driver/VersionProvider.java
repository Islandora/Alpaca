package ca.islandora.alpaca.driver;/*
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import picocli.CommandLine;

/**
 * Provides the current version of Alpaca to picocli
 * @author whikloj
 */
public class VersionProvider implements CommandLine.IVersionProvider {

    /**
     * Name of the file to locate the version in.
     */
    private static final String VERSION_FILENAME = "alpaca.properties";

    @Override
    public String[] getVersion() throws Exception {
        final var filestream = getClass().getClassLoader().getResourceAsStream(VERSION_FILENAME);
        final String version = new BufferedReader(
                new InputStreamReader(filestream, StandardCharsets.UTF_8))
                .lines()
                .filter(a -> a.startsWith("version"))
                .map(a -> a.split("=")[1])
                .map(String::trim)
                .findAny().orElse("0");
        return new String[] {version};
    }
}
