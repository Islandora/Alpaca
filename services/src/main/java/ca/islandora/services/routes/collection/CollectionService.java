/**
 * 
 */
package ca.islandora.services.routes.collection;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

/**
 * Routes exposed by the CollectionEndpoint.
 * 
 * @author danny
 */
public class CollectionService extends RouteBuilder {

    /* (non-Javadoc)
     * @see org.apache.camel.builder.RouteBuilder#configure()
     */
    @Override
    public void configure() throws Exception {
        
        from("direct:derp")
            .transform().simple("DERPADOODOO");
        
        from("direct:createCollection")
            .description("Creates a Collection node in Fedora from a Drupal node.")
            .beanRef("collectionServiceProcessor", "deserializeNode")
            .beanRef("collectionServiceProcessor", "constructSparql")
            .log("SPARQL: ${body}")
            .to("fcrepo:{{fcrepo.baseurl}}")
            .log("RESULTS: ${body}");
//            .transacted()
//            .beanRef("collectionServiceProcessor", "processForDrupalPOST")
//            .recipientList(simple("http4:{{drupal.baseurl}}/node/$simple{property.collectionUUID}"))
//            .beanRef("collectionServiceProcessor", "processForFedoraPOST")
//            .to("seda:toFedora")
//            .beanRef("collectionServiceProcessor", "processForHibernatePOST")
//            .to("hibernate:ca.islandora.services.uuid.UUIDMap")
//            .marshal().json(JsonLibrary.Jackson);
        
        from("seda:toFedora")
            .description("Ducks out of a transacted thread for Fedora calls so TransactionManagers don't collide.")
            .to("fcrepo:{{fcrepo.baseurl}}");
    }

}
