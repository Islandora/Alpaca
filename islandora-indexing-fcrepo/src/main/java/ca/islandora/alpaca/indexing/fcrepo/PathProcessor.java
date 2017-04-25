package ca.islandora.alpaca.indexing.fcrepo;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;

public class PathProcessor implements Processor {

    public String getDrupalBaseUrl() {
        return drupalBaseUrl;
    }

    public void setDrupalBaseUrl(String drupalBaseUrl) {
        this.drupalBaseUrl = drupalBaseUrl;
    }

    public String getMillinerBaseUrl() {
        return millinerBaseUrl;
    }

    public void setMillinerBaseUrl(String millinerBaseUrl) {
        this.millinerBaseUrl = millinerBaseUrl;
    }

    @PropertyInject("drupal.baseUrl")
    private String drupalBaseUrl;

    @PropertyInject("milliner.baseUrl")
    private String millinerBaseUrl;

    public void process(Exchange exchange) throws Exception {
        AS2Event event = exchange.getIn().getBody(AS2Event.class);
        String uri = event.getObject();

        drupalBaseUrl = addTrailingSlash(drupalBaseUrl);
        millinerBaseUrl = addTrailingSlash(millinerBaseUrl);

        String path = uri.substring(uri.lastIndexOf(drupalBaseUrl) + drupalBaseUrl.length());
        exchange.setProperty("destination", millinerBaseUrl + path);

        exchange.getIn().removeHeaders("*");
        exchange.getIn().setBody(null);
    }

    private String addTrailingSlash(String str) {
        str = str.trim();
        if (!str.endsWith("/")) {
            return str + "/";
        }
        return str;
    }
}
