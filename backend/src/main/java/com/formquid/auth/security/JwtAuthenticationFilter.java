package com.formquid.auth.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticación JWT.
 *
 * Utiliza el UserDetailsService estándar definido por Spring Security.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
    extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Autentica la petición cuando contiene un Bearer token válido.
     *
     * Un JWT inválido, manipulado o expirado no debe provocar un error 500:
     * se deja el contexto sin autenticar para que Spring Security gestione
     * posteriormente el acceso al endpoint.
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
            request.getHeader("Authorization");

        if (
            authHeader == null
            || !authHeader.startsWith("Bearer ")
        ) {
            filterChain.doFilter(
                request,
                response
            );
            return;
        }

        String token =
            authHeader.substring(7).trim();

        if (token.isEmpty()) {
            filterChain.doFilter(
                request,
                response
            );
            return;
        }

        try {
            authenticateToken(
                token,
                request
            );
        } catch (
            JwtException
            | IllegalArgumentException exception
        ) {
            /*
             * Se elimina cualquier autenticación parcial. Un endpoint
             * protegido terminará devolviendo 401 mediante Spring Security.
             */
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(
            request,
            response
        );
    }

    /**
     * Valida el token y registra la autenticación en SecurityContext.
     */
    private void authenticateToken(
        String token,
        HttpServletRequest request
    ) {
        if (
            SecurityContextHolder
                .getContext()
                .getAuthentication()
            != null
        ) {
            return;
        }

        String username =
            jwtService.extractUsername(token);

        if (
            username == null
            || username.isBlank()
        ) {
            return;
        }

        UserDetails userDetails =
            userDetailsService.loadUserByUsername(
                username
            );

        if (
            !jwtService.isTokenValid(
                token,
                userDetails
            )
        ) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );

        authentication.setDetails(
            new WebAuthenticationDetailsSource()
                .buildDetails(request)
        );

        SecurityContextHolder
            .getContext()
            .setAuthentication(authentication);
    }
}
