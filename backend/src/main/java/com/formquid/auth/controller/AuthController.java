package com.formquid.auth.controller;

import com.formquid.auth.dto.LoginRequestDto;
import com.formquid.auth.dto.LoginResponseDto;
import com.formquid.auth.dto.LoginResult;
import com.formquid.auth.security.JwtCookieService;
import com.formquid.auth.service.AuthService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.springframework.security.web.csrf.CsrfToken;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtCookieService jwtCookieService;


    /**
     * Genera el token CSRF.
     *
     * El navegador recibirá la cookie XSRF-TOKEN.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {

        csrfToken.getToken();

        return ResponseEntity.noContent().build();
    }


    /**
     * Inicio de sesión.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
        @Valid @RequestBody LoginRequestDto request
    ) {

        LoginResult result = authService.login(request);

        return ResponseEntity.ok()
            .header(
                HttpHeaders.SET_COOKIE,
                jwtCookieService
                    .createCookie(result.token())
                    .toString()
            )
            .header(
                HttpHeaders.CACHE_CONTROL,
                "no-store"
            )
            .body(
                new LoginResponseDto(result.user())
            );
    }


    /**
     * Cierra la sesión eliminando la cookie JWT.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {

        return ResponseEntity.noContent()
            .header(
                HttpHeaders.SET_COOKIE,
                jwtCookieService
                    .deleteCookie()
                    .toString()
            )
            .build();
    }
}