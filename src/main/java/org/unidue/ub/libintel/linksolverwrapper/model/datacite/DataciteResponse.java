package org.unidue.ub.libintel.linksolverwrapper.model.datacite;

public class DataciteResponse {

    private String doi;

    private DataciteMetadata metadata;

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public DataciteMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DataciteMetadata metadata) {
        this.metadata = metadata;
    }
}
