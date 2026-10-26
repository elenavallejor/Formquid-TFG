package com.formquid.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Recibe el nuevo propietario de una encuesta y la contraseña del propietario actual.
 */
public record TransferOwnershipRequestDto(
    @NotNull Long newOwnerId,
    @NotBlank @Size(max = 120) String currentPassword
) {
}
