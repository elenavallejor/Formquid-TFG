package com.formquid.config;

import com.formquid.auth.security.JwtAuthenticationFilter;
import com.formquid.common.service.EmailNormalizer;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


/**
 * Configuración principal de seguridad de la aplicación.
 *
 * Se utiliza:
 *
 * - Spring Security para autenticación y autorización.
 * - JWT para identificar a los usuarios autenticados.
 * - Cookie HttpOnly para almacenar el JWT.
 * - Protección CSRF para las operaciones que modifican datos.
 * - BCrypt para almacenar las contraseñas de forma segura.
 * - CORS para permitir únicamente el frontend autorizado.
 *
 * La aplicación sigue siendo STATELESS:
 * Spring no crea ni mantiene sesiones en el servidor.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsProperties corsProperties;


    /**
     * Configura la cadena principal de filtros de Spring Security.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthenticationProvider authenticationProvider,
        JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        /*
         * Configuración necesaria para que Spring pueda recibir
         * peticiones procedentes del frontend React.
         */
        http.cors(cors ->
            cors.configurationSource(corsConfigurationSource())
        );


        CookieCsrfTokenRepository csrfTokenRepository =
            CookieCsrfTokenRepository.withHttpOnlyFalse();

        /*
         * Permite que Spring lea directamente el token enviado
         * mediante la cabecera X-XSRF-TOKEN.
         */
        CsrfTokenRequestAttributeHandler csrfTokenHandler =
            new CsrfTokenRequestAttributeHandler();

        csrfTokenHandler.setCsrfRequestAttributeName(null);

        http.csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .csrfTokenRequestHandler(csrfTokenHandler)
        );



        http.sessionManagement(session ->
            session.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
            )
        );


        /*
         * Indica a Spring Security qué proveedor debe utilizar
         * para autenticar email + contraseña.
         */
        http.authenticationProvider(authenticationProvider);



        http.addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );



        http.authorizeHttpRequests(auth -> auth

            .requestMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()

            .requestMatchers("/api/v1/auth/**")
            .permitAll()

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/registration-requests"
            )
            .permitAll()

            .requestMatchers("/api/v1/public/**")
            .permitAll()

            .requestMatchers(HttpMethod.GET, "/api/v1/categories")
            .permitAll()

            .anyRequest()
            .authenticated()
        );


        /*
         * Si una petición intenta acceder a un recurso protegido
         * sin estar autenticada, Spring devuelve 401.
         */
        http.exceptionHandling(exception ->
            exception.authenticationEntryPoint(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            )
        );


        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
            new CorsConfiguration();


        /*
         * Solo se permite el frontend conocido.
         */
        configuration.setAllowedOrigins(
            corsProperties.allowedOrigins()
        );


        /*
         * Métodos HTTP permitidos desde el frontend.
         */
        configuration.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
            )
        );


        /*
         * Cabeceras que React puede enviar.
         * X-XSRF-TOKEN contiene el token de protección CSRF.
         */
        configuration.setAllowedHeaders(
            List.of(
                "Accept",
                "Content-Type",
                "X-XSRF-TOKEN"
            )
        );


        /*
         *
         * fetch(..., {
         *     credentials: 'include'
         * })
         */
        configuration.setAllowCredentials(true);


        /*
         * La configuración CORS se aplica a toda la API.
         */
        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }


    /**
     * Define cómo Spring comprueba email y contraseña.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
        UserDetailsService userDetailsService
    ) {

        DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider(userDetailsService);

        /*
         *  BCrypt
         */
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }


    /**
     * email como identificador del usuario.
     */
    @Bean
    public UserDetailsService userDetailsService(
        UserRepository userRepository
    ) {

        return username ->
            userRepository
                .findByEmail(
                    EmailNormalizer.normalize(username)
                )
                .orElseThrow(() ->
                    new UsernameNotFoundException(
                        "Usuario no encontrado"
                    )
                );
    }


    /**
     * AuthenticationManager utilizado por AuthService para
     * comprobar las credenciales durante el login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }


    /**
     * ------------------------------------------------------------
     * CIFRADO DE CONTRASEÑAS
     * ------------------------------------------------------------
     *
     * BCrypt genera hashes con salt y está específicamente
     * diseñado para almacenar contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
