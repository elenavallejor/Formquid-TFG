package com.formquid.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Recibe los cambios editables de la cuenta del usuario autenticado.
 */
public record UpdateAccountRequestDto(
    @Valid ProfileData profile,
    @Valid EmailData email,
    @Valid PasswordData password,
    @NotBlank @Size(max = 120) String currentPassword
) {
    public record ProfileData(
        @Size(max = 150) String name,
        @Size(max = 200) String institution,
        @Size(max = 100) String country
    ) {
    }

    public record EmailData(
        @Size(max = 320) String newEmail
    ) {
    }

    public record PasswordData(
        @Size(min = 8, max = 120) String newPassword
    ) {
    }
}
