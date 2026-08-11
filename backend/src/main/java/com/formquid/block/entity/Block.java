package com.formquid.block.entity;

import com.formquid.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

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
            name = "uk_blocks_name_normalized",
            columnNames = "name_normalized"
        )
    },
    indexes = {
        @Index(name = "idx_blocks_status", columnList = "status"),
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
     * Nombre normalizado para búsquedas y control de duplicados.
     *
     * Se almacena en minúsculas y sin espacios laterales para evitar
     * duplicados como "Datos generales" y "datos generales".
     */
    @Column(name = "name_normalized", nullable = false, length = 120)
    private String nameNormalized;

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