package com.formquid.registration.dto;

import jakarta.validation.constraints.Size;

/**
 * Datos opcionales enviados al resolver una solicitud de registro.
 */
public record ResolveRegistrationRequestDto(

    @Size(max = 1000, message = "El motivo de rechazo no puede superar los 1000 caracteres")
    String rejectionReason

) {
}