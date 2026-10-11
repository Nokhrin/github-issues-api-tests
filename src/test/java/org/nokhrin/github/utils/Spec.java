package org.nokhrin.github.utils;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.nokhrin.github.config.GitHubHeaders;
import org.nokhrin.github.config.GitHubMediaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public final class Spec {
    private static final Logger LOGGER = LoggerFactory.getLogger(Spec.class);


    private Spec() {
    }

    public static RequestSpecification base(TestConfig config) {
        LOGGER.debug("Загружена конфигурация: {}", config);

        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
            .setBaseUri(config.githubBaseUrl())
            .setConfig(RestAssured.config()
                .logConfig(LogConfig.logConfig()
                    .blacklistHeader("Authorization")))
            .addHeader("Accept", GitHubMediaTypes.JSON)
            .addHeader("X-GitHub-Api-Version", GitHubHeaders.API_VERSION)
            .addHeader("User-Agent", config.githubOwner());

        if (LOGGER.isDebugEnabled()) {
            requestSpecBuilder.addFilter(new RequestLoggingFilter(
                LogDetail.ALL, true, System.out, true, Set.of("Authorization")
            ));
            requestSpecBuilder.addFilter(new ResponseLoggingFilter(LogDetail.ALL, System.out));
        }


        return requestSpecBuilder.build();
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

    public static RequestSpecification withAuth(RequestSpecification spec, String token) {
        return new RequestSpecBuilder()
            .addRequestSpecification(spec)
            .addHeader("Authorization", "Bearer " + token)
            .build();
    }

}
