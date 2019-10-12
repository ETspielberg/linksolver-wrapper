package org.unidue.ub.libintel.linksolverwrapper.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.unidue.ub.libintel.linksolverwrapper.model.ShibbolethData;
import org.unidue.ub.libintel.linksolverwrapper.repository.ShibbolethDataRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ShibbolethBuilder {

    private final ShibbolethDataRepository shibbolethDataRepository;

    // address of the identity provider shibboleth endpoint
    @Value("${libintel.shibboleth.idp.url}")
    private String idpUrl;

    // entity id of the institution
    @Value("${libintel.shibboleth.entity.id}")
    private String entityId;

    // the addresses to be excluded from the shibboleth builder
    @Value("${libintel.shibboleth.exclusion}")
    private String[] shiboblethFreeSubnet;

    private Logger log = LoggerFactory.getLogger(ShibbolethBuilder.class);

    @Autowired
    public ShibbolethBuilder(ShibbolethDataRepository shibbolethDataRepository) {
        this.shibbolethDataRepository = shibbolethDataRepository;
    }

    /**
     * Takes an URL and checks the database to build the corresponding WAYFless URL. If the IP address of the requestor
     * is within the excluded IP range or no Shibboleth data are found, the original URL is returned.
     * @param urlString The string for the desired resource
     * @return the WAYFless URL to the resource
     */
    public String constructWayflessUrl(String urlString, String ipAddress) {
        if (!matches(ipAddress)) {
            log.debug("constructing wayfless url");
            try {

                // get host for database checking
                URL url = new URL(urlString);
                String host = url.getHost();
                log.debug("retrieving data for host \"" + host + "\"");
                Optional<ShibbolethData> shibbolethDataMayBe = shibbolethDataRepository.findById(host);

                // if shibboleth data are found in the database, try to build the corresponding wayfless URL
                if (shibbolethDataMayBe.isPresent()) {
                    log.debug("found shibboleth data");
                    ShibbolethData shibbolethData = shibbolethDataMayBe.get();
                    Map<String, String> parameters = new HashMap<>();
                    String shibboleth_url = "";
                    // building the URL for SP-side WAYFless
                    if (shibbolethData.isSpSideWayfless()) {
                        parameters.put(shibbolethData.getEntityIdString(), entityId);
                        parameters.put(shibbolethData.getTargetString(), urlString);
                        log.debug("generated SP-side WAYFLESS-URL");
                        log.debug(shibbolethData.getServiceproviderSibbolethUrl() + Utilities.mapToString(parameters));
                        log.info("In IP-Range: false, Shibboleth-Daten: true, type: SP-side");
                        shibboleth_url = shibbolethData.getServiceproviderSibbolethUrl() + Utilities.mapToString(parameters);


                    }
                    // building the URL for IP-side WAYFless
                    else {
                        parameters.put("target", urlString);
                        parameters.put("shire", shibbolethData.getShire());
                        parameters.put("providerId", shibbolethData.getProviderId());
                        log.debug("generated IP-side WAYFLESS-URL");
                        log.debug(idpUrl + Utilities.mapToString(parameters));
                        log.info("In IP-Range: false, Shibboleth-Daten: true, type: IP-side");
                        shibboleth_url = idpUrl + Utilities.mapToString(parameters);

                    }
                    if (shibbolethData.getAdditionalUrlParameters() != null) {
                        shibboleth_url += shibbolethData.getAdditionalUrlParameters();
                    }
                    return shibboleth_url;

                } else {
                    log.info("In IP-Range: false, Shibboleth-Daten: false, type: none");
                    return urlString;
                }
            } catch (MalformedURLException mue) {
                log.info("In IP-Range: false, Shibboleth-Daten: false, type: error");
                log.debug("given url is malformed, returning original URL");
                log.debug(urlString);
                return urlString;
            }
        }
        else
            log.info("In IP-Range: true, Shibboleth-Daten: none, type: none");
        return urlString;
    }

    private boolean matches(String ip) {
        for (String exclusion: shiboblethFreeSubnet) {
            IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(exclusion);
            if (ipAddressMatcher.matches(ip))
                return true;
        }
        return false;
    }
}
