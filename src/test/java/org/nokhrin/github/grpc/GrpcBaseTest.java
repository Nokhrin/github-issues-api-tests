package org.nokhrin.github.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.nokhrin.github.grpc.generated.IssueServiceGrpc;


public abstract class GrpcBaseTest {

    private Server server;
    private ManagedChannel channel;
    protected Credentials credentials;
    protected IssueServiceGrpc.IssueServiceBlockingStub authService, unAuthService, invalidAuthService;

    @BeforeEach
    void startServer() throws Exception {
        credentials = Credentials.random();
        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder
            .forName(serverName)
            .directExecutor()
            .addService(ServerInterceptors.intercept(
                new InMemoryIssueService(),
                new AuthServerInterceptor(credentials)
            ))
            .build()
            .start();

        channel = InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build();

        authService = IssueServiceGrpc.newBlockingStub(channel)
            .withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(credentials.metadata())
            );

        unAuthService = IssueServiceGrpc.newBlockingStub(channel);

        invalidAuthService = IssueServiceGrpc.newBlockingStub(channel)
            .withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    Credentials.of("invalid-token").metadata()
                )
            );
    }


    @AfterEach
    void stopServer() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }
}
