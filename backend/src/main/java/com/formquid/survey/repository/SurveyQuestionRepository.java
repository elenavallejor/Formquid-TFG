package com.formquid.survey.repository;

import com.formquid.survey.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para preguntas de encuesta.
 */
public interface SurveyQuestionRepository
    extends JpaRepository<SurveyQuestion, Long> {

    /**
     * Obtiene las preguntas de una encuesta respetando el orden
     * de bloques y preguntas.
     */
    @Query("""
        select question
        from SurveyQuestion question
        join question.surveyBlock block
        where block.survey.id = :surveyId
        order by block.position asc, question.position asc
    """)
    List<SurveyQuestion> findBySurvey(
        @Param("surveyId") Long surveyId
    );

    /**
     * Recupera una pregunta comprobando en la propia consulta
     * que pertenece a la encuesta indicada.
     */
    @Query("""
        select question
        from SurveyQuestion question
        join question.surveyBlock block
        where question.id = :questionId
          and block.survey.id = :surveyId
    """)
    Optional<SurveyQuestion> findByIdAndSurveyId(
        @Param("questionId") Long questionId,
        @Param("surveyId") Long surveyId
    );

    /**
     * Comprueba si una encuesta tiene al menos una pregunta.
     *
     * Se usa antes de publicar una encuesta sin necesidad de cargar
     * toda su estructura en memoria.
     */
    @Query("""
        select case when count(question) > 0 then true else false end
        from SurveyQuestion question
        where question.surveyBlock.survey.id = :surveyId
    """)
    boolean existsBySurveyId(
        @Param("surveyId") Long surveyId
    );
}