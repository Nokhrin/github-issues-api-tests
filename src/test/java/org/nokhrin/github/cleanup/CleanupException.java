package org.nokhrin.github.cleanup;

public class CleanupException extends RuntimeException {
    public CleanupException(String message) {
        super(message);
    }

    public CleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}
