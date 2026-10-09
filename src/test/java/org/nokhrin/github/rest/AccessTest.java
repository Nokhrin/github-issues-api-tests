package org.nokhrin.github.rest;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.notNullValue;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUES;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AccessTest extends RestBaseTest {


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
        Long issueNumber = createIssue(randomIssueTitle(), randomIssueDescription());

        try {
            RestAssured.given()
                .spec(roAuthSpec)
                .when()
                .patch("/repos/{owner}/{repo}/issues/{issue_number}", issueNumber)
                .then()
                .statusCode(403)
                .body("message", containsStringIgnoringCase("not accessible"))
                .body("documentation_url", notNullValue())
                .log().ifValidationFails()
            ;
        } finally {
            closeIssue(issueNumber);
        }
    }
}
