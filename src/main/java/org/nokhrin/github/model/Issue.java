package org.nokhrin.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "url",
        "repository_url",
        "labels_url",
        "comments_url",
        "events_url",
        "html_url",
        "id",
        "node_id",
        "number",
        "title",
        "user",
        "labels",
        "state",
        "locked",
        "assignee",
        "assignees",
        "milestone",
        "comments",
        "created_at",
        "updated_at",
        "closed_at",
        "author_association",
        "active_lock_reason",
        "body",
        "performed_via_github_app"
})

@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
//@Builder
//@AllArgsConstructor
public class Issue {

    @JsonProperty("url")
    private String url;
    @JsonProperty("repository_url")
    private String repositoryUrl;
    @JsonProperty("labels_url")
    private String labelsUrl;
    @JsonProperty("comments_url")
    private String commentsUrl;
    @JsonProperty("events_url")
    private String eventsUrl;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("id")
    private Long id;
    @JsonProperty("node_id")
    private String nodeId;
    @JsonProperty("number")
    private Integer number;
    @JsonProperty("title")
    private String title;
    @JsonProperty("user")
    private User user;
    @JsonProperty("labels")
    private List<Object> labels = null;
    @JsonProperty("state")
    private String state;
    @JsonProperty("locked")
    private Boolean locked;
    @JsonProperty("assignee")
    private Object assignee;
    @JsonProperty("assignees")
    private List<Object> assignees = null;
    @JsonProperty("milestone")
    private Object milestone;
    @JsonProperty("comments")
    private Integer comments;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("closed_at")
    private Object closedAt;
    @JsonProperty("author_association")
    private String authorAssociation;
    @JsonProperty("active_lock_reason")
    private Object activeLockReason;
    @JsonProperty("body")
    private Object body;
    @JsonProperty("performed_via_github_app")
    private Object performedViaGithubApp;

}
