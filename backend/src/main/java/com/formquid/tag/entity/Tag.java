package com.formquid.tag.entity;

import com.formquid.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa una etiqueta libre utilizada para clasificar encuestas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "tags",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tags_name_normalized",
            columnNames = "name_normalized"
        )
    },
    indexes = {
        @Index(name = "idx_tags_created_by", columnList = "created_by_user_id")
    }
)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre visible de la etiqueta.
     */
    @Column(nullable = false, length = 60)
    private String name;

    /**
     * Nombre normalizado para evitar duplicados por mayúsculas,
     * minúsculas o tildes.
     */
    @Column(name = "name_normalized", nullable = false, length = 60)
    private String nameNormalized;

    /**
     * Usuario que utilizó por primera vez esta etiqueta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}