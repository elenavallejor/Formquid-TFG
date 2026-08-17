package com.formquid.block.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos necesarios para crear un bloque.
 */
public record CreateBlockRequestDto(

    @NotBlank(message = "El nombre del bloque es obligatorio")
    @Size(max = 120, message = "El nombre del bloque no puede superar los 120 caracteres")
    String name,

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    String description

) {
}
