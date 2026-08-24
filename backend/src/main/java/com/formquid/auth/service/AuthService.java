package com.formquid.auth.service;

import com.formquid.auth.dto.AuthUserResponseDto;
import com.formquid.auth.dto.LoginRequestDto;
import com.formquid.auth.dto.LoginResult;
import com.formquid.auth.security.JwtService;
import com.formquid.common.service.EmailNormalizer;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación.
 *
 * La consulta realizada por UserDetailsService durante authenticate() se
 * reutiliza para generar la respuesta y el JWT, evitando una segunda lectura
 * del mismo usuario.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResult login(LoginRequestDto request) {
        String email = EmailNormalizer.normalize(request.email());

        Authentication authentication =
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    email,
                    request.password()
                )
            );

        User user = (User) authentication.getPrincipal();
        String token = jwtService.generateToken(user);

        return new LoginResult(
            token,
            toAuthUser(user)
        );
    }

    private AuthUserResponseDto toAuthUser(User user) {
        return new AuthUserResponseDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole()
        );
    }
}
