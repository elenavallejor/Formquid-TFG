package com.formquid.collaboration.dto;

import com.formquid.collaboration.entity.CollaborationRequestType;
import com.formquid.collaboration.entity.CollaborationPermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record CreateCollaborationRequestDto(
    Long userId,
    @Email @Size(max = 254) String email,
    CollaborationPermission permission,
    @NotNull CollaborationRequestType requestType,
    @Size(max = 1000) String message
) {
}
