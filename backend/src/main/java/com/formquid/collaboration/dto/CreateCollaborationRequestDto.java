package com.formquid.collaboration.dto;

import com.formquid.collaboration.entity.CollaborationRequestType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record CreateCollaborationRequestDto(
    @NotNull Long userId,
    @NotNull CollaborationRequestType requestType,
    @Size(max = 1000) String message
) {
}