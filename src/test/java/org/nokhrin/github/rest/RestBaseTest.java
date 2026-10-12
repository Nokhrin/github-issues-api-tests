package org.nokhrin.github.rest;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.nokhrin.github.utils.IssuesRest;
import org.nokhrin.github.utils.Spec;
import org.nokhrin.github.utils.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class RestBaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestBaseTest.class);
    protected String baseUrl, owner, repo;
    protected TestConfig config;
    protected RequestSpecification baseSpec, unAuthSpec, rwAuthSpec, roAuthSpec;

    @BeforeAll
    void setUp() {
        config = TestConfig.fromEnv();

        baseUrl = config.githubBaseUrl();
        owner = config.githubOwner();
        repo = config.githubRepo();

        RestAssured.config = RestAssured.config()
            .logConfig(LogConfig.logConfig().blacklistHeader("Authorization"));

        baseSpec = Spec.base(config);

        unAuthSpec = Spec.withOwnerAndRepo(
            baseSpec,
            owner,
            repo
        );

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

    private final Set<Long> createdIssueNumbers = ConcurrentHashMap.newKeySet();

    protected Long createTestIssue(String title, String body) {
        Long number = IssuesRest.createIssue(rwAuthSpec, title, body);
        createdIssueNumbers.add(number);
        return number;
    }

    @AfterEach
    void closeCreatedTestIssues() {
        for (Long number : createdIssueNumbers) {
            try {
                IssuesRest.closeIssueQuietly(rwAuthSpec, number);
            } catch (RuntimeException e) {
                LOGGER.warn("Cleanup failed for issue {}", number, e);
            }
        }

        createdIssueNumbers.clear();
    }

}
