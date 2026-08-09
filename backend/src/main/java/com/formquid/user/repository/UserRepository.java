package com.formquid.user.repository;

import com.formquid.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para usuarios.
 *
 * Incluye las consultas necesarias para autenticación y validación
 * de correos electrónicos.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por correo electrónico.
     *
     * @param email correo electrónico del usuario.
     * @return usuario encontrado, si existe.
     */
    Optional<User> findByEmail(String email);

    /**
     * Comprueba si ya existe un usuario con el correo indicado.
     *
     * @param email correo electrónico a comprobar.
     * @return true si el correo ya está registrado.
     */
    boolean existsByEmail(String email);
}