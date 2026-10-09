package org.nokhrin.github.rest;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.nokhrin.github.config.GitHubEndpoints.REPO;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepoTest extends RestBaseTest {

    @Feature("GitHub Issues API")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    public void verifyIssuesContainsFields() {
        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get(REPO)
            .then()
            .statusCode(200)
            .body("name", equalTo(config.githubRepo()))
            .body("full_name", equalTo(config.githubOwner() + "/" + config.githubRepo()))
            .body("owner.login", equalTo(config.githubOwner()))
            .body("private", equalTo(true))
            .body("default_branch", equalTo("main"))
            .body(matchesJsonSchemaInClasspath("schemas/repository-schema.json"))
            .log().ifValidationFails()
        ;
    }

    @Test
    public void requestAuthResponseContainsField() {
        RestAssured.given()
            .spec(unAuthSpec)
            .when()
            .get(REPO)
            .then()
            .statusCode(404)
            .log().ifValidationFails()
        ;

        RestAssured.given()
            .spec(rwAuthSpec)
            .when()
            .get(REPO)
            .then()
            .statusCode(200)
            .body("owner.login", equalTo(config.githubOwner()))
            .log().ifValidationFails()
        ;
    }

    @Test
    void publicReposUnauth_responseReceived() {
        RestAssured.given()
            .spec(baseSpec)
            .when()
            .get("/repositories")
            .then()
            .statusCode(200)
            .log().ifValidationFails()
        ;
    }

    @Test
    void repoResponseMatchesJsonSchema() {
        RestAssured.given()
            .spec(roAuthSpec)
            .when()
            .get(REPO)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/repository-schema.json"))
            .log().all()
        ;
    }
}
