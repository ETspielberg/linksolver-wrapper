package unidue.ub.linksolverwrapper.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * POJO containing the data for constructing WAYFless URLs
 */
@Entity
@Table(name="shibboleth_data")
public class ShibbolethData {

    @Id
    private String host;

    @Column(name="serviceprovider_sibboleth_url")
    private String serviceproviderSibbolethUrl;

    @Column(name="sp_side_wayfless")
    private boolean spSideWayfless = false;

    @Column(name="entity_id_string")
    private String entityIdString = "entityID";

    @Column(name="target_string")
    private String targetString = "target";

    private String shire;

    @Column(name="provider_id")
    private String providerId;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getServiceproviderSibbolethUrl() {
        return serviceproviderSibbolethUrl;
    }

    public void setServiceproviderSibbolethUrl(String serviceproviderSibbolethUrl) {
        this.serviceproviderSibbolethUrl = serviceproviderSibbolethUrl;
    }

    public boolean isSpSideWayfless() {
        return spSideWayfless;
    }

    public void setSpSideWayfless(boolean spSideWayfless) {
        this.spSideWayfless = spSideWayfless;
    }

    public String getEntityIdString() {
        return entityIdString;
    }

    public void setEntityIdString(String entityIdString) {
        this.entityIdString = entityIdString;
    }

    public String getTargetString() {
        return targetString;
    }

    public void setTargetString(String targetString) {
        this.targetString = targetString;
    }

    public String getShire() {
        return shire;
    }

    public void setShire(String shire) {
        this.shire = shire;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
