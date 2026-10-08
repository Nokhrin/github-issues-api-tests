package org.nokhrin.github.config;

public class Endpoints {

    private Endpoints() {
    }

    public static final String REPO = "/repos/{owner}/{repo}";
    public static final String ISSUES = "/repos/{owner}/{repo}/issues";
    public static final String GRAPHQL = "/graphql";
}
