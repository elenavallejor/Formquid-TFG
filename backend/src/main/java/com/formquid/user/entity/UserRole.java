package com.formquid.user.entity;

/**
 * Enum que define los roles disponibles para los usuarios registrados de FormQuid.
 *
 * SUPERADMIN: usuario con permisos globales sobre la plataforma.
 * ADMIN: usuario encargado de gestionar elementos reutilizables, solicitudes y encuestas.
 * EDITOR: usuario investigador que crea, edita y analiza encuestas.
 */
public enum UserRole {
    SUPERADMIN,
    ADMIN,
    EDITOR
}
