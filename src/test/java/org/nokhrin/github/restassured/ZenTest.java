
package org.nokhrin.github.restassured;

import io.restassured.config.LogConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.nokhrin.github.model.Issue;
import org.nokhrin.github.config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZenTest extends BaseTest {

    @Test
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
