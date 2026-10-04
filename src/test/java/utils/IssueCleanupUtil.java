package utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class IssueCleanupUtil {
    public static void closeAllOpenIssues(RequestSpecification spec) {
        int page = 1;
        while (true) {
            List<Map<String, Object>> open = RestAssured.given()
                .spec(spec)
                .queryParam("state", "open")
                .queryParam("per_page", 100)
                .queryParam("page", page)
                .when()
                .get("/repos/{owner}/{repo}/issues")
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

}
