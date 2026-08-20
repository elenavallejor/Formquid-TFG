package com.formquid.auth.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class JwtCookieService {

    @Value("${security.jwt.cookie-name:access_token}")
    private String cookieName;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${security.jwt.cookie-secure:false}")
    private boolean secure;

    @Value("${security.jwt.cookie-same-site:Lax}")
    private String sameSite;

    public ResponseCookie createCookie(String token) {

        return ResponseCookie.from(cookieName, token)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(Duration.ofMillis(expirationMs))
            .build();
    }

    public ResponseCookie deleteCookie() {

        return ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(Duration.ZERO)
            .build();
    }
}