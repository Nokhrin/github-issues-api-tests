package org.nokhrin.github.config;

public final class GitHubEndpoints {

    private GitHubEndpoints() {
    }

    public static final String REPO = "/repos/{owner}/{repo}";
    public static final String ISSUES = "/repos/{owner}/{repo}/issues";
    public static final String GRAPHQL = "/graphql";
}
