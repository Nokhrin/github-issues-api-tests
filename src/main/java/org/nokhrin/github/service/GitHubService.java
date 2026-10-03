package org.nokhrin.github.service;

import org.nokhrin.github.model.Issue;
import org.nokhrin.github.model.Repository;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface GitHubService {
    @GET("zen")
    Call<String> getZen();

    @GET("repos/{owner}/{repo}")
    Call<Repository> getRepo(
            @Path("owner") String owner,
            @Path("repo") String repo
    );
    @GET("repos/{owner}/{repo}/issues")
    Call<List<Issue>> getIssues(
            @Path("owner") String owner,
            @Path("repo") String repo
    );
    @GET("repos/{owner}/{repo}/issues/{number}")
    Call<Issue> getIssue(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Path("number") String number
    );

    @POST("repos/{owner}/{repo}/issues")
    Call<Issue> createIssuePojo(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Body Issue issue
    );

    @POST("repos/{owner}/{repo}/issues")
    Call<Issue> createIssueMap(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Body Map<String,Object> issueBody
    );

    @POST("repos/{owner}/{repo}/issues")
    Call<Issue> createIssueRawJson(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Body RequestBody issueBody
    );

    @POST("repos/{owner}/{repo}/issues")
    Call<ResponseBody> createIssueQuery(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Query("title") String title,
            @Query("body") String issueBody
    );

    @PATCH("repos/{owner}/{repo}/issues/{number}")
    Call<Issue> updateIssueMap(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Path("number") Long number,
            @Body Map<String,Object> issueBody
    );


}
