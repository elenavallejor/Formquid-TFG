package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.SurveyCollaboration;
import com.formquid.collaboration.entity.SurveyCollaborationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de colaboraciones e invitaciones de encuestas.
 */
public interface SurveyCollaborationRepository
    extends JpaRepository<SurveyCollaboration, Long> {

    @Override
    @EntityGraph(attributePaths = {
        "survey",
        "survey.owner",
        "requestedBy",
        "editor"
    })
    Optional<SurveyCollaboration> findById(Long id);

    @EntityGraph(attributePaths = {
        "survey",
        "requestedBy",
        "editor"
    })
    List<SurveyCollaboration> findBySurveyIdAndStatusOrderByCreatedAtDesc(
        Long surveyId,
        SurveyCollaborationStatus status
    );

    @EntityGraph(attributePaths = {
        "survey",
        "survey.owner",
        "requestedBy",
        "editor"
    })
    List<SurveyCollaboration> findByEditorIdAndStatusOrderByCreatedAtDesc(
        Long editorId,
        SurveyCollaborationStatus status
    );

    @EntityGraph(attributePaths = {
        "survey",
        "survey.owner",
        "requestedBy",
        "editor"
    })
    List<SurveyCollaboration> findByEditorIdOrderByCreatedAtDesc(Long editorId);

    @EntityGraph(attributePaths = {
        "survey",
        "requestedBy",
        "editor"
    })
    Optional<SurveyCollaboration> findBySurveyIdAndEditorId(
        Long surveyId,
        Long editorId
    );

    boolean existsBySurveyIdAndEditorIdAndStatus(
        Long surveyId,
        Long editorId,
        SurveyCollaborationStatus status
    );
}
