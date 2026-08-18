package com.formquid.collaboration.entity;

import com.formquid.common.entity.RequestStatus;
import com.formquid.survey.entity.Survey;
import com.formquid.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = "request_type in ('INVITATION', 'ACCESS_REQUEST') and status in ('PENDING', 'APPROVED', 'REJECTED')")
@Table(
    name = "collaboration_requests",
    indexes = {
        @Index(name = "idx_collaboration_requests_survey", columnList = "survey_id"),
        @Index(name = "idx_collaboration_requests_user", columnList = "user_id")
    }
)
public class CollaborationRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private CollaborationRequestType requestType;

    @Column(columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = RequestStatus.PENDING;
        }
    }

    public void approve() {
        status = RequestStatus.APPROVED;
        resolvedAt = Instant.now();
    }

    public void reject() {
        status = RequestStatus.REJECTED;
        resolvedAt = Instant.now();
    }
}
