package com.formquid.participation.repository;

import com.formquid.participation.entity.Participation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de acceso a datos para participaciones anónimas.
 */
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    /**
     * Lista las participaciones de una encuesta cargando sus respuestas.
     *
     * Se usa EntityGraph para evitar consultas repetidas al convertir
     * las participaciones a DTO.
     */
    @EntityGraph(attributePaths = {
        "survey",
        "answers",
        "answers.surveyQuestion"
    })
    @Query("""
        select distinct participation
        from Participation participation
        where participation.survey.id = :surveyId
        order by participation.submittedAt desc
    """)
    List<Participation> findBySurveyIdOrderBySubmittedAtDesc(
        @Param("surveyId") Long surveyId
    );

    /**
     * Cuenta las participaciones asociadas a una encuesta.
     */
    long countBySurveyId(Long surveyId);

    /**
     * Comprueba si una encuesta tiene al menos una participación.
     */
    boolean existsBySurveyId(Long surveyId);
}