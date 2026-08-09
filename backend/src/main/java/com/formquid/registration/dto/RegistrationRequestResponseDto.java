package com.formquid.registration.dto;

import com.formquid.registration.entity.RegistrationRequestStatus;
import com.formquid.user.entity.UserGender;

import java.time.LocalDateTime;

/**
 * Datos de una solicitud de registro devueltos por la API.
 */
public record RegistrationRequestResponseDto(
    Long id,
    String fullName,
    String email,
    UserGender gender,
    String institution,
    String motivation,
    RegistrationRequestStatus status,
    LocalDateTime requestedAt,
    LocalDateTime resolvedAt,
    Long resolvedByUserId,
    String rejectionReason
) {
}