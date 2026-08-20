package com.formquid.auth.security;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;

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
 * Intercepta cada petición HTTP y comprueba si incluye un token
 * válido en la cabecera. Si el token es correcto, carga el usuario asociado.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Value("${security.jwt.cookie-name:access_token}")
    private String cookieName;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String token = getTokenFromCookie(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String email = jwtService.extractUsername(token);

            if (
                email != null &&
                SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null
            ) {

                UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {

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

        } catch (JwtException | IllegalArgumentException ex) {

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromCookie(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {

            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}