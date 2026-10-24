-- Mantiene las encuestas y el catálogo al reunir los ordenadores y retirar
-- la categoría específica de IA generativa.
INSERT INTO categories (code, name, icon)
VALUES ('ORDENADORES', 'Ordenadores', 'BsPcDisplay')
ON CONFLICT (code) DO NOTHING;

CREATE TEMP TABLE retired_category_mapping (
    old_code varchar(64) PRIMARY KEY,
    new_code varchar(64) NOT NULL
) ON COMMIT DROP;

INSERT INTO retired_category_mapping (old_code, new_code) VALUES
    ('ORDENADOR_SOBREMESA', 'ORDENADORES'),
    ('PORTATIL', 'ORDENADORES'),
    ('IA_GENERATIVA', 'INTELIGENCIA_ARTIFICIAL');

UPDATE surveys item SET category = mapping.new_code
FROM retired_category_mapping mapping WHERE item.category = mapping.old_code;
UPDATE templates item SET category = mapping.new_code
FROM retired_category_mapping mapping WHERE item.category = mapping.old_code;
UPDATE blocks item SET category = mapping.new_code
FROM retired_category_mapping mapping WHERE item.category = mapping.old_code;
UPDATE questions item SET category = mapping.new_code
FROM retired_category_mapping mapping WHERE item.category = mapping.old_code;

UPDATE element_requests request
SET request_payload = jsonb_set(request.request_payload, '{category}', to_jsonb(mapping.new_code))
FROM retired_category_mapping mapping
WHERE request.request_payload ->> 'category' = mapping.old_code;

DELETE FROM categories category USING retired_category_mapping mapping
WHERE category.code = mapping.old_code;

-- Los iconos pueden proceder de los paquetes de React Icons usados por la UI.
ALTER TABLE categories DROP CONSTRAINT ck_categories_icon;
ALTER TABLE categories ADD CONSTRAINT ck_categories_icon
    CHECK (icon ~ '^(Bs|Fa|Ci|Io|Tb|Lia|Pi)[A-Za-z0-9]{2,62}$');

UPDATE categories category
SET name = labels.name,
    icon = labels.icon,
    version = category.version + 1
FROM (VALUES
    ('TABLETA', 'Tabletas', 'FaTabletAlt'),
    ('TELEFONO_INTELIGENTE', 'Teléfonos móviles', 'CiMobile3'),
    ('TELEVISION_INTELIGENTE', 'Televisión', 'IoIosTv'),
    ('IMPRESION_3D', 'Impresoras 3D', 'BsBadge3dFill'),
    ('REALIDAD_VIRTUAL', 'Experiencias de realidad virtual', 'PiVirtualReality'),
    ('PLATAFORMA_APRENDIZAJE', 'Plataformas de aprendizaje', 'TbSchool'),
    ('ROBOTICA_EDUCATIVA', 'Robots educativos', 'LiaRobotSolid'),
    ('PROYECTOR_DIGITAL', 'Proyectores digitales', 'TbDeviceProjector')
) AS labels(code, name, icon)
WHERE category.code = labels.code
  AND (category.name IS DISTINCT FROM labels.name OR category.icon IS DISTINCT FROM labels.icon);
