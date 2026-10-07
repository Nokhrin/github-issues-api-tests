package org.nokhrin.github.restassured.utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.nokhrin.github.config.Endpoints.ISSUES;

public class IssueUtils {
    public static void closeOpenIssues(RequestSpecification spec) {
        int page = 1;
        while (true) {
            List<Map<String, Object>> open = RestAssured.given()
                .spec(spec)
                .queryParam("state", "open")
                .queryParam("per_page", 100)
                .queryParam("page", page)
                .when()
                .get(ISSUES)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("");

            if (open.isEmpty()) break;

            for (Map<String, Object> item : open) {
                if (item.containsKey("pull_request")) continue;
                long number = ((Number) item.get("number")).longValue();
                closeIssue(spec, number);
            }
            page++;
        }
    }

    private static void closeIssue(RequestSpecification spec, long number) {
        RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(Map.of("state", "closed"))
            .patch("/repos/{owner}/{repo}/issues/{number}", number)
            .then()
            .statusCode(200);
    }

    public static PaginationResult getIssuesPage(RequestSpecification spec, int page, int perPage) {
        Response response = RestAssured.given()
            .spec(spec)
            .queryParam("filter", "all")
            .queryParam("state", "open")
            .queryParam("per_page", perPage)
            .queryParam("page", page)
            .when()
            .get(ISSUES)
            .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract()
            .response();
        return new PaginationResult(
            response.jsonPath().getList("number", Long.class),
            response.getHeader("Link"));
    }


    public static List<Long> getIssueNumbersByState(RequestSpecification spec, String state) {
        return RestAssured.given()
            .spec(spec)
            .queryParam("filter", "all")
            .queryParam("state", state)
            .queryParam("since", Instant.now().minusSeconds(600).toString())
            .queryParam("per_page", 100)
            .when()
            .get(ISSUES)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("number", Long.class);
    }

}
