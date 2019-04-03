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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static unidue.ub.linksolverwrapper.utils.Utilities.mapListToString;
import static unidue.ub.linksolverwrapper.utils.Utilities.mapToString;

@Controller
public class LinksolverWrapperController {

    private final ShibbolethBuilder shibbolethBuilder;

    private final static Logger log = LoggerFactory.getLogger(LinksolverWrapperController.class);

    private final String modernDoiRegExp = "^doi:10.\\d{4,9}/[-._;()/:A-Za-z0-9]+$";

    private final String oldWileyDoiRegExp = "^doi:10.1002/[\\S]+$";

    // the address of the linksolver
    @Value("${libintel.linksolver.url}")
    private String linksolverUrl;

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
    public RedirectView resolve(@RequestParam MultiValueMap<String, String> requestParams, HttpServletRequest request) {

        String remoteAddress = request.getRemoteAddr();
        log.info("call from " + remoteAddress);
        RedirectView redirectView = new RedirectView();
        String urlFromDoi = "";
        String urlFromLinksolver;
        String doi = "";
        // first, check for DOI
        if (requestParams.containsKey("id")) {
            List<String> ids = requestParams.get("id");
            for (Object id : ids) {
                String value = (String) id;
                if (isDoi(value)) {
                    doi = value.replace("doi:", "");
                    urlFromDoi = RedirectLinkRetriever.getLinkForDoi(value);
                    log.info("retrieved link from DOI: " + urlFromDoi);
                }
            }
        }
        // retrieve availability information from linksolver
        try {
            String queryParameters = mapListToString(requestParams);
            Document doc = Jsoup.connect(linksolverUrl + queryParameters).timeout(10 * 1000).get();
            for (Element link : doc.select("a")) {
                String linkType = link.text();
                switch (linkType) {
                    case "Link zum Artikel": {
                        urlFromLinksolver = RedirectLinkRetriever.getLinkFromRedirect(linksolverUrl + link.attr("href"));
                        log.info("retrieved link from linksolver: " + urlFromLinksolver);
                        log.info("full text available. Redirecting to resource.");
                        // check for shibboleth
                        String url;
                        if (!"".equals(urlFromDoi)) {
                            log.info("trying to construct shibboleth link with doi link.");
                            url = shibbolethBuilder.constructWayflessUrl(urlFromDoi, remoteAddress);
                        } else {
                            log.info("trying to construct shibboleth link with  linksolver link.");
                            url = shibbolethBuilder.constructWayflessUrl(urlFromLinksolver, remoteAddress);
                        }
                        // redirect to url
                        redirectView.setUrl(url);
                        return redirectView;
                    }
                    case "Fernleihe Zeitschriften": {
                        if (urlFromDoi.contains("sciencedirect") || urlFromDoi.contains("elsevier")) {
                            redirectView.setUrl("https://www.uni-due.de/ub/elsevierersatz.php?doi=" + doi);
                        } else {
                            requestParams.set("sid", "464_465:Zeitschriftenkatalog");
                            requestParams.set("pid", "<location>464_465<%2Flocation>");
                            requestParams.set("genre", "journal");
                            redirectView.setUrl("https://www.digibib.net/openurl" + mapListToString(requestParams));
                        }
                        return redirectView;
                    }
                    case "Elektronischer und gedruckter Bestand der UB": {
                        String issn = requestParams.getFirst("issn");
                        if (issn.isEmpty())
                            issn = requestParams.getFirst("eissn");
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
                        return redirectView;
                    }
                }
            }
        } catch (IOException e) {
            redirectView.setUrl("/error");
            e.printStackTrace();
            return redirectView;
        }
        return redirectView;
    }

    @GetMapping("/useShibboleth")
    public RedirectView useShibboleth(@RequestParam String target, HttpServletRequest request) {
        RedirectView redirectView = new RedirectView();
        String url = shibbolethBuilder.constructWayflessUrl(target, request.getRemoteAddr());
        redirectView.setUrl(url);
        return redirectView;
    }

    private boolean isDoi(String test) {
    return (test.matches(modernDoiRegExp) || test.matches(oldWileyDoiRegExp));
    }
}
