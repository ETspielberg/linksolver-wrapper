package org.unidue.ub.libintel.linksolverwrapper;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.unidue.ub.libintel.linksolverwrapper.controller.LinksolverWrapperController;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LinksolverControllerIntegrationTest {

    @Autowired
    LinksolverWrapperController linksolverWrapperController;




    public class TestDataProvider {
        public Object[] provideTestRequestMaps() {
            return new Object[] {
                    requestParams("isbn"),
                    requestParams("issn"),
                    requestParams("doi")
            };
            }

        private MultiValueMap<String, String> requestParams(String kindOfTest) {
            final MultiValueMap<String, String> args = new LinkedMultiValueMap<>();
            switch (kindOfTest) {
                case "isbn": {
                    args.put("isbn", Arrays.asList("978-123-232-123-5"));
                    break;
                }
                case "issn": {
                    args.put("issn", Arrays.asList("1234-1234"));
                }
                case "doi": {
                    args.put("id", Arrays.asList("doi:10.1021/ic503095"));
                }
            }
            return args;
        }
    }


}
