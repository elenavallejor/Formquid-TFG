package com.formquid.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = """
    (max_length is null or max_length > 0)
    and (step is null or step > 0)
    and (min_value is null or max_value is null or min_value <= max_value)
    and (min_selections is null or min_selections > 0)
    and (max_selections is null or max_selections > 0)
    and (min_selections is null or max_selections is null or min_selections <= max_selections)
    and (true_evaluation_factor is null or true_evaluation_factor between 0 and 1)
    and (false_evaluation_factor is null or false_evaluation_factor between 0 and 1)
    and (text_color is null or text_color ~ '^#[0-9A-Fa-f]{6}$')
    and (button_shape is null or button_shape in ('RECTANGULAR', 'ROUNDED', 'PILL'))
    and (scale_display_type is null or scale_display_type in ('NUMERIC', 'STARS', 'FACES'))
""")
@Table(name = "question_settings")
public class QuestionSettings {

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "min_value")
    private BigDecimal minValue;

    @Column(name = "max_value")
    private BigDecimal maxValue;

    private BigDecimal step;

    @Column(name = "min_selections")
    private Integer minSelections;

    @Column(name = "max_selections")
    private Integer maxSelections;

    @Column(name = "left_label", columnDefinition = "text")
    private String leftLabel;

    @Column(name = "right_label", columnDefinition = "text")
    private String rightLabel;

    @Column(name = "true_label", length = 100)
    private String trueLabel;

    @Column(name = "false_label", length = 100)
    private String falseLabel;

    @Column(name = "true_evaluation_factor")
    private BigDecimal trueEvaluationFactor;

    @Column(name = "false_evaluation_factor")
    private BigDecimal falseEvaluationFactor;

    @Column(name = "font_family", length = 80)
    private String fontFamily;

    @Column(name = "font_size")
    private Short fontSize;

    @Column(name = "text_color", length = 7)
    private String textColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "button_shape", length = 30)
    private ButtonShape buttonShape;

    @Enumerated(EnumType.STRING)
    @Column(name = "scale_display_type", length = 30)
    private ScaleDisplayType scaleDisplayType;
}