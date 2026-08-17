package com.formquid.auth.controller;

import com.formquid.auth.dto.AuthUserResponseDto;
import com.formquid.auth.dto.CompletePasswordSetupRequestDto;
import com.formquid.auth.dto.LoginRequestDto;
import com.formquid.auth.dto.LoginResponseDto;
import com.formquid.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Controlador REST encargado de la autenticación.
 *
 * Expone el inicio de sesión y la consulta del usuario autenticado.
 * El correo electrónico actúa como identidad del usuario en Spring Security.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Autentica un usuario y devuelve un token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
        @Valid @RequestBody LoginRequestDto request
    ) {
        return ResponseEntity.ok(
            authService.login(request)
        );
    }

    /**
     * Configura la contraseña inicial de una cuenta aprobada.
     */
    @PostMapping("/password-setup")
    public ResponseEntity<AuthUserResponseDto> completePasswordSetup(
        @Valid @RequestBody CompletePasswordSetupRequestDto request
    ) {
        return ResponseEntity.ok(
            authService.completePasswordSetup(request)
        );
    }

    /**
     * Devuelve los datos básicos del usuario autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthUserResponseDto> me(
        Principal principal
    ) {
        return ResponseEntity.ok(
            authService.getCurrentUser(
                getAuthenticatedEmail(principal)
            )
        );
    }

    /**
     * Obtiene de forma segura el correo del usuario autenticado.
     */
    private String getAuthenticatedEmail(
        Principal principal
    ) {
        if (
            principal == null
            || principal.getName() == null
            || principal.getName().isBlank()
        ) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return principal.getName();
    }
}
