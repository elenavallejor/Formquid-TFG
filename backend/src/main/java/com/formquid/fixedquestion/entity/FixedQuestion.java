package com.formquid.fixedquestion.entity;

import com.formquid.block.entity.Block;
import com.formquid.user.entity.User;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una pregunta fija reutilizable.
 *
 * Las preguntas fijas pueden usarse directamente en encuestas o formar parte
 * de plantillas. Las opciones y filas de matriz se almacenan como JSONB para
 * mantener un modelo flexible sin crear tablas auxiliares.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "fixed_questions",
    indexes = {
        @Index(name = "idx_fixed_questions_status", columnList = "status"),
        @Index(name = "idx_fixed_questions_block_id", columnList = "block_id"),
        @Index(name = "idx_fixed_questions_block_status", columnList = "block_id, status"),
        @Index(name = "idx_fixed_questions_created_by", columnList = "created_by_user_id"),
        @Index(name = "idx_fixed_questions_type", columnList = "type")
    }
)
public class FixedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Enunciado principal de la pregunta.
     */
    @Column(nullable = false, length = 1000)
    private String text;

    /**
     * Descripción opcional o aclaración de la pregunta.
     */
    @Column(length = 1000)
    private String description;

    /**
     * Tipo funcional de respuesta esperada.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private QuestionType type;

    /**
     * Indica cómo debe interpretarse la puntuación de la pregunta.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_direction", nullable = false, length = 30)
    private EvaluationDirection evaluationDirection;

    /**
     * Estado de disponibilidad de la pregunta fija.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FixedQuestionStatus status;

    /**
     * Bloque reutilizable al que pertenece la pregunta.
     *
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    /**
     * Usuario administrador que creó la pregunta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    /**
     * Indica si la pregunta será obligatoria por defecto al copiarse.
     */
    @Builder.Default
    @Column(name = "required_by_default", nullable = false)
    private boolean requiredByDefault = false;

    /**
     * Valor mínimo permitido para respuestas numéricas o de escala.
     */
    @Column(name = "min_value")
    private Integer minValue;

    /**
     * Valor máximo permitido para respuestas numéricas o de escala.
     */
    @Column(name = "max_value")
    private Integer maxValue;

    /**
     * Representación visual de una escala.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scale_display_type", length = 30)
    private ScaleDisplayType scaleDisplayType;

    /**
     * Etiqueta izquierda para escalas bipolares.
     */
    @Column(name = "left_label", length = 255)
    private String leftLabel;

    /**
     * Etiqueta derecha para escalas bipolares.
     */
    @Column(name = "right_label", length = 255)
    private String rightLabel;

    /**
     * Opciones configurables de preguntas de selección.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<OptionConfig> options = new ArrayList<>();

    /**
     * Filas configurables de preguntas de matriz.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matrix_rows", columnDefinition = "jsonb")
    @Builder.Default
    private List<MatrixRowConfig> matrixRows = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = FixedQuestionStatus.ACTIVE;
        }

        if (this.evaluationDirection == null) {
            this.evaluationDirection = EvaluationDirection.NEUTRAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}