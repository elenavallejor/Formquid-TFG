package com.formquid.config.initializer;

import com.formquid.common.service.EmailNormalizer;
import com.formquid.common.CountryCodes;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el superadministrador inicial cuando la base de datos todavía no contiene usuarios.
 */
@Component
@RequiredArgsConstructor
public class SuperadminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.superadmin.email:${SUPERADMIN_EMAIL:admin@formquid.local}}")
    private String email;

    @Value("${app.superadmin.password:${SUPERADMIN_PASSWORD:change-me-superadmin}}")
    private String password;

    @Value("${app.superadmin.name:${SUPERADMIN_NAME:Superadministrador}}")
    private String name;

    @Value("${app.superadmin.institution:${SUPERADMIN_INSTITUTION:FormQuid}}")
    private String institution;

    @Value("${app.superadmin.country:${SUPERADMIN_COUNTRY:}}")
    private String country;

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsAny()) {
            userRepository.save(User.builder()
                .role(UserRole.SUPERADMIN)
                .email(EmailNormalizer.normalize(email))
                .name(name)
                .institution(institution)
                .country(country.isBlank() ? null : CountryCodes.normalizeConfigured(country))
                .passwordHash(passwordEncoder.encode(password))
                .build());
            return;
        }

        if (!userRepository.existsByRole(UserRole.SUPERADMIN)) {
            throw new IllegalStateException(
                "La base debe contener al menos un superadministrador"
            );
        }
    }

}
