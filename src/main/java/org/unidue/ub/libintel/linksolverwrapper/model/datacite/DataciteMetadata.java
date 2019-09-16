package org.unidue.ub.libintel.linksolverwrapper.model.datacite;

import java.util.Date;
import java.util.List;

public class DataciteMetadata {

    private String doi;

    private String descrioption;

    private String licence;

    private String title;

    private String language;

    private List<String> kexwords;

    private Date publicationDate;

    private List<DataciteCreator> creator;

    private String accessRight;

    private DataciteResourceType resourceType;

    private DataciteRelatedIdentifiers relatedIdentifiers;

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getDescrioption() {
        return descrioption;
    }

    public void setDescrioption(String descrioption) {
        this.descrioption = descrioption;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getKexwords() {
        return kexwords;
    }

    public void setKexwords(List<String> kexwords) {
        this.kexwords = kexwords;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public List<DataciteCreator> getCreator() {
        return creator;
    }

    public void setCreator(List<DataciteCreator> creator) {
        this.creator = creator;
    }

    public String getAccessRight() {
        return accessRight;
    }

    public void setAccessRight(String accessRight) {
        this.accessRight = accessRight;
    }

    public DataciteResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(DataciteResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public DataciteRelatedIdentifiers getRelatedIdentifiers() {
        return relatedIdentifiers;
    }

    public void setRelatedIdentifiers(DataciteRelatedIdentifiers relatedIdentifiers) {
        this.relatedIdentifiers = relatedIdentifiers;
    }
}
