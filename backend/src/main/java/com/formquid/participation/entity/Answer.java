package com.formquid.participation.entity;

import com.formquid.survey.entity.SurveyQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa la respuesta dada a una pregunta concreta de una encuesta.
 *
 * La estructura permite almacenar respuestas textuales, numéricas, booleanas,
 * de selección y de matriz sin crear una tabla distinta para cada tipo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "answers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_answers_participation_question",
            columnNames = {"participation_id", "survey_question_id"}
        )
    },
    indexes = {
        @Index(name = "idx_answers_survey_question_id", columnList = "survey_question_id"),
        @Index(name = "idx_answers_question_score", columnList = "survey_question_id, score_value"),
        @Index(name = "idx_answers_question_numeric", columnList = "survey_question_id, numeric_value"),
        @Index(name = "idx_answers_question_boolean", columnList = "survey_question_id, boolean_value")
    }
)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Participación a la que pertenece la respuesta.
     *
     * La restricción única participation_id + survey_question_id  
     *  índice para búsquedas por participation_id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private Participation participation;

    /**
     * Pregunta de encuesta respondida.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_question_id", nullable = false)
    private SurveyQuestion surveyQuestion;

    /**
     * Valor textual para preguntas de texto corto o largo.
     */
    @Column(name = "text_value", columnDefinition = "text")
    private String textValue;

    /**
     * Valor numérico para preguntas NUMBER, SCALE o BIPOLAR_SCALE.
     */
    @Column(name = "numeric_value", precision = 10, scale = 2)
    private BigDecimal numericValue;

    /**
     * Valor booleano para preguntas de sí/no.
     */
    @Column(name = "boolean_value")
    private Boolean booleanValue;

    /**
     * Valores seleccionados en preguntas de selección.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_values", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> selectedValues = new ArrayList<>();

    /**
     * Valores respondidos en preguntas de matriz.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matrix_values", columnDefinition = "jsonb")
    @Builder.Default
    private List<MatrixAnswerValue> matrixValues = new ArrayList<>();

    /**
     * Texto libre asociado a opciones que lo permitan.
     */
    @Column(name = "free_text_value", columnDefinition = "text")
    private String freeTextValue;

    /**
     * Puntuación calculada para facilitar estadísticas.
     */
    @Column(name = "score_value", precision = 10, scale = 2)
    private BigDecimal scoreValue;
}