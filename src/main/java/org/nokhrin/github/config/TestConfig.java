package org.nokhrin.github.config;

import org.jetbrains.annotations.NotNull;

public record TestConfig(
    String githubBaseUrl,
    String githubOwner,
    String githubRepo,
    String readAndWriteToken,
    String readToken
) {

    public static TestConfig fromEnv() {
        return new TestConfig(
            required("API_BASE_URL"),
            required("REPO_OWNER"),
            required("REPO_NAME"),
            required("READ_AND_WRITE_TOKEN"),
            required("READ_TOKEN")
        );
    }

    private static String required(String varName) {
        String varValue = System.getenv(varName);
        if (varValue == null || varValue.isBlank()) {
            throw new IllegalStateException("Не установлена переменная окружения: " + varName);
        }
        return varValue;
    }

    @NotNull
    @Override
    public String toString() {
        return String.format("""
            TestConfig:
                githubBaseUrl = '%s'
                githubOwner = '%s'
                githubRepo = '%s'
                readAndWriteToken = [скрыт, длина: %d]
                readToken = [скрыт, длина: %d]
            """.formatted(
            githubBaseUrl,
            githubOwner,
            githubRepo,
            readAndWriteToken.length(),
            readToken.length()
        ));
    }
}
