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

/**
 * Inicializador de datos para el entorno de desarrollo.
 *
 * Este componente crea un usuario super inicial
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
     * super inicial, se crea automáticamente con rol SUPERADMIN.
     *
     * @param args argumentos recibidos por línea de comandos.
     */
    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(initialSuperadminEmail)) {
            return;
        }

        User superadmin = User.builder()
            .fullName("Superadministrador FormQuid")
            .email(initialSuperadminEmail)
            .passwordHash(passwordEncoder.encode(initialSuperadminPassword))
            .role(UserRole.SUPERADMIN)
            .gender(UserGender.OTHER)
            .institution("FormQuid")
            .enabled(true)
            .build();

        userRepository.save(superadmin);
    }
}