package com.formquid.auth.security;

import com.formquid.user.entity.User;
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
 * Servicio encargado de generar, leer y validar tokens JWT.
 */
@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Genera un JWT para el usuario autenticado.
     *
     * El correo se usa como subject, mientras que userId y role se incluyen
     * como claims informativos.
     */
    public String generateToken(
        User user
    ) {
        Instant now = Instant.now();
        Instant expiration =
            now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Extrae el correo almacenado como subject del token.
     */
    public String extractUsername(
        String token
    ) {
        return extractAllClaims(token)
            .getSubject();
    }

    /**
     * Comprueba que el JWT corresponda al usuario cargado
     * y que todavía no haya expirado.
     */
    public boolean isTokenValid(
        String token,
        UserDetails userDetails
    ) {
        Claims claims =
            extractAllClaims(token);

        String username =
            claims.getSubject();

        Date expiration =
            claims.getExpiration();

        return username != null
            && username.equals(
                userDetails.getUsername()
            )
            && expiration != null
            && expiration.after(new Date());
    }

    /**
     * Valida la firma del JWT y devuelve sus claims.
     */
    private Claims extractAllClaims(
        String token
    ) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Obtiene la clave HMAC empleada para firmar y verificar los tokens.
     *
     * La propiedad security.jwt.secret debe tener longitud suficiente
     * para el algoritmo HMAC utilizado.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes =
            jwtSecret.getBytes(
                StandardCharsets.UTF_8
            );

        return Keys.hmacShaKeyFor(
            keyBytes
        );
    }
}
