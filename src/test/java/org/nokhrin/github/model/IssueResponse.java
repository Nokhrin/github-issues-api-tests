package org.nokhrin.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueResponse(
    String id,
    long number,
    String title,
    String body,
    String state,
    String createdAt,
    String updatedAt,
    String url
) {
}
