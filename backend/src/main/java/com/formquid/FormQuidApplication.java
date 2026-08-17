package com.formquid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación backend de FormQuid.
 *
 * Se encarga de iniciar el contexto de Spring Boot y cargar todos los
 * componentes, configuraciones, controladores, servicios y repositorios
 * definidos dentro del paquete base {@code com.formquid}.
 */
@SpringBootApplication
public class FormQuidApplication {

    /**
     * Punto de entrada principal de la aplicación.
     *
     * @param args argumentos recibidos por línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(FormQuidApplication.class, args);
    }
}
