package org.nokhrin.github.restassured;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nokhrin.github.model.Issue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IssueTest extends BaseTest {

    @Test
    public void createIssueRawCreated() {
        String issueTitle = randomIssueTitle();
        String issueDescription = randomIssueDescription();
        String requestBody = """
            {"title": "%s", "body": "%s"}
            """.formatted(issueTitle, issueDescription);
        Long issueNum = null;

        try {
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
        } finally {
            closeIssue(issueNum);
        }

    }

    @Test
    public void verifyPostPojo() {
        Long issueNum = createIssue(randomIssueTitle(), randomIssueDescription());
        try {
            RestAssured.given()
                .spec(rwAuthSpec)
                .when()
                .get("/repos/{owner}/{repo}/issues/{number}", issueNum)
                .then()
                .statusCode(200)
                .extract()
                .as(Issue.class);
        } finally {
            closeIssue(issueNum);
        }
    }


    @Test
    public void verifyPostMap() {
        Map<String, Object> requestBody = Map.of(
            "title", randomIssueTitle(),
            "body", randomIssueDescription()
        );
        Long number = null;
        try {
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


        } finally {
            closeIssue(number);
        }
    }

    @Test
    public void verifyPostPojoWithJsonPath() {
        Long issueNum = createIssue(randomIssueTitle(), randomIssueDescription());
        try {
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
        } finally {
            closeIssue(issueNum);
        }
    }

    @Test
    public void createIssueWithQueryParamsFailure() {
        RestAssured.given()
            .log().all()
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

        try {
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

        } finally {
            closeIssue(issueNumber);
        }
    }
}
