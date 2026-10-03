package com.formquid.participation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AnonymousRespondentService {

    public static final String COOKIE_NAME = "formquid_respondent";

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_LENGTH = 43;
    private static final Duration COOKIE_DURATION = Duration.ofDays(365);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${security.jwt.cookie-secure:false}")
    private boolean secure;

    @Value("${security.jwt.cookie-same-site:Lax}")
    private String sameSite;

    public Identity resolve(String currentToken, String publicCode) {
        boolean hasValidToken = isValid(currentToken);
        String token = hasValidToken ? currentToken : generateToken();
        return new Identity(hash(token, publicCode), hasValidToken ? null : createCookie(token));
    }

    private boolean isValid(String token) {
        return token != null
            && token.length() == TOKEN_LENGTH
            && token.chars().allMatch(character ->
                Character.isLetterOrDigit(character) || character == '-' || character == '_'
            );
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token, String publicCode) {
        try {
            String scopedToken = publicCode.trim().toUpperCase(Locale.ROOT) + ':' + token;
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(scopedToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private ResponseCookie createCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(COOKIE_DURATION)
            .build();
    }

    public record Identity(String tokenHash, ResponseCookie newCookie) {
    }
}
