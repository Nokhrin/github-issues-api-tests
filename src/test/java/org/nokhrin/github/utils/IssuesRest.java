package org.nokhrin.github.utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.nokhrin.github.cleanup.CleanupException;
import org.nokhrin.github.model.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUES;
import static org.nokhrin.github.config.GitHubEndpoints.ISSUE_BY_NUMBER;

public class IssuesRest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IssuesRest.class);
    public static final String TEST_TITLE_PREFIX = "Issue [test:";


    public static void closeIssueQuietly(RequestSpecification spec, long issueNumber) {
        Response response = RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(Map.of("state", "closed"))
            .when()
            .patch(ISSUE_BY_NUMBER, issueNumber)
            .then()
            .extract()
            .response();

        int status = response.statusCode();

        if (status >= 200 && status < 300) {
            return;
        }

        if (status == 404) {
            LOGGER.info("Issue {} already absent during cleanup", issueNumber);
            return;
        }

        throw new CleanupException(
            "Cannot close issue " + issueNumber
                + ", status=" + status
                + ", body=" + response.asString()
        );
    }

    public static void closeIssueForTest(RequestSpecification spec, long issueNumber) {
        RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(Map.of("state", "closed"))
            .when()
            .patch(ISSUE_BY_NUMBER, issueNumber)
            .then()
            .statusCode(200)
            .log().ifValidationFails();
    }

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
                .log().ifValidationFails()
                .extract()
                .jsonPath().getList("");

            if (open.isEmpty()) break;

            for (Map<String, Object> item : open) {
                if (item.containsKey("pull_request")) {
                    continue;
                }
                Number issueNumber = (Number) item.get("number");
                closeIssue(spec, issueNumber.longValue());
            }
            page++;
        }
    }


    public static List<Long> getIssueNumbersByState(RequestSpecification spec, String state) {
        return RestAssured.given()
            .spec(spec)
            .queryParam("filter", "all")
            .queryParam("state", state)
            .queryParam("since", Instant.now().minus(Duration.ofMinutes(10).truncatedTo(ChronoUnit.SECONDS)).toString())
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
        Wait.forCondition(
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
        return createIssue(spec, new Issue().setTitle(title).setBody(body)
        );
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
            .jsonPath()
            .getLong("number");
    }


    public static void closeIssue(RequestSpecification spec, Long issueNumber) {
        Wait.forCondition(
            () -> {
                int status = RestAssured.given()
                    .spec(spec)
                    .log().all()
                    .when()
                    .get(ISSUE_BY_NUMBER, issueNumber)
                    .then()
                    .log().all()
                    .extract()
                    .statusCode();
                return status == 200 ? issueNumber : null;
            },
            Duration.ofSeconds(15),
            Duration.ofSeconds(5),
            "issue " + issueNumber + " readable before close"
        );

        RestAssured.given()
            .spec(spec)
            .contentType(ContentType.JSON)
            .body(Map.of("state", "closed"))
            .patch(ISSUE_BY_NUMBER, issueNumber)
            .then()
            .statusCode(200);
    }

    public record IssueData(String title, String description) {
    }

    public static IssueData randomIssue() {
        String id = UUID.randomUUID().toString();

        return new IssueData(
            "Issue [test:" + id + "]",
            "Description for test issue. Correlation ID: " + id
        );
    }
}
