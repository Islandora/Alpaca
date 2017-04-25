package ca.islandora.alpaca.indexing.fcrepo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AS2Event {

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public AS2Actor getActor() {
        return actor;
    }

    public void setActor(AS2Actor actor) {
        this.actor = actor;
    }

    @JsonProperty(value="@context")
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    private String context;
    private String type;
    private String object;
    private AS2Actor actor;

}
