package com.formquid.survey.entity;

import com.formquid.survey.model.SurveyStructure;
import com.formquid.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una encuesta real creada por un editor.
 *
 * Una encuesta puede estar en borrador, ser pública, privada u oculta.
 * Su estructura se almacena como snapshot propio para que no se vea afectada
 * por cambios posteriores en el catálogo o en las plantillas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = """
    status in ('DRAFT', 'PUBLIC', 'PRIVATE', 'HIDDEN')
    and (
        (
            status = 'PRIVATE'
            and access_password_hash is not null
        )
        or (
            status <> 'PRIVATE'
            and access_password_hash is null
        )
    )
""")
@Table(
    name = "surveys",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_surveys_public_code",
            columnNames = "public_code"
        )
    },
    indexes = {
        @Index(name = "idx_surveys_owner", columnList = "owner_user_id"),
        @Index(name = "idx_surveys_status", columnList = "status"),
        @Index(name = "idx_surveys_owner_status", columnList = "owner_user_id, status"),
        @Index(name = "idx_surveys_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_surveys_published_at", columnList = "published_at")
    }
)
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_code", nullable = false, length = 80)
    private String publicCode;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 1500)
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SurveyStatus status;

    @Column(name = "access_password_hash", length = 255)
    private String accessPasswordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    /**
     * Etiquetas libres propias de la encuesta.
     *
     * No existe tabla global de tags porque una etiqueta solo tiene sentido
     * dentro de la encuesta que la usa.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tag_names", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tagNames = new ArrayList<>();

    /**
     * Snapshot completo de bloques y preguntas de la encuesta.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private SurveyStructure structure = new SurveyStructure();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Indica si el contenido de la encuesta puede modificarse.
     *
     * Las encuestas únicamente son editables mientras permanecen
     * en estado DRAFT.
     */
    public boolean isEditable() {
        return status == SurveyStatus.DRAFT;
    }

    /**
     * Indica si la encuesta contiene al menos una pregunta en su snapshot.
     */
    public boolean hasQuestions() {
        return structure != null && structure.hasQuestions();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = SurveyStatus.DRAFT;
        }

        normalizeJsonFields();
        normalizePrivateAccess();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeJsonFields();
        normalizePrivateAccess();

        this.updatedAt = LocalDateTime.now();
    }

    private void normalizeJsonFields() {
        if (this.tagNames == null) {
            this.tagNames = new ArrayList<>();
        }

        if (this.structure == null) {
            this.structure = new SurveyStructure();
        }
    }

    private void normalizePrivateAccess() {
        if (this.status == SurveyStatus.PRIVATE) {
            if (this.accessPasswordHash == null || this.accessPasswordHash.isBlank()) {
                throw new IllegalStateException(
                    "Las encuestas privadas deben tener hash de contraseña de acceso"
                );
            }

            return;
        }

        this.accessPasswordHash = null;
    }
}
