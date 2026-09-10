package com.formquid.question.entity;

import com.formquid.content.entity.ContentItem;
import com.formquid.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una pregunta de catálogo, local o copiada dentro de otro contenido.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = """
    scope in ('CATALOG_FIXED', 'LOCAL', 'FIXED_SNAPSHOT')
    and question_type in ('TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SCALE', 'MATRIX_SCALE', 'BOOLEAN')
    and evaluation_direction in ('POSITIVE', 'NEGATIVE', 'NEUTRAL')
    and (
        (scope = 'CATALOG_FIXED' and parent_item_id is null and source_fixed_question_id is null)
        or (scope = 'LOCAL' and parent_item_id is not null)
        or (scope = 'FIXED_SNAPSHOT' and parent_item_id is not null)
    )
    and (
        (evaluation_direction = 'NEUTRAL' and evaluation_weight is null)
        or (evaluation_direction in ('POSITIVE', 'NEGATIVE') and evaluation_weight > 0)
    )
""")
@Table(
    name = "questions",
    uniqueConstraints = @UniqueConstraint(name = "uk_questions_parent_item", columnNames = "parent_item_id"),
    indexes = {
        @Index(name = "idx_questions_scope", columnList = "scope"),
        @Index(name = "idx_questions_source_fixed", columnList = "source_fixed_question_id"),
        @Index(name = "idx_questions_created_by", columnList = "created_by_user_id")
    }
)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", unique = true)
    private ContentItem parentItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionScope scope;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Column(columnDefinition = "text")
    private String annotation;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 40)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_direction", nullable = false, length = 30)
    private EvaluationDirection evaluationDirection;

    @Column(name = "evaluation_weight")
    private BigDecimal evaluationWeight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_fixed_question_id")
    private Question sourceFixedQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private QuestionSettings settings;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 30)
    @Builder.Default
    private List<QuestionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 30)
    @Builder.Default
    private List<MatrixRow> matrixRows = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (evaluationDirection == null) {
            evaluationDirection = EvaluationDirection.NEUTRAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void setSettings(QuestionSettings settings) {
        this.settings = settings;
        if (settings != null) {
            settings.setQuestion(this);
        }
    }

    public void addOption(QuestionOption option) {
        options.add(option);
        option.setQuestion(this);
    }

    public void addMatrixRow(MatrixRow row) {
        matrixRows.add(row);
        row.setQuestion(this);
    }
}