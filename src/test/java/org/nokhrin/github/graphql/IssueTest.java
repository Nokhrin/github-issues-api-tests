package org.nokhrin.github.graphql;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nokhrin.github.model.IssueResponse;
import org.nokhrin.github.utils.IssuesGraphql;
import org.nokhrin.github.utils.ReadResource;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.nokhrin.github.config.GitHubEndpoints.GRAPHQL;

public class IssueTest extends GraphQLBaseTest {

    @Test
    @Tag("read")
    void issuesQueryReturnsValidIssues() {
        String query = ReadResource.readResource("graphql/queries/get-issues-list.graphql");

        Map<String, Object> variables = Map.of(
            "owner", config.githubOwner(),
            "name", config.githubRepo(),
            "limit", 5
        );
        Map<String, Object> requestBody = Map.of(
            "query", query,
            "variables", variables
        );
        RestAssured.given()
            .spec(roAuthSpec)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)
            .when()
            .post(GRAPHQL)
            .then()
            .statusCode(200)
            .log().all()
            .body("errors", nullValue())
            .body(matchesJsonSchemaInClasspath(
                "schemas/graphql/get-issues-response.schema.json"
            ))
        ;
    }


    @Test
    @Tag("write")
    void createIssue_issueCreated() {
        String title = "graphql-roundtrip-" + System.currentTimeMillis();
        String body = "test body";

        IssueResponse created = IssuesGraphql.createIssue(rwAuthSpec, owner, repo, title, body);

        try {
            IssueResponse fetched = IssuesGraphql.getIssueByNumber(roAuthSpec, owner, repo, created.number());

            assertAll(
                () -> assertEquals(created.id(), fetched.id()),
                () -> assertEquals(created.number(), fetched.number()),
                () -> assertEquals(title, created.title()),
                () -> assertEquals(title, fetched.title()),
                () -> assertEquals("OPEN", created.state()),
                () -> assertEquals("OPEN", fetched.state()),
                () -> assertEquals(body, fetched.body()),
                () -> assertNotNull(fetched.createdAt()),
                () -> assertNotNull(fetched.url())
            );
        } finally {
            IssuesGraphql.closeIssue(rwAuthSpec, created);
        }
    }

    @Test
    @Tag("write")
    void getIssueByNumber_validNumber_returnsIssue() {
        String title = "test-issue-" + System.currentTimeMillis();
        IssueResponse issue = IssuesGraphql.createIssue(rwAuthSpec, owner, repo, title, "test body");

        try {
            String query = ReadResource.readResource("graphql/queries/get-issue-by-number.graphql");
            Map<String, Object> variables = Map.of(
                "owner", config.githubOwner(),
                "name", config.githubRepo(),
                "issueNumber", issue.number()
            );

            IssueResponse issueActual = RestAssured.given()
                .spec(roAuthSpec)
                .contentType(ContentType.JSON)
                .body(Map.of("query", query, "variables", variables))
                .when()
                .post(GRAPHQL)
                .then()
                .statusCode(200)
                .body("errors", nullValue())
                .extract()
                .jsonPath()
                .getObject("data.repository.issue", IssueResponse.class);

            assertAll(
                () -> assertEquals(issue.id(), issueActual.id()),
                () -> assertEquals(issue.number(), issueActual.number()),
                () -> assertEquals(title, issueActual.title()),
                () -> assertEquals("OPEN", issueActual.state())
            );
        } finally {
            IssuesGraphql.closeIssue(rwAuthSpec, issue);
        }
    }


    private static Stream<Arguments> stateFilters() {
        return Stream.of(
            Arguments.of("OPEN_ONLY", List.of("OPEN")),
            Arguments.of("CLOSED_ONLY", List.of("CLOSED")),
            Arguments.of("ALL_STATES", List.of("OPEN", "CLOSED"))
        );
    }

    @ParameterizedTest(name = "фильтр {0}")
    @MethodSource("stateFilters")
    @Tag("write")
    void getIssuesInState_variousStates_filtersCorrectly(String scenario, List<String> states) {
        List<IssueResponse> openIssues = new ArrayList<>();
        List<IssueResponse> closedIssues = new ArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                openIssues.add(IssuesGraphql.createIssue(rwAuthSpec, owner, repo,
                    "open-" + System.currentTimeMillis() + "-" + i, "test"));
                closedIssues.add(IssuesGraphql.createIssue(rwAuthSpec, owner, repo,
                    "closed-" + System.currentTimeMillis() + "-" + i, "test"));
            }

            for (IssueResponse issue : closedIssues) {
                IssuesGraphql.closeIssue(rwAuthSpec, issue);
            }

            String query = ReadResource.readResource("graphql/queries/get-issues-in-state.graphql");
            Map<String, Object> variables = Map.of(
                "owner", config.githubOwner(),
                "name", config.githubRepo(),
                "states", states,
                "first", 100
            );

            Response response = RestAssured.given()
                .spec(roAuthSpec)
                .contentType(ContentType.JSON)
                .body(Map.of("query", query, "variables", variables))
                .when()
                .post(GRAPHQL)
                .then()
                .statusCode(200)
                .body("errors", nullValue())
                .extract().response();

            List<Map<String, Object>> nodes = response.path("data.repository.issues.nodes");

            for (Map<String, Object> node : nodes) {
                String state = (String) node.get("state");
                assertTrue(states.contains(state),
                    "Issue в состоянии " + state + " не соответствует фильтру " + states);
            }

        } finally {
            for (IssueResponse issue : openIssues) {
                IssuesGraphql.closeIssue(rwAuthSpec, issue);
            }
        }
    }

    private static Stream<Arguments> paginationSizes() {
        return Stream.of(
            Arguments.of("min", 1),
            Arguments.of("medium", 5),
            Arguments.of("large", 100)
        );
    }

    @ParameterizedTest(name = "first={1}")
    @MethodSource("paginationSizes")
    @Tag("write")
    void getIssues_paginationBoundaries(String scenario, int pageSize) {
        List<IssueResponse> createdIssues = new ArrayList<>();
        try {
            for (int i = 0; i < 5; i++) {
                createdIssues.add(IssuesGraphql.createIssue(rwAuthSpec, owner, repo,
                    "pagination-test-" + i, "test"
                ));
            }

            String query = ReadResource.readResource("graphql/queries/get-issues-list.graphql");
            Map<String, Object> variables = Map.of(
                "owner", config.githubOwner(),
                "name", config.githubRepo(),
                "limit", pageSize
            );

            Response response = RestAssured.given()
                .spec(roAuthSpec)
                .contentType(ContentType.JSON)
                .body(Map.of("query", query, "variables", variables))
                .when()
                .post(GRAPHQL)
                .then()
                .statusCode(200)
                .body("errors", nullValue())
                .extract().response();

            List<Map<String, Object>> nodes = response.path("data.repository.issues.nodes");
            Boolean hasNextPage = response.path("data.repository.issues.pageInfo.hasNextPage");
            String endCursor = response.path("data.repository.issues.pageInfo.endCursor");

            assertAll(
                () -> assertTrue(nodes.size() <= pageSize,
                    "Размер nodes (" + nodes.size() + ") не должен превышать " + pageSize),
                () -> assertTrue(hasNextPage != null,
                    "pageInfo.hasNextPage должен присутствовать"),
                () -> {
                    if (hasNextPage && endCursor != null) {
                        Map<String, Object> variables2 = Map.of(
                            "owner", config.githubOwner(),
                            "name", config.githubRepo(),
                            "limit", pageSize
                        );
                        assertTrue(!endCursor.isEmpty(),
                            "endCursor не должен быть пустым при hasNextPage=true");
                    }
                }
            );

        } finally {
            for (IssueResponse issue : createdIssues) {
                IssuesGraphql.closeIssue(rwAuthSpec, issue
                );
            }
        }
    }

    @Test
    @Tag("write")
    void getIssues_sortedDesc_returnsInCorrectOrder() {
        List<IssueResponse> createdIssues = new ArrayList<>();
        try {

            for (int i = 0; i < 3; i++) {
                createdIssues.add(IssuesGraphql.createIssue(rwAuthSpec, owner, repo,
                    "sort-test-" + i, "test"
                ));
                if (i < 2) {
                    Thread.sleep(1000);
                }
            }

            String query = ReadResource.readResource("graphql/queries/get-issues-list.graphql");

            Map<String, Object> variables = Map.of(
                "owner", config.githubOwner(),
                "name", config.githubRepo(),
                "limit", 10
            );

            Response response = RestAssured.given()
                .spec(roAuthSpec)
                .contentType(ContentType.JSON)
                .body(Map.of("query", query, "variables", variables))
                .when()
                .post(GRAPHQL)
                .then()
                .statusCode(200)
                .body("errors", nullValue())
                .extract().response();

            List<Map<String, Object>> nodes = response.path("data.repository.issues.nodes");

            for (int i = 0; i < nodes.size() - 1; i++) {
                ZonedDateTime current = ZonedDateTime.parse((String) nodes.get(i).get("createdAt"));
                ZonedDateTime next = ZonedDateTime.parse((String) nodes.get(i + 1).get("createdAt"));
                assertTrue(!current.isBefore(next),
                    "При сортировке DESC элемент " + i + " должен быть >= элемента " + (i + 1));
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            for (IssueResponse issue : createdIssues) {
                IssuesGraphql.closeIssue(rwAuthSpec, issue
                );
            }
        }
    }
}
