package org.nokhrin.github.rest;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nokhrin.github.model.Issue;
import org.nokhrin.github.utils.IssuesRest;
import org.nokhrin.github.utils.Parser;
import org.nokhrin.github.utils.Wait;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUES;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUE_BY_NUMBER;
import static org.nokhrin.github.config.GitHubHeaders.API_VERSION;
import static org.nokhrin.github.config.GitHubMediaTypes.JSON;
import static org.nokhrin.github.utils.IssuesRest.closeIssue;
import static org.nokhrin.github.utils.IssuesRest.createIssue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IssueTest extends RestBaseTest {


    @Test
    @Tag("write")
    public void createIssueRawJsonValid_Created() {
        var testIssue = IssuesRest.randomIssue();
        String requestBody = """
            {"title": "%s", "body": "%s"}
            """.formatted(testIssue.title(), testIssue.description());

        Long issueNum = createIssue(rwAuthSpec, requestBody);
        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get(ISSUE_BY_NUMBER, issueNum)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .body("title", equalTo(testIssue.title()))
            .body("body", equalTo(testIssue.description()))
            .body(matchesJsonSchemaInClasspath("schemas/rest/issue.json"))
        ;

    }

    @Test
    @Tag("write")
    public void verifyPostPojo() {
        var testIssue = IssuesRest.randomIssue();
        Long issueNum = createTestIssue(testIssue.title(), testIssue.description());
        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get(ISSUE_BY_NUMBER, issueNum)
            .then()
            .statusCode(200)
            .extract()
            .as(Issue.class);
    }


    @Test
    @Tag("write")
    public void verifyPostMap() {
        var testIssue = IssuesRest.randomIssue();
        Map<String, Object> requestBody = Map.of(
            "title", testIssue.title(),
            "body", testIssue.description()
        );
        Long number = RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post(ISSUES)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath().getLong("number");


        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get(ISSUE_BY_NUMBER, number)
            .then()
            .statusCode(200)
            .body("body", equalTo(testIssue.description()))
        ;

    }

    @Test
    @Tag("write")
    public void verifyPostPojoWithJsonPath() {
        var testIssue = IssuesRest.randomIssue();
        Long issueNum = createTestIssue(testIssue.title(), testIssue.description());
        Issue issue = RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get(ISSUE_BY_NUMBER, issueNum)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .as(Issue.class);

        assertEquals(owner, issue.getUser().getLogin());
    }

    @Test
    @Tag("write")
    public void createIssueWithQueryParamsFailure() {
        var testIssue = IssuesRest.randomIssue();
        RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("title", testIssue.title())
            .queryParam("body", testIssue.description())
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
    @Tag("write")
    void invalidTitleValidationFails(String caseName, String title) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("body", "description for issue: " + title);

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
    @Tag("write")
    void createCloseStateVerified() {
        var testIssue = IssuesRest.randomIssue();
        Long issueNumber = createTestIssue(testIssue.title(), testIssue.description());

        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .when()
            .get(ISSUE_BY_NUMBER, issueNumber)
            .then()
            .statusCode(200)
            .body("title", equalTo(testIssue.title()))
            .body("state", equalTo("open"))
            .log().ifValidationFails()
        ;

        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(Map.of("state", "closed"))
            .when()
            .patch(ISSUE_BY_NUMBER, issueNumber)
            .then()
            .statusCode(200)
            .body("title", equalTo(testIssue.title()))
            .body("state", equalTo("closed"))
            .body("closed_at", notNullValue())
            .log().ifValidationFails()
        ;

        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .when()
            .get(ISSUE_BY_NUMBER, issueNumber)
            .then()
            .statusCode(200)
            .body("title", equalTo(testIssue.title()))
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
    @Tag("write")
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
    @Tag("write")
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
    @Tag("read")
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
    @Tag("read")
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
    @Tag("write")
    void requestJsonBodySyntaxError_responseBodyValid() {
        var testIssue = IssuesRest.randomIssue();
        String invalidBody = """
            {
              "title": "%s",
              "body": "%s",
              "assignees": ["octocat"],
              "milestone": 1,
              "labels": ["bug"],
            }
            """.formatted(testIssue.title(), testIssue.description());
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
            .body(not(hasItem(testIssue.title())))
            .log().ifValidationFails()
        ;
    }

    @Test
    @Tag("read")
    void verifyNotFoundResponseBody() {
        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get(ISSUE_BY_NUMBER, -1)
            .then()
            .statusCode(404)
            .body("message", equalTo("Not Found"))
            .body("documentation_url", startsWith("https://docs.github.com"))
            .body("status", equalTo("404"))
            .log().ifValidationFails()
        ;

    }


    @Test
    @Tag("write")
    void verifyFilterByState_openedIssueReturned() {
        var testIssue = IssuesRest.randomIssue();

        Long openedIssueNumber = createTestIssue(testIssue.title(), testIssue.description());

        Wait.forCondition(
            () -> {
                List<Long> allIssuesNumbers = IssuesRest.getIssueNumbersByState(rwAuthSpec, "open");
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
    @Tag("write")
    void verifyFilterByState_closedIssueReturned() {
        var testIssue = IssuesRest.randomIssue();
        Long closedIssueNumber = createTestIssue(testIssue.title(), testIssue.description());

        closeIssue(rwAuthSpec, closedIssueNumber);

        Wait.forCondition(
            () -> {
                List<Long> allIssuesNumbers = IssuesRest.getIssueNumbersByState(rwAuthSpec, "closed");
                return allIssuesNumbers.contains(closedIssueNumber) ? allIssuesNumbers : null;
            },
            Duration.ofSeconds(15),
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
    @Tag("write")
    void verifyFilterByState_all() {
        var testOpenedIssue = IssuesRest.randomIssue();
        String openedIssueTitle = testOpenedIssue.title();
        String openedIssueDescription = testOpenedIssue.description();
        Long openedIssueNumber = createTestIssue(openedIssueTitle, openedIssueDescription);

        var testClosedIssue = IssuesRest.randomIssue();
        String closedIssueTitle = testClosedIssue.title();
        String closedIssueDescription = testClosedIssue.description();
        Long closedIssueNumber = createTestIssue(closedIssueTitle, closedIssueDescription);
        closeIssue(rwAuthSpec, closedIssueNumber);

        Wait.forCondition(
            () -> {
                List<Long> allIssuesNumbers = IssuesRest.getIssueNumbersByState(rwAuthSpec, "all");
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
    @Tag("write")
    void verifyPaginationWithLinkNavigation() {
        List<Long> created = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            IssuesRest.IssueData testIssue = IssuesRest.randomIssue();
            Long number = createTestIssue(testIssue.title(), testIssue.description());
            created.add(number);
            IssuesRest.waitIssueReadable(rwAuthSpec, number);
        }

        Wait.forCondition(
            () -> {
                List<Long> open = IssuesRest.getIssueNumbersByState(rwAuthSpec, "open");
                return open.containsAll(created) ? open : null;
            },
            Duration.ofSeconds(15),
            Duration.ofSeconds(1),
            "созданные issue найдены, состояние state=open"
        );

        int perPage = 2;

        RequestSpecification absoluteUrlSpec = new RequestSpecBuilder()
            .addHeader("Authorization", "Bearer " + config.readAndWriteToken())
            .addHeader("Accept", JSON)
            .addHeader("X-GitHub-Api-Version", API_VERSION)
            .setUrlEncodingEnabled(false)
            .build();

        Response response = RestAssured.given()
            .spec(rwAuthSpec)
            .queryParam("state", "open")
            .queryParam("per_page", perPage)
            .when()
            .get(ISSUES)
            .then()
            .statusCode(200)
            .extract()
            .response();

        Set<Long> seen = new HashSet<>();
        int pageCount = 0;

        while (true) {
            pageCount++;

            List<Long> pageIds = response.jsonPath().getList("number", Long.class);

            assertThat(pageIds, hasSize(lessThanOrEqualTo(perPage)));

            Set<Long> pageSet = new HashSet<>(pageIds);
            assertEquals(
                pageIds.size(),
                pageSet.size(),
                "Внутри страницы есть дубликаты number: " + pageIds
            );

            Set<Long> overlap = new HashSet<>(seen);
            overlap.retainAll(pageSet);
            assertTrue(
                overlap.isEmpty(),
                "Страницы пересекаются: " + overlap
            );

            seen.addAll(pageSet);

            Map<String, String> links = Parser.parseLinkHeader(response.getHeader("Link"));

            if (!links.containsKey("next")) {
                assertFalse(links.containsKey("next"));

                if (pageCount > 1) {
                    assertTrue(
                        links.containsKey("prev"),
                        "Не первая страница должна содержать rel=\"prev\": " + links
                    );
                }

                break;
            }

            String nextUrl = links.get("next");

            response = RestAssured.given()
                .spec(absoluteUrlSpec)
                .when()
                .get(nextUrl)
                .then()
                .statusCode(200)
                .extract()
                .response();
        }

        assertTrue(
            seen.containsAll(created)
        );

        assertThat(
            pageCount,
            greaterThanOrEqualTo(2)
        );
    }

    @Test
    @Tag("read")
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
    @Tag("read")
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
    @Tag("write")
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
    @Tag("write")
    void issueResponseMatchesIssueSchema() {
        var testIssue = IssuesRest.randomIssue();
        Long issueNumber = createTestIssue(testIssue.title(), testIssue.description());
        RestAssured.given()
            .spec(rwAuthSpec)
            .accept(ContentType.JSON)
            .when()
            .get(ISSUE_BY_NUMBER, issueNumber)
            .then()
            .statusCode(200)
            .log().all()
            .body(matchesJsonSchemaInClasspath("schemas/rest/issue.json"));
    }
}
