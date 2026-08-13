package com.formquid.collaboration.repository;

import com.formquid.collaboration.entity.SurveyCollaborator;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para colaboradores de encuestas.
 */
public interface SurveyCollaboratorRepository extends JpaRepository<SurveyCollaborator, Long> {

    /**
     * Lista los colaboradores de una encuesta.
     */
    @EntityGraph(attributePaths = {
        "survey",
        "editor",
        "addedBy"
    })
    List<SurveyCollaborator> findBySurveyId(Long surveyId);

    /**
     * Lista las colaboraciones de un editor.
     *
     * Se cargan también propietario y etiquetas de la encuesta porque este
     * método se usa para mostrar listados en el frontend.
     */
    @EntityGraph(attributePaths = {
        "survey",
        "survey.owner",
        "survey.tags",
        "editor",
        "addedBy"
    })
    List<SurveyCollaborator> findByEditorId(Long editorId);

    /**
     * Busca un colaborador concreto dentro de una encuesta.
     */
    @EntityGraph(attributePaths = {
        "survey",
        "editor",
        "addedBy"
    })
    Optional<SurveyCollaborator> findBySurveyIdAndEditorId(
        Long surveyId,
        Long editorId
    );

    /**
     * Comprueba si un editor ya colabora en una encuesta.
     */
    boolean existsBySurveyIdAndEditorId(
        Long surveyId,
        Long editorId
    );

    /**
     * Elimina un colaborador de una encuesta.
     */
    void deleteBySurveyIdAndEditorId(
        Long surveyId,
        Long editorId
    );
}