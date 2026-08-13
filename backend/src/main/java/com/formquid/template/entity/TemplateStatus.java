package com.formquid.template.entity;

/**
 * Estado de una plantilla reutilizable.
 *
 * Permite controlar su disponibilidad sin eliminarla físicamente
 * de la base de datos.
 */
public enum TemplateStatus {
    ACTIVE,
    DISABLED,
    ARCHIVED
}