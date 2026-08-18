package com.formquid.common.exception;

import java.time.Instant;
import java.util.Map;

public record FieldValidationErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> fields
) {
    public FieldValidationErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields
    ) {
        this(Instant.now(), status, error, message, path, fields);
    }
}
