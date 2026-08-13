package com.formquid.survey.entity;

import com.formquid.fixedquestion.entity.*;
import com.formquid.template.entity.TemplateQuestion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una pregunta concreta dentro de una encuesta.
 *
 * Guarda una copia completa de la configuración de la pregunta para garantizar
 * que la encuesta no cambie si se modifican preguntas fijas o plantillas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "survey_questions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_survey_questions_block_position",
            columnNames = {"survey_block_id", "position"}
        )
    },
    indexes = {
        @Index(name = "idx_survey_questions_source_fixed", columnList = "source_fixed_question_id"),
        @Index(name = "idx_survey_questions_source_template", columnList = "source_template_question_id"),
        @Index(name = "idx_survey_questions_type", columnList = "type")
    }
)
public class SurveyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Bloque o grupo al que pertenece la pregunta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_block_id", nullable = false)
    private SurveyBlock surveyBlock;

    /**
     * Origen de la pregunta.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SurveyQuestionSource source;

    /**
     * Pregunta fija original, si procede.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_fixed_question_id")
    private FixedQuestion sourceFixedQuestion;

    /**
     * Pregunta de plantilla original, si procede.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_template_question_id")
    private TemplateQuestion sourceTemplateQuestion;

    /**
     * Copia del enunciado de la pregunta.
     */
    @Column(nullable = false, length = 1000)
    private String text;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_direction", nullable = false, length = 30)
    private EvaluationDirection evaluationDirection;

    @Builder.Default
    @Column(nullable = false)
    private boolean required = false;

    @Column(name = "min_value")
    private Integer minValue;

    @Column(name = "max_value")
    private Integer maxValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "scale_display_type", length = 30)
    private ScaleDisplayType scaleDisplayType;

    @Column(name = "left_label", length = 255)
    private String leftLabel;

    @Column(name = "right_label", length = 255)
    private String rightLabel;

    /**
     * Opciones de respuesta copiadas en la encuesta.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<OptionConfig> options = new ArrayList<>();

    /**
     * Filas de matriz copiadas en la encuesta.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matrix_rows", columnDefinition = "jsonb")
    @Builder.Default
    private List<MatrixRowConfig> matrixRows = new ArrayList<>();

    /**
     * Posición calculada por el backend según el orden recibido.
     */
    @Column(nullable = false)
    private Integer position;
}