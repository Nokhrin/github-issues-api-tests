package org.nokhrin.github.restassured;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nokhrin.github.model.Issue;

import java.util.*;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static utils.IssueCleanupUtil.closeOpenIssues;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IssueTest extends BaseTest {

    @AfterAll
    void tearDown() {
        closeOpenIssues(rwAuthSpec);
    }

    @Test
    public void createIssueRawJsonValid_Created() {
        String issueTitle = randomIssueTitle();
        String issueDescription = randomIssueDescription();
        String requestBody = """
            {"title": "%s", "body": "%s"}
            """.formatted(issueTitle, issueDescription);
        Long issueNum = null;

        issueNum = createIssue(requestBody);
        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get("/repos/{owner}/{repo}/issues/{number}", issueNum)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .body("title", equalTo(issueTitle))
            .body("body", equalTo(issueDescription))
        ;

    }

    @Test
    public void verifyPostPojo() {
        Long issueNum = createIssue(randomIssueTitle(), randomIssueDescription());
        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get("/repos/{owner}/{repo}/issues/{number}", issueNum)
            .then()
            .statusCode(200)
            .extract()
            .as(Issue.class);
    }


    @Test
    public void verifyPostMap() {
        Map<String, Object> requestBody = Map.of(
            "title", randomIssueTitle(),
            "body", randomIssueDescription()
        );
        Long number = null;
        number = RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath().getLong("number")
        ;


        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get("/repos/{owner}/{repo}/issues/{number}", number)
            .then()
            .statusCode(200)
            .body("body", equalTo(randomIssueDescription()))
        ;

    }

    @Test
    public void verifyPostPojoWithJsonPath() {
        Long issueNum = createIssue(randomIssueTitle(), randomIssueDescription());
        Issue issue = RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get("/repos/{owner}/{repo}/issues/{number}", issueNum)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .as(Issue.class);

        assertEquals(owner, issue.getUser().getLogin());
    }

    @Test
    public void createIssueWithQueryParamsFailure() {
        RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("title", randomIssueTitle())
            .queryParam("body", randomIssueDescription())
            .when()
            .post("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(422)
            .log().ifValidationFails()
        ;
    }


    private static Stream<Arguments> invalidTitles() {
        return Stream.of(
            Arguments.of("empty", ""),
            Arguments.of("space", " "),
            Arguments.of("new line", "\n"),
            Arguments.of("tab", "\t"),
            Arguments.of("car ret", "\r")
        );
    }

    @ParameterizedTest(name = "{0} -> 422")
    @MethodSource("invalidTitles")
    void invalidTitleValidationFails(String caseName, String issueTitle) {
        Map<String, Object> body = new HashMap<>();
        if (issueTitle != null) {
            body.put("title", issueTitle);
        }
        body.put("body", randomIssueDescription());

        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(422)
            .body("message", equalTo("Validation Failed"))
            .body("errors[0].message", equalTo("title can't be blank"))
            .body("errors[0].value", nullValue())
            .body("errors[0].resource", equalTo("Issue"))
            .body("errors[0].field", equalTo("title"))
            .body("errors[0].code", equalTo("invalid"))
            .log().ifValidationFails()
        ;
    }

    @Test
    void createCloseStateVerified() {
        String issueTitle = randomIssueTitle();
        String issueDescription = randomIssueDescription();
        Long issueNumber = createIssue(issueTitle, issueDescription);

        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .when()
            .get("/repos/{owner}/{repo}/issues/{issue_number}", issueNumber)
            .then()
            .statusCode(200)
            .body("title", equalTo(issueTitle))
            .body("state", equalTo("open"))
            .log().ifValidationFails()
        ;

        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(Map.of("state", "closed"))
            .when()
            .patch("/repos/{owner}/{repo}/issues/{issue_number}", issueNumber)
            .then()
            .statusCode(200)
            .body("title", equalTo(issueTitle))
            .body("state", equalTo("closed"))
            .body("closed_at", notNullValue())
            .log().ifValidationFails()
        ;

        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .when()
            .get("/repos/{owner}/{repo}/issues/{issue_number}", issueNumber)
            .then()
            .statusCode(200)
            .body("title", equalTo(issueTitle))
            .body("state", equalTo("closed"))
            .body("closed_at", notNullValue())
            .log().ifValidationFails()
        ;

    }

    @Test
    void invalidJsonBody_badRequest() {
        String invalidBody = """
            {
              "title": "Found a bug",
              "body": "I'm having a problem with this.",
              "assignees": ["octocat"],
              "milestone": 1,
              "labels": ["bug"],
            }
            """;
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(invalidBody)
            .when()
            .post("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(400)
            .body("message", containsStringIgnoringCase("json"))
            .body("documentation_url", notNullValue())
            .log().ifValidationFails()
        ;
    }

    @Test
    void requestJsonBodySyntaxError_responseBodyValid() {
        String issueTitle = randomIssueTitle();
        String invalidBody = """
            {
              "title": "%s",
              "body": "I'm having a problem with this.",
              "assignees": ["octocat"],
              "milestone": 1,
              "labels": ["bug"],
            }
            """.formatted(issueTitle);
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(invalidBody)
            .when()
            .post("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(400)
            .body("message", containsStringIgnoringCase("json"))
            .body("documentation_url", notNullValue())
            .log().ifValidationFails()
        ;

        RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("state", "open")
            .queryParam("per_page", 100)
            .when()
            .get("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(200)
            .body(not(hasItem(issueTitle)))
            .log().ifValidationFails()
        ;
    }

    @Test
    void verifyNotFoundResponseBody() {
        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get("/repos/{owner}/{repo}/issues/{issue_number}", -1)
            .then()
            .statusCode(404)
            .body("message", equalTo("Not Found"))
            .body("documentation_url", startsWith("https://docs.github.com"))
            .body("status", equalTo("404"))
            .log().ifValidationFails()
        ;

    }

    @Test
    void verifyValidationFailedResponseBody_SemanticError() {
        String issueBody = randomIssueDescription();
        Map<String, String> invalidBody = Map.of(
            "title", "",
            "body", issueBody
        );
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(invalidBody)
            .when()
            .post("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(422)
            .log().ifValidationFails()
            .body("message", containsStringIgnoringCase("Validation Failed"))
            .body("errors[0].message", notNullValue())
            .body("errors[0].value", nullValue())
            .body("errors[0].resource", equalTo("Issue"))
            .body("errors[0].field", equalTo("title"))
            .body("errors[0].code", containsStringIgnoringCase("invalid"))
            .body("documentation_url", notNullValue())
            .body("status", equalTo("422"))
        ;

    }


    private static Stream<Arguments> validStates() {
        return Stream.of(
            Arguments.of("open"),
            Arguments.of("closed"),
            Arguments.of("all")
        );
    }

    @ParameterizedTest
    @MethodSource("validStates")
    void verifyFilterByState(String state) {
        Response response = RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("state", state)
            .queryParam("per_page", 100)
            .get("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .response();

        response.prettyPrint();
        List<String> issueStates = response
            .jsonPath()
            .getList("state", String.class);

        if (state.equals("all")) {
            assertThat(issueStates, everyItem(oneOf("open", "closed")));
        } else {
            assertThat(issueStates, everyItem(equalTo(state)));
        }

    }

    @Test
    void verifyPagination() {
        //предусловие: нет открытых issue
        closeOpenIssues(rwAuthSpec);

        List<Long> idsExpected = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            idsExpected.add(createIssue());
        }

        int per_page = 2;

        //prep
        int maxAttempts = 5;
        int attempt = 0;
        List<Long> idsActual = new ArrayList<>();

        while (attempt < maxAttempts) {

            Response probe = RestAssured.given()
                .spec(rwAuthSpec)
                .queryParam("filter", "all")
                .queryParam("state", "open")
                .queryParam("per_page", 100)
                .when()
                .get("/repos/{owner}/{repo}/issues")
                .then()
                .statusCode(200)
                .log().ifValidationFails()
                .extract()
                .response();

            idsActual = probe.jsonPath()
                .getList("id", Long.class);

            if (idsActual.size() == 3) {
                break;
            }
            attempt++;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        if (idsActual.size() < 3) {
            throw new AssertionError("Failed to fetch created issues");
        }

        //1
        Response responsePage1 = RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("filter", "all")
            .queryParam("state", "open")
            .queryParam("per_page", per_page)
            .queryParam("page", 1)
            .when()
            .get("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .response();

        List<Long> idsPage1 = responsePage1
            .jsonPath()
            .getList("id", Long.class);

        String linkHeaderPage1 = responsePage1.getHeader("Link");

        //2
        Response responsePage2 = RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("filter", "all")
            .queryParam("state", "open")
            .queryParam("per_page", per_page)
            .queryParam("page", 2)
            .when()
            .get("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .response();

        List<Long> idsPage2 = responsePage2
            .jsonPath()
            .getList("id", Long.class);

        String linkHeaderPage2 = responsePage2.getHeader("Link");

        List<Long> finalIdsActual = idsActual;
        assertAll(
            () -> assertThat(idsPage1, hasSize(per_page)),
            () -> assertThat(linkHeaderPage1, startsWithIgnoringCase("<https://api.github.com/repositories/")),
            () -> assertThat(linkHeaderPage1, containsString("rel=\"next\"")),
            () -> assertThat(idsPage2, hasSize(per_page)),
            () -> assertThat(linkHeaderPage2, startsWithIgnoringCase("<https://api.github.com/repositories/")),
            () -> assertThat(linkHeaderPage2, not(containsString("rel=\"next\""))),
            () -> assertThat(Collections.disjoint(idsPage1, idsPage2), is(true)),
            () -> assertThat(idsExpected, containsInAnyOrder(finalIdsActual))
        );
    }

    @Test
    void verifyXRateLimitHeaders() {
        Response response = RestAssured.given()
            .spec(rwAuthSpec)
            .get("/repos/{owner}/{repo}/issues")
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .response();

        String limit = response.getHeader("X-RateLimit-Limit");
        String remaining = response.getHeader("X-RateLimit-Remaining");
        String used = response.getHeader("X-RateLimit-Used");
        String resource = response.getHeader("X-RateLimit-Resource");
        String reset = response.getHeader("X-RateLimit-Reset");

        assertAll(
            () -> assertThat(limit, notNullValue()),
            () -> assertThat(remaining, notNullValue()),
            () -> assertThat(used, notNullValue()),
            () -> assertThat(resource, notNullValue()),
            () -> assertThat(reset, notNullValue()),

            () -> assertThat(limit, matchesPattern("d+")),
            () -> assertThat(remaining, matchesPattern("d+")),
            () -> assertThat(used, matchesPattern("d+")),
            () -> assertThat(resource, equalTo("core")),
            () -> assertThat(reset, matchesPattern("d+")),

            () -> {
                Integer limitInt = Integer.parseInt(limit);
                Integer remainingInt = Integer.parseInt(remaining);
                Integer usedInt = Integer.parseInt(used);
                Long resetLong = Long.parseLong(reset);

                assertThat(limitInt, greaterThan(0));
                assertThat(remainingInt, greaterThanOrEqualTo(0));
                assertThat(usedInt, greaterThanOrEqualTo(0));
                assertThat(resetLong, greaterThanOrEqualTo(0L));
            }

        );

    }
}
