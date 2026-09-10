package com.formquid.question.entity;

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
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = "position >= 1 and (evaluation_factor is null or evaluation_factor between 0 and 1)")
@Table(
    name = "question_options",
    uniqueConstraints = @UniqueConstraint(name = "uk_question_options_position", columnNames = {"question_id", "position"}),
    indexes = @Index(name = "idx_question_options_question", columnList = "question_id, position")
)
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, columnDefinition = "text")
    private String label;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "is_other", nullable = false)
    private boolean other;

    @Column(name = "evaluation_factor")
    private BigDecimal evaluationFactor;
}