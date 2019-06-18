package unidue.ub.linksolverwrapper.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import unidue.ub.linksolverwrapper.model.UnpaywallResponse;

@FeignClient(name="unpaywallClient", url="https://api.unpaywall.org")
@Component
public interface UnpaywallClient {

    @RequestMapping(method= RequestMethod.GET, value="/my/request/{doi}")
    UnpaywallResponse getUnpaywallData(@PathVariable String doi, @RequestParam String email);
}
