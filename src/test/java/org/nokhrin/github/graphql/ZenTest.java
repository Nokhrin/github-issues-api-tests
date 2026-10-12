package org.nokhrin.github.graphql;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.nokhrin.github.utils.ReadResource;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.nokhrin.github.config.GitHubEndpoints.GRAPHQL;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZenTest extends GraphQLBaseTest {

    @Test
    @Tag("read")
    public void verify_GraphQLendPointAvailable_tokenValid_gqlRequestValid() {
        String query = ReadResource.readResource("graphql/queries/healthcheck.graphql");

        Response response = RestAssured.given()
            .spec(roAuthSpec)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of("query", query))
            .when()
            .post(GRAPHQL)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .body("errors", nullValue())
            .extract()
            .response();
        int limit = response.path("data.rateLimit.limit");
        int remaining = response.path("data.rateLimit.remaining");
        int used = response.path("data.rateLimit.used");

        assertAll(
            () -> assertTrue(limit >= 0),
            () -> assertTrue(remaining >= 0),
            () -> assertTrue(used >= 0),
            () -> assertTrue(remaining <= limit),
            () -> assertEquals(limit, remaining + used)
        );
    }
}
