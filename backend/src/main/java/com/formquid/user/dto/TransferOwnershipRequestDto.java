package com.formquid.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferOwnershipRequestDto(
    @NotNull UUID newOwnerId
) {
}
