package org.nokhrin.github.cleanup;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.TestInstance;
import org.nokhrin.github.config.GitHubHeaders;
import org.nokhrin.github.config.GitHubMediaTypes;
import org.nokhrin.github.config.TestConfig;
import org.nokhrin.github.utils.IssueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CleanupRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupRunner.class);
    private static String baseUrl, owner, repo;
    private static TestConfig config;
    private static RequestSpecification rwAuthSpec;


    static void main() {
        LOGGER.debug("Clean");
        config = TestConfig.fromEnv();

        baseUrl = config.githubBaseUrl();
        owner = config.githubOwner();
        repo = config.githubRepo();


        RestAssured.config = RestAssured.config()
            .logConfig(LogConfig.logConfig().blacklistHeader("Authorization"));

        RequestSpecification baseSpec = new RequestSpecBuilder()
            .setBaseUri(baseUrl)
            .addHeader("Accept", GitHubMediaTypes.JSON)
            .addHeader("X-GitHub-Api-Version", GitHubHeaders.API_VERSION)
            .addHeader("User-Agent", owner)
            .build();

        rwAuthSpec = new RequestSpecBuilder()
            .addRequestSpecification(baseSpec)
            .addPathParam("owner", config.githubOwner())
            .addPathParam("repo", config.githubRepo())
            .addHeader("Authorization", "Bearer " + config.readAndWriteToken())
            .build();


        IssueUtils.closeOpenIssues(rwAuthSpec);
    }
}
