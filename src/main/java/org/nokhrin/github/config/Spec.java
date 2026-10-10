package org.nokhrin.github.config;


import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Spec {
    private static final Logger LOGGER = LoggerFactory.getLogger(Spec.class);
    private static boolean configured = false;

    private Spec() {
    }

    public static RequestSpecification base(TestConfig config) {
        if (!configured) {
            LOGGER.debug("Загружена конфигурация: {}", config);
            RestAssured.config = RestAssured.config()
                .logConfig(LogConfig.logConfig().blacklistHeader("Authorization"));
            configured = true;
        }
        return new RequestSpecBuilder()
            .setBaseUri(config.githubBaseUrl())
            .addHeader("Accept", GitHubMediaTypes.JSON)
            .addHeader("X-GitHub-Api-Version", GitHubHeaders.API_VERSION)
            .addHeader("User-Agent", config.githubOwner())
            .build();
    }

    public static RequestSpecification withAuth(RequestSpecification spec, String token) {
        return new RequestSpecBuilder()
            .addRequestSpecification(spec)
            .addHeader("Authorization", "Bearer " + token)
            .build();
    }

    public static RequestSpecification withOwnerAndRepo(
        RequestSpecification spec, String owner, String repo
    ) {
        return new RequestSpecBuilder()
            .addRequestSpecification(spec)
            .addPathParam("owner", owner)
            .addPathParam("repo", repo)
            .build();
    }
}
