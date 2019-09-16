package org.unidue.ub.libintel.linksolverwrapper.model.datacite;

public class DataciteRelatedIdentifiers {

    public String scheme;

    private String relation;

    private String identifier;

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
