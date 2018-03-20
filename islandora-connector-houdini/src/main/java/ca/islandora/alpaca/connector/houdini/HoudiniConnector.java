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

package ca.islandora.alpaca.connector.houdini;

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.slf4j.LoggerFactory.getLogger;

import ca.islandora.alpaca.connector.houdini.event.AS2Event;
import ca.islandora.alpaca.connector.houdini.event.AS2Url;
import ca.islandora.alpaca.connector.houdini.event.AS2AttachmentContent;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

/**
 * @author dhlamb
 */
public class HoudiniConnector extends RouteBuilder {

    private static final Logger LOGGER = getLogger(HoudiniConnector.class);

    @Override
    public void configure() {
        // Global exception handler for the indexer.
        // Just logs after retrying X number of times.
        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log(
                ERROR,
                LOGGER,
                "Error connecting ${property.uri} to Houdini: ${exception.message}\n\n${exception.stacktrace}"
            );

        from("{{in.stream}}")
            .routeId("IslandoraConnectorHoudini")
              // Parse the event into a POJO.
              .unmarshal().json(JsonLibrary.Jackson, AS2Event.class)

              // Stash the event on the exchange.
              // Get the node url out of the event.
              .process(ex -> {
                  final AS2Event event = ex.getIn().getBody(AS2Event.class);
                  ex.setProperty("event", event);
                  final AS2Url[] urls = event.getObject().getUrl();
                  for (AS2Url url : urls) {
                      if (url.getRel().equals("canonical")) {
                          ex.setProperty("nodeUrl", url.getHref());
                          break;
                      }
                  }
              })

              // Make a HEAD request for the Node
              .removeHeaders("*", "Authorization")
              .setHeader(Exchange.HTTP_METHOD, constant("HEAD"))
              .transform(simple("${null}"))
              .toD("${exchangeProperty.nodeUrl}")

              // Extract/construct the urls for the derivative.
              .process(ex -> {
                  final String[] links = ex.getIn().getHeader("Link", String[].class);
                  final AS2AttachmentContent content = ex.getProperty("event", AS2Event.class).getAttachment().getContent();
                  final String source = content.getSource();
                  LOGGER.info("SOURCE: " + source);
                  final String destination = content.getDestination();
                  LOGGER.info("DESTINATION: " + destination);

                  regexSearchAndSetProperty(
                      "<(.*)>.*field=\"" + source + "\"",
                      links,
                      "sourceUrl",
                      ex
                  );

                  regexSearchAndSetProperty(
                      "<(.*)>.*field=\"" + destination + "\"",
                      links,
                      "destinationUrl",
                      ex
                  );

                  if (ex.getProperty("destinationUrl") == null) {
                    final String bundle = content.getBundle();
                    final String nodeUrl = ex.getProperty("nodeUrl", String.class);
                    ex.setProperty("addUrl", nodeUrl + "/media/" + destination + "/add/" + bundle);
                  }
              })
              .log(INFO, LOGGER, "SOURCE URL: ${exchangeProperty.sourceUrl}")
              .log(INFO, LOGGER, "DESTINATION URL: ${exchangeProperty.destinationUrl}")
              .log(INFO, LOGGER, "ADD URL: ${exchangeProperty.addUrl}")

              // Route to either adding or updating the derivative.
              .choice()
                  .when(exchangeProperty("destinationUrl")).to("direct:updateDerivative")
                  .when(exchangeProperty("addUrl")).to("direct:addDerivative")
              .end();

        from("direct:generateDerivative")
            .routeId("IslandoraConnectorHoudiniGenerateDerivative")

            // Perform a HEAD request for the source media.
            .removeHeaders("*", "Authorization")
            .setHeader(Exchange.HTTP_METHOD, constant("HEAD"))
            .transform(simple("${null}"))
            .toD("${exchangeProperty.sourceUrl}")

            // Extract the file URI from returned link headers.
            .process(ex -> {
                regexSearchAndSetProperty(
                    "<(.*)>.*rel=\"describes\"",
                    ex.getIn().getHeader("Link", String[].class),
                    "fileUrl",
                    ex
                );
            })

            // Make the Crayfish request using the file uri.
            .removeHeaders("*", "Authorization")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .setHeader("Accept", simple("${exchangeProperty.event.attachment.content.mimetype}"))
            .setHeader("X-Islandora-Args", simple("${exchangeProperty.event.attachment.content.args}"))
            .setHeader("Apix-Ldp-Resource", simple("${exchangeProperty.fileUrl}"))
            .transform(simple("${null}"))
            .to("{{houdini.convert.url}}");

        from("direct:updateDerivative")
            .routeId("IslandoraConnectorHoudiniUpdateDerivative")

            // Make a HEAD request for the destination Media.
            .removeHeaders("*", "Authorization")
            .setHeader(Exchange.HTTP_METHOD, constant("HEAD"))
            .transform(simple("${null}"))
            .toD("${exchangeProperty.destinationUrl}")

            // Extract the edit-media URL from returned link headers.
            .process(ex -> {
                regexSearchAndSetProperty(
                    "<(.*)>.*rel=\"edit-media\"",
                    ex.getIn().getHeader("Link", String[].class),
                    "updateUrl",
                    ex
                );
            })

            // Generate the derivative.
            .to("direct:generateDerivative")

            // Update the destination Media's contents.
            .removeHeaders("*", "Authorization", "Content-Type")
            .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
            .toD("${exchangeProperty.updateUrl}");

        from("direct:addDerivative")
            .routeId("IslandoraConnectorHoudiniAddDerivative")

            // Generate the derivative.
            .to("direct:generateDerivative")

            // Make up a filename for the upload.
            .process(ex -> {
                final String mime = ex.getIn().getHeader("Content-Type", String.class);
                final String ext = mime.split("/")[1];

                final AS2Event event = ex.getProperty("event", AS2Event.class);
                final String destination = event.getAttachment().getContent().getDestination();

                final URL url = new URL(ex.getProperty("fileUrl", String.class));
                final String name = URLDecoder.decode(FilenameUtils.getBaseName(url.getPath()));

                ex.getIn().setHeader("Content-Disposition", "attachment; filename=\"" + name + "_" + destination + "." + ext + "\"");
            })

            // Add a new Media to the Node.
            .removeHeaders("*", "Authorization", "Content-Type", "Content-Disposition")
            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
            .toD("${exchangeProperty.addUrl}");
    }

    private void regexSearchAndSetProperty(String regex, String[] links, String propertyName, Exchange exchange) {
        Pattern pattern = Pattern.compile(regex);
        for (String link : links) {
            Matcher matcher = pattern.matcher(link);
            if (matcher.find()) {
                exchange.setProperty(propertyName, matcher.group(1));
                break;
            }
        }
    }
}
