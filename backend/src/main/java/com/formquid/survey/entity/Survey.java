package com.formquid.survey.entity;

import com.formquid.tag.entity.Tag;
import com.formquid.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa una encuesta real creada por un editor.
 *
 * Una encuesta puede estar en borrador, ser pública, privada u oculta.
 * Sus preguntas se almacenan como copia propia para que no se vean afectadas
 * por cambios posteriores en preguntas fijas o plantillas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
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

    @Column(name = "access_password", length = 255)
    private String accessPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    /**
     * Etiquetas libres asociadas a la encuesta.
     */
    @ManyToMany
    @BatchSize(size = 20)
    @JoinTable(
        name = "survey_tags",
        joinColumns = @JoinColumn(name = "survey_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"),
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_survey_tags_survey_tag",
                columnNames = {"survey_id", "tag_id"}
            )
        },
        indexes = {
            @Index(name = "idx_survey_tags_tag_id", columnList = "tag_id")
        }
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    /**
     * Bloques o grupos de preguntas de la encuesta.
     *
     */
    @OneToMany(
        mappedBy = "survey",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("position ASC")
    @BatchSize(size = 20)
    @Builder.Default
    private List<SurveyBlock> blocks = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public void addBlock(SurveyBlock block) {
        blocks.add(block);
        block.setSurvey(this);
    }

    /**
     * Indica si el contenido de la encuesta puede modificarse.
     *
     * Las encuestas únicamente son editables mientras permanecen
     * en estado DRAFT.
     */
    public boolean isEditable() {
        return status == SurveyStatus.DRAFT;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = SurveyStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}