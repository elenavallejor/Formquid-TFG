package com.formquid.auth.service;

import com.formquid.auth.dto.AuthUserResponseDto;
import com.formquid.auth.dto.LoginRequestDto;
import com.formquid.auth.dto.LoginResponseDto;
import com.formquid.auth.security.CustomUserDetails;
import com.formquid.auth.security.JwtService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio encargado de gestionar la autenticación de usuarios en FormQuid.
 *
 * Valida las credenciales recibidas desde el frontend (Spring Security)
 * si son correctas, genera un token JWT que permitirá al usuario acceder
 * a endpoints protegidos de la API.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Autentica a un usuario mediante correo electrónico y contraseña.
     *
     * Si las credenciales son válidas, se genera un token JWT y se devuelven
     * los datos básicos del usuario autenticado. Si no son válidas, se devuelve
     *  un error HTTP 401.
     *
     * @param request DTO con las credenciales del usuario.
     * @return respuesta con token JWT y datos básicos del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        try {
            var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password()
                )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            String token = jwtService.generateToken(userDetails);

            return new LoginResponseDto(
                token,
                TOKEN_TYPE,
                mapToAuthUserResponseDto(user)
            );

        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas"
            );
        }
    }

    /**
     * Convierte una entidad User en un DTO seguro para devolver al frontend.
     *
     * @param user usuario autenticado.
     * @return DTO con los datos básicos del usuario.
     */
    public AuthUserResponseDto mapToAuthUserResponseDto(User user) {
        return new AuthUserResponseDto(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.getGender()
        );
    }
}