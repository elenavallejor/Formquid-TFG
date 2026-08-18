package com.formquid.block.entity;

import com.formquid.content.entity.ContentItem;
import com.formquid.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = """
    id ~ '^B[1-9][0-9]*$'
    and scope in ('CATALOG', 'SNAPSHOT')
    and (
        (scope = 'CATALOG' and parent_item_id is null)
        or (scope = 'SNAPSHOT' and parent_item_id is not null)
    )""")
@Table(
    name = "blocks",
    uniqueConstraints = @UniqueConstraint(name = "uk_blocks_parent_item", columnNames = "parent_item_id"),
    indexes = {
        @Index(name = "idx_blocks_scope", columnList = "scope"),
        @Index(name = "idx_blocks_source", columnList = "source_block_id"),
        @Index(name = "idx_blocks_created_by", columnList = "created_by_user_id")
    }
)

/**
 * Entidad que representa un bloque de preguntas.
 *
 * Puede ser un bloque de catálogo reutilizable o una copia insertada
 * en una encuesta o plantilla. Los bloques copiados mantienen referencia
 * al bloque original.
 */
public class Block {

    @Id
    @Column(length = 20)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", unique = true)
    private ContentItem parentItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BlockScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_block_id")
    private Block sourceBlock;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @OneToMany(mappedBy = "parentBlock", cascade = CascadeType.ALL, orphanRemoval = true)
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
        if (scope == null) {
            scope = BlockScope.CATALOG;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
