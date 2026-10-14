package org.nokhrin.github.grpc;

import io.grpc.Metadata;

import java.util.UUID;

public final class Credentials {

    public static final Metadata.Key<String> AUTHENTICATION_KEY =
        Metadata.Key.of("authentication", Metadata.ASCII_STRING_MARSHALLER);

    private final String token;

    private Credentials(String token) {
        this.token = token;
    }

    public static Credentials random() {
        return new Credentials("grpc-token-" + UUID.randomUUID());
    }

    public static Credentials of(String token) {
        return new Credentials(token);
    }

    public String token() {
        return token;
    }

    public Metadata metadata() {
        Metadata metadata = new Metadata();
        metadata.put(AUTHENTICATION_KEY, token);
        return metadata;
    }
}
