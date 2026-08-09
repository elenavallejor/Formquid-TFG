package com.formquid.config;

import com.formquid.auth.security.CustomUserDetailsService;
import com.formquid.auth.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración principal de seguridad de la API REST de FormQuid.
 *
 * Define qué endpoints son públicos, qué endpoints requieren autenticación,
 * cómo se validan las credenciales de los usuarios y cómo se integra el filtro
 * JWT dentro de la cadena de seguridad de Spring.
 *
 * La aplicación utiliza una arquitectura sin estado, por lo que no se mantiene
 * una sesión HTTP tradicional en el servidor. En su lugar, las peticiones
 * protegidas deben incluir un token JWT en la cabecera Authorization.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Define la cadena principal de filtros de seguridad.
     *
     * Se desactivan mecanismos propios de aplicaciones web tradicionales,
     * como formulario de login, HTTP Basic, logout de sesión y CSRF, porque
     * el backend funciona como API REST autenticada mediante JWT.
     *
     * @param http objeto de configuración de seguridad HTTP.
     * @return cadena de filtros de seguridad configurada.
     * @throws Exception si ocurre un error al construir la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/registration-requests",
                    "/api/v1/public/**",
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /**
     * Define el proveedor de autenticación basado en la base de datos.
     *
     * Utiliza {@link CustomUserDetailsService} para cargar usuarios desde
     * PostgreSQL y {@link PasswordEncoder} para comparar la contraseña enviada
     * en el login con la contraseña cifrada almacenada.
     *
     * @return proveedor de autenticación configurado.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider =
            new DaoAuthenticationProvider(customUserDetailsService);

        authenticationProvider.setPasswordEncoder(passwordEncoder());

        return authenticationProvider;
    }

    /**
     * Expone el AuthenticationManager utilizado por el servicio de autenticación.
     *
     * El AuthenticationManager será usado en AuthService para validar las
     * credenciales enviadas en el login.
     *
     * @param authenticationConfiguration configuración de autenticación de Spring.
     * @return gestor de autenticación de Spring Security.
     * @throws Exception si no se puede obtener el gestor de autenticación.
     */
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Define el algoritmo utilizado para cifrar y verificar contraseñas.
     *
     * BCrypt es una opción adecuada para almacenar contraseñas porque incorpora
     * salt y un coste computacional configurable.
     *
     * @return codificador de contraseñas basado en BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}