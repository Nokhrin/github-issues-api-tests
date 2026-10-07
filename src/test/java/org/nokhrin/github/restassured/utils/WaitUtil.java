package org.nokhrin.github.restassured.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class WaitUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtil.class);

    public static <T> T waitFor(
        Supplier<T> condition,
        Duration timeout,
        Duration interval,
        String description
    ) {
        long startTimeMs = System.currentTimeMillis();
        long timeoutMs = timeout.toMillis();
        long intervalMs = interval.toMillis();
        int tryCount = 1;

        while (System.currentTimeMillis() - startTimeMs < timeoutMs) {
            LOGGER.debug("Try #{}", tryCount);
            T result = condition.get();
            if (result != null) {
                return result;
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(
                    String.format("Polling interrupted  after %s while waiting for: %s\nError: %s", timeoutMs, description, e));
            }
            LOGGER.debug("Condition is not met, try again");
            tryCount++;
        }
        throw new AssertionError(String.format("Timeout after %s while waiting for: %s", timeoutMs, description));
    }
}
