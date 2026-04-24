package com.smartpark.backend.exceptions;

import lombok.Getter;

@Getter
public class GeminiRateLimitException extends RuntimeException {

    private final Integer retryAfterSeconds;

    public GeminiRateLimitException(String message, Integer retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public GeminiRateLimitException(String message, Integer retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}

