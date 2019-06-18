package unidue.ub.linksolverwrapper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnpaywallResponse {

    private Unpaywall[] results;

    public Unpaywall[] getResults() {
        return results;
    }

    public void setResults(Unpaywall[] results) {
        this.results = results;
    }
}
