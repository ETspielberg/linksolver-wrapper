package unidue.ub.linksolverwrapper.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class Utilities {
    /**
     * Converst a map of parameters into an url-encoded string to be added to the server.
     * Thanks to StackOverflow question https://stackoverflow.com/questions/2809877/how-to-convert-map-to-url-query-string
     * @param map a map of parameters
     * @return a string with parameters including the initial '?'
     */
    public static String mapToString(Map<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String key : map.keySet()) {
            if (stringBuilder.length() == 0) {
                stringBuilder.append("?");
            } else
                stringBuilder.append("&");
            String value = map.get(key);
            try {
                stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));
                stringBuilder.append("=");
                stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }
        return stringBuilder.toString();
    }
}
