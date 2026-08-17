package com.formquid.registration.repository;

import com.formquid.registration.entity.RegistrationRequest;
import com.formquid.registration.entity.RegistrationRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para solicitudes de registro.
 *
 * Las consultas que posteriormente se convierten a DTO cargan resolvedBy
 * mediante EntityGraph para evitar consultas adicionales.
 */
public interface RegistrationRequestRepository
    extends JpaRepository<RegistrationRequest, Long> {

    /**
     * Busca una solicitud cargando al administrador que la resolvió.
     */
    @Override
    @EntityGraph(attributePaths = {"resolvedBy"})
    Optional<RegistrationRequest> findById(Long id);

    /**
     * Comprueba si existe una solicitud de un correo y estado concretos,
     * ignorando mayúsculas y minúsculas.
     *
     * Se usa para impedir solicitudes PENDING duplicadas.
     */
    boolean existsByEmailIgnoreCaseAndStatus(
        String email,
        RegistrationRequestStatus status
    );

    /**
     * Obtiene todas las solicitudes, empezando por la más reciente.
     */
    @EntityGraph(attributePaths = {"resolvedBy"})
    List<RegistrationRequest> findAllByOrderByRequestedAtDesc();

    /**
     * Obtiene las solicitudes de un estado concreto.
     *
     * Para las pendientes se muestran primero las más antiguas,
     * facilitando su revisión en orden de llegada.
     */
    @EntityGraph(attributePaths = {"resolvedBy"})
    List<RegistrationRequest> findByStatusOrderByRequestedAtAsc(
        RegistrationRequestStatus status
    );

    /**
     * Cuenta solicitudes por estado directamente en la base de datos.
     */
    long countByStatus(
        RegistrationRequestStatus status
    );
}
