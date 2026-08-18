package com.formquid.common.exception;

import java.time.Instant;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path
) {
    public ApiErrorResponse(
        int status,
        String error,
        String message,
        String path
    ) {
        this(Instant.now(), status, error, message, path);
    }
}
