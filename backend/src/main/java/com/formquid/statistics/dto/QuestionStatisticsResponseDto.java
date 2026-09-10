package com.formquid.statistics.dto;

import com.formquid.question.entity.QuestionType;

import java.util.List;

public record QuestionStatisticsResponseDto(
    Long questionId,
    String statement,
    QuestionType questionType,
    long answers,
    long omissions,
    double responseRate,
    NumericStatisticsDto numericStatistics,
    List<ChoiceOptionStatisticsDto> choiceStatistics,
    BooleanStatisticsDto booleanStatistics,
    List<MatrixRowStatisticsDto> matrixStatistics,
    Double normalizedScore,
    Double evaluationScore
) {
}