package com.formquid.elementrequest.repository;

import com.formquid.elementrequest.entity.ElementRequest;
import com.formquid.elementrequest.entity.ElementRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de solicitudes de elementos del catálogo.
 */
public interface ElementRequestRepository
    extends JpaRepository<ElementRequest, Long> {

    @Override
    @EntityGraph(attributePaths = {"requestedBy", "resolvedBy"})
    Optional<ElementRequest> findById(Long id);

    /** Obtiene todas las solicitudes, de más reciente a más antigua. */
    @EntityGraph(attributePaths = {"requestedBy", "resolvedBy"})
    List<ElementRequest> findAllByOrderByCreatedAtDesc();

    /** Obtiene solicitudes por estado. */
    @EntityGraph(attributePaths = {"requestedBy", "resolvedBy"})
    List<ElementRequest> findByStatusOrderByCreatedAtDesc(
        ElementRequestStatus status
    );

    /** Obtiene las solicitudes creadas por un usuario. */
    @EntityGraph(attributePaths = {"requestedBy"})
    List<ElementRequest> findByRequestedByIdOrderByCreatedAtDesc(Long userId);

    /** Cuenta solicitudes por estado. */
    long countByStatus(ElementRequestStatus status);
}
