package org.nokhrin.github.graphql;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.nokhrin.github.config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GraphQLBaseTest {
    protected final Logger LOGGER = LoggerFactory.getLogger(GraphQLBaseTest.class);

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
            .build();

        rwAuthSpec = new RequestSpecBuilder()
            .addRequestSpecification(baseSpec)
            .addHeader("Authorization", "Bearer " + config.readAndWriteToken())
            .build();

        roAuthSpec = new RequestSpecBuilder()
            .addRequestSpecification(baseSpec)
            .addHeader("Authorization", "Bearer " + config.readToken())
            .build();

    }
}
