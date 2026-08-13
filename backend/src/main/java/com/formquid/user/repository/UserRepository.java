package com.formquid.user.repository;

import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para usuarios.
 *
 * Incluye consultas para autenticación, validación de correos,
 * administración de usuarios y búsqueda de editores.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por correo electrónico.
     *
     * Se usa durante el proceso de autenticación.
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario por correo electrónico ignorando mayúsculas.
     *
     * Se usa al invitar colaboradores y al validar solicitudes de registro.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Comprueba si ya existe un usuario con el correo indicado.
     */
    boolean existsByEmail(String email);

    /**
     * Lista todos los usuarios ordenados por nombre.
     */
    List<User> findAllByOrderByNameAsc();

    /**
     * Lista usuarios por rol.
     */
    List<User> findByRoleOrderByNameAsc(UserRole role);

    /**
     * Cuenta usuarios activos de un rol concreto.
     *
     * Se usa para impedir dejar el sistema sin ningún SUPERADMIN activo.
     */
    long countByRoleAndEnabled(UserRole role, boolean enabled);

    /**
     * Busca usuarios activos por rol.
     *
     * Se usa para buscar editores al crear colaboraciones. Permite filtrar
     * por nombre, correo o institución. También permite excluir al usuario
     * autenticado para evitar que aparezca como candidato.
     */
    @Query("""
        select appUser
        from User appUser
        where appUser.role = :role
          and appUser.enabled = true
          and (:excludedUserId is null or appUser.id <> :excludedUserId)
          and (
              :query is null
              or lower(appUser.name) like concat('%', :query, '%')
              or lower(appUser.email) like concat('%', :query, '%')
              or lower(appUser.institution) like concat('%', :query, '%')
          )
        order by appUser.name asc
    """)
    List<User> searchEnabledUsersByRole(
        @Param("role") UserRole role,
        @Param("query") String query,
        @Param("excludedUserId") Long excludedUserId
    );
}