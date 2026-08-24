package com.formquid.auth.security;

import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio de gestión de tokens JWT.
 *
 * El token contiene únicamente los datos necesarios para autenticar y
 * autorizar peticiones sin consultar la base de datos en cada request.
 */
@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Genera un JWT firmado para un usuario autenticado.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
            .subject(user.getEmail())
            .claim("userId", user.getId().toString())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Valida firma y expiración una sola vez y reconstruye el principal
     * mínimo que Spring Security necesita durante la petición.
     *
     * No realiza consultas a PostgreSQL.
     */
    public User extractAuthenticatedUser(String token) {
        Claims claims = extractAllClaims(token);

        String email = claims.getSubject();
        String userId = claims.get("userId", String.class);
        String role = claims.get("role", String.class);
        Date expiration = claims.getExpiration();

        if (email == null || email.isBlank()
            || userId == null || userId.isBlank()
            || role == null || role.isBlank()
            || expiration == null || !expiration.after(new Date())) {
            throw new IllegalArgumentException("JWT incompleto o expirado");
        }

        return User.builder()
            .id(UUID.fromString(userId))
            .email(email)
            .role(UserRole.valueOf(role))
            .build();
    }

    /**
     * Parsea el token y verifica su firma criptográfica.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
            jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
