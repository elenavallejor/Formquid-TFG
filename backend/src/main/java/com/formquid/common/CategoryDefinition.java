package com.formquid.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor
public class CategoryDefinition {
    public static final String DEFAULT_ICON = "BsTag";

    @Id
    @Column(length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "personal_data", nullable = false)
    private boolean personalData;

    @Column(nullable = false, length = 64)
    private String icon;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public CategoryDefinition(String code, String name) {
        this(code, name, DEFAULT_ICON);
    }

    public CategoryDefinition(String code, String name, String icon) {
        this.code = code;
        this.name = name;
        this.personalData = false;
        this.icon = icon;
        this.createdAt = Instant.now();
    }

    public void changeIcon(String icon) {
        this.icon = icon;
    }
}
