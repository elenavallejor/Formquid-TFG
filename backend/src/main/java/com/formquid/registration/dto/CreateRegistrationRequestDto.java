package com.formquid.registration.dto;

import com.formquid.user.entity.UserGender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRegistrationRequestDto(

    @Email
    @NotBlank
    @Size(max = 320)
    String email,

    @NotBlank
    @Size(max = 150)
    String name,

    @NotNull
    UserGender gender,

    @NotBlank
    @Size(max = 200)
    String institution,

    @NotBlank
    @Size(max = 1000)
    String motivation,

    @NotBlank
    @Size(min = 8, max = 120)
    String password

) {
}