package com.formquid.auth.controller;

import com.formquid.auth.dto.LoginRequestDto;
import com.formquid.auth.dto.LoginResponseDto;
import com.formquid.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de autenticación.
 *
 * Recibe las peticiones de inicio de sesión y delega la validación
 * de credenciales en AuthService. La validación básica del formato
 * de entrada se aplica mediante las anotaciones del DTO.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Recibe email y contraseña. Devuelve un JWT y los datos básicos del usuario.
     */
    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }
}
