package com.formquid.config.initializer;

import com.formquid.user.entity.User;
import com.formquid.user.entity.UserGender;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Inicializador de datos para el entorno de desarrollo.
 *
 * Este componente crea el superadministrador inicial.
 *
 * Solo se ejecuta cuando el perfil activo es {@code dev}.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-superadmin.email}")
    private String initialSuperadminEmail;

    @Value("${app.initial-superadmin.password}")
    private String initialSuperadminPassword;

    /**
     * Ejecuta la inicialización de datos al arrancar la aplicación.
     *
     * Si no existe ningún usuario con el correo configurado para el
     * superadministrador inicial, se crea automáticamente con rol SUPERADMIN.
     *
     * @param args argumentos recibidos por línea de comandos.
     */
    @Override
    public void run(String... args) {
        String superadminEmail =
            initialSuperadminEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(superadminEmail)) {
            return;
        }

        User superadmin = User.builder()
            .name("Superadministrador FormQuid")
            .email(superadminEmail)
            .passwordHash(passwordEncoder.encode(initialSuperadminPassword))
            .role(UserRole.SUPERADMIN)
            .gender(UserGender.OTHER)
            .institution("FormQuid")
            .enabled(true)
            .build();

        userRepository.save(superadmin);
    }
}
