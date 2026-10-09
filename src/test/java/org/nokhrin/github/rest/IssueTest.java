package org.nokhrin.github.rest;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
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
import org.nokhrin.github.utils.IssueUtils;
import org.nokhrin.github.utils.WaitUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUES;
import static org.nokhrin.github.utils.IssueUtils.closeOpenIssues;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IssueTest extends RestBaseTest {

    @AfterAll
    void tearDown() {
        closeOpenIssues(rwAuthSpec);
    }

    @Feature("GitHub Issues API")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    public void createIssueRawJsonValid_Created() {
        String issueTitle = randomIssueTitle();
        String issueDescription = randomIssueDescription();
        String requestBody = """
            {"title": "%s", "body": "%s"}
            """.formatted(issueTitle, issueDescription);

        Long issueNum = createIssue(requestBody);
        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get("/repos/{owner}/{repo}/issues/{number}", issueNum)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .body("title", equalTo(issueTitle))
            .body("body", equalTo(issueDescription))
            .body(matchesJsonSchemaInClasspath("schemas/issue-schema.json"))
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
            .post(ISSUES)
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
            .post(ISSUES)
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
            .post(ISSUES)
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

    private static Stream<Arguments> invalidJsonBodies() {
        return Stream.of(
            Arguments.of("trailing comma",
                """
                    {
                      "title": "Found a bug",
                      "body": "I'm having a problem with this.",
                      "assignees": ["octocat"],
                      "milestone": 1,
                      "labels": ["bug"],
                    }
                    """),
            Arguments.of("missing bracket",
                """
                    {
                      "title": "Found a bug",
                      "body": "I'm having a problem with this.",
                      "assignees": ["octocat"],
                      "milestone": 1,
                      "labels": ["bug"
                    }
                    """),
            Arguments.of("unquoted string value",
                """
                    {
                      "title": Found a bug,
                      "body": "I'm having a problem with this.",
                      "assignees": ["octocat"],
                      "milestone": 1,
                      "labels": ["bug"]
                    }
                    """)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidJsonBodies")
    void invalidJsonBody_badRequest(String scenario, String invalidBody) {
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(invalidBody)
            .when()
            .post(ISSUES)
            .then()
            .statusCode(400)
            .body("message", containsStringIgnoringCase("json"))
            .body("documentation_url", notNullValue())
            .log().ifValidationFails()
        ;
    }


    @Test
    void verifyRequiredFieldValidation() {
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "",
                "body", "I'm having a problem with this."
            ))
            .when()
            .post(ISSUES)
            .then()
            .statusCode(422)
            .log().all()
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

    @Test
    void verifyValidationError_invalidMilestoneType() {
        String body = """
            {"title": "Found a bug", "milestone": "one"}
            """;
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(ISSUES)
            .then()
            .statusCode(422)
            .body("message", equalTo("Validation Failed"))
            .body("errors[0].field", equalTo("milestone"))
            .body("errors[0].value", equalTo("one"))
            .body("errors[0].code", equalTo("invalid"))
            .body("errors[0].resource", equalTo("Issue"))
            .body("documentation_url", notNullValue());
    }

    @Test
    void verifyValidationError_invalidLabelsType() {
        String body = """
            {"title": "Found a bug", "labels": {"name": "bug"}}
            """;
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(ISSUES)
            .then()
            .statusCode(422)
            .body("message", containsString("Invalid request"))
            .body("message", containsString("not an array"))
            .body("documentation_url", notNullValue())
            .body("status", equalTo("422"))
            .rootPath("")
            .body("errors", nullValue());
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
            .post(ISSUES)
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
            .get(ISSUES)
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
    void verifyFilterByState_openedIssueReturned() {
        Long openedIssueNumber = createIssue();

        WaitUtil.waitFor(
            () -> {
                List<Long> allIssuesNumbers = IssueUtils.getIssueNumbersByState(rwAuthSpec, "open");
                return allIssuesNumbers.contains(openedIssueNumber) ? allIssuesNumbers : null;
            },
            Duration.ofSeconds(10),
            Duration.ofSeconds(1),
            "test issue available"
        );

        String state = "open";
        Response response = RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("state", state)
            .queryParam("since", Instant.now().minusSeconds(600).toString())
            .queryParam("per_page", 100)
            .get(ISSUES)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .response();

        List<String> issueStates = response.jsonPath().getList("state", String.class);
        List<Long> issueNumbers = response.jsonPath().getList("number", Long.class);

        assertAll(
            () -> assertThat(issueStates, everyItem(equalTo(state))),
            () -> assertThat(issueNumbers, hasItem(openedIssueNumber))
        );

    }

    @Test
    void verifyFilterByState_closedIssueReturned() {
        Long closedIssueNumber = createIssue();
        closeIssue(closedIssueNumber);

        WaitUtil.waitFor(
            () -> {
                List<Long> allIssuesNumbers = IssueUtils.getIssueNumbersByState(rwAuthSpec, "closed");
                return allIssuesNumbers.contains(closedIssueNumber) ? allIssuesNumbers : null;
            },
            Duration.ofSeconds(10),
            Duration.ofSeconds(1),
            "test issue available"
        );

        String state = "closed";
        Response response = RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("state", state)
            .queryParam("since", Instant.now().minusSeconds(600).toString())
            .queryParam("per_page", 100)
            .get(ISSUES)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .response();

        List<String> issueStates = response.jsonPath().getList("state", String.class);
        List<Long> issueNumbers = response.jsonPath().getList("number", Long.class);

        assertAll(
            () -> assertThat(issueStates, everyItem(equalTo(state))),
            () -> assertThat(issueNumbers, hasItem(closedIssueNumber))
        );
    }

    @Test
    void verifyFilterByState_all() {
        Long openedIssueNumber = createIssue();
        Long closedIssueNumber = createIssue();
        closeIssue(closedIssueNumber);

        WaitUtil.waitFor(
            () -> {
                List<Long> allIssuesNumbers = IssueUtils.getIssueNumbersByState(rwAuthSpec, "all");
                boolean openFound = allIssuesNumbers.contains(openedIssueNumber);
                boolean closeFound = allIssuesNumbers.contains(closedIssueNumber);
                return openFound && closeFound ? allIssuesNumbers : null;
            },
            Duration.ofSeconds(10),
            Duration.ofSeconds(1),
            "2 issues available"
        );

        Response response = RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("state", "all")
            .queryParam("since", Instant.now().minusSeconds(600).toString())
            .queryParam("per_page", 100)
            .get(ISSUES)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .response();

        List<Long> issueNumbers = response.jsonPath().getList("number", Long.class);

        assertAll(
            () -> assertThat(issueNumbers, hasItem(openedIssueNumber)),
            () -> assertThat(issueNumbers, hasItem(closedIssueNumber))
        );
    }


    @Test
    void verifyPagination() {

        closeOpenIssues(rwAuthSpec);

        List<Long> issueNumbersExpected = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            issueNumbersExpected.add(createIssue());
        }

        int per_page = 2;

        WaitUtil.waitFor(
            () -> {
                List<Long> issueNumbersActual = IssueUtils.getIssueNumbersByState(rwAuthSpec, "open");
                return issueNumbersActual.size() == 3 ? issueNumbersActual : null;
            },
            Duration.ofSeconds(10),
            Duration.ofSeconds(1),
            "Creating 3 issues"
        );

        PaginationResult page1 = IssueUtils.getIssuesPage(rwAuthSpec, 1, per_page);
        PaginationResult page2 = IssueUtils.getIssuesPage(rwAuthSpec, 2, per_page);
        List<Long> issueNumbersActual = new ArrayList<>(page1.issueNumbers());
        issueNumbersActual.addAll(page2.issueNumbers());

        assertAll(
            () -> assertThat(page1.issueNumbers(), hasSize(2)),
            () -> assertThat(page1.headerList(), startsWithIgnoringCase("<https://api.github.com/repositories/")),
            () -> assertThat(page1.headerList(), containsString("rel=\"next\"")),
            () -> assertThat(page2.issueNumbers(), hasSize(1)),
            () -> assertThat(page2.headerList(), startsWithIgnoringCase("<https://api.github.com/repositories/")),
            () -> assertThat(page2.headerList(), containsString("rel=\"prev\"")),
            () -> assertThat(Collections.disjoint(page1.issueNumbers(), page2.issueNumbers()), is(true)),
            () -> assertThat(issueNumbersActual, containsInAnyOrder(issueNumbersExpected.toArray(new Long[0])))
        );
    }

    @Test
    void verifyXRateLimitHeaders() {
        Response response = RestAssured.given()
            .spec(rwAuthSpec)
            .get(ISSUES)
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

            () -> assertThat(limit, matchesPattern("\\d+")),
            () -> assertThat(remaining, matchesPattern("\\d+")),
            () -> assertThat(used, matchesPattern("\\d+")),
            () -> assertThat(resource, equalTo("core")),
            () -> assertThat(reset, matchesPattern("\\d+")),

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


    private static Stream<Arguments> fieldValidationErrorScenarios() {
        return Stream.of(
            Arguments.of(
                "empty required field",
                Map.of("title", "", "body", "valid body"),
                "Validation Failed"
            ),
            Arguments.of(
                "invalid field type",
                """
                    {"title": "test", "milestone": "one"}""",
                "Validation Failed"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fieldValidationErrorScenarios")
    void verifyValidationFieldTypeErrorFormats(
        String scenario,
        Object body,
        String expectedMessageFragment
    ) {
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(ISSUES)
            .then()
            .statusCode(422)
            .body("message", containsStringIgnoringCase(expectedMessageFragment))
            .body("documentation_url", notNullValue())
            .body("status", equalTo("422"))
            .body("errors", notNullValue())
            .body("errors", not(empty()));
    }

    private static Stream<Arguments> validationErrorScenarios() {
        return Stream.of(
            Arguments.of(
                "schema violation",
                """
                    {"title": "test", "labels": {"name": "bug"}}""",
                "Invalid request"
            ),
            Arguments.of(
                "missing required field",
                Map.of("body", "no title"),
                "wasn't supplied"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validationErrorScenarios")
    void verifyValidationErrorFormats(
        String scenario,
        Object body,
        String expectedMessageFragment
    ) {
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(ISSUES)
            .then()
            .statusCode(422)
            .body("message", containsStringIgnoringCase(expectedMessageFragment))
            .body("documentation_url", notNullValue())
            .body("status", equalTo("422"))
            .body("errors", nullValue());

    }

    @Test
    void issueResponseMatchesIssueSchema() {
        Long issueNumber = createIssue();
        RestAssured.given()
            .spec(rwAuthSpec)
            .accept(ContentType.JSON)
            .when()
            .get("/repos/{owner}/{repo}/issues/{issue_number}", issueNumber)
            .then()
            .statusCode(200)
            .log().all()
            .body(matchesJsonSchemaInClasspath("schemas/issue-schema.json"));
    }


}
