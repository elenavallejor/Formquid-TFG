package com.formquid.question.dto;

import com.formquid.question.entity.ButtonShape;
import com.formquid.question.entity.ScaleDisplayType;

import java.math.BigDecimal;

public record QuestionSettingsDto(
    Integer maxLength,
    BigDecimal minValue,
    BigDecimal maxValue,
    BigDecimal step,
    Integer minSelections,
    Integer maxSelections,
    String leftLabel,
    String rightLabel,
    String trueLabel,
    String falseLabel,
    BigDecimal trueEvaluationFactor,
    BigDecimal falseEvaluationFactor,
    String fontFamily,
    Short fontSize,
    String textColor,
    ButtonShape buttonShape,
    ScaleDisplayType scaleDisplayType
) {
}