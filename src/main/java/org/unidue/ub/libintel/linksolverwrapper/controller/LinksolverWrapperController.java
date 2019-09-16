/*
 * major help from https://stackoverflow.com/a/41482123/9006787
 * doi regexps from crossref-blog: https://www.crossref.org/blog/dois-and-matching-regular-expressions/
 */
package org.unidue.ub.libintel.linksolverwrapper.controller;

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
import org.unidue.ub.libintel.linksolverwrapper.service.UnpaywallService;
import org.unidue.ub.libintel.linksolverwrapper.utils.RedirectLinkRetriever;
import org.unidue.ub.libintel.linksolverwrapper.utils.ShibbolethBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.unidue.ub.libintel.linksolverwrapper.utils.Utilities.mapListToString;
import static org.unidue.ub.libintel.linksolverwrapper.utils.Utilities.mapToString;

@Controller
public class LinksolverWrapperController {

    // the ShibbolethBuilder, autowired by the constructor
    private final ShibbolethBuilder shibbolethBuilder;

    private final UnpaywallService unpaywallService;

    private final static Logger log = LoggerFactory.getLogger(LinksolverWrapperController.class);

    private boolean isDoiUrl;

    // the address of the linksolver retrieved from the properties (available at the config server)
    @Value("${libintel.linksolver.url}")
    private String linksolverUrl;


