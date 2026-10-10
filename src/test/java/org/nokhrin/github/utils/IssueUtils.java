package org.nokhrin.github.utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.RandomStringUtils;
import org.nokhrin.github.model.Issue;
import org.nokhrin.github.rest.PaginationResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUES;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUE_BY_NUMBER;

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
            .patch(ISSUE_BY_NUMBER, number)
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

    public static void waitIssueReadable(RequestSpecification spec, Long issueNumber) {
        WaitUtil.waitFor(
            () -> {
                int status = RestAssured.given()
                    .spec(spec)
                    .when()
                    .get(ISSUE_BY_NUMBER, issueNumber)
                    .then()
                    .body("number", equalTo(issueNumber.intValue()))
                    .extract()
                    .statusCode();
                return status == 200 ? issueNumber : null;
            },
            Duration.ofSeconds(15),
            Duration.ofSeconds(1),
            "issue #" + issueNumber + " is readable"
        );
    }

    public static Long createIssue(RequestSpecification spec, String title, String body) {
        return createIssue(spec, new Issue().setTitle(title).setBody(body));
    }

    public static Long createIssue(RequestSpecification spec) {
        return createIssue(spec, randomIssueTitle(), randomIssueDescription());
    }

    public static Long createIssue(RequestSpecification spec,
                                   Object requestBody) {
        return RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post(ISSUES)
            .then()
            .statusCode(201)
            .log().ifValidationFails()
            .extract()
            .jsonPath().getLong("number");
    }

    public static void closeIssue(RequestSpecification spec, Long issueNum) {
        if (issueNum == null) {
            return;
        }
        RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(Map.of("state", "closed"))
            .patch(ISSUE_BY_NUMBER, issueNum)
            .then()
            .statusCode(200)
        ;
    }

    public static String randomIssueTitle() {
        return "issue " + RandomStringUtils.randomAlphabetic(5);
    }

    public static String randomIssueDescription() {
        return "Description of new issue";
    }
}
