package com.formquid.auth.security;

import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

/**
 * Servicio de gestión de tokens JWT.
 */
@Service
public class JwtService {

    private static final String LEGACY_INSECURE_SECRET =
        "FormQuidSecretKeyForJwtAuthenticationMustBeAtLeastThirtyTwoChars";
    private static final String FINGERPRINT_CLAIM = "credentialsFingerprint";
    private static final byte[] FINGERPRINT_KEY_CONTEXT =
        "FormQuid JWT credentials fingerprint key v1".getBytes(StandardCharsets.UTF_8);

    private final UserRepository userRepository;
    private final SecretKey signingKey;
    private final SecretKey fingerprintKey;
    private final long jwtExpirationMs;

    public JwtService(
        UserRepository userRepository,
        @Value("${security.jwt.secret}") String jwtSecret,
        @Value("${security.jwt.expiration-ms}") long jwtExpirationMs
    ) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException("JWT_SECRET debe estar configurada");
        }
        if (LEGACY_INSECURE_SECRET.equals(jwtSecret)) {
            throw new IllegalArgumentException("JWT_SECRET no puede usar la clave pública anterior");
        }
        if (jwtExpirationMs <= 0) {
            throw new IllegalArgumentException("La duración del JWT debe ser positiva");
        }
        this.userRepository = userRepository;
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.fingerprintKey = new SecretKeySpec(
            hmac(new SecretKeySpec(secretBytes, "HmacSHA256"), FINGERPRINT_KEY_CONTEXT),
            "HmacSHA256"
        );
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Genera un JWT firmado para un usuario autenticado.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
            .subject(user.getEmail())
            .claim("userId", user.getId().toString())
            .claim(FINGERPRINT_CLAIM, fingerprint(user))
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(signingKey)
            .compact();
    }

    /**
     * Valida el token y carga el usuario actual para aplicar sus permisos vigentes.
     */
    public User extractAuthenticatedUser(String token) {
        Claims claims = extractAllClaims(token);

        String email = claims.getSubject();
        String userId = claims.get("userId", String.class);
        String tokenFingerprint = claims.get(FINGERPRINT_CLAIM, String.class);
        Date expiration = claims.getExpiration();

        if (email == null || email.isBlank()
            || userId == null || userId.isBlank()
            || tokenFingerprint == null || tokenFingerprint.isBlank()
            || expiration == null || !expiration.after(new Date())) {
            throw new IllegalArgumentException("JWT incompleto o expirado");
        }

        User user = userRepository.findById(Long.parseLong(userId))
            .orElseThrow(() -> new JwtException("El usuario del JWT ya no existe"));
        if (!email.equals(user.getEmail()) || !user.isEnabled()
            || !user.isAccountNonLocked() || !user.isAccountNonExpired()
            || !user.isCredentialsNonExpired()) {
            throw new JwtException("La cuenta del JWT ya no está activa");
        }
        byte[] suppliedFingerprint;
        try {
            suppliedFingerprint = Base64.getUrlDecoder().decode(tokenFingerprint);
        } catch (IllegalArgumentException exception) {
            throw new JwtException("Huella de credenciales inválida", exception);
        }
        if (!MessageDigest.isEqual(suppliedFingerprint, credentialFingerprint(user))) {
            throw new JwtException("El JWT se emitió antes del último cambio de contraseña");
        }
        return user;
    }

    private String fingerprint(User user) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(credentialFingerprint(user));
    }

    private byte[] credentialFingerprint(User user) {
        String passwordHash = Objects.requireNonNull(user.getPasswordHash(),
            "El usuario debe tener una contraseña almacenada");
        String value = user.getId() + ":" + passwordHash;
        return hmac(fingerprintKey, value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmac(SecretKey key, byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(value);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 no está disponible", exception);
        }
    }

    /**
     * Parsea el token y verifica su firma criptográfica.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

}
