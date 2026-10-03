package org.nokhrin.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repository {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("owner")
    private User owner;

    @JsonProperty("private")
    private Boolean isPrivate;

    @JsonProperty("visibility")
    private String visibility;

    @JsonProperty("default_branch")
    private String defaultBranch;

    @JsonProperty("has_issues")
    private Boolean hasIssues;
}