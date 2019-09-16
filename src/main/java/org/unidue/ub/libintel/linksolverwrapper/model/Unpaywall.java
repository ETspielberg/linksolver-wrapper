package org.unidue.ub.libintel.linksolverwrapper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Unpaywall {

    private String doi;

    @JsonProperty("doi_resolver")
    private String doiResolver;

    private String evidence;

    @JsonProperty("free_fulltext_url")
    private String freeFulltextUrl;

    @JsonProperty("is_boai_license")
    private boolean isBoaiLicense;

    @JsonProperty("is_free_to_read")
    private boolean isFreeToRead;

    @JsonProperty("is_subscription_journal")
    private boolean isSubscritpionJournal;

    private String licence;

    @JsonProperty("oa_color")
    private String oaColor;

    @JsonProperty("reported_noncompliant_copies")
    private String[] reportedNoncompliantCopies;

    private String title;

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getDoiResolver() {
        return doiResolver;
    }

    public void setDoiResolver(String doiResolver) {
        this.doiResolver = doiResolver;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getFreeFulltextUrl() {
        return freeFulltextUrl;
    }

    public void setFreeFulltextUrl(String freeFulltextUrl) {
        this.freeFulltextUrl = freeFulltextUrl;
    }

    public boolean isBoaiLicense() {
        return isBoaiLicense;
    }

    public void setBoaiLicense(boolean boaiLicense) {
        isBoaiLicense = boaiLicense;
    }

    public boolean isFreeToRead() {
        return isFreeToRead;
    }

    public void setFreeToRead(boolean freeToRead) {
        isFreeToRead = freeToRead;
    }

    public boolean isSubscritpionJournal() {
        return isSubscritpionJournal;
    }

    public void setSubscritpionJournal(boolean subscritpionJournal) {
        isSubscritpionJournal = subscritpionJournal;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public String getOaColor() {
        return oaColor;
    }

    public void setOaColor(String oaColor) {
        this.oaColor = oaColor;
    }

    public String[] getReportedNoncompliantCopies() {
        return reportedNoncompliantCopies;
    }

    public void setReportedNoncompliantCopies(String[] reportedNoncompliantCopies) {
        this.reportedNoncompliantCopies = reportedNoncompliantCopies;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
