package org.nokhrin.github.utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.nokhrin.github.config.GitHubEndpoints;
import org.nokhrin.github.model.CreateIssueInput;
import org.nokhrin.github.model.GraphQLRequest;
import org.nokhrin.github.model.IssueResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class IssuesGraphql {

    private static final Map<String, String> REPO_ID_CACHE = new ConcurrentHashMap<>();

    private static String getRepositoryId(RequestSpecification spec, String owner, String repo) {
        return REPO_ID_CACHE.computeIfAbsent(owner + ":" + repo, k -> fetchRepositoryId(spec, owner, repo));
    }

    private static String fetchRepositoryId(RequestSpecification spec, String owner, String repo) {
        String query = ReadResource.readResource("graphql/queries/get-repository-id.graphql");
        return RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(GraphQLRequest.of(query, Map.of("owner", owner, "name", repo)))
            .when()
            .post(GitHubEndpoints.GRAPHQL)
            .then()
            .statusCode(200)
            .body("errors", nullValue())
            .body("data.repository.id", notNullValue())
            .log().ifValidationFails()
            .extract()
            .jsonPath()
            .getString("data.repository.id");
    }

    public static IssueResponse createIssue(RequestSpecification spec, String owner, String repo,
                                            String title, String body) {

        String mutation = ReadResource.readResource("graphql/mutations/create-issue.graphql");

        String repositoryId = getRepositoryId(spec, owner, repo);

        CreateIssueInput input = new CreateIssueInput(repositoryId, title, body);
        Map<String, Object> variables = Map.of("input", input);
        GraphQLRequest payload = GraphQLRequest.of(mutation, variables);

        return RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post(GitHubEndpoints.GRAPHQL)
            .then()
            .statusCode(200)
            .body("errors", nullValue())
            .log().ifValidationFails()
            .extract()
            .jsonPath()
            .getObject("data.createIssue.issue", IssueResponse.class);
    }

    public static void closeIssue(RequestSpecification spec, String issueId) {
        String mutation = ReadResource.readResource(
            "graphql/mutations/close-issue.graphql");
        Map<String, Object> variables = Map.of("issueId", issueId);
        GraphQLRequest payload = GraphQLRequest.of(mutation, variables);

        RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post(GitHubEndpoints.GRAPHQL)
            .then()
            .statusCode(200)
            .body("errors", nullValue())
            .log().ifValidationFails();
    }

    public static void closeIssue(RequestSpecification spec, IssueResponse issue) {
        closeIssue(spec, issue.id());
    }

    public static IssueResponse getIssueByNumber(RequestSpecification spec, String owner,
                                                 String repo, long issueNumber) {
        String query = ReadResource.readResource("graphql/queries/get-issue-by-number.graphql");
        Map<String, Object> variables = Map.of(
            "owner", owner,
            "name", repo,
            "issueNumber", Math.toIntExact(issueNumber)
        );
        return RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(GraphQLRequest.of(query, variables))
            .when()
            .post(GitHubEndpoints.GRAPHQL)
            .then()
            .statusCode(200)
            .body("errors", nullValue())
            .body("data.repository.issue.id", notNullValue())
            .log().ifValidationFails()
            .extract()
            .jsonPath()
            .getObject("data.repository.issue", IssueResponse.class);
    }
}
