package com.formquid.registration.entity;

import com.formquid.common.service.EmailNormalizer;
import com.formquid.user.entity.UserGender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Check(constraints = "gender in ('MALE', 'FEMALE', 'OTHER') and status in ('PENDING', 'APPROVED', 'REJECTED') and email = lower(btrim(email))")
@Table(
    name = "registration_requests",
    indexes = @Index(name = "idx_registration_requests_email", columnList = "email")
)
public class RegistrationRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserGender gender;

    @Column(nullable = false, length = 200)
    private String institution;

    @Column(nullable = false, length = 1000)
    private String motivation;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        email = EmailNormalizer.normalize(email);
    }
}