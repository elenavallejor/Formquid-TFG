package com.formquid.participation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
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
@Check(constraints = """
    (
        text_value is not null
        and numeric_value is null
        and boolean_value is null
        and jsonb_array_length(coalesce(selected_values, '[]'::jsonb)) = 0
        and jsonb_array_length(coalesce(matrix_values, '[]'::jsonb)) = 0
        and free_text_value is null
    )
    or (
        text_value is null
        and numeric_value is not null
        and boolean_value is null
        and jsonb_array_length(coalesce(selected_values, '[]'::jsonb)) = 0
        and jsonb_array_length(coalesce(matrix_values, '[]'::jsonb)) = 0
        and free_text_value is null
    )
    or (
        text_value is null
        and numeric_value is null
        and boolean_value is not null
        and jsonb_array_length(coalesce(selected_values, '[]'::jsonb)) = 0
        and jsonb_array_length(coalesce(matrix_values, '[]'::jsonb)) = 0
        and free_text_value is null
    )
    or (
        text_value is null
        and numeric_value is null
        and boolean_value is null
        and jsonb_array_length(coalesce(selected_values, '[]'::jsonb)) > 0
        and jsonb_array_length(coalesce(matrix_values, '[]'::jsonb)) = 0
    )
    or (
        text_value is null
        and numeric_value is null
        and boolean_value is null
        and jsonb_array_length(coalesce(selected_values, '[]'::jsonb)) = 0
        and jsonb_array_length(coalesce(matrix_values, '[]'::jsonb)) > 0
        and free_text_value is null
    )
""")
@Table(
    name = "answers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_answers_participation_question",
            columnNames = {"participation_id", "question_key"}
        )
    },
    indexes = {
        @Index(name = "idx_answers_question_key", columnList = "question_key"),
        @Index(name = "idx_answers_question_score", columnList = "question_key, score_value"),
        @Index(name = "idx_answers_question_numeric", columnList = "question_key, numeric_value"),
        @Index(name = "idx_answers_question_boolean", columnList = "question_key, boolean_value")
    }
)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Participación a la que pertenece la respuesta.
     *
     * La restricción única participation_id + question_key evita que una
     * misma participación responda dos veces a la misma pregunta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private Participation participation;

    /**
     * Clave estable de la pregunta respondida dentro del snapshot.
     */
    @Column(name = "question_key", nullable = false, length = 80)
    private String questionKey;

    /**
     * Copia del enunciado en el momento de responder.
     */
    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

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
    @Column(name = "selected_values", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> selectedValues = new ArrayList<>();

    /**
     * Valores respondidos en preguntas de matriz.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matrix_values", nullable = false, columnDefinition = "jsonb")
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

    @PrePersist
    @PreUpdate
    protected void validateBeforeSave() {
        normalizeJsonValues();
        validateSingleAnswerShape();
        validateQuestionBelongsToParticipationSurvey();
    }

    private void normalizeJsonValues() {
        if (questionKey != null) {
            questionKey = questionKey.trim();
        }

        if (questionText != null) {
            questionText = questionText.trim();
        }

        textValue = cleanOptional(textValue);
        freeTextValue = cleanOptional(freeTextValue);

        if (selectedValues == null) {
            selectedValues = new ArrayList<>();
        } else {
            selectedValues = new ArrayList<>(
                selectedValues
                    .stream()
                    .filter(this::hasText)
                    .map(String::trim)
                    .toList()
            );
        }

        if (matrixValues == null) {
            matrixValues = new ArrayList<>();
        }
    }

    private void validateSingleAnswerShape() {
        int valueKinds = 0;

        if (hasText(textValue)) {
            valueKinds++;
        }

        if (numericValue != null) {
            valueKinds++;
        }

        if (booleanValue != null) {
            valueKinds++;
        }

        if (!selectedValues.isEmpty()) {
            valueKinds++;
        }

        if (!matrixValues.isEmpty()) {
            validateMatrixValues();
            valueKinds++;
        }

        if (valueKinds != 1) {
            throw new IllegalStateException(
                "Cada respuesta debe almacenar exactamente un tipo de valor"
            );
        }

        if (!selectedValues.isEmpty()) {
            return;
        }

        if (hasText(freeTextValue)) {
            throw new IllegalStateException(
                "El texto libre solo es válido en respuestas de selección"
            );
        }
    }

    private void validateQuestionBelongsToParticipationSurvey() {
        if (participation == null
            || participation.getSurvey() == null
            || participation.getSurvey().getStructure() == null
            || questionKey == null
            || questionKey.isBlank()
            || questionText == null
            || questionText.isBlank()) {
            throw new IllegalStateException(
                "La respuesta debe pertenecer a una participación y a una pregunta de encuesta válidas"
            );
        }

        if (participation
            .getSurvey()
            .getStructure()
            .findQuestion(questionKey)
            .isEmpty()) {
            throw new IllegalStateException(
                "La pregunta respondida no pertenece a la encuesta de la participación"
            );
        }
    }

    private void validateMatrixValues() {
        for (MatrixAnswerValue value : matrixValues) {
            if (value == null
                || !hasText(value.getRowKey())
                || !hasText(value.getValue())) {
                throw new IllegalStateException(
                    "Cada valor de matriz debe indicar fila y valor"
                );
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String cleanOptional(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
