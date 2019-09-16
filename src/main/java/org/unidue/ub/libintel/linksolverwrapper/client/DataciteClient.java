package org.unidue.ub.libintel.linksolverwrapper.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.unidue.ub.libintel.linksolverwrapper.model.datacite.DataciteResponse;


@FeignClient(name="dataciteClient", url="https://data.datacite.org")
@Component
public interface DataciteClient {

    @RequestMapping(method= RequestMethod.GET, value="/application/vnd.datacite.datacite+xml/{doi}")
    DataciteResponse getDataciteData(@PathVariable String doi, @RequestParam String email);
}
