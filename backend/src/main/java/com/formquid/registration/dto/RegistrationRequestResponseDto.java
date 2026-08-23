package com.formquid.registration.dto;

import com.formquid.user.entity.UserGender;

import java.time.Instant;
import java.util.UUID;

public record RegistrationRequestResponseDto(
    UUID id,
    String email,
    String name,
    UserGender gender,
    String institution,
    String motivation,
    Instant createdAt
) {
}
