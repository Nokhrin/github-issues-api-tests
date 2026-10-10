package org.nokhrin.github.rest;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.nokhrin.github.config.Spec;
import org.nokhrin.github.config.TestConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class RestBaseTest {
    protected String baseUrl, owner, repo;
    protected TestConfig config;
    protected RequestSpecification baseSpec, rwAuthSpec, roAuthSpec;

    @BeforeAll
    void setUp() {
        config = TestConfig.fromEnv();

        baseUrl = config.githubBaseUrl();
        owner = config.githubOwner();
        repo = config.githubRepo();

        RestAssured.config = RestAssured.config()
            .logConfig(LogConfig.logConfig().blacklistHeader("Authorization"));

        baseSpec = Spec.base(config);
        rwAuthSpec = Spec.withOwnerAndRepo(
            Spec.withAuth(baseSpec, config.readAndWriteToken()),
            owner,
            repo
        );

        roAuthSpec = Spec.withOwnerAndRepo(
            Spec.withAuth(baseSpec, config.readToken()),
            owner,
            repo
        );

    }

}
