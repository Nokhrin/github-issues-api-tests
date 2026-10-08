package org.nokhrin.github.graphql;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.nokhrin.github.utils.ReadResource;

import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.nullValue;
import static org.nokhrin.github.config.Endpoints.GRAPHQL;

public class IssueTest extends GraphQLBaseTest {

    @Test
    public void verifyIssuesQueryReturnsValidIssues() {
        String query = ReadResource.readResource("graphql/get-issues.graphql");

        Map<String, Object> variables = Map.of(
            "owner", config.githubOwner(),
            "name", config.githubRepo(),
            "limit", 5
        );
        Map<String, Object> requestBody = Map.of(
            "query", query,
            "variables", variables
        );
        RestAssured.given()
            .spec(roAuthSpec)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)
            .when()
            .post(GRAPHQL)
            .then()
            .statusCode(200)
            .log().all()
            .body("errors", nullValue())
            .body(matchesJsonSchemaInClasspath(
                "schemas/get-issues-response.json"
            ))
        ;
    }

}
