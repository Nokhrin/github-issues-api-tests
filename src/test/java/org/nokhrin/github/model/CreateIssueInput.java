package org.nokhrin.github.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateIssueInput(String repositoryId, String title, String body) {}
