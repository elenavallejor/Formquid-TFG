package com.formquid.template.entity;

import com.formquid.survey.model.SurveyStructure;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Representa una plantilla reutilizable para crear encuestas.
 *
 * Una plantilla agrupa un snapshot de bloques y preguntas que puede copiarse
 * posteriormente a una encuesta real. Las plantillas no almacenan respuestas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = "status in ('ACTIVE', 'DISABLED', 'ARCHIVED')")
@Table(
    name = "survey_templates",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_survey_templates_name_normalized",
            columnNames = "name_normalized"
        )
    },
    indexes = {
        @Index(name = "idx_survey_templates_status", columnList = "status"),
        @Index(name = "idx_survey_templates_created_by", columnList = "created_by_user_id"),
        @Index(name = "idx_survey_templates_status_name", columnList = "status, name"),
        @Index(name = "idx_survey_templates_created_at", columnList = "created_at")
    }
)
public class SurveyTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre visible de la plantilla.
     */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Nombre normalizado usado para evitar duplicados.
     */
    @Column(name = "name_normalized", nullable = false, length = 150)
    private String nameNormalized;

    /**
     * Descripción opcional de la plantilla.
     */
    @Column(length = 1000)
    private String description;

    /**
     * Estado de disponibilidad de la plantilla.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TemplateStatus status;

    /**
     * Estructura reutilizable de bloques y preguntas.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_structure", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private SurveyStructure templateStructure = new SurveyStructure();

    /**
     * Usuario administrador que creó la plantilla.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

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
            this.status = TemplateStatus.ACTIVE;
        }

        normalizeStructure();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeStructure();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Indica si la plantilla contiene al menos una pregunta reutilizable.
     */
    public boolean hasQuestions() {
        return templateStructure != null && templateStructure.hasQuestions();
    }

    private void normalizeStructure() {
        if (this.templateStructure == null) {
            this.templateStructure = new SurveyStructure();
        }
    }
}
