package org.nokhrin.github.restassured;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.nokhrin.github.config.TestConfig;
import org.nokhrin.github.model.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.nokhrin.github.config.Endpoints.ISSUES;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {
    protected final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);

    protected String baseUrl, owner, repo;
    protected TestConfig config;
    protected RequestSpecification baseSpec, unAuthSpec, rwAuthSpec, roAuthSpec;

    @BeforeAll
    void setUp() {
        config = TestConfig.fromEnv();
        LOGGER.debug("Загружена конфигурация: " + config);

        baseUrl = config.githubBaseUrl();
        owner = config.githubOwner();
        repo = config.githubRepo();

        RestAssured.config = RestAssured.config()
            .logConfig(LogConfig.logConfig().blacklistHeader("Authorization"));

        baseSpec = new RequestSpecBuilder()
            .setBaseUri(baseUrl)
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2026-03-10")
            .addHeader("User-Agent", owner)
            .build();

        unAuthSpec = new RequestSpecBuilder()
            .addRequestSpecification(baseSpec)
            .addPathParam("owner", owner)
            .addPathParam("repo", repo)
            .build();

        rwAuthSpec = new RequestSpecBuilder()
            .addRequestSpecification(baseSpec)
            .addPathParam("owner", owner)
            .addPathParam("repo", repo)
            .addHeader("Authorization", "Bearer " + config.readAndWriteToken())
            .build();

        roAuthSpec = new RequestSpecBuilder()
            .addRequestSpecification(baseSpec)
            .addPathParam("owner", owner)
            .addPathParam("repo", repo)
            .addHeader("Authorization", "Bearer " + config.readToken())
            .build();

    }

    protected final Long createIssue(String title, String body) {
        return createIssue(new Issue().setTitle(title).setBody(body));
    }

    protected final Long createIssue() {
        return createIssue(randomIssueTitle(), randomIssueDescription());
    }

    protected final Long createIssue(Object requestBody) {
        return RestAssured.given()
            .spec(rwAuthSpec)
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

    protected final void closeIssue(Long issueNum) {
        if (issueNum == null) {
            return;
        }
        RestAssured.given()
            .spec(rwAuthSpec)
            .contentType(ContentType.JSON)
            .body(Map.of("state", "closed"))
            .patch("/repos/{owner}/{repo}/issues/{number}", issueNum)
            .then()
            .statusCode(200)
        ;
    }

    protected final String randomIssueTitle() {
        return "issue " + RandomStringUtils.randomAlphabetic(5);
    }

    protected final String randomIssueDescription() {
        return "Description of new issue";
    }

}