    // include the Shibboleth WAYFLless URL Builder and the Unpaywall Feign client
    @Autowired
    public LinksolverWrapperController(ShibbolethBuilder shibbolethBuilder, UnpaywallService unpaywallService) {
        this.shibbolethBuilder = shibbolethBuilder;
        this.unpaywallService = unpaywallService;
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

        this.isDoiUrl = false;


        requestParams = cleanUpRequestPrams(requestParams);

        // in case of empty issn and given eissn, add eissn value as issn parameter to request parameter map.
        requestParams = setIssnIfOnlyEissnIsGiven(requestParams);

        // read the referrer url from the request and extract the host address. If none is present, set the referer to 'linksolver'
        String referer = getReferer(httpServletRequest);

        // read the the remoteaddress from  the request. If none is present set it to 127.0.0.1.
        String remoteAddress = determineRemoteAddress(httpServletRequest);

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
                    log.debug("retrieved link from DOI: " + urlFromDoi);

                    this.isDoiUrl = !urlFromDoi.isEmpty();

                    if (this.isDoiUrl) {
                        log.debug("querying unpaywall for OA status");
                        String freeUrl = unpaywallService.checkUnpaywall(doi, urlFromDoi);

                        // if a free full text url is returned, redirect directly to the resource.
                        if (freeUrl != null) {
                            redirectView.setUrl(freeUrl);
                            log.info("OA: true, status: 'Volltext', remote: " + remoteAddress + ", referer: " + referer);
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
                    // applicable also for ebooks where full-text is available.
                    case "Link zum Artikel":
                    case "Volltexte": {
                        urlFromLinksolver = RedirectLinkRetriever.getLinkFromRedirect(linksolverUrl + link.attr("href"));
                        log.debug("retrieved link from linksolver: " + urlFromLinksolver);
                        // check for shibboleth
                        String url = getShibbolethUrl(urlFromDoi, urlFromLinksolver, remoteAddress);
                        // redirect to url
                        redirectView.setUrl(url);
                        log.info("OA: false, status: 'Volltext', remote: " + remoteAddress + ", referer: " + referer);
                        return redirectView;
                    }

                    // if a target specially designed for Elsevier is present, also direct to the order page
                    case "Elsevier Zeitschriften - Link zum Bestellformular": {
                        log.debug("no fulltext available and elsevier journal. redirecting to order page.");
                        redirectView.setUrl("https://www.uni-due.de/ub/elsevierersatz.php?doi=" + doi + "&source=" + referer);
                        log.info("OA: false, status: 'Elsevier-Bestellseite', remote: " + remoteAddress + ", referer: " + referer);
                        break;
                    }

                    // printed or online media are available but linksolver does not return URL.
                    // In this case redirect to journal online and print page (JOP-Button)
                    case "Elektronischer und gedruckter Bestand der UB":
                    case "zur Zeitschrift": {
                        log.debug("printed or online access without resource url. redirecting to journals online and print page.");
                        String issn = requestParams.getFirst("issn");
                        if (issn != null)
                            if (issn.isEmpty()) {
                                issn = requestParams.getFirst("eissn");
                                if (issn != null)
                                    issn = issn.trim();
                            } else
                                issn = issn.trim();
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
                            log.info("OA: false, status: 'JOP-Seite', remote: " + remoteAddress + ", referer: " + referer);
                        } else {
                            // if no issn is given, redirect to the linksolver
                            redirectView.setUrl(linksolverUrl + queryParameters);
                            log.info("OA: false, status: 'Linksolver (no ISSN)', remote: " + remoteAddress + ", referer: " + referer);
                        }
                        break;
                    }

                    // only interlibrary loan is available. check for specific conditions (elsevier).
                    // If elsevier or science direct is present, redirect to order page and fill doi and source parameters.
                    // Otherwise redirect to the interlibrary loan page and fill in needed request params for the Fernleihe.
                    case "Fernleihe":
                    case "Fernleihe Zeitschriften": {
                        if (urlFromDoi.contains("sciencedirect") || urlFromDoi.contains("elsevier")) {
                            log.debug("no fulltext available and elsevier journal. redirecting to order page.");
                            redirectView.setUrl("https://www.uni-due.de/ub/elsevierersatz.php?doi=" + doi + "&source=" + referer);
                            log.info("OA: false, status: 'Elsevier-Bestellseite', remote: " + remoteAddress + ", referer: " + referer);
                        } else {
                            log.debug("no fulltext available. redirecting to interlibrary loan page");
                            requestParams.set("sid", "464_465:Zeitschriftenkatalog");
                            requestParams.set("pid", "<location>464_465<%2Flocation>");
                            requestParams.set("genre", "journal");
                            redirectView.setUrl("https://www.digibib.net/openurl" + mapListToString(requestParams));
                            log.debug("redirect url: " + redirectView.getUrl());
                            log.info("OA: false, status: 'Fernleihe', remote: " + remoteAddress + ", referer: " + referer);
                        }
                    }
                    default: {
                        redirectView.setUrl(linksolverUrl + queryParameters);
                        // log.info("OA: false, status: 'Link-Name unbekannt', remote: " + remoteAddress + ", referer: " + referer);
                    }
                }
            }
        }
        // if any errors occur when trying to connect to linkresolver or doi resolver send error.
        catch (Exception e) {
            log.warn("encountered IO exception", e);
            String queryParameters = mapListToString(requestParams);
            if (urlFromDoi == null || urlFromDoi.isEmpty()) {
                redirectView.setUrl(linksolverUrl + queryParameters);
            }
            log.debug("redirect to " + redirectView.getUrl());
            log.info("OA: false, status: 'IO Exception', remote: " + remoteAddress + ", referer: " + referer);
            return redirectView;
        }
        return redirectView;
    }

    private MultiValueMap<String, String> cleanUpRequestPrams(MultiValueMap<String, String> requestParams) {
        requestParams.keySet().forEach(
                key -> {
                    List<String> values = requestParams.get(key);
                    List<String> newValues = new ArrayList<>();
                    for (String value: values)
                        newValues.add(cleanUpString(value));
                    requestParams.replace(key, newValues);
                }
        );
        return requestParams;
    }

    private String determineRemoteAddress(HttpServletRequest httpServletRequest) {
        String remoteAddress = "127.0.0.1";
        if (httpServletRequest.getHeader("remoteAddress") != null)
            remoteAddress = httpServletRequest.getHeader("remoteAddress");
        log.debug("call from " + remoteAddress);
        return remoteAddress;
    }

    private String getReferer(HttpServletRequest httpServletRequest) {
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
        return referer;
    }

    private MultiValueMap<String, String> setIssnIfOnlyEissnIsGiven(MultiValueMap<String, String> requestParams) {
        if (requestParams.getFirst("eissn") != null && !requestParams.getFirst("eissn").isEmpty()) {
            if (requestParams.getFirst("issn") == null || requestParams.getFirst("issn").isEmpty()) {
                log.debug("eissn given, but issn is empty. adding issn with eissn value");
                requestParams.add("issn", requestParams.get("eissn").get(0));
            }
        }
        return requestParams;
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
            log.debug("trying to construct shibboleth link with linksolver link.");
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

    private String cleanUpString(String queryString) {
        if (queryString != null) {
            if (queryString.contains("{"))
                queryString = queryString.replace("{", "");
            if (queryString.contains("%0A"))
                queryString = queryString.replace("%0A", "+");
            if (queryString.contains("%0D"))
                queryString = queryString.replace("%0D", "+");
            if (queryString.contains("\n"))
                queryString = queryString.replace("\n", "+");
            if (queryString.contains("\r"))
                queryString = queryString.replace("\r", "+");
        }
        return queryString;
    }

}
