package org.nokhrin.github.cleanup;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.TestInstance;
import org.nokhrin.github.utils.IssuesRest;
import org.nokhrin.github.utils.Spec;
import org.nokhrin.github.utils.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CleanupRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupRunner.class);
    private static String baseUrl, owner, repo;
    private static TestConfig config;
    private static RequestSpecification rwAuthSpec;


    static void main() {
        LOGGER.debug("Close opened issues: started");
        config = TestConfig.fromEnv();

        baseUrl = config.githubBaseUrl();
        owner = config.githubOwner();
        repo = config.githubRepo();


        RestAssured.config = RestAssured.config()
            .logConfig(LogConfig.logConfig().blacklistHeader("Authorization"));

        rwAuthSpec =
            Spec.withAuth(
                Spec.withOwnerAndRepo(Spec.base(config), owner, repo),
                config.readAndWriteToken()
            );


        IssuesRest.closeOpenIssues(rwAuthSpec);
        LOGGER.debug("Close opened issues: completed");
    }
}
