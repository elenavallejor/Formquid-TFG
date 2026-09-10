package com.formquid.registration.dto;

import com.formquid.user.entity.UserGender;

import java.time.Instant;

public record RegistrationRequestResponseDto(
    Long id,
    String email,
    String name,
    UserGender gender,
    String institution,
    String motivation,
    Instant createdAt
) {
}