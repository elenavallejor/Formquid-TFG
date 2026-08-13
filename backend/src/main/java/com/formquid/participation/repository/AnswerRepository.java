package com.formquid.participation.repository;

import com.formquid.participation.entity.Answer;
import com.formquid.statistics.projection.BooleanStatisticsProjection;
import com.formquid.statistics.projection.ChoiceCountProjection;
import com.formquid.statistics.projection.MatrixRowStatisticsProjection;
import com.formquid.statistics.projection.NumericStatisticsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de acceso a datos para respuestas.
 *
 */
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByParticipationId(Long participationId);

    List<Answer> findBySurveyQuestionId(Long surveyQuestionId);

    List<Answer> findBySurveyQuestionIdOrderByIdAsc(Long surveyQuestionId);

    /**
     * Calcula estadísticas numéricas de una pregunta usando la base de datos.
     */
    @Query(
        value = """
            select
                count(a.id) as answer_count,
                avg(a.numeric_value) as average,
                min(a.numeric_value) as minimum,
                max(a.numeric_value) as maximum
            from answers a
            where a.survey_question_id = :questionId
              and a.numeric_value is not null
        """,
        nativeQuery = true
    )
    NumericStatisticsProjection calculateNumericStatistics(
        @Param("questionId") Long questionId
    );

    /**
     * Calcula el número de respuestas textuales no vacías de una pregunta.
     */
    @Query(
        value = """
            select count(a.id)
            from answers a
            where a.survey_question_id = :questionId
              and a.text_value is not null
              and btrim(a.text_value) <> ''
        """,
        nativeQuery = true
    )
    long countTextAnswers(@Param("questionId") Long questionId);

    /**
     * Obtiene una muestra limitada de respuestas textuales.
     *
     * No se devuelven todas para evitar respuestas HTTP demasiado grandes.
     */
    @Query(
        value = """
            select a.text_value
            from answers a
            where a.survey_question_id = :questionId
              and a.text_value is not null
              and btrim(a.text_value) <> ''
            order by a.id asc
            limit :limit
        """,
        nativeQuery = true
    )
    List<String> findTextAnswerSample(
        @Param("questionId") Long questionId,
        @Param("limit") int limit
    );

    /**
     * Calcula los recuentos de respuestas booleanas en base de datos.
     */
    @Query(
        value = """
            select
                count(*) filter (where a.boolean_value = true) as yes_count,
                count(*) filter (where a.boolean_value = false) as no_count
            from answers a
            where a.survey_question_id = :questionId
              and a.boolean_value is not null
        """,
        nativeQuery = true
    )
    BooleanStatisticsProjection calculateBooleanStatistics(
        @Param("questionId") Long questionId
    );

    /**
     * Cuenta cuántas respuestas tienen al menos una opción seleccionada.
     *
     * Se usa como denominador para calcular porcentajes en preguntas
     * de selección simple y múltiple.
     */
    @Query(
        value = """
            select count(a.id)
            from answers a
            where a.survey_question_id = :questionId
              and jsonb_array_length(coalesce(a.selected_values, '[]'::jsonb)) > 0
        """,
        nativeQuery = true
    )
    long countSelectionAnswers(@Param("questionId") Long questionId);

    /**
     * Cuenta cuántas veces se selecciona cada opción.
     *
     * selected_values es una columna JSONB con un array de storedValue.
     */
    @Query(
        value = """
            select
                selected_value.value as stored_value,
                count(*) as answer_count
            from answers a
            cross join lateral jsonb_array_elements_text(
                coalesce(a.selected_values, '[]'::jsonb)
            ) as selected_value(value)
            where a.survey_question_id = :questionId
            group by selected_value.value
        """,
        nativeQuery = true
    )
    List<ChoiceCountProjection> countSelectedValues(
        @Param("questionId") Long questionId
    );

    /**
     * Cuenta cuántas respuestas tienen valores de matriz.
     */
    @Query(
        value = """
            select count(a.id)
            from answers a
            where a.survey_question_id = :questionId
              and jsonb_array_length(coalesce(a.matrix_values, '[]'::jsonb)) > 0
        """,
        nativeQuery = true
    )
    long countMatrixAnswers(@Param("questionId") Long questionId);

    /**
     * Calcula estadísticas por fila de matriz usando JSONB.
     *
     * matrix_values es un array JSONB. Cada elemento contiene:
     * - rowKey
     * - value
     * - scoreValue
     *
     * Se prioriza scoreValue si existe. Si no existe, se intenta convertir
     * value a número.
     */
    @Query(
        value = """
            select
                row_key as row_key,
                count(numeric_value) as answer_count,
                avg(numeric_value) as average,
                min(numeric_value) as minimum,
                max(numeric_value) as maximum
            from (
                select
                    matrix_value.item ->> 'rowKey' as row_key,
                    case
                        when matrix_value.item ->> 'scoreValue' is not null
                             and matrix_value.item ->> 'scoreValue' ~ '^-?[0-9]+(\\.[0-9]+)?$'
                            then (matrix_value.item ->> 'scoreValue')::numeric
                        when matrix_value.item ->> 'value' is not null
                             and matrix_value.item ->> 'value' ~ '^-?[0-9]+(\\.[0-9]+)?$'
                            then (matrix_value.item ->> 'value')::numeric
                        else null
                    end as numeric_value
                from answers a
                cross join lateral jsonb_array_elements(
                    coalesce(a.matrix_values, '[]'::jsonb)
                ) as matrix_value(item)
                where a.survey_question_id = :questionId
            ) matrix_rows
            where row_key is not null
              and numeric_value is not null
            group by row_key
        """,
        nativeQuery = true
    )
    List<MatrixRowStatisticsProjection> calculateMatrixStatistics(
        @Param("questionId") Long questionId
    );
}