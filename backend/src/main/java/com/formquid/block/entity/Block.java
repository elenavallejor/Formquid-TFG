package com.formquid.block.entity;

import com.formquid.common.Category;
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
    scope in ('CATALOG', 'SNAPSHOT')
    and (
        (scope = 'CATALOG' and parent_item_id is null)
        or (scope = 'SNAPSHOT' and parent_item_id is not null)
    )""")
@Table(
    name = "blocks",
    uniqueConstraints = @UniqueConstraint(name = "uk_blocks_parent_item", columnNames = "parent_item_id"),
    indexes = {
        @Index(name = "idx_blocks_scope", columnList = "scope"),
        @Index(name = "idx_blocks_created_by", columnList = "created_by_user_id")
    }
)

/**
 * Representa un bloque de catálogo o una copia incluida en otro contenido.
 */
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", unique = true)
    private ContentItem parentItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BlockScope scope;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 64)
    @Builder.Default
    private Category category = Category.OTROS;

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
        if (category == null) category = Category.OTROS;
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
