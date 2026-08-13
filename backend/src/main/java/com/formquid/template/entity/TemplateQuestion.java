package com.formquid.template.entity;

import com.formquid.fixedquestion.entity.EvaluationDirection;
import com.formquid.fixedquestion.entity.FixedQuestion;
import com.formquid.fixedquestion.entity.MatrixRowConfig;
import com.formquid.fixedquestion.entity.OptionConfig;
import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.entity.ScaleDisplayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una pregunta incluida en una plantilla.
 *
 * Puede ser una pregunta fija reutilizable o una pregunta propia creada
 * directamente dentro de la plantilla.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "template_questions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_template_questions_block_question",
            columnNames = {"template_block_id", "fixed_question_id"}
        ),
        @UniqueConstraint(
            name = "uk_template_questions_block_position",
            columnNames = {"template_block_id", "position"}
        )
    },
    indexes = {
        @Index(name = "idx_template_questions_fixed_question_id", columnList = "fixed_question_id"),
        @Index(name = "idx_template_questions_source", columnList = "question_source")
    }
)
public class TemplateQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Grupo de plantilla al que pertenece la pregunta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_block_id", nullable = false)
    private TemplateBlock templateBlock;

    /**
     * Origen de la pregunta dentro de la plantilla.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_source", nullable = false, length = 30)
    private TemplateQuestionSource source;

    /**
     * Pregunta fija reutilizada.
     *
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_question_id")
    private FixedQuestion fixedQuestion;

    /**
     * Enunciado de una pregunta propia.
     *
     * En preguntas fijas puede ser null, ya que el texto se obtiene de
     * fixedQuestion.
     */
    @Column(length = 1000)
    private String text;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_direction", length = 30)
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
     * Opciones configurables para preguntas propias de selección.
     *
     * Se almacenan como JSONB para mantener el orden y evitar tablas
     * auxiliares innecesarias.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<OptionConfig> options = new ArrayList<>();

    /**
     * Filas configurables para preguntas propias de matriz.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matrix_rows", columnDefinition = "jsonb")
    @Builder.Default
    private List<MatrixRowConfig> matrixRows = new ArrayList<>();

    /**
     * Posición de la pregunta dentro de su grupo.
     */
    @Column(nullable = false)
    private Integer position;
}