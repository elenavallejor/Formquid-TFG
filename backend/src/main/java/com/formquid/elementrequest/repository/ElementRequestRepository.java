package com.formquid.elementrequest.repository;

import com.formquid.elementrequest.dto.ElementRequestResponseDto;
import com.formquid.elementrequest.entity.ElementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ElementRequestRepository
    extends JpaRepository<ElementRequest, UUID> {

    /**
     * Proyección de administración. Solo recupera los campos mostrados por
     * la pantalla de solicitudes.
     */
    @Query("""
        select new com.formquid.elementrequest.dto.ElementRequestResponseDto(
            request.id,
            request.requestedBy.id,
            request.elementType,
            request.message,
            request.requestPayload,
            request.createdAt
        )
        from ElementRequest request
        order by request.createdAt desc
    """)
    List<ElementRequestResponseDto> findAllResponses();
}
