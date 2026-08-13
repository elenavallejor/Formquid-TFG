package com.formquid.block.entity;

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

import java.time.LocalDateTime;

/**
 * Representa un bloque reutilizable de FormQuid.
 *
 * Un bloque permite agrupar preguntas bajo una misma sección lógica,
 * por ejemplo: datos generales, contexto de uso o accesibilidad.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "blocks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_blocks_name",
            columnNames = "name"
        )
    },
    indexes = {
        @Index(name = "idx_blocks_status", columnList = "status"),
        @Index(name = "idx_blocks_created_by", columnList = "created_by_user_id"),
        @Index(name = "idx_blocks_status_name", columnList = "status, name")
    }
)
public class Block {

    /**
     * Identificador único del bloque.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre visible del bloque.
     */
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * Descripción opcional del bloque.
     */
    @Column(length = 500)
    private String description;

    /**
     * Estado actual del bloque.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BlockStatus status;

    /**
     * Usuario que creó el bloque.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    /**
     * Fecha de creación.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha de última modificación.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Inicializa estado y fechas antes de guardar.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = BlockStatus.ACTIVE;
        }
    }

    /**
     * Actualiza la fecha de modificación antes de guardar cambios.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}