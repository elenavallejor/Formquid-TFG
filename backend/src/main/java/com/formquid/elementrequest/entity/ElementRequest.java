package com.formquid.elementrequest.entity;

import com.formquid.user.entity.User;
import jakarta.persistence.CheckConstraint;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "element_requests",
    check = @CheckConstraint(constraint = "element_type in ('FIXED_QUESTION', 'BLOCK', 'TEMPLATE')"),
    indexes = {
        @Index(
            name = "idx_element_requests_created_at",
            columnList = "created_at"
        ),
        @Index(
            name = "idx_element_requests_user",
            columnList = "requested_by_user_id"
        )
    }
)
public class ElementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "requested_by_user_id",
        nullable = false
    )
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "element_type",
        nullable = false,
        length = 40
    )
    private RequestedElementType elementType;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "request_payload",
        nullable = false,
        columnDefinition = "jsonb"
    )
    @Builder.Default
    private Map<String, Object> requestPayload =
        new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}