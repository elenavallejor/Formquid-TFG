package com.formquid.collaboration.dto;

import com.formquid.collaboration.entity.CollaborationPermission;
import jakarta.validation.constraints.NotNull;

public record UpdateCollaborationPermissionDto(
    @NotNull CollaborationPermission permission
) {
}
