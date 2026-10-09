package org.nokhrin.github.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LinkHeaderParser {
    private static final Pattern ENTRY = Pattern.compile("<([^>]+)>\\s*;\\s*rel=\"([a-z]+)\"");

    private LinkHeaderParser() {
    }

    public static Map<String, String> parse(String header) {
        if (header == null || header.isBlank()) {
            return Map.of();
        }
        Map<String, String> rels = new HashMap<>();
        Matcher matcher = ENTRY.matcher(header);
        while (matcher.find()) {
            rels.put(matcher.group(2), matcher.group(1));
        }
        return Map.copyOf(rels);
    }

    public static Optional<String> rel(String header, String relName) {
        return Optional.ofNullable(parse(header).get(relName));
    }
}
