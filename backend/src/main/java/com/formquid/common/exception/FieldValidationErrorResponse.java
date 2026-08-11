package com.formquid.common.exception;

/**
 * Representa un error de validación asociado a un campo concreto.
 */
public record FieldValidationErrorResponse(
    String field,
    String message
) {
}