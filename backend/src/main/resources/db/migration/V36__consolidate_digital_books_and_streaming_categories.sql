CREATE TEMP TABLE retired_category_mapping (
    old_code varchar(64) PRIMARY KEY,
    new_code varchar(64) NOT NULL
) ON COMMIT DROP;

INSERT INTO retired_category_mapping (old_code, new_code) VALUES
    ('LECTOR_LIBROS_ELECTRONICOS', 'LIBROS_TEXTO_DIGITALES'),
    ('PLATAFORMAS_TRANSMISION', 'OTROS');

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

UPDATE categories SET name = 'Libros digitales', version = version + 1
WHERE code = 'LIBROS_TEXTO_DIGITALES' AND name <> 'Libros digitales';
