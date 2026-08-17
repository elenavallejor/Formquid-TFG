package com.formquid.registration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa la ejecución de tareas programadas utilizadas por el módulo de
 * registro para realizar labores automáticas de mantenimiento.
 */
@Configuration
@EnableScheduling
public class RegistrationSchedulingConfig {
    // La configuración se realiza mediante las anotaciones de Spring.
}
