/*
 * major help from https://stackoverflow.com/a/41482123/9006787
 * doi regexps from crossref-blog: https://www.crossref.org/blog/dois-and-matching-regular-expressions/
 */
package unidue.ub.linksolverwrapper.controller;

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

    private final ShibbolethBuilder shibbolethBuilder;

    private final static Logger log = LoggerFactory.getLogger(LinksolverWrapperController.class);

    private boolean isDoiUrl;

    // the address of the linksolver
    @Value("${libintel.linksolver.url}")
    private String linksolverUrl;


    // include the Shibboleth WAYFLless URL Builder
    @Autowired
    public LinksolverWrapperController(ShibbolethBuilder shibbolethBuilder) {
        this.shibbolethBuilder = shibbolethBuilder;
    }

    /**
     * reads the extensive request parameters for the OpenURL, retrieves the resource URL and forwards it to the resource
     *
     * @param requestParams the OpenURL parameters
     * @return the redirect to the resource location
     */
    @GetMapping("/resolve")
    public RedirectView resolve(@RequestParam MultiValueMap<String, String> requestParams, HttpServletRequest httpServletRequest) {

        // in case of empty issn and given eissn add eissn value to request parameter map.
        if (requestParams.get("eissn") != null && !requestParams.get("eissn").isEmpty()) {
            if (requestParams.get("issn") == null || requestParams.get("issn").isEmpty()) {
                log.debug("eissn given, but issn is empty. Setting issn to eissn value");
                requestParams.add("issn", requestParams.get("eissn").get(0));
            }
        }

        // read the referrer url from the request and extract the host address. If none is present, set the referer to 'linksolver'
        String referer = "linksolver";
        if (httpServletRequest.getHeader("referer") != null) {
            if (!httpServletRequest.getHeader("referer").isEmpty()) {
                referer = httpServletRequest.getHeader("referer");
                log.debug("referer request header: " + referer);
                try {
                    URI uri = new URI(referer);
                    referer = URLEncoder.encode(uri.getHost(), StandardCharsets.UTF_8);
                } catch (URISyntaxException e) {
                    log.warn("could not decode uri from referrer");
                }

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
                    doi = value.replace("doi:", "");
                    // ask doi resolver for redirect url
                    urlFromDoi = RedirectLinkRetriever.getLinkForDoi(value);
                    log.info("retrieved link from DOI: " + urlFromDoi);
                    this.isDoiUrl = !urlFromDoi.isEmpty();
                    redirectView.setUrl(urlFromDoi);
                }
            }
        }

        // retrieve availability information from linksolver
        try {
            String queryParameters = mapListToString(requestParams);
            log.debug("getting response from linksolver");
            Document doc = Jsoup.connect(linksolverUrl + queryParameters).timeout(60 * 1000).get();
            log.debug("found " + doc.select("a").size() + " links in linksolver response");
            for (Element link : doc.select("a")) {
                String linkType = link.text();
                log.debug("linksolver returned option " + linkType);
                if (linkType.startsWith("Volltexte "))
                    linkType = linkType.substring(0, 9);
                switch (linkType) {
                    // first case: full text is online available, redirect directly to resource, construct WAYFless URL on the fly
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
                    // second case: only interlibrary loan is available, the check for specific conditions (elsevier).
                    // If Elsevier is present, redirect to order page and fill doi and source parameters.
                    // Otherwise redirect to the interlibrary loan page and fill in needed request params for the Fernleihe
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
                    case "Elsevier Zeitschriften - Link zum Bestellformular": {
                        log.debug("no fulltext available and elsevier journal. redirecting to order page.");
                        redirectView.setUrl("https://www.uni-due.de/ub/elsevierersatz.php?doi=" + doi + "&source=" + referer);
                    }
                    // third case: printed or online media are available but linksolver does not return URL.
                    // In this case redirect to journal online and print page (JOP-Button)
                    case "Elektronischer und gedruckter Bestand der UB": case "zur Zeitschrift": {
                        log.debug("printed or online access without resource url. redirecting to journals online and print page.");
                        String issn = requestParams.getFirst("issn");
                        if (issn != null)
                            if (issn.isEmpty())
                                issn = requestParams.getFirst("eissn");
                        if (issn != null) {
                            if (!issn.isEmpty()) {
                                Map<String, String> iopRequestParams = new HashMap<>();
                                iopRequestParams.put("sid", "bib:ughe");
                                iopRequestParams.put("pid", "bibid%3DUGHE");
                                iopRequestParams.put("genre", "journal");
                                iopRequestParams.put("issn", issn);
                                // if "Link zum Artikel" is not present, redirect to the linksolver
                                String url = "https://www.uni-due.de/ub/ghbsys/jop" + mapToString(iopRequestParams);
                                redirectView.setUrl(url);
                            } else
                                redirectView.setUrl(linksolverUrl + queryParameters);
                        }
                        return redirectView;
                    }
                    // fourth case: applicable for ebooks where full-text is available.
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
     * @param urlFromDoi the url returned from the doi resolver
     * @param urlFromLinksolver the url returned from the linksolver
     * @param remoteAddress the remote address from the request
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
     * @param target the original target url
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
     * @param test the string to be tested
     * @return true, if the string contains a doi
     */
    private boolean isDoi(String test) {
        String oldWileyDoiRegExp = "^doi:10.1002/[\\S]+$";
        String modernDoiRegExp = "^doi:10.\\d{4,9}/[-._;()/:A-Za-z0-9]+$";
        return (test.matches(modernDoiRegExp) || test.matches(oldWileyDoiRegExp));
    }
}
