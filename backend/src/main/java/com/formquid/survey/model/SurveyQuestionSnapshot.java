package com.formquid.survey.model;

import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.MatrixRowConfig;
import com.formquid.question.entity.OptionConfig;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.entity.ScaleDisplayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Copia inmutable de una pregunta incluida en una encuesta o plantilla.
 *
 * No es una entidad JPA. Se persiste como parte de un JSONB para que los
 * cambios posteriores del catálogo no alteren encuestas ya creadas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyQuestionSnapshot {

    /**
     * Clave estable de la pregunta dentro de la encuesta.
     */
    private String questionKey;

    /**
     * Pregunta de catálogo de la que se tomó la copia, si existe.
     */
    private Long sourceQuestionId;

    private SurveyQuestionSource source;
    private String text;
    private String description;
    private QuestionType type;
    private EvaluationDirection evaluationDirection;
    private boolean required;
    private Integer minValue;
    private Integer maxValue;
    private ScaleDisplayType scaleDisplayType;
    private String leftLabel;
    private String rightLabel;

    @Builder.Default
    private List<OptionConfig> options = new ArrayList<>();

    @Builder.Default
    private List<MatrixRowConfig> matrixRows = new ArrayList<>();

    private Integer position;
}
