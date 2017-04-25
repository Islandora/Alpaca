package ca.islandora.alpaca.indexing.fcrepo;

public class AS2Actor {

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String type;
    private String id;

}
