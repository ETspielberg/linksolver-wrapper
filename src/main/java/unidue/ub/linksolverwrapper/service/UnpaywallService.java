package unidue.ub.linksolverwrapper.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import unidue.ub.linksolverwrapper.client.UnpaywallClient;
import unidue.ub.linksolverwrapper.model.Unpaywall;
import unidue.ub.linksolverwrapper.model.UnpaywallResponse;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class UnpaywallService {


    // the email which will be attached to the unpaywall requests (available at the config server)
    @Value("${libintel.unpaywall.email}")
    private String email;

    private UnpaywallClient unpaywallClient;

    private UnpaywallResponse unpaywallResponse;

    private static Logger log = LoggerFactory.getLogger(UnpaywallService.class);

    public UnpaywallService(UnpaywallClient unpaywallClient) {
        this.unpaywallClient = unpaywallClient;
    }

    /**
     * checks unpaywall for the given doi to check for open access publications
     *
     * @param doi the doi of the requested publication
     * @return the url to the fulltext if present, otherwise null
     */
    public String checkUnpaywall(String doi, String doiHost) {
        try {
            // execute feign client for unpaywall response. If no data are found, unpaywall returns 404 resulting in
            // a FeignException.
            this.unpaywallResponse = this.unpaywallClient.getUnpaywallData(doi, email);
            String url = getPreferredUrlForHost(doiHost);
            if (url == null)
                url = getFirstUrl();
            return url;
        } catch (FeignException fe) {
            // if no data are found on Unpaywall, return null as well
            log.info("no Unpaywalld data available");
            log.debug("could not retrieve unpaywall data. ", fe);
            return null;
        }
    }

    private String getPreferredUrlForHost(String host) {
        if (this.unpaywallResponse.getResults() != null && this.unpaywallResponse.getResults().length > 0) {
            for (Unpaywall unpaywall : this.unpaywallResponse.getResults()) {
                if (unpaywall.isFreeToRead()) {
                    if (unpaywall.getFreeFulltextUrl() != null && !unpaywall.getFreeFulltextUrl().isEmpty()) {
                        String url = unpaywall.getFreeFulltextUrl();
                        if (url.isEmpty())
                            continue;
                        try {
                            URI uri = new URI(unpaywall.getFreeFulltextUrl());
                            if (uri.getHost().equals(host))
                                return url;
                        } catch (URISyntaxException use) {
                            use.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getFirstUrl() {
        if (this.unpaywallResponse.getResults() != null && this.unpaywallResponse.getResults().length > 0) {
            // cycle through all results. as soon as a free fulltext url is found, return this value.
            for (Unpaywall unpaywall : unpaywallResponse.getResults()) {
                if (unpaywall.isFreeToRead()) {
                    if (unpaywall.getFreeFulltextUrl() != null && !unpaywall.getFreeFulltextUrl().isEmpty()) {
                        return (unpaywall.getFreeFulltextUrl());
                    }
                }
            }
        }
        return null;
    }
}
