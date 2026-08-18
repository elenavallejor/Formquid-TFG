package com.formquid.collaboration.dto;

import com.formquid.collaboration.entity.CollaborationRequestType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCollaborationRequestDto(
    @NotNull UUID userId,
    @NotNull CollaborationRequestType requestType,
    @Size(max = 1000) String message
) {
}
