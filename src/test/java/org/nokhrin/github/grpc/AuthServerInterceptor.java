package org.nokhrin.github.grpc;

import io.grpc.*;


public class AuthServerInterceptor implements ServerInterceptor {
    private final Credentials credentials;

    public AuthServerInterceptor(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String authentication=metadata.get(credentials.AUTHENTICATION_KEY);

        if (authentication == null || authentication.isBlank()){
            serverCall.close(Status.UNAUTHENTICATED
                .withDescription("Authentication data not provided"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        if (!credentials.token().equals(authentication)) {
            serverCall.close(
                Status.UNAUTHENTICATED.withDescription("Invalid authentication data"),
                new Metadata()
            );
            return new ServerCall.Listener<>() {};
        }

        return serverCallHandler.startCall(serverCall,metadata);
    }
}
