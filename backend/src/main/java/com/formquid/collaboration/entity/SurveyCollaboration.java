package com.formquid.collaboration.entity;

import com.formquid.survey.entity.Survey;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

/**
 * Relación de colaboración entre una encuesta y un editor.
 *
 * La misma fila conserva la invitación, su resolución y una posible retirada.
 * Así se reduce el número de tablas y se mantiene trazabilidad histórica.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = """
    requested_by_user_id <> editor_user_id
    and status in ('PENDING', 'ACCEPTED', 'REJECTED', 'REMOVED')
    and (
        (
            status = 'PENDING'
            and resolved_at is null
        )
        or (
            status <> 'PENDING'
            and resolved_at is not null
        )
    )
""")
@Table(
    name = "survey_collaborations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_survey_collaborations_survey_editor",
            columnNames = {"survey_id", "editor_user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_survey_collaborations_survey", columnList = "survey_id"),
        @Index(name = "idx_survey_collaborations_editor", columnList = "editor_user_id"),
        @Index(name = "idx_survey_collaborations_status", columnList = "status"),
        @Index(name = "idx_survey_collaborations_requested_by", columnList = "requested_by_user_id")
    }
)
public class SurveyCollaboration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "editor_user_id", nullable = false)
    private User editor;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SurveyCollaborationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = SurveyCollaborationStatus.PENDING;
        }
    }

    /**
     * Reactiva una colaboración resuelta como nueva invitación pendiente.
     */
    public void reopen(User requester, String message) {
        this.requestedBy = requester;
        this.message = message;
        this.status = SurveyCollaborationStatus.PENDING;
        this.resolvedAt = null;
        this.createdAt = LocalDateTime.now();
    }

    public void accept() {
        resolve(SurveyCollaborationStatus.ACCEPTED);
    }

    public void reject() {
        resolve(SurveyCollaborationStatus.REJECTED);
    }

    public void remove() {
        resolve(SurveyCollaborationStatus.REMOVED);
    }

    private void resolve(SurveyCollaborationStatus finalStatus) {
        if (status == finalStatus) {
            return;
        }

        this.status = finalStatus;
        this.resolvedAt = LocalDateTime.now();
    }
}
