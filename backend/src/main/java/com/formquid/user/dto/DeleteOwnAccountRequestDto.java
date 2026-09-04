package com.formquid.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Recibe la contraseña actual para confirmar la baja de la cuenta.
 */
public record DeleteOwnAccountRequestDto(
    @NotBlank @Size(max = 120) String currentPassword
) {
}