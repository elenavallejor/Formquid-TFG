package com.formquid.statistics.dto;

import com.formquid.question.entity.QuestionType;

import java.util.List;

/**
 * Estadísticas calculadas para una pregunta de encuesta.
 */
public record QuestionStatisticsResponseDto(
    String questionKey,
    String text,
    QuestionType type,
    boolean required,
    long totalAnswers,
    NumericStatisticsDto numericStatistics,
    List<ChoiceOptionStatisticsDto> choiceStatistics,
    List<MatrixRowStatisticsDto> matrixStatistics,
    List<String> textAnswers
) {}
