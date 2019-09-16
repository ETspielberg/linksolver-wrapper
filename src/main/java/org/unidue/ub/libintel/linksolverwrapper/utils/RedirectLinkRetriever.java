package org.unidue.ub.libintel.linksolverwrapper.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class RedirectLinkRetriever {

    /**
     * takes a DOI and tries to get the corresponding resource link by reading the location header from the redirect.
     * @param doi a DOI
     * @return the link to the resource as string
     */
    public static String getLinkForDoi(String doi) {
            String url = "https://doi.org/" + doi;
            return getLinkFromRedirect(url);
    }

    /**
     * takes a Link and tries to get the corresponding resource link by reading the location header from the redirect.
     * @param link a a link to a resolver
     * @return the link to the resource as string
     */
    public static String getLinkFromRedirect(String link) {
        try {
            URL url = new URL(link);
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
                return link;
            }
        } catch (IOException ioe) {
            // return the link to doi.org and let them handle problems.
            return link;
        }
    }

}
