package unidue.ub.linksolverwrapper.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
            stringBuilder.append((key != null ? URLEncoder.encode(key, StandardCharsets.UTF_8) : ""));
            stringBuilder.append("=");
            stringBuilder.append(value != null ? URLEncoder.encode(value, StandardCharsets.UTF_8) : "");
        }
        return stringBuilder.toString();
    }
}
