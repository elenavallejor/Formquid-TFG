package com.formquid.auth.service;

import com.formquid.auth.dto.AuthUserResponseDto;
import com.formquid.auth.dto.LoginRequestDto;
import com.formquid.auth.dto.LoginResult;
import com.formquid.auth.security.JwtService;
import com.formquid.common.service.EmailNormalizer;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación.
 *
 * Gestiona el inicio de sesión de los usuarios registrados. Normaliza
 * el correo, delega la comprobación de credenciales en Spring Security
 * y genera un token JWT cuando la autenticación es correcta.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Recibe email y contraseña y devuelve internamente
     * el JWT y los datos del usuario autenticado.
     *
     * El JWT será almacenado posteriormente en una cookie HttpOnly
     * por el controlador.
     */
    public LoginResult login(LoginRequestDto request) {
        String email = EmailNormalizer.normalize(request.email());

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                email,
                request.password()
            )
        );

        User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                new com.formquid.common.exception.ApiException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Credenciales no válidas"
                )
            );

        String token = jwtService.generateToken(user);

        return new LoginResult(
            token,
            toAuthUser(user)
        );
    }

    /**
     * Recibe un usuario autenticado. Devuelve los datos seguros
     * que pueden enviarse al frontend.
     */
    private AuthUserResponseDto toAuthUser(User user) {
        return new AuthUserResponseDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole()
        );
    }
}