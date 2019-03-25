package unidue.ub.linksolverwrapper.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class DoiConnector {

    /**
     * takes a DOI and tries to get the corresponding resource link by reading the location header from the redirect.
     * @param doi a DOI
     * @return the link to the resource as string
     */
    public static String getLink(String doi) {
        try {
            URL url = new URL("https://doi.org/" + doi);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // do not follow the redirect, just obtain the resource url
            connection.setInstanceFollowRedirects(false);
            int responseCode = connection.getResponseCode();

            //check whether it is a redirect and return the location
            if (responseCode == HttpURLConnection.HTTP_SEE_OTHER
                    || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                return connection.getHeaderField("Location");
            } else {
                // return the link to doi.org
                return "https://doi.org/" + doi;
            }
        } catch (IOException ioe) {
            // return the link to doi.org and let them handle problems.
            return "https://doi.org/" + doi;
        }
    }
}
