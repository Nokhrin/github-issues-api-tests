package org.nokhrin.github.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nokhrin.github.grpc.generated.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("grpc")
@Feature("gRPC")
@Story("GitHub Issue Service emulation")
class IssueServiceTest extends GrpcBaseTest {

    @Test
    void createIssue_returnsCreatedIssue() {
        CreateIssueRequest request = CreateIssueRequest.newBuilder()
            .setTitle("test title")
            .setBody("test body")
            .build();

        Issue response = authService.createIssue(request);

        assertAll(
            () -> assertTrue(response.getNumber() > 0),
            () -> assertEquals("test title", response.getTitle()),
            () -> assertEquals("test body", response.getBody()),
            () -> assertEquals("open", response.getState())
        );
    }

    @Test
    void createIssue_emptyTitle_returnsInvalidArgument() {
        CreateIssueRequest request = CreateIssueRequest.newBuilder()
            .setTitle("")
            .setBody("body")
            .build();

        StatusRuntimeException exception = assertThrows(
            StatusRuntimeException.class,
            () -> authService.createIssue(request)
        );

        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

    @Test
    void getIssue_existingNumber_returnsIssue() {
        Issue created = authService.createIssue(
            CreateIssueRequest.newBuilder()
                .setTitle("fetch me")
                .setBody("body")
                .build()
        );

        Issue fetched = authService.getIssue(
            GetIssueRequest.newBuilder()
                .setNumber(created.getNumber())
                .build()
        );

        assertEquals(created.getNumber(), fetched.getNumber());
        assertEquals("fetch me", fetched.getTitle());
    }

    @Test
    void getIssue_nonExistingNumber_returnsNotFound() {
        StatusRuntimeException exception = assertThrows(
            StatusRuntimeException.class,
            () -> authService.getIssue(
                GetIssueRequest.newBuilder().setNumber(999999L).build()
            )
        );

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    @Test
    void closeIssue_existingNumber_changesState() {
        Issue created = authService.createIssue(
            CreateIssueRequest.newBuilder()
                .setTitle("close me")
                .setBody("body")
                .build()
        );

        Issue closed = authService.closeIssue(
            CloseIssueRequest.newBuilder()
                .setNumber(created.getNumber())
                .build()
        );

        assertEquals("closed", closed.getState());
    }

    @Test
    void closeIssue_nonExistingNumber_returnsNotFound() {
        StatusRuntimeException exception = assertThrows(
            StatusRuntimeException.class,
            () -> authService.closeIssue(
                CloseIssueRequest.newBuilder().setNumber(888888L).build()
            )
        );

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }


    @Test
    void listIssues_returnsServerStreamWithCreatedIssues() {
        Issue first = authService.createIssue(
            CreateIssueRequest.newBuilder()
                .setTitle("stream-first")
                .setBody("body")
                .build()
        );

        Issue second = authService.createIssue(
            CreateIssueRequest.newBuilder()
                .setTitle("stream-second")
                .setBody("body")
                .build()
        );

        Iterator<Issue> iterator = authService.listIssues(
            ListIssuesRequest.newBuilder()
                .setState("open")
                .build()
        );

        List<Issue> received = new ArrayList<>();
        Set<Long> numbers = new HashSet<>();
        Set<String> titles = new HashSet<>();

        while (iterator.hasNext()) {
            Issue issue = iterator.next();
            received.add(issue);
            numbers.add(issue.getNumber());
            titles.add(issue.getTitle());
        }

        assertAll(
            () -> assertEquals(2, received.size()),
            () -> assertTrue(numbers.contains(first.getNumber())),
            () -> assertTrue(numbers.contains(second.getNumber())),
            () -> assertTrue(titles.contains("stream-first")),
            () -> assertTrue(titles.contains("stream-second"))
        );
    }
}
