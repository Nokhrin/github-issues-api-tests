
package org.nokhrin.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.nokhrin.github.model.Issue;
import org.nokhrin.github.config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RestAssuredAvailabilityTests {
    private final Logger LOGGER= LoggerFactory.getLogger(RestAssuredAvailabilityTests.class);

    private String issueTitle = String.format("issue %s", RandomStringUtils.randomAlphabetic(5));
    private String issueDescription = "Description of new issue";

    private String baseUrl;
    private String owner;
    private String repo;
    private TestConfig config;
    private RequestSpecification baseSpec;
    private RequestSpecification authSpec;
    private RequestSpecification unauthSpec;

    @BeforeAll
    void setUp() {
        config = TestConfig.fromEnv();
        LOGGER.debug("Загружена конфигурация: " + config);

        baseUrl = config.githubBaseUrl();
        owner = config.githubOwner();
        repo = config.githubRepo();


        baseSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .addHeader("User-Agent", "sqa-lab-tests/1.0")
                .build();

        authSpec = new RequestSpecBuilder()
                .addRequestSpecification(baseSpec)
                .addPathParam("owner", owner)
                .addPathParam("repo", repo)
                .addHeader("Authorization", "Bearer " + config.readAndWriteToken())
                .build();

        unauthSpec = new RequestSpecBuilder()
                .addRequestSpecification(baseSpec)
                .addPathParam("owner", owner)
                .addPathParam("repo", repo)
                .build();

    }

    /*
        01. Проверяем, что приходит 200 код в ответ на простой GET
    */
    @Test
    public void verifyHealthcheckTest() {
        RestAssured.given()
                .spec(baseSpec)
                .when()
                .get("/zen")
                .then()
                .statusCode(200)
                .log().ifValidationFails();
    }

    /*
        02. Проверяем, что приходит непустое тело ответа на простой GET
    */
    @Test
    public void verifyDefunktBodyTest() {
        RestAssured.given()
                .spec(baseSpec)
                .when()
                .get("/zen")
                .then()
                .statusCode(200)
                .body(not(emptyString()))
                .log().ifValidationFails()
                ;
    }

    /*
        03. Проверяем, что тело ответа содержит поле, равное значению
    */
    @Test
    public void verifyIssuesContainTest() {
        RestAssured.given()
                .spec(authSpec)
                .when()
                .get("/repos/{owner}/{repo}")
                .then()
                .statusCode(200)
                .body("name", equalTo(config.githubRepo()))
                .body("full_name", equalTo(config.githubOwner() + "/" + config.githubRepo()))
                .body("owner.login", equalTo(config.githubOwner()))
                .body("private", equalTo(true))
                .body("default_branch", equalTo("main"))
                .log().ifValidationFails()
        ;
    }

    /*
        04. Проверяем, что тело ответа содержит поле после авторизации
        проверка разрграничения доступа
    */
    @Test
    public void verifyIssuesAuthorized() {
        RestAssured.given()
                .spec(unauthSpec)
                .when()
                .get("/repos/{owner}/{repo}")
                .then()
                .statusCode(404)
                .log().ifValidationFails()
        ;

        RestAssured.given()
                .spec(authSpec)
                .when()
                .get("/repos/{owner}/{repo}")
                .then()
                .statusCode(200)
        .body("owner.login",equalTo(config.githubOwner()))
                .log().ifValidationFails()
                ;
    }

    /*
        05. Проверяем, что тело ответа содержит ошибку и 403 код
        запросы к GitHub API должны содержать User-Agent
    */
    @Test
    public void verifyIssuesNoUserAgent() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .removeHeader("User-Agent")
                                .build()
                ))
                .build();
        Request request=new Request.Builder()
                .url(baseUrl+"zen")
                        .build();

        try {
            Response response=httpClient.newCall(request).execute();
            String body=response.body().string();
            assertAll(
                    ()->assertEquals(403, response.code()),
                    ()->assertTrue(body.contains("User-Agent"))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
        06. Проверяем, что ишью публикуется (тело запроса в строке)
    */
    @Test
    public void verifyPostIssues() {
        String requestBody= """
                {"title": "%s", "body": "%s"}
                """.formatted(issueTitle,issueDescription);
        Long issueNumber = null;

        try {
            issueNumber = postIssue(requestBody);
            RestAssured.given()
                    .spec(authSpec)
                    .when()
                    .get("/repos/{owner}/{repo}/issues/{number}", issueNumber)
                    .then()
                    .statusCode(200)
                    .body("title", equalTo(issueTitle))
                    .body("body", equalTo(issueDescription))
                    .log().ifValidationFails()
                    ;
        } finally {
            closeIssue(issueNumber);
        }

    }

    /*
        07. Проверяем, что тело ответа содержит ошибку и 403 код
        Попытка создания с помощью query параметров, без Content-Type application/json
        ожидаем код 422
        https://docs.github.com/en/rest/about-the-rest-api/breaking-changes?apiVersion=2026-03-10
    */
    @Test
    public void verifyPostIssuesUrlParam() {
        RestAssured.given()
                .spec(authSpec)
                .queryParam("title",issueTitle)
                .queryParam("body",issueDescription)
                .when()
                .post("/repos/{owner}/{repo}/issues")
                .then()
                .statusCode(422)
                .log().ifValidationFails()
                ;
    }

    /*
        08. Проверяем, что ишью публикуется (тело запроса в POJO)
    */
    @Test
    public void verifyPostPojo() {
        Long number = postIssue();
        RestAssured.given()
                .spec(authSpec)
                .when()
                .get("/repos/{owner}/{repo}/issues/{number}", number)
                .then()
                .statusCode(200)
                .extract()
                .as(Issue.class);
    }

    private void closeIssue(Long issueNum) {
        if (issueNum == null) {
            return;
        }
        RestAssured.given()
                .spec(authSpec)
                .contentType(ContentType.JSON)
                .body(Map.of("state", "closed"))
                .patch("/repos/{owner}/{repo}/issues/{number}", issueNum)
                .then()
                .statusCode(200)
        ;
    }

    /*
        09. Проверяем, что ишью публикуется (тело запроса в Map)
    */
    @Test
    public void verifyPostMap() {
        Map<String,Object>requestBody = Map.of(
                "title",issueTitle,
                "body",issueDescription
        );
        Long number = null;
        try {
            number = RestAssured.given()
                    .spec(authSpec)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/repos/{owner}/{repo}/issues")
                    .then()
                    .statusCode(201)
                    .extract()
                    .jsonPath().getLong("number")
                    ;


        RestAssured.given()
                    .spec(authSpec)
                    .when()
                    .get("/repos/{owner}/{repo}/issues/{number}", number)
                    .then()
                    .statusCode(200)
                .body("body", equalTo(issueDescription))
        ;


        } finally {
            closeIssue(number);
        }
    }

    /*
        10. Проверяем, что ишью публикуется (тело запроса в POJO, поиск с помощью json path)
    */
    @Test
    public void verifyPostPojoWithJsonPath() {
        Long issueNumber = postIssue();
        try {
            Issue issue = RestAssured.given()
                    .spec(authSpec)
                    .when()
                    .get("/repos/{owner}/{repo}/issues/{number}", issueNumber)
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .extract()
                    .as(Issue.class)
                    ;

            assertEquals(owner, issue.getUser().getLogin());
        }finally {
            closeIssue(issueNumber);
        }
    }

    private Long postIssue() {
        return postIssue(issueTitle, issueDescription);
    }
    private Long postIssue(String title, String body) {
        return postIssue(new Issue().setTitle(title).setBody(body));
    }

    private Long postIssue(Object requestBody) {
        return RestAssured.given()
                .spec(authSpec)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/repos/{owner}/{repo}/issues")
                .then()
                .statusCode(201)
                .log().ifValidationFails()
                .extract()
                .jsonPath().getLong("number");
    }
}
