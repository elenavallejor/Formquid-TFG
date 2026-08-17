package com.formquid.auth.service;

import com.formquid.auth.dto.AuthUserResponseDto;
import com.formquid.auth.dto.CompletePasswordSetupRequestDto;
import com.formquid.auth.dto.LoginRequestDto;
import com.formquid.auth.dto.LoginResponseDto;
import com.formquid.auth.security.JwtService;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Servicio encargado del flujo de autenticación.
 *
 * AuthenticationManager valida las credenciales y UserRepository permite
 * recuperar la entidad de dominio necesaria para generar el JWT y el DTO.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Inicia sesión con correo y contraseña.
     */
    public LoginResponseDto login(
        LoginRequestDto request
    ) {
        String email =
            normalizeEmail(request.email());

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                email,
                request.password()
            )
        );

        User user =
            findUserByEmail(email);

        String token =
            jwtService.generateToken(user);

        return new LoginResponseDto(
            token,
            AuthUserResponseDto.from(user)
        );
    }

    /**
     * Obtiene los datos del usuario autenticado.
     */
    public AuthUserResponseDto getCurrentUser(
        String email
    ) {
        return AuthUserResponseDto.from(
            findUserByEmail(email)
        );
    }

    /**
     * Configura la contraseña inicial de una cuenta aprobada.
     */
    @Transactional
    public AuthUserResponseDto completePasswordSetup(
        CompletePasswordSetupRequestDto request
    ) {
        String tokenHash = hashSetupToken(request.token().trim());

        User user = userRepository
            .findByPasswordSetupTokenHash(tokenHash)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El token de configuración no es válido"
            ));

        if (user.getPasswordSetupTokenExpiresAt() == null
            || user.getPasswordSetupTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El token de configuración ha caducado"
            );
        }

        user.completePasswordSetup(
            passwordEncoder.encode(request.password())
        );

        return AuthUserResponseDto.from(user);
    }

    /**
     * Recupera un usuario por correo ignorando mayúsculas.
     */
    private User findUserByEmail(
        String email
    ) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository
            .findByEmailIgnoreCase(
                email.trim()
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario autenticado no encontrado"
                )
            );
    }

    /**
     * Normaliza el correo antes de autenticar.
     */
    private String normalizeEmail(
        String email
    ) {
        return email
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    private String hashSetupToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "No se pudo calcular el hash del token temporal",
                exception
            );
        }
    }
}
