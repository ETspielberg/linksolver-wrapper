package org.unidue.ub.libintel.linksolverwrapper.utils;

import org.springframework.util.MultiValueMap;

import java.util.Map;

public class Utilities {
    /**
     * Converst a map of parameters into an url-encoded string to be added to the server.
     * Thanks to StackOverflow question https://stackoverflow.com/questions/2809877/how-to-convert-map-to-url-query-string
     * @param map a map of parameters
     * @return a string with parameters including the initial '?'
     */
    public static String mapListToString(MultiValueMap<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();
        // go through all paramter lists
        for (String key : map.keySet()) {
            // go through all key-value pairs
            for (String value : map.get(key)) {
                // add a "?" at the beginning, later on a "&"
                if (stringBuilder.length() == 0) {
                    stringBuilder.append("?");
                } else
                    stringBuilder.append("&");
                // append the key-value-pair
                stringBuilder.append(key).append("=").append(value);
            }
        }
        return stringBuilder.toString();
    }

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
            stringBuilder.append(key).append("=").append(value);
        }
        return stringBuilder.toString();
    }
}
