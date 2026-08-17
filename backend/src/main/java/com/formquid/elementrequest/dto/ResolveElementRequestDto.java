package com.formquid.elementrequest.dto;

import jakarta.validation.constraints.Size;

/**
 * Datos opcionales proporcionados por el administrador
 * al aprobar o rechazar una solicitud.
 */
public record ResolveElementRequestDto(

    @Size(max = 1500,message = "El mensaje de resolución no puede superar los 1500 caracteres")
    String resolutionMessage
) {}
