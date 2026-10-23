package com.formquid.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmDto(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
    @NotBlank @Size(min = 8, max = 120) String password
) {
}
