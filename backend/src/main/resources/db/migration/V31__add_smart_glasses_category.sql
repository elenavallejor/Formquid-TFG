-- Añade la categoría Gafas inteligentes con su icono.

INSERT INTO categories (code, name, icon)
VALUES ('GAFAS_INTELIGENTES', 'Gafas inteligentes', 'BsEyeglasses')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    icon = EXCLUDED.icon,
    version = categories.version + 1;
