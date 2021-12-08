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
package ca.islandora.alpaca.driver;

import static ca.islandora.alpaca.support.config.PropertyConfig.ALPACA_CONFIG_PROPERTY;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import picocli.CommandLine;

/**
 * Command line application class
 * @author whikloj
 */
@CommandLine.Command(name = "alpaca", mixinStandardHelpOptions = true, sortOptions = false,
        versionProvider = VersionProvider.class)
public class AlpacaDriver implements Callable<Integer> {

    /**
     * Logger instance.
     */
    private static final Logger LOGGER = getLogger(AlpacaDriver.class);

    /**
     * Configuration file.
     */
    @CommandLine.Option(names = {"--config", "-c"}, required = false, order = 1,
            description = "The path to the configuration file")
    private Path configurationFilePath;

    @Override
    public Integer call() throws Exception {
        if (configurationFilePath != null) {
            System.setProperty(ALPACA_CONFIG_PROPERTY, configurationFilePath.toFile().getAbsolutePath());
        }
        final var appContext = new AnnotationConfigApplicationContext("ca.islandora.alpaca");
        try {
            appContext.start();
            LOGGER.info("Alpaca started.");

            while (appContext.isRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    throw new RuntimeException("This should never happen");
                }
            }
            return 0;
        } finally {
            appContext.close();
        }
    }

    /**
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        final AlpacaDriver driver = new AlpacaDriver();
        final CommandLine cmd = new CommandLine(driver);
        //cmd.setExecutionExceptionHandler(new AppExceptionHandler(driver));
        cmd.execute(args);
    }

}
