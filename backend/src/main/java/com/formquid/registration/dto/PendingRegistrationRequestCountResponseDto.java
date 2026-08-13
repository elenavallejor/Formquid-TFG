package com.formquid.registration.dto;

/**
 * DTO de salida para devolver el número de solicitudes
 * de registro pendientes.
 */
public record PendingRegistrationRequestCountResponseDto(
    long count
) {}
