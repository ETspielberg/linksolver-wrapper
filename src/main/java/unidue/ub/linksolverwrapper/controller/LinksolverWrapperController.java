/*
 * major help from https://stackoverflow.com/a/41482123/9006787
 * doi regexps from crossref-blog: https://www.crossref.org/blog/dois-and-matching-regular-expressions/
 */
package unidue.ub.linksolverwrapper.controller;

import feign.FeignException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import unidue.ub.linksolverwrapper.client.UnpaywallClient;
import unidue.ub.linksolverwrapper.model.Unpaywall;
import unidue.ub.linksolverwrapper.model.UnpaywallResponse;
import unidue.ub.linksolverwrapper.utils.RedirectLinkRetriever;
import unidue.ub.linksolverwrapper.utils.ShibbolethBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static unidue.ub.linksolverwrapper.utils.Utilities.mapListToString;
import static unidue.ub.linksolverwrapper.utils.Utilities.mapToString;

@Controller
public class LinksolverWrapperController {

    // the ShibbolethBuilder, autowired by the constructor
    private final ShibbolethBuilder shibbolethBuilder;

    // the Unpaywall Feign client, autowired by the constructor
    private final UnpaywallClient unpaywallClient;

    private final static Logger log = LoggerFactory.getLogger(LinksolverWrapperController.class);

    private boolean isDoiUrl;

    // the address of the linksolver retrieved from the properties (available at the config server)
    @Value("${libintel.linksolver.url}")
    private String linksolverUrl;

    // the email which will be attached to the unpaywall requests (available at the config server)
    @Value("${libintel.unpaywall.email}")
    private String email;

    // include the Shibboleth WAYFLless URL Builder and the Unpaywall Feign client
    @Autowired
    public LinksolverWrapperController(ShibbolethBuilder shibbolethBuilder, UnpaywallClient unpaywallClient) {
        this.shibbolethBuilder = shibbolethBuilder;
        this.unpaywallClient = unpaywallClient;
    }

    /**
     * reads the extensive request parameters for the OpenURL, retrieves the resource URL and forwards the request
     * to the resource
     *
     * @param requestParams the OpenURL parameters
     * @return the redirect to the resource location
     */
    @GetMapping("/resolve")
    public RedirectView resolve(@RequestParam MultiValueMap<String, String> requestParams, HttpServletRequest httpServletRequest) {

        // in case of empty issn and given eissn, add eissn value as issn parameter to request parameter map.
        if (requestParams.get("eissn") != null && !requestParams.get("eissn").isEmpty()) {
            if (requestParams.get("issn") == null || requestParams.get("issn").isEmpty()) {
                log.debug("eissn given, but issn is empty. adding issn with eissn value");
                requestParams.add("issn", requestParams.get("eissn").get(0));
            }
        }

        // read the referrer url from the request and extract the host address. If none is present, set the referer to 'linksolver'
        String referer = "linksolver";
        if (httpServletRequest.getHeader("referer") != null && !httpServletRequest.getHeader("referer").isEmpty()) {
            referer = httpServletRequest.getHeader("referer");
            log.debug("referer request header: " + referer);
            try {
                URI uri = new URI(referer);
                referer = URLEncoder.encode(uri.getHost(), StandardCharsets.UTF_8);
            } catch (URISyntaxException e) {
                log.warn("could not decode uri from referrer", e);
            }

        }
        log.info("referred from " + referer);

        // read the the remoteaddress from  the request. If none is present set it to 127.0.0.1.
        String remoteAddress = "127.0.0.1";
        if (httpServletRequest.getHeader("remoteAddress") != null)
            remoteAddress = httpServletRequest.getHeader("remoteAddress");
        log.info("call from " + remoteAddress);

        // prepare and initalize other variables
        RedirectView redirectView = new RedirectView();
        String urlFromDoi = "";
        String urlFromLinksolver;
        String doi = "";

        // first, check for DOI
        if (requestParams.containsKey("id")) {
            log.debug("reading id parameters from request");
            List<String> ids = requestParams.get("id");
            log.debug("found " + ids.size() + " id parameters");
            for (Object id : ids) {
                String value = (String) id;
                if (isDoi(value)) {
                    log.debug(value + " identified as doi");

                    // remove 'doi:' given in the request parameter
                    doi = value.replace("doi:", "");

                    // ask doi resolver for redirect url
                    urlFromDoi = RedirectLinkRetriever.getLinkForDoi(value);
                    log.info("retrieved link from DOI: " + urlFromDoi);

                    this.isDoiUrl = !urlFromDoi.isEmpty();

                    if (this.isDoiUrl) {
                        log.debug("querying unpaywall for OA status");
                        String freeUrl = checkUnpaywall(doi);

                        // if a free full text url is returned, redirect directly to the resource.
                        if (freeUrl != null) {
                            redirectView.setUrl(freeUrl);
                            return redirectView;
                        }
                    }

                    // if no free full text url is found, set the doi url as redirect link
                    redirectView.setUrl(urlFromDoi);
                }
            }
        }

        // retrieve availability information from linksolver
        try {
            String queryParameters = mapListToString(requestParams);
            log.debug("getting response from linksolver");

            // use Jsoup to retrieve parameters from html response
            Document doc = Jsoup.connect(linksolverUrl + queryParameters).timeout(60 * 1000).get();
            log.debug("found " + doc.select("a").size() + " links in linksolver response");

            for (Element link : doc.select("a")) {
                String linkType = link.text();
                log.debug("linksolver returned option " + linkType);

                // clean up the different targets 'Volltexte über XXX'
                if (linkType.startsWith("Volltexte "))
                    linkType = linkType.substring(0, 9);

                switch (linkType) {
                    // full text is online available, redirect directly to resource, construct WAYFless URL on the fly
                    case "Link zum Artikel": {
                        urlFromLinksolver = RedirectLinkRetriever.getLinkFromRedirect(linksolverUrl + link.attr("href"));
                        log.debug("retrieved link from linksolver: " + urlFromLinksolver);
                        log.info("full text available. Redirecting to resource.");
                        // check for shibboleth
                        String url = getShibbolethUrl(urlFromDoi, urlFromLinksolver, remoteAddress);
                        // redirect to url
                        redirectView.setUrl(url);
                        return redirectView;
                    }
                    // only interlibrary loan is available. check for specific conditions (elsevier).
                    // If elsevier or science direct is present, redirect to order page and fill doi and source parameters.
                    // Otherwise redirect to the interlibrary loan page and fill in needed request params for the Fernleihe.
                    case "Fernleihe Zeitschriften": {
                        if (urlFromDoi.contains("sciencedirect") || urlFromDoi.contains("elsevier")) {
                            log.debug("no fulltext available and elsevier journal. redirecting to order page.");
                            redirectView.setUrl("https://www.uni-due.de/ub/elsevierersatz.php?doi=" + doi + "&source=" + referer);
                        } else {
                            log.debug("no fulltext available. redirecting to interlibrary loan page");
                            requestParams.set("sid", "464_465:Zeitschriftenkatalog");
                            requestParams.set("pid", "<location>464_465<%2Flocation>");
                            requestParams.set("genre", "journal");
                            redirectView.setUrl("https://www.digibib.net/openurl" + mapListToString(requestParams));
                        }
                        return redirectView;
                    }
                    // if a target specially designed for Elsevier is present, also direct to the order page
                    case "Elsevier Zeitschriften - Link zum Bestellformular": {
                        log.debug("no fulltext available and elsevier journal. redirecting to order page.");
                        redirectView.setUrl("https://www.uni-due.de/ub/elsevierersatz.php?doi=" + doi + "&source=" + referer);
                    }
                    // printed or online media are available but linksolver does not return URL.
                    // In this case redirect to journal online and print page (JOP-Button)
                    case "Elektronischer und gedruckter Bestand der UB":
                    case "zur Zeitschrift": {
                        log.debug("printed or online access without resource url. redirecting to journals online and print page.");
                        String issn = requestParams.getFirst("issn").trim();
                        if (issn != null && issn.isEmpty())
                            issn = requestParams.getFirst("eissn").trim();
                        if (issn != null && !issn.isEmpty()) {

                            // the jop api needs the issn with a '-' in the middle. so insert one, if none is present.
                            if (!issn.contains("-") && issn.length() == 8)
                                issn = issn.substring(0, 4) + "-" + issn.substring(4);

                            // fill in necessary paramteters for iop
                            Map<String, String> iopRequestParams = new HashMap<>();
                            iopRequestParams.put("sid", "bib:ughe");
                            iopRequestParams.put("pid", "bibid%3DUGHE");
                            iopRequestParams.put("genre", "journal");
                            iopRequestParams.put("issn", issn);

                            String url = "https://www.uni-due.de/ub/ghbsys/jop" + mapToString(iopRequestParams);
                            redirectView.setUrl(url);
                        } else
                            // if no issn is given, redirect to the linksolver
                            redirectView.setUrl(linksolverUrl + queryParameters);
                        return redirectView;
                    }
                    // applicable for ebooks where full-text is available.
                    // Redirect to resource and construct WAYFless URL if necessary
                    case "Volltexte": {
                        urlFromLinksolver = RedirectLinkRetriever.getLinkFromRedirect(linksolverUrl + link.attr("href"));
                        log.debug("retrieved link from linksolver: " + urlFromLinksolver);
                        log.info("full text available. Redirecting to resource.");
                        // check for shibboleth
                        String url = getShibbolethUrl(urlFromDoi, urlFromLinksolver, remoteAddress);

                        // redirect to url
                        redirectView.setUrl(url);
                        return redirectView;
                    }
                    default: {
                        redirectView.setUrl(linksolverUrl + queryParameters);
                    }
                }
            }
        }
        // if any errors occur when trying to connect to linkresolver or doi resolver send error.
        catch (IOException e) {
            log.warn("encountered IO exception", e);
            redirectView.setUrl("/error");
            return redirectView;
        }
        log.info("redirecting to " + redirectView.getUrl());
        return redirectView;
    }

    /**
     * takes the urls from linksolver and doi resolver and constructs a WAYFless URL.
     * If present, the doi link is preferred.
     *
     * @param urlFromDoi        the url returned from the doi resolver
     * @param urlFromLinksolver the url returned from the linksolver
     * @param remoteAddress     the remote address from the request
     * @return the url string to redirect to
     */
    private String getShibbolethUrl(String urlFromDoi, String urlFromLinksolver, String remoteAddress) {
        String url;
        if (this.isDoiUrl) {
            log.debug("trying to construct shibboleth link with doi link.");
            url = shibbolethBuilder.constructWayflessUrl(urlFromDoi, remoteAddress);
        } else {
            log.debug("trying to construct shibboleth link with  linksolver link.");
            url = shibbolethBuilder.constructWayflessUrl(urlFromLinksolver, remoteAddress);
        }
        return url;
    }

    /**
     * general endpoint to construct WAYFless urls. receiving a target URL a WAYFless URL is constructed if possible
     * and a redirect is issued.
     *
     * @param target  the original target url
     * @param request the http request object
     * @return a redirect to the WAYFless URL for this target
     */
    @GetMapping("/useShibboleth")
    public RedirectView useShibboleth(@RequestParam String target, HttpServletRequest request) {
        RedirectView redirectView = new RedirectView();
        String url = shibbolethBuilder.constructWayflessUrl(target, request.getRemoteAddr());
        redirectView.setUrl(url);
        return redirectView;
    }

    /**
     * checks whether the test string is a doi starting with: 'doi:'
     *
     * @param test the string to be tested
     * @return true, if the string contains a doi
     */
    private boolean isDoi(String test) {
        String oldWileyDoiRegExp = "^doi:10.1002/[\\S]+$";
        String modernDoiRegExp = "^doi:10.\\d{4,9}/[-._;()/:A-Za-z0-9]+$";
        return (test.matches(modernDoiRegExp) || test.matches(oldWileyDoiRegExp));
    }

    /**
     * checks unpaywall for the given doi to check for open access publications
     *
     * @param doi the doi of the requested publication
     * @return the url to the fulltext if present, otherwise null
     */
    private String checkUnpaywall(String doi) {
        try {
            // execute feign client for unpaywall response. If no data are found, unpaywall returns 404 resulting in
            // a FeignException.
            UnpaywallResponse unpaywallResponse = this.unpaywallClient.getUnpaywallData(doi, email);

            // in the results block the indiviudal Feign results are given as list.
            Unpaywall[] unpaywalls = unpaywallResponse.getResults();
            if (unpaywalls != null && unpaywalls.length > 0) {
                // cycle through all results. as soon as a free fulltext url is found, return this value.
                for (Unpaywall unpaywall : unpaywalls) {
                    log.debug("is free to read: " + unpaywall.isFreeToRead());
                    if (unpaywall.isFreeToRead()) {
                        log.info("requested resource is listed as open access");
                        if (unpaywall.getFreeFulltextUrl() != null && !unpaywall.getFreeFulltextUrl().isEmpty()) {
                            log.debug("redirecting to free fulltext url " + unpaywall.getFreeFulltextUrl());
                            return (unpaywall.getFreeFulltextUrl());
                        }
                    }
                }
            }
            // if no free full text url is found return null
            return null;
        } catch (FeignException fe) {
            // if no data are found on Unpaywall, return null as well
            log.warn("could not retrieve unpaywall data. ", fe);
            return null;
        }
    }
}
