-- Crea el superadministrador que necesitan las migraciones durante las pruebas.

INSERT INTO users (
    role,
    email,
    name,
    gender,
    institution,
    password_hash,
    created_at,
    updated_at
) VALUES (
    'SUPERADMIN',
    'test-superadmin@formquid.local',
    'Superadministrador de pruebas',
    'OTHER',
    'FormQuid',
    'not-used-in-tests',
    now(),
    now()
);
