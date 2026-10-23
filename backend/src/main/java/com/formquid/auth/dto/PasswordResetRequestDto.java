package com.formquid.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequestDto(
    @Email @NotBlank @Size(max = 320) String email
) {
}
