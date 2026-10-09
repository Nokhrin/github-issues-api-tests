package org.nokhrin.github.config;

public final class GitHubEndpoints {

    private GitHubEndpoints() {
    }

    public static final String REPO = "/repos/{owner}/{repo}";
    public static final String ISSUES = "/repos/{owner}/{repo}/issues";
    public static final String GRAPHQL = "/graphql";
    public static final String ISSUE_BY_NUMBER = "/repos/{owner}/{repo}/issues/{number}";
}
