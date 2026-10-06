package com.formquid.survey.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Modela una encuesta y sus metadatos persistentes dentro de la plataforma.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = """
    lifecycle_status in ('DRAFT', 'OPEN', 'CLOSED')
    and access_mode in ('PUBLIC', 'PASSWORD_PROTECTED', 'AUTHENTICATED')
    and visibility in ('LISTED', 'UNLISTED')
    and (lifecycle_status <> 'DRAFT' or visibility = 'UNLISTED')
    and font_family in ('OPEN_DYSLEXIC', 'TIMES_NEW_ROMAN', 'LEXEND', 'ARIAL')
    and (
        (access_mode = 'PASSWORD_PROTECTED' and access_password_hash is not null)
        or (access_mode <> 'PASSWORD_PROTECTED' and access_password_hash is null)
    )
    and cardinality(tags) <= 10
""")
@Table(
    name = "surveys",
    uniqueConstraints = @UniqueConstraint(name = "uk_surveys_public_code", columnNames = "public_code"),
    indexes = {
        @Index(name = "idx_surveys_owner", columnList = "owner_id"),
        @Index(
            name = "idx_surveys_public_listing",
            columnList = "lifecycle_status, visibility, created_at DESC, id DESC"
        ),
        @Index(name = "idx_surveys_updated_at", columnList = "updated_at")
    }
)
public class Survey {

    public static final int MAX_TAGS = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column( name = "public_code", unique = true, length = 12)
    private String publicCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_key", columnDefinition = "text")
    private String imageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "font_family", nullable = false, length = 30)
    private SurveyFontFamily fontFamily;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 30)
    private SurveyLifecycle lifecycleStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 40)
    private SurveyAccessMode accessMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SurveyVisibility visibility;

    @Column(name = "access_password_hash", length = 255)
    private String accessPasswordHash;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "varchar(50)[]")
    @Builder.Default
    private String[] tags = new String[0];

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 30)
    @Builder.Default
    private List<ContentItem> contentItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        normalize();
    }

    public void normalize() {
        if (lifecycleStatus == null) {
            lifecycleStatus = SurveyLifecycle.DRAFT;
        }
        if (accessMode == null) {
            accessMode = SurveyAccessMode.PUBLIC;
        }
        if (lifecycleStatus == SurveyLifecycle.DRAFT) {
            visibility = SurveyVisibility.UNLISTED;
        } else if (visibility == null) {
            visibility = SurveyVisibility.LISTED;
        }
        if (fontFamily == null) {
            fontFamily = SurveyFontFamily.TIMES_NEW_ROMAN;
        }
        if (accessMode != SurveyAccessMode.PASSWORD_PROTECTED) {
            accessPasswordHash = null;
        }
        if (tags == null) {
            tags = new String[0];
        }
    }
}
