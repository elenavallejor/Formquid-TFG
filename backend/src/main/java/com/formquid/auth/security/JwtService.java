package com.formquid.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Servicio encargado de generar y validar tokens JWT.
 */
@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Genera un token para el usuario autenticado.
     *
     * @param userDetails usuario autenticado.
     * @return token JWT firmado.
     */
    public String generateToken(CustomUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
            .subject(userDetails.getUsername())
            .claim("userId", userDetails.getId().toString())
            .claim("role", userDetails.getUser().getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Extrae el correo electrónico contenido en el token.
     *
     * @param token token JWT.
     * @return email del usuario.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Comprueba si el token corresponde al usuario y no ha expirado.
     *
     * @param token token JWT.
     * @param userDetails usuario cargado desde la base de datos.
     * @return true si el token es válido.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);

        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Comprueba si el token ha expirado.
     *
     * @param token token JWT.
     * @return true si el token ha expirado.
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token)
            .getExpiration()
            .before(new Date());
    }

    /**
     * Extrae los datos internos del token.
     *
     * @param token token JWT.
     * @return claims del token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Obtiene la clave usada para firmar y verificar tokens.
     *
     * @return clave secreta.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}