package com.formquid.registration.dto;

import com.formquid.registration.entity.RegistrationRequestStatus;
import com.formquid.user.entity.UserGender;

import java.time.LocalDateTime;

/**
 * DTO de salida para mostrar una solicitud de registro.
 *
 * La contraseña cifrada nunca se expone a través de la API.
 */
public record RegistrationRequestResponseDto(
    Long id,
    String name,
    String email,
    UserGender gender,
    String institution,
    String motivation,
    RegistrationRequestStatus status,
    LocalDateTime requestedAt,
    LocalDateTime resolvedAt,
    Long resolvedByUserId,
    String resolvedByName,
    String rejectionReason
) {}
