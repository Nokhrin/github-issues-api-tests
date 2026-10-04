package org.nokhrin.github.restassured;

import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.Matchers.equalTo;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepoTest extends BaseTest {

    @Test
    public void verifyIssuesContainTest() {
        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get("/repos/{owner}/{repo}")
            .then()
            .statusCode(200)
            .body("name", equalTo(config.githubRepo()))
            .body("full_name", equalTo(config.githubOwner() + "/" + config.githubRepo()))
            .body("owner.login", equalTo(config.githubOwner()))
            .body("private", equalTo(true))
            .body("default_branch", equalTo("main"))
            .log().ifValidationFails()
        ;
    }

    @Test
    public void requestAuthResponseContainsField() {
        RestAssured.given()
            .spec(unAuthSpec)
            .when()
            .get("/repos/{owner}/{repo}")
            .then()
            .statusCode(404)
            .log().ifValidationFails()
        ;

        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get("/repos/{owner}/{repo}")
            .then()
            .statusCode(200)
            .body("owner.login", equalTo(config.githubOwner()))
            .log().ifValidationFails()
        ;
    }

}
