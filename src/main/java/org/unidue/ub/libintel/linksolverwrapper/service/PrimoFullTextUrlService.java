package org.unidue.ub.libintel.linksolverwrapper.service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.List;

@Service
public class PrimoFullTextUrlService {

    @Value("${libintel.primo.search.url}")
    private String primoApiUrl;

    @Value("${libintel.primo.url}")
    private String primoUrl;

    @Value("${libintel.primo.api.key}")
    private String primoApiKey;

    private final static Logger log = LoggerFactory.getLogger(PrimoFullTextUrlService.class);

    public PrimoFullTextUrlService() {
    }

    public String getPrimoResponse(String identifier) {
        String response = getResponseForJson(identifier);
        if (!"".equals(response)) {
            DocumentContext jsonContext = JsonPath.parse(response);
            List<Object> documents = jsonContext.read("$['docs'][*]");
            log.debug("found " + documents.size() + " documents");
            int numberOfDocs = documents.size();
            for (int i = 0; i < numberOfDocs; i++) {
                String basePath = "$['docs'][" + i + "]";
                try {
                    String test = jsonContext.read(basePath + "['delivery']['availabilityLinksUrl'][0]");
                    if (!test.isEmpty())
                        return test;
                } catch (PathNotFoundException pnfe) {
                    log.debug("no url given");
                }
            }
        }
        return null;
    }


    private String getResponseForJson(String isbn) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        String query = "isbn,contains," + isbn;
        String resourceUrl = primoApiUrl + "&q=" + query + "&apikey=" + primoApiKey;
        log.info("querying Primo API with " + resourceUrl);
        ResponseEntity<String> response = restTemplate.getForEntity(resourceUrl, String.class);
        if (response.getStatusCode().equals(HttpStatus.OK))
            return response.getBody();
        else
            return "";
    }

}
