package com.formquid.user.dto;

/**
 * DTO resumido de usuario.
 *
 * Se usa en búsquedas simples, por ejemplo para localizar editores
 * al crear una colaboración. No expone datos innecesarios.
 */
public record UserSummaryResponseDto(
    Long id,
    String name,
    String email,
    String institution
) {}
