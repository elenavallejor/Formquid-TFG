-- Conserva los elementos existentes al retirar categorías demasiado específicas.
CREATE TEMP TABLE retired_category_mapping (
    old_code varchar(64) PRIMARY KEY,
    new_code varchar(64) NOT NULL
) ON COMMIT DROP;

INSERT INTO retired_category_mapping (old_code, new_code) VALUES
    ('BOTS_CONVERSACION', 'INTELIGENCIA_ARTIFICIAL'),
    ('MENSAJERIA_INSTANTANEA', 'REDES_SOCIALES'),
    ('VIDEOCONFERENCIAS', 'OTROS'),
    ('RELOJ_INTELIGENTE', 'OTROS'),
    ('CONSOLA_VIDEOJUEGOS', 'VIDEOJUEGOS'),
    ('APLICACIONES_EDUCATIVAS', 'PLATAFORMA_APRENDIZAJE');

UPDATE surveys item SET category = mapping.new_code
FROM retired_category_mapping mapping WHERE item.category = mapping.old_code;
UPDATE templates item SET category = mapping.new_code
FROM retired_category_mapping mapping WHERE item.category = mapping.old_code;
UPDATE blocks item SET category = mapping.new_code
FROM retired_category_mapping mapping WHERE item.category = mapping.old_code;
UPDATE questions item SET category = mapping.new_code
FROM retired_category_mapping mapping WHERE item.category = mapping.old_code;

-- Las sugerencias aún pendientes guardan la categoría dentro de un JSONB.
UPDATE element_requests request
SET request_payload = jsonb_set(request.request_payload, '{category}', to_jsonb(mapping.new_code))
FROM retired_category_mapping mapping
WHERE request.request_payload ->> 'category' = mapping.old_code;

DELETE FROM categories category USING retired_category_mapping mapping
WHERE category.code = mapping.old_code;

UPDATE categories SET name = CASE code
    WHEN 'ROBOTICA_EDUCATIVA' THEN 'Robótica'
    WHEN 'TELEFONO_INTELIGENTE' THEN 'Teléfonos'
    WHEN 'TABLETA' THEN 'Tablets'
    WHEN 'TELEVISION_INTELIGENTE' THEN 'Televisión'
END, version = version + 1
WHERE code IN ('ROBOTICA_EDUCATIVA', 'TELEFONO_INTELIGENTE', 'TABLETA', 'TELEVISION_INTELIGENTE');
