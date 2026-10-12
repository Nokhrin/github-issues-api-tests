package org.nokhrin.github.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

public class Wait {
    private static final Logger LOGGER = LoggerFactory.getLogger(Wait.class);

    public static <T> T forCondition(
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
                throw new RuntimeException("""
                    Polling interrupted after %s while waiting for: %s
                    Error: %s
                    """.formatted(timeoutMs, description, e));
            }
            LOGGER.debug("Condition is not met, try again");
            tryCount++;
        }
        throw new AssertionError(String.format("Timeout after %s while waiting for: %s", timeoutMs, description));
    }
}
