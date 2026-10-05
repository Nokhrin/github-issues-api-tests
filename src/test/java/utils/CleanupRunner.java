package utils;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.nokhrin.github.config.TestConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CleanupRunner {
    private RequestSpecification rwAuthSpec;

    @BeforeAll
    void setUp() {
        TestConfig config = TestConfig.fromEnv();

        RestAssured.config = RestAssured.config()
            .logConfig(LogConfig.logConfig().blacklistHeader("Authorization"));

        RequestSpecification baseSpec = new RequestSpecBuilder()
            .setBaseUri(config.githubBaseUrl())
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .addHeader("User-Agent", "sqa-lab-tests/1.0")
            .build();

        rwAuthSpec = new RequestSpecBuilder()
            .addRequestSpecification(baseSpec)
            .addPathParam("owner", config.githubOwner())
            .addPathParam("repo", config.githubRepo())
            .addHeader("Authorization", "Bearer " + config.readAndWriteToken())
            .build();
    }

    @Test
    @EnabledIfSystemProperty(named = "cleanup", matches = "true")
    void runCleanup() {
        IssueCleanupUtil.closeOpenIssues(rwAuthSpec);
    }
}
