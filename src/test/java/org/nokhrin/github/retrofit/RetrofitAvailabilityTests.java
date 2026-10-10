package org.nokhrin.github.retrofit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.nokhrin.github.config.GitHubMediaTypes;
import org.nokhrin.github.config.HttpClient;
import org.nokhrin.github.config.TestConfig;
import org.nokhrin.github.model.Issue;
import org.nokhrin.github.model.Repository;
import org.nokhrin.github.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RetrofitAvailabilityTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetrofitAvailabilityTests.class);

    private static Retrofit authRetrofit;
    private static OkHttpClient authClient;

    private static GitHubService gitHubServiceAuth;
    private static GitHubService gitHubServiceUnAuth;

    private String issueTitle = String.format("issue %s", RandomStringUtils.randomAlphabetic(5));
    private String issueDescription = "Description of new issue";

    private TestConfig config;
    private String baseUrl;
    private String owner;
    private String repo;


    public RetrofitAvailabilityTests() {

    }

    @BeforeAll
    void setUp() {
        config = TestConfig.fromEnv();
        baseUrl = config.githubBaseUrl();
        owner = config.githubOwner();
        repo = config.githubRepo();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(LOGGER::debug);

        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.redactHeader("Authorization");

        Interceptor baseHeaders = chain -> chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", owner)
                .header("Accept", GitHubMediaTypes.JSON)
                .build()
        );

        Interceptor authHeaders = chain -> chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", owner)
                .header("Accept", GitHubMediaTypes.JSON)
                .header("Authorization", "Bearer " + config.readAndWriteToken())
                .build()
        );

        OkHttpClient unAuthClient = HttpClient.create(baseHeaders, loggingInterceptor);
        authClient = HttpClient.create(authHeaders, loggingInterceptor);

        ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


        authRetrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build();
        gitHubServiceAuth = authRetrofit.create(GitHubService.class);

        Retrofit unAuthRetrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(unAuthClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build();
        gitHubServiceUnAuth = unAuthRetrofit.create(GitHubService.class);
    }

    @AfterAll
    void tearDown() {
        HttpClient.shutdown();
    }

    /*
        01. Проверяем, что приходит 200 код в ответ на простой GET
    */
    @Test
    @Tag("read")
    public void verifyHealthcheckTest() throws IOException {
        Response<String> response = gitHubServiceUnAuth.getZen().execute();
        assertAll(
            () -> assertEquals(200, response.code())
        );
    }

    /*
        02. Проверяем, что приходит непустое тело ответа на простой GET
    */
    @Test
    @Tag("read")
    public void verifyDefunktBodyTest() throws IOException {
        Response<String> response = gitHubServiceUnAuth.getZen().execute();
        assertAll(
            () -> assertEquals(200, response.code()),
            () -> {
                assertNotNull(response.body());
                assertFalse(response.body().isBlank());
            }
        );
    }

    /*
        03. Проверяем, что тело ответа содержит поле, равное значению
    */
    @Test
    @Tag("read")
    public void verifyIssuesContainTest() throws IOException {
        Issue issue = new Issue()
            .setTitle(issueTitle)
            .setBody(issueDescription);

        Response<Issue> createResponse = gitHubServiceAuth.createIssuePojo(
            owner,
            repo,
            issue
        ).execute();
        assertEquals(201, createResponse.code());
        assertNotNull(createResponse.body());
        Long issueNumber = Long.valueOf(createResponse.body().getNumber());

        try {
            Response<Issue> response = gitHubServiceAuth.getIssue(owner, repo, issueNumber.toString()).execute();
            assertEquals(200, response.code());
            Issue issueFound = response.body();
            assertNotNull(issueFound);

            assertAll(
                () -> assertEquals(issueNumber.intValue(), issueFound.getNumber()),
                () -> assertEquals(issue.getTitle(), issueFound.getTitle()),
                () -> assertEquals(issue.getBody(), issueFound.getBody()),
                () -> assertEquals("open", issueFound.getState()),
                () -> assertEquals(owner, issueFound.getUser().getLogin())
            );
        } finally {
            gitHubServiceAuth.updateIssueMap(
                owner, repo, issueNumber,
                Map.of("state", "closed")
            ).execute();
        }
    }

    /*
        04. Проверяем, что тело ответа содержит поле после авторизации
    */
    @Test
    @Tag("read")
    public void verifyIssuesAuthorized() throws IOException {
        Response<Repository> unAuthResponse = gitHubServiceUnAuth.getRepo(owner, repo).execute();
        assertEquals(404, unAuthResponse.code());

        Response<Repository> authResponse = gitHubServiceAuth.getRepo(owner, repo).execute();
        assertEquals(200, authResponse.code());

        Repository foundRepo = authResponse.body();
        assertAll(
            () -> assertNotNull(foundRepo),
            () -> {
                assertNotNull(foundRepo);
                assertEquals(repo, foundRepo.getName());
            },
            () -> {
                assertNotNull(foundRepo);
                assertNotNull(owner, foundRepo.getOwner().getLogin());
            }
        );

    }

    /*
        05. Проверяем, что тело ответа содержит ошибку и 403 код
    */
    @Test
    @Tag("read")
    public void verifyIssuesNoUserAgent() throws IOException {
        OkHttpClient noAgentClient = authClient.newBuilder()
            .addNetworkInterceptor(chain -> chain.proceed(
                chain.request().newBuilder()
                    .removeHeader("User-Agent")
                    .build()))
            .build();

        Retrofit retrofitNoAgent = authRetrofit.newBuilder()
            .client(noAgentClient)
            .build();

        GitHubService gitHubServiceNoAgent = retrofitNoAgent.create(GitHubService.class);

        Response<String> response = gitHubServiceNoAgent
            .getZen()
            .execute();

        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";

        assertAll(
            () -> assertEquals(403, response.code()),
            () -> assertTrue(errorBody.contains("User-Agent"))
        );
    }

    /*
        06. Проверяем, что ишью публикуется
    */
    @Test
    @Tag("read")
    public void verifyPostIssues() throws IOException {
        String rawJson = """
            {"title": "%s", "body": "%s"}
            """.formatted(issueTitle, issueDescription);

        RequestBody body = RequestBody.create(
            rawJson,
            okhttp3.MediaType.parse("application/json; charset=utf-8"));

        Response<Issue> response = gitHubServiceAuth
            .createIssueRawJson(owner, repo, body)
            .execute();

        assertEquals(201, response.code());
        Issue created = response.body();
        assertNotNull(created);
        Long issueNumber = Long.valueOf(created.getNumber());

        try {
            Response<Issue> getResponse = gitHubServiceAuth
                .getIssue(owner, repo, issueNumber.toString())
                .execute();
            assertEquals(200, getResponse.code());

            Issue found = getResponse.body();
            assertNotNull(found);

            assertAll(
                () -> assertEquals(issueNumber.intValue(), found.getNumber()),
                () -> assertEquals(issueTitle, found.getTitle()),
                () -> assertEquals(issueDescription, found.getBody()),
                () -> assertEquals("open", found.getState())
            );
        } finally {
            gitHubServiceAuth.updateIssueMap(
                owner, repo, issueNumber,
                Map.of("state", "closed")
            ).execute();
        }

    }

    /*
        07. Проверяем, что тело ответа содержит ошибку и 403 код
    */
    @Test
    @Tag("read")
    public void verifyPostIssuesUrlParam() throws IOException {
        Response<ResponseBody> response = gitHubServiceAuth
            .createIssueQuery(owner, repo, issueTitle, issueDescription)
            .execute();

        assertTrue(response.code() >= 400);
        assertNotNull(response.errorBody());
        String errorBody = response.errorBody().string();

        assertAll(
            () -> assertEquals(422, response.code()),
            () -> assertTrue(errorBody.contains("message"))
        );
    }

    /*
        08. Проверяем, что ишью публикуется (тело запроса в POJO)
    */
    @Test
    @Tag("read")
    public void verifyPostPojo() throws IOException {
        Issue issue = new Issue()
            .setTitle(issueTitle)
            .setBody(issueDescription);

        Response<Issue> response = gitHubServiceAuth
            .createIssuePojo(owner, repo, issue)
            .execute();

        assertEquals(201, response.code());
        Issue created = response.body();
        assertNotNull(created);
        Long issueNumber = Long.valueOf(created.getNumber());

        try {
            assertAll(
                () -> assertEquals(issueNumber.intValue(), created.getNumber()),
                () -> assertEquals(issueTitle, created.getTitle()),
                () -> assertEquals(issueDescription, created.getBody()),
                () -> assertEquals("open", created.getState()),
                () -> assertEquals(owner, created.getUser().getLogin())
            );
        } finally {
            gitHubServiceAuth.updateIssueMap(
                owner, repo, issueNumber,
                Map.of("state", "closed")
            ).execute();
        }
    }

    /*
        09. Проверяем, что ишью публикуется (тело запроса в Map)
    */
    @Test
    @Tag("read")
    public void verifyPostMap() throws IOException {
        Map<String, Object> requestBody = Map.of(
            "title", issueTitle,
            "body", issueDescription
        );

        Response<Issue> response = gitHubServiceAuth
            .createIssueMap(owner, repo, requestBody)
            .execute();

        assertEquals(201, response.code());
        Issue createdIssue = response.body();
        assertNotNull(createdIssue);

        Long issueNumber = createdIssue.getNumber().longValue();

        try {
            assertAll(
                () -> assertEquals(issueTitle, createdIssue.getTitle()),
                () -> assertEquals(issueDescription, createdIssue.getBody()),
                () -> assertEquals("open", createdIssue.getState())
            );
        } finally {
            gitHubServiceAuth.updateIssueMap(
                owner, repo, issueNumber,
                Map.of("state", "closed")
            ).execute();
        }
    }

}
