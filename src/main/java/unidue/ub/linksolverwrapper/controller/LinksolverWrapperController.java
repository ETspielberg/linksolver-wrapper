package unidue.ub.linksolverwrapper.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import unidue.ub.linksolverwrapper.utils.DoiConnector;
import unidue.ub.linksolverwrapper.utils.ShibbolethBuilder;

import java.io.IOException;
import java.util.Map;

import static unidue.ub.linksolverwrapper.utils.Utilities.mapToString;

@Controller
public class LinksolverWrapperController {

    private final ShibbolethBuilder shibbolethBuilder;

    private final static Logger log = LoggerFactory.getLogger(LinksolverWrapperController.class);

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
    public RedirectView resolve(@RequestParam Map<String, String> requestParams) {
        String urlString = "";
        RedirectView redirectView = new RedirectView();

        // first, check for DOI
        if (requestParams.containsKey("id")) {
            if (requestParams.get("id").startsWith("doi:")) {
                String doi = requestParams.get("id");

                // get resource link for DOI
                urlString = DoiConnector.getLink(doi);
                log.info("retreived link from DOI:");
            }
        }

        // if no doi is found, use the linksolver
        else {
            try {
                String queryParameters = mapToString(requestParams);
                Document doc = Jsoup.connect(linksolverUrl + queryParameters).get();
                for (Element link : doc.select("a")) {
                    String linkType = link.text();

                    // assuming "Link zum Artikel" is always top
                    if ("Link zum Artikel".equals(linkType)) {
                        urlString = linksolverUrl + link.attr("href");
                        break;
                    }

                    // if "Link zum Artikel" is not present, redirect to the linksolver
                    urlString = linksolverUrl + queryParameters;
                    redirectView.setUrl(linksolverUrl + queryParameters);
                }
            } catch (IOException e) {
                // TODO: handle problems with connection to linksolver
                e.printStackTrace();
            }

        }
        log.info("redirecting to url: " + urlString);

        // check for shibboleth
        String url = shibbolethBuilder.constructWayflessUrl(urlString);

        // redirect to url
        redirectView.setUrl(url);
        return redirectView;
    }

    @GetMapping("/useShibboleth")
    public RedirectView useShibboleth(@RequestParam String targetUrl) {
        RedirectView redirectView = new RedirectView();
        String url = shibbolethBuilder.constructWayflessUrl(targetUrl);
        redirectView.setUrl(url);
        return redirectView;
    }
}
