package org.nokhrin.github.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.nokhrin.github.grpc.generated.*;

import java.util.HashMap;
import java.util.Map;

public class InMemoryIssueService extends IssueServiceGrpc.IssueServiceImplBase {

    private final Map<Long, Issue> storage = new HashMap<>();
    private long nextIssueNumber = 1;

    @Override
    public void getIssue(GetIssueRequest request, StreamObserver<Issue> responseObserver) {
        Issue issue = storage.get(request.getNumber());
        if (issue == null) {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("Issue not found: " + request.getNumber())
                    .asRuntimeException()
            );
            return;
        }
        responseObserver.onNext(issue);
        responseObserver.onCompleted();
    }

    @Override
    public void createIssue(CreateIssueRequest request, StreamObserver<Issue> responseObserver) {
        if (request.getTitle().isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("Not empty title required")
                    .asRuntimeException()
            );
            return;
        }

        long issueNumber = nextIssueNumber++;
        Issue issue = Issue.newBuilder()
            .setNumber(issueNumber)
            .setTitle(request.getTitle())
            .setBody(request.getBody())
            .setState("open")
            .build();

        storage.put(issueNumber, issue);

        responseObserver.onNext(issue);
        responseObserver.onCompleted();

    }

    @Override
    public void closeIssue(CloseIssueRequest request, StreamObserver<Issue> responseObserver) {
        Issue existingIssue = storage.get(request.getNumber());

        if (existingIssue == null) {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("Issue not found: " + request.getNumber())
                    .asRuntimeException()
            );
            return;
        }

        Issue closedIssue = existingIssue.toBuilder()
            .setState("closed")
            .build();

        storage.put(request.getNumber(), closedIssue);

        responseObserver.onNext(closedIssue);
        responseObserver.onCompleted();
    }

    @Override
    public void listIssues(ListIssuesRequest request, StreamObserver<Issue> responseObserver) {
        String state = request.getState();
        if (!("open".equals(state) || "closed".equals(state))){
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("States allowed: open, closed; got: "+state)
                    .asRuntimeException()
            );
            return;
        }

        for (Issue issue: storage.values()){
            if (state.equals(issue.getState())){
                responseObserver.onNext(issue);
            }
        }

        responseObserver.onCompleted();
    }
}
