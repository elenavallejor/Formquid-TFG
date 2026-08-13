package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.CollaborationRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para solicitudes pendientes de colaboración.
 */
public interface CollaborationRequestRepository extends JpaRepository<CollaborationRequest, Long> {

    /**
     * Busca una solicitud cargando las relaciones necesarias para convertirla a DTO.
     */
    @Override
    @EntityGraph(attributePaths = {
        "survey",
        "survey.owner",
        "requestedBy",
        "invitedEditor"
    })
    Optional<CollaborationRequest> findById(Long id);

    /**
     * Lista las solicitudes pendientes de una encuesta.
     */
    @EntityGraph(attributePaths = {
        "survey",
        "requestedBy",
        "invitedEditor"
    })
    List<CollaborationRequest> findBySurveyIdOrderByCreatedAtDesc(Long surveyId);

    /**
     * Lista las solicitudes recibidas por un editor.
     */
    @EntityGraph(attributePaths = {
        "survey",
        "survey.owner",
        "requestedBy",
        "invitedEditor"
    })
    List<CollaborationRequest> findByInvitedEditorIdOrderByCreatedAtDesc(Long editorId);

    /**
     * Busca una solicitud pendiente entre una encuesta y un editor.
     */
    Optional<CollaborationRequest> findBySurveyIdAndInvitedEditorId(
        Long surveyId,
        Long invitedEditorId
    );

    /**
     * Comprueba si ya existe una solicitud pendiente.
     */
    boolean existsBySurveyIdAndInvitedEditorId(Long surveyId, Long invitedEditorId);

    /**
     * Elimina una solicitud pendiente concreta.
     */
    void deleteBySurveyIdAndInvitedEditorId(Long surveyId, Long invitedEditorId);
}