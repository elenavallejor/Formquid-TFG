package com.formquid.question.dto;

import com.formquid.question.entity.ButtonShape;
import com.formquid.question.entity.ScaleDisplayType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record QuestionSettingsDto(
    @Positive Integer maxLength,
    BigDecimal minValue,
    BigDecimal maxValue,
    @Positive BigDecimal step,
    @Positive Integer minSelections,
    @Positive Integer maxSelections,
    String leftLabel,
    String rightLabel,
    @Size(max = 100) String trueLabel,
    @Size(max = 100) String falseLabel,
    @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal trueEvaluationFactor,
    @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal falseEvaluationFactor,
    @Size(max = 80) String fontFamily,
    @Positive Short fontSize,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String textColor,
    ButtonShape buttonShape,
    ScaleDisplayType scaleDisplayType
) {
}
