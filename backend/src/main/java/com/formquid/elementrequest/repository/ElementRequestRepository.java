package com.formquid.elementrequest.repository;

import com.formquid.elementrequest.entity.ElementRequest;
import com.formquid.elementrequest.entity.ElementRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para solicitudes de elementos.
 *
 * Las consultas que posteriormente se convierten a DTO precargan
 * requestedBy y resolvedBy mediante EntityGraph para evitar N+1.
 */
public interface ElementRequestRepository
    extends JpaRepository<ElementRequest, Long> {

    /**
     * Busca una solicitud cargando solicitante y administrador resolutor.
     */
    @Override
    @EntityGraph(attributePaths = {"requestedBy", "resolvedBy"})
    Optional<ElementRequest> findById(Long id);

    /**
     * Obtiene todas las solicitudes ordenadas de más reciente a más antigua.
     */
    @EntityGraph(attributePaths = {"requestedBy", "resolvedBy"})
    List<ElementRequest> findAllByOrderByCreatedAtDesc();

    /**
     * Obtiene solicitudes filtradas por estado.
     */
    @EntityGraph(attributePaths = {"requestedBy", "resolvedBy"})
    List<ElementRequest> findByStatusOrderByCreatedAtDesc(
        ElementRequestStatus status
    );

    /**
     * Obtiene el historial de solicitudes creadas por un usuario.
     */
    @EntityGraph(attributePaths = {"requestedBy", "resolvedBy"})
    List<ElementRequest> findByRequestedByIdOrderByCreatedAtDesc(
        Long userId
    );

    /**
     * Cuenta solicitudes por estado directamente en base de datos.
     */
    long countByStatus(ElementRequestStatus status);
}
