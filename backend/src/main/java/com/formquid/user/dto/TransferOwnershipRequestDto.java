package com.formquid.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Recibe el usuario al que se transferirán los recursos de una cuenta.
 */
public record TransferOwnershipRequestDto(
    @NotNull Long newOwnerId
) {
}