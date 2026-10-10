package org.nokhrin.github.graphql;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.nokhrin.github.config.Spec;
import org.nokhrin.github.config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GraphQLBaseTest {
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

        rwAuthSpec = Spec.withAuth(baseSpec, config.readAndWriteToken());

        roAuthSpec = Spec.withAuth(baseSpec, config.readToken());
    }
}
