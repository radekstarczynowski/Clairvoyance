package com.rashidmayes.clairvoyance.util;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class MapParser {

    public static Map<String, String> parseMap(String mapString, String delim) {
        var map = new HashMap<String, String>();
        for (String pair : StringUtils.split(mapString, delim)) {
            var kvPair = pair.split("=");
            map.put(kvPair[0], kvPair[1]);
        }
        return map;
    }

    public static String getString(Map<String, String> properties, String... keys) {
        String value = getProperty(properties, keys);
        return (value == null) ? "" : value;
    }

    public static Long getLong(Map<String, String> properties, String... keys) {
        String value = getProperty(properties, keys);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException nfe) {
                ClairvoyanceLogger.logger.warn(nfe.getMessage());
            }
        }
        return null;
    }

    private static String getProperty(Map<String, String> properties, String... keys) {
        if (properties != null) {
            for (String key : keys) {
                var value = properties.get(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

}
