package com.formquid.registration.dto;


import java.time.Instant;

public record RegistrationRequestResponseDto(
    Long id,
    String email,
    String name,
    String institution,
    String country,
    String motivation,
    Instant createdAt
) {
}
