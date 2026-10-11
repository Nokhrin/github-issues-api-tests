package org.nokhrin.github.rest;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.nokhrin.github.utils.IssuesRest;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.notNullValue;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUES;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUE_BY_NUMBER;
import static org.nokhrin.github.utils.IssuesRest.closeOpenIssues;
import static org.nokhrin.github.utils.IssuesRest.createIssue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AccessTest extends RestBaseTest {

    @AfterAll
    void tearDown() {
        closeOpenIssues(rwAuthSpec);
    }


    @Test
    @Tag("read")
    void createIssueReadOnlyFailed() {
        RestAssured.given()
            .spec(roAuthSpec)
            .when()
            .post(ISSUES)
            .then()
            .statusCode(403)
            .body("message", containsStringIgnoringCase("not accessible"))
            .body("documentation_url", notNullValue())
            .log().ifValidationFails()
        ;
    }

    @Test
    @Tag("write")
    void updateIssueReadOnlyFailed() {
        var testIssue = IssuesRest.randomIssue();
        Long issueNumber = createTestIssue(testIssue.title(), testIssue.description());

        RestAssured.given()
            .spec(roAuthSpec)
            .when()
            .patch(ISSUE_BY_NUMBER, issueNumber)
            .then()
            .statusCode(403)
            .body("message", containsStringIgnoringCase("not accessible"))
            .body("documentation_url", notNullValue())
            .log().ifValidationFails()
        ;
    }
}
