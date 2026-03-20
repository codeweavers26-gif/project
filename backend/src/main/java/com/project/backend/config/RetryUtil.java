package com.project.backend.config;

import java.util.function.Supplier;

public class RetryUtil {

    public static <T> T executeWithRetry(Supplier<T> action, int maxAttempts) {

        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxAttempts) {
            try {
                return action.get();
            } catch (Exception e) {
                attempts++;
                lastException = e;

                System.out.println("Retry attempt: " + attempts);

                try {
                    Thread.sleep(1000 * attempts);
                } catch (InterruptedException ignored) {}
            }
        }

        throw new RuntimeException("All retries failed", lastException);
    }
}