package com.formquid.config;

import com.formquid.auth.security.JwtAuthenticationFilter;
import com.formquid.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Configuración central de Spring Security.
 *
 * La API utiliza JWT y no mantiene sesiones en el servidor.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final HandlerExceptionResolver resolver;

    /**
     * El filtro JWT no se inyecta en el constructor de esta configuración.
     *
     * Se recibe como parámetro del bean SecurityFilterChain para evitar
     * una dependencia circular con UserDetailsService.
     */
    public SecurityConfig(
        @Qualifier("handlerExceptionResolver")
        HandlerExceptionResolver resolver
    ) {
        this.resolver = resolver;
    }

    /**
     * Configura autorización, manejo de errores y filtro JWT.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .authorizeHttpRequests(auth -> auth
                /*
                 * El inicio de sesión y la configuración inicial de contraseña
                 * son los únicos flujos públicos dentro de /api/auth.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/password-setup"
                ).permitAll()

                /*
                 * Recursos destinados a participantes sin cuenta.
                 */
                .requestMatchers(
                    "/api/v1/public/**"
                ).permitAll()

                /*
                 * Una persona sin cuenta puede solicitar el alta como editor.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/registration-requests"
                ).permitAll()

                .anyRequest()
                .authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                    (request, response, exception) ->
                        resolver.resolveException(
                            request,
                            response,
                            null,
                            exception
                        )
                )
                .accessDeniedHandler(
                    (request, response, exception) ->
                        resolver.resolveException(
                            request,
                            response,
                            null,
                            exception
                        )
                )
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }

    /**
     * Carga usuarios por correo sin distinguir mayúsculas.
     *
     * El estado enabled se transfiere a Spring Security para impedir
     * el inicio de sesión de cuentas deshabilitadas.
     */
    @Bean
    public UserDetailsService userDetailsService(
        UserRepository userRepository
    ) {
        return username ->
            userRepository
                .findByEmailIgnoreCase(
                    username.trim()
                )
                .map(user ->
                    org.springframework.security.core.userdetails.User
                        .builder()
                        .username(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities(
                            "ROLE_"
                                + user.getRole().name()
                        )
                        .disabled(!user.isEnabled())
                        .build()
                )
                .orElseThrow(() ->
                    new UsernameNotFoundException(
                        "Usuario no encontrado: "
                            + username
                    )
                );
    }

    /**
     * Codificador BCrypt para las contraseñas de usuario.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el AuthenticationManager configurado por Spring Security.
     */
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration
            .getAuthenticationManager();
    }
}
