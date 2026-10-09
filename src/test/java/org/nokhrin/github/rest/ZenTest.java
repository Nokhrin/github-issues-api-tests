package org.nokhrin.github.rest;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZenTest extends RestBaseTest {

    @Test
    @Tag("read")
    public void verifyHealthcheck200Test() {
        RestAssured.given()
            .spec(baseSpec)
            .when()
            .get("/zen")
            .then()
            .statusCode(200)
            .log().ifValidationFails();
    }

    @Test
    @Tag("read")
    public void verifyHealthcheckNotEmptyBodyTest() {
        RestAssured.given()
            .spec(baseSpec)
            .when()
            .get("/zen")
            .then()
            .statusCode(200)
            .body(not(emptyString()))
            .log().ifValidationFails()
        ;
    }
}
