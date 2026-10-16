package com.formquid.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor
public class CategoryDefinition {
    @Id
    @Column(length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "personal_data", nullable = false)
    private boolean personalData;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public CategoryDefinition(String code, String name) {
        this.code = code;
        this.name = name;
        this.personalData = false;
        this.createdAt = Instant.now();
    }
}
