package com.formquid.user.dto;

import com.formquid.user.entity.UserGender;
import com.formquid.user.entity.UserRole;

import java.time.LocalDateTime;

/**
 * DTO de salida para mostrar usuarios en el panel de administración.
 *
 * No expone la contraseña cifrada ni ningún dato sensible de autenticación.
 */
public record UserResponseDto(
    Long id,
    String name,
    String email,
    UserRole role,
    UserGender gender,
    String institution,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}