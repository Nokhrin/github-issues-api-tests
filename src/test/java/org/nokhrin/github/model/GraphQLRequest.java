package org.nokhrin.github.model;

import java.util.Map;

public record GraphQLRequest(String query, Map<String, Object> variables) {
    public static GraphQLRequest of(String query) {
        return new GraphQLRequest(query, Map.of());
    }

    public static GraphQLRequest of(String query, Map<String, Object> variables) {
        return new GraphQLRequest(query, variables);
    }
}
