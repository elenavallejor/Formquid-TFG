package com.formquid.common.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta estándar para errores devueltos por la API.
 */
public record ApiErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldValidationErrorResponse> fieldErrors
) {

    /**
     * Crea una respuesta de error sin errores de campo.
     *
     * @param status código HTTP.
     * @param error nombre del error HTTP.
     * @param message mensaje descriptivo.
     * @param path ruta que produjo el error.
     * @return respuesta de error.
     */
    public static ApiErrorResponse of(
        int status,
        String error,
        String message,
        String path
    ) {
        return new ApiErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            message,
            path,
            List.of()
        );
    }

    /**
     * Crea una respuesta de error con errores de validación de campos.
     *
     * @param status código HTTP.
     * @param error nombre del error HTTP.
     * @param message mensaje descriptivo.
     * @param path ruta que produjo el error.
     * @param fieldErrors errores de validación.
     * @return respuesta de error.
     */
    public static ApiErrorResponse withFieldErrors(
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationErrorResponse> fieldErrors
    ) {
        return new ApiErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            message,
            path,
            fieldErrors
        );
    }
}