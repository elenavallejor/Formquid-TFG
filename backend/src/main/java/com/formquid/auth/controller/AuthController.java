package com.formquid.auth.controller;

import com.formquid.auth.dto.AuthUserResponseDto;
import com.formquid.auth.dto.LoginRequestDto;
import com.formquid.auth.dto.LoginResponseDto;
import com.formquid.auth.security.CustomUserDetails;
import com.formquid.auth.service.AuthService;
import com.formquid.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST encargado de gestionar la autenticación de usuarios.
 *
 * Expone los endpoints necesarios para iniciar sesión y consultar los datos
 * básicos del usuario autenticado mediante token JWT.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
    name = "Autenticación",
    description = "Operaciones relacionadas con el inicio de sesión y el usuario autenticado"
)
public class AuthController {

    private final AuthService authService;

    /**
     * Inicia sesión en la plataforma.
     *
     * Recibe las credenciales del usuario, las valida y devuelve un token JWT
     * junto con los datos básicos del usuario autenticado.
     *
     * @param request credenciales del usuario.
     * @return token JWT y datos del usuario autenticado.
     */
    @PostMapping("/login")
    @Operation(
        summary = "Iniciar sesión",
        description = "Valida las credenciales del usuario y devuelve un token JWT si son correctas."
    )
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }

    /**
     * Obtiene los datos básicos del usuario autenticado.
     *
     * Este endpoint requiere enviar un token JWT válido en la cabecera
     * Authorization.
     *
     * @param userDetails usuario autenticado obtenido desde el contexto de seguridad.
     * @return datos básicos del usuario autenticado.
     */
    @GetMapping("/me")
    @Operation(
        summary = "Obtener usuario autenticado",
        description = "Devuelve los datos básicos del usuario asociado al token JWT enviado en la petición."
    )
    public AuthUserResponseDto me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();

        return authService.mapToAuthUserResponseDto(user);
    }
}