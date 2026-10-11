package org.nokhrin.github.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Parser {
    private static final Pattern LINK_HEADER = Pattern.compile("<([^>]+)>\\s*;\\s*rel=\"([a-z]+)\"");

    private Parser() {
    }

    public static Map<String, String> parseLinkHeader(String header) {
        if (header == null || header.isBlank()) {
            return Map.of();
        }
        Map<String, String> rels = new HashMap<>();
        Matcher matcher = LINK_HEADER.matcher(header);
        while (matcher.find()) {
            rels.put(matcher.group(2), matcher.group(1));
        }
        return Map.copyOf(rels);
    }

    public static Optional<String> relFromLinkHeader(String header, String relName) {
        return Optional.ofNullable(parseLinkHeader(header).get(relName));
    }
}
