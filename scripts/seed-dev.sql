BEGIN;

DO $guard$
BEGIN
    IF current_setting('formquid.allow_dev_seed', true) IS DISTINCT FROM 'yes' THEN
        RAISE EXCEPTION 'Activa formquid.allow_dev_seed=yes para permitir esta carga destructiva';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '38' AND success) THEN
        RAISE EXCEPTION 'Es necesario aplicar las migraciones hasta V38';
    END IF;
    IF (SELECT count(*) FROM superadmin_guard) <> 1 THEN
        RAISE EXCEPTION 'Falta el superadministrador protegido';
    END IF;
END;
$guard$;

TRUNCATE TABLE
    password_reset_tokens, notifications, matrix_answer_values, answer_options,
    answers, participations, element_requests, matrix_rows, question_options,
    question_settings, questions, blocks, content_items, publication_requests,
    collaboration_requests, survey_collaborators, surveys, templates,
    registration_requests
RESTART IDENTITY;
DELETE FROM users WHERE id <> (SELECT user_id FROM superadmin_guard WHERE singleton);

INSERT INTO users (role, email, name, institution, country, password_hash, created_at, updated_at)
SELECT CASE WHEN n <= 2 THEN 'ADMIN' ELSE 'EDITOR' END,
       CASE WHEN n <= 2 THEN 'admin' || lpad(n::text, 2, '0') || '@formquid.example'
            ELSE 'editor' || lpad((n - 2)::text, 2, '0') || '@formquid.example' END,
       CASE WHEN n <= 2 THEN format('Administrador %s', n)
            ELSE format('Editor %s', n - 2) END,
       'Centro de demostración FormQuid', 'ES',
       '$2a$10$1PODhcZKKDlt/U53ogsOmOR6ygMDNxrXY.PumZG0OIHsHUS5HP7dO',
       now(), now()
FROM generate_series(1, 12) AS series(n);

CREATE TEMP TABLE seed_topics (
    code varchar(64) PRIMARY KEY REFERENCES categories(code),
    label text NOT NULL,
    subject text NOT NULL,
    ordinal integer NOT NULL UNIQUE
) ON COMMIT DROP;
INSERT INTO seed_topics (code, label, subject, ordinal) VALUES
    ('ORDENADOR_SOBREMESA', 'Ordenador de sobremesa', 'el ordenador', 1),
    ('PORTATIL', 'Portátil', 'un portátil', 2),
    ('TABLETA', 'Tablets', 'una tablet', 3),
    ('TELEFONO_INTELIGENTE', 'Teléfonos', 'un teléfono móvil', 4),
    ('GAFAS_INTELIGENTES', 'Gafas inteligentes', 'unas gafas inteligentes', 5),
    ('TELEVISION_INTELIGENTE', 'Televisión', 'la televisión', 6),
    ('PIZARRA_INTERACTIVA', 'Pizarra interactiva', 'la pizarra digital', 7),
    ('PROYECTOR_DIGITAL', 'Proyector digital', 'el proyector de clase', 8),
    ('PLATAFORMA_APRENDIZAJE', 'Plataforma de aprendizaje', 'la web de clase', 9),
    ('LIBROS_TEXTO_DIGITALES', 'Libros digitales', 'los libros digitales', 10),
    ('ROBOTICA_EDUCATIVA', 'Robótica', 'los robots de clase', 11),
    ('IMPRESION_3D', 'Impresión 3D', 'una impresora 3D', 12),
    ('INTELIGENCIA_ARTIFICIAL', 'Inteligencia artificial', 'una herramienta con inteligencia artificial', 13),
    ('IA_GENERATIVA', 'IA generativa', 'una herramienta que crea textos o dibujos', 14),
    ('ASISTENTES_VOZ', 'Asistentes de voz', 'un altavoz que responde a tu voz', 15),
    ('REALIDAD_VIRTUAL', 'Realidad virtual', 'unas gafas de realidad virtual', 16),
    ('REALIDAD_AUMENTADA', 'Realidad aumentada', 'una app que pone dibujos en la cámara', 17),
    ('REDES_SOCIALES', 'Redes sociales', 'las redes sociales', 18),
    ('VIDEOJUEGOS', 'Videojuegos', 'los videojuegos', 19),
    ('OTROS', 'Otras tecnologías', 'otras tecnologías', 20);

DELETE FROM categories
WHERE code <> 'DATOS_PERSONALES'
  AND code NOT IN (SELECT code FROM seed_topics);
DO $categories$
BEGIN
    IF (SELECT count(*) FROM categories) <> 21 THEN
        RAISE EXCEPTION 'El catálogo de categorías iniciales no coincide con el esperado';
    END IF;
END;
$categories$;

CREATE FUNCTION pg_temp.seed_question(
    p_category varchar, p_scope varchar, p_parent bigint, p_creator bigint,
    p_statement text, p_type varchar, p_direction varchar DEFAULT 'NEUTRAL',
    p_weight numeric DEFAULT 1
) RETURNS bigint LANGUAGE plpgsql AS $function$
DECLARE
    question_id bigint;
    option_label text;
    option_position integer;
BEGIN
    INSERT INTO questions (parent_item_id, scope, statement, question_type,
                           evaluation_direction, evaluation_weight, created_by_user_id,
                           category, created_at, updated_at)
    VALUES (p_parent, p_scope, p_statement, p_type, p_direction,
            CASE WHEN p_direction = 'NEUTRAL' THEN NULL
                 WHEN p_direction = 'NEGATIVE' THEN -abs(p_weight)
                 ELSE abs(p_weight) END,
            p_creator, p_category, now(), now())
    RETURNING id INTO question_id;
    INSERT INTO question_settings (
        question_id, max_length, min_value, max_value, step, min_selections,
        max_selections, left_label, right_label, true_label, false_label,
        true_evaluation_factor, false_evaluation_factor, scale_display_type
    ) VALUES (
        question_id,
        CASE WHEN p_type = 'TEXT' THEN 200 ELSE NULL END,
        CASE WHEN p_type = 'NUMBER' THEN 0 WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN 1 END,
        CASE WHEN p_type = 'NUMBER' THEN 7 WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN 5 END,
        CASE WHEN p_type IN ('NUMBER', 'SCALE', 'MATRIX_SCALE') THEN 1 END,
        CASE WHEN p_type = 'MULTIPLE_CHOICE' THEN 1 END,
        CASE WHEN p_type = 'MULTIPLE_CHOICE' THEN 4 END,
        CASE WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN 'Nada' END,
        CASE WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN 'Mucho' END,
        CASE WHEN p_type = 'BOOLEAN' THEN 'Sí' END,
        CASE WHEN p_type = 'BOOLEAN' THEN 'No' END,
        CASE WHEN p_type = 'BOOLEAN' AND p_direction <> 'NEUTRAL' THEN 1 END,
        CASE WHEN p_type = 'BOOLEAN' AND p_direction <> 'NEUTRAL' THEN 0 END,
        CASE WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN 'NUMERIC' END
    );
    IF p_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE') THEN
        FOR option_label, option_position IN
            SELECT CASE WHEN p_type = 'SINGLE_CHOICE'
                        THEN single_label ELSE multiple_label END, position
            FROM (VALUES
                (1, 'Nunca', 'En clase'),
                (2, 'Algunos días', 'En casa'),
                (3, 'Casi todos los días', 'En casa de un amigo o amiga'),
                (4, 'Todos los días', 'En otro lugar')
            ) AS options(position, single_label, multiple_label)
        LOOP
            INSERT INTO question_options (question_id, label, position, is_other, evaluation_factor)
            VALUES (question_id, option_label, option_position, false,
                    CASE WHEN p_direction = 'NEUTRAL' THEN NULL
                         ELSE (option_position - 1)::numeric / 3 END);
        END LOOP;
    END IF;
    IF p_type = 'MATRIX_SCALE' THEN
        INSERT INTO matrix_rows (question_id, label, position) VALUES
            (question_id, 'Es fácil de usar', 1),
            (question_id, 'Me ayuda a aprender', 2),
            (question_id, 'Me gusta usar esta tecnología', 3);
    END IF;
    RETURN question_id;
END;
$function$;

DO $seed_catalog$
DECLARE
    topic record;
    slot integer;
    question_type varchar;
    statement_text text;
    admin_id bigint;
    block_id bigint;
    template_id bigint;
    item_id bigint;
    personal_question_id bigint;
    personal_question record;
BEGIN
    SELECT id INTO STRICT admin_id FROM users WHERE email = 'admin01@formquid.example';
    FOR topic IN SELECT * FROM seed_topics ORDER BY ordinal LOOP
        FOR slot IN 1..7 LOOP
            question_type := (ARRAY[
                'TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE',
                'SCALE', 'MATRIX_SCALE', 'BOOLEAN'
            ])[slot];
            statement_text := CASE slot
                WHEN 1 THEN format('¿Qué haces cuando usas %s?', topic.subject)
                WHEN 2 THEN format('¿Cuántos días a la semana usas %s?', topic.subject)
                WHEN 3 THEN format('¿Cada cuánto usas %s?', topic.subject)
                WHEN 4 THEN format('¿Dónde usas %s? Puedes elegir varias opciones.', topic.subject)
                WHEN 5 THEN format('¿Cuánto te gusta usar %s?', topic.subject)
                WHEN 6 THEN format('¿Cómo te va con %s?', topic.subject)
                ELSE format('¿Has usado %s alguna vez?', topic.subject)
            END;
            PERFORM pg_temp.seed_question(topic.code, 'CATALOG_FIXED', NULL,
                                          admin_id, statement_text, question_type);
        END LOOP;
        IF topic.ordinal <= 6 THEN
            INSERT INTO blocks (scope, name, description, created_by_user_id,
                                category, created_at, updated_at)
            VALUES ('CATALOG', 'Bloque de ' || topic.label,
                    'Preguntas cortas sobre el uso de esta tecnología.', admin_id,
                    topic.code, now(), now()) RETURNING id INTO block_id;
            INSERT INTO content_items (parent_block_id, item_type, position, is_required)
            VALUES (block_id, 'QUESTION', 1, true) RETURNING id INTO item_id;
            PERFORM pg_temp.seed_question(topic.code, 'LOCAL', item_id, admin_id,
                format('¿Usas %s en clase?', topic.subject), 'BOOLEAN');
        END IF;
        IF topic.ordinal <= 4 THEN
            INSERT INTO templates (title, description, created_by_user_id,
                                   category, created_at, updated_at)
            VALUES ('Plantilla de ' || topic.label, 'Preguntas sencillas para niños y niñas.',
                    admin_id, topic.code, now(), now()) RETURNING id INTO template_id;
            INSERT INTO content_items (template_id, item_type, position, is_required)
            VALUES (template_id, 'QUESTION', 1, true) RETURNING id INTO item_id;
            PERFORM pg_temp.seed_question(topic.code, 'FIXED_SNAPSHOT', item_id, admin_id,
                format('¿Cada cuánto usas %s?', topic.subject), 'SINGLE_CHOICE');
            INSERT INTO content_items (template_id, item_type, position, text_body)
            VALUES (template_id, 'TEXT', 2, 'Gracias por contar lo que piensas.');
        END IF;
    END LOOP;

    FOR personal_question IN SELECT * FROM (VALUES
        ('¿Cuántos años tienes?', 'NUMBER'),
        ('¿En qué curso estás?', 'SINGLE_CHOICE'),
        ('¿Dónde usas más la tecnología?', 'SINGLE_CHOICE'),
        ('¿Usas internet en casa?', 'BOOLEAN'),
        ('¿Necesitas ayuda para usar algún aparato?', 'BOOLEAN'),
        ('¿Cuántos días a la semana usas internet?', 'NUMBER'),
        ('¿Con quién usas más la tecnología?', 'SINGLE_CHOICE'),
        ('¿Qué te ayuda a entender una pregunta? Puedes elegir varias opciones.', 'MULTIPLE_CHOICE'),
        ('Solo para una persona adulta: ¿Estás con el niño o la niña cuando usa internet?', 'BOOLEAN'),
        ('Solo para una persona adulta: ¿Cuánto tiempo usa internet el niño o la niña cada día?', 'SINGLE_CHOICE')
    ) AS personal(statement, type) LOOP
        personal_question_id := pg_temp.seed_question('DATOS_PERSONALES', 'CATALOG_FIXED', NULL,
                                                      admin_id, personal_question.statement,
                                                      personal_question.type);
        IF personal_question.statement = '¿Cuántos años tienes?' THEN
            UPDATE question_settings SET min_value = 3, max_value = 17
            WHERE question_id = personal_question_id;
        ELSIF personal_question.statement = '¿En qué curso estás?' THEN
            UPDATE question_options SET label = (ARRAY[
                'Infantil', '1.º o 2.º de primaria', '3.º a 6.º de primaria', 'Otro curso'
            ])[position] WHERE question_id = personal_question_id;
        ELSIF personal_question.statement = '¿Dónde usas más la tecnología?' THEN
            UPDATE question_options SET label = (ARRAY[
                'En casa', 'En clase', 'En los dos sitios', 'En otro lugar'
            ])[position] WHERE question_id = personal_question_id;
        ELSIF personal_question.statement = '¿Con quién usas más la tecnología?' THEN
            UPDATE question_options SET label = (ARRAY[
                'Solo o sola', 'Con mi familia', 'Con mis profes', 'Con mis amigos'
            ])[position] WHERE question_id = personal_question_id;
        ELSIF personal_question.statement = '¿Qué te ayuda a entender una pregunta? Puedes elegir varias opciones.' THEN
            UPDATE question_options SET label = (ARRAY[
                'Leerla', 'Escucharla', 'Ver dibujos', 'Pedir ayuda'
            ])[position] WHERE question_id = personal_question_id;
        ELSIF personal_question.statement = 'Solo para una persona adulta: ¿Cuánto tiempo usa internet el niño o la niña cada día?' THEN
            UPDATE question_options SET label = (ARRAY[
                'Menos de 1 hora', 'Entre 1 y 2 horas', 'Más de 2 horas', 'No lo sé'
            ])[position] WHERE question_id = personal_question_id;
        END IF;
    END LOOP;
END;
$seed_catalog$;

DO $seed_surveys$
DECLARE
    topic record;
    survey_id bigint;
    owner_id bigint;
    collaborator_id bigint;
    item_id bigint;
    block_id bigint;
    block_copy_id bigint;
    lifecycle varchar;
    access_mode varchar;
    extra_number integer;
BEGIN
    FOR topic IN SELECT * FROM seed_topics ORDER BY ordinal LOOP
        SELECT id INTO STRICT owner_id FROM users
        WHERE email = 'editor' || lpad((((topic.ordinal - 1) % 10) + 1)::text, 2, '0') || '@formquid.example';
        lifecycle := CASE WHEN topic.ordinal <= 8 THEN 'DRAFT'
                          WHEN topic.ordinal <= 16 THEN 'OPEN' ELSE 'CLOSED' END;
        access_mode := CASE topic.ordinal % 3
            WHEN 0 THEN 'PUBLIC' WHEN 1 THEN 'PASSWORD_PROTECTED'
            ELSE 'AUTHENTICATED' END;
        INSERT INTO surveys (
            public_code, owner_id, title, description, lifecycle_status,
            access_mode, visibility, access_password_hash, font_family,
            category, created_at, updated_at
        ) VALUES (
            CASE WHEN lifecycle = 'DRAFT' THEN NULL
                 ELSE upper(substr(md5('category:' || topic.code), 1, 12)) END,
            owner_id, 'Encuesta sobre ' || topic.label,
            'Cuéntanos cómo usas esta tecnología. No hay respuestas buenas ni malas.',
            lifecycle, access_mode,
            CASE WHEN lifecycle = 'DRAFT' OR topic.ordinal % 4 = 0
                 THEN 'UNLISTED' ELSE 'LISTED' END,
            CASE WHEN access_mode = 'PASSWORD_PROTECTED'
                 THEN '$2a$10$1PODhcZKKDlt/U53ogsOmOR6ygMDNxrXY.PumZG0OIHsHUS5HP7dO'
                 ELSE NULL END,
            'LEXEND', topic.code, now(), now()
        ) RETURNING id INTO survey_id;

        INSERT INTO content_items (survey_id, item_type, position, is_required)
        VALUES (survey_id, 'QUESTION', 1, true) RETURNING id INTO item_id;
        PERFORM pg_temp.seed_question(topic.code, 'FIXED_SNAPSHOT', item_id, owner_id,
            format('¿Cada cuánto usas %s?', topic.subject), 'SINGLE_CHOICE');
        INSERT INTO content_items (survey_id, item_type, position, is_required)
        VALUES (survey_id, 'QUESTION', 2, false) RETURNING id INTO item_id;
        PERFORM pg_temp.seed_question(topic.code, 'LOCAL', item_id, owner_id,
            CASE WHEN topic.ordinal % 2 = 0
                 THEN format('¿Cuánto te gusta usar %s?', topic.subject)
                 ELSE format('¿Cuánto te cuesta usar %s?', topic.subject) END,
            'SCALE', CASE WHEN topic.ordinal % 2 = 0 THEN 'POSITIVE' ELSE 'NEGATIVE' END,
            1 + ((topic.ordinal - 1) % 5));
        INSERT INTO content_items (survey_id, item_type, position, is_required)
        VALUES (survey_id, 'QUESTION', 3, false) RETURNING id INTO item_id;
        PERFORM pg_temp.seed_question(topic.code, 'LOCAL', item_id, owner_id,
            format('¿Qué cambiarías de %s?', topic.subject), 'TEXT');

        IF topic.ordinal <= 6 THEN
            SELECT id INTO STRICT block_id FROM blocks
            WHERE scope = 'CATALOG' AND category = topic.code;
            INSERT INTO content_items (survey_id, item_type, position)
            VALUES (survey_id, 'BLOCK', 4) RETURNING id INTO item_id;
            INSERT INTO blocks (parent_item_id, scope, name, description,
                                created_by_user_id, category, created_at, updated_at)
            VALUES (item_id, 'SNAPSHOT', 'Bloque de ' || topic.label,
                    'Preguntas cortas sobre el uso de esta tecnología.',
                    owner_id, topic.code, now(), now()) RETURNING id INTO block_copy_id;
            INSERT INTO content_items (parent_block_id, item_type, position, is_required)
            VALUES (block_copy_id, 'QUESTION', 1, true) RETURNING id INTO item_id;
            PERFORM pg_temp.seed_question(topic.code, 'LOCAL', item_id, owner_id,
                format('¿Usas %s en clase?', topic.subject), 'BOOLEAN');
        END IF;

        IF topic.ordinal <= 4 THEN
            INSERT INTO content_items (survey_id, item_type, position, text_body)
            VALUES (survey_id, 'TEXT', 5, 'Gracias por contar lo que piensas.');
        END IF;

        IF topic.ordinal IN (1, 2) THEN
            SELECT id INTO STRICT collaborator_id FROM users
            WHERE email = 'editor' || lpad((topic.ordinal + 2)::text, 2, '0') || '@formquid.example';
            INSERT INTO survey_collaborators (survey_id, user_id, permission, joined_at)
            VALUES (survey_id, collaborator_id, 'EDIT', now());
            INSERT INTO publication_requests (survey_id, requested_by_user_id, message, created_at)
            VALUES (survey_id, collaborator_id,
                    'Solicito publicar esta encuesta de demostración.', now());
        END IF;
    END LOOP;

    FOR extra_number IN 1..10 LOOP
        SELECT * INTO STRICT topic FROM seed_topics WHERE ordinal = extra_number;
        SELECT id INTO STRICT owner_id FROM users
        WHERE email = 'editor' || lpad(extra_number::text, 2, '0') || '@formquid.example';
        INSERT INTO surveys (
            public_code, owner_id, title, description, lifecycle_status,
            access_mode, visibility, font_family, category, created_at, updated_at
        ) VALUES (
            upper(substr(md5('short:' || extra_number::text), 1, 12)),
            owner_id, format('Encuesta rápida %s: %s', extra_number, topic.label),
            'Dos preguntas rápidas sobre esta tecnología.', 'OPEN', 'PUBLIC',
            'LISTED', 'ARIAL', topic.code, now(), now()
        ) RETURNING id INTO survey_id;
        INSERT INTO content_items (survey_id, item_type, position, is_required)
        VALUES (survey_id, 'QUESTION', 1, true) RETURNING id INTO item_id;
        PERFORM pg_temp.seed_question(topic.code, 'FIXED_SNAPSHOT', item_id, owner_id,
            format('¿Has usado %s alguna vez?', topic.subject), 'BOOLEAN');
        INSERT INTO content_items (survey_id, item_type, position, is_required)
        VALUES (survey_id, 'QUESTION', 2, false) RETURNING id INTO item_id;
        PERFORM pg_temp.seed_question(topic.code, 'LOCAL', item_id, owner_id,
            format('¿Qué te gusta de %s?', topic.subject), 'TEXT');
    END LOOP;
END;
$seed_surveys$;

INSERT INTO registration_requests (
    email, name, institution, country, motivation, password_hash, created_at
) VALUES
    ('solicitud01@formquid.example', 'Ana Prueba', 'Centro de demostración', 'ES',
     'Quiero preparar encuestas educativas.',
     '$2a$10$1PODhcZKKDlt/U53ogsOmOR6ygMDNxrXY.PumZG0OIHsHUS5HP7dO', now()),
    ('solicitud02@formquid.example', 'Luis Prueba', 'Centro de demostración', 'ES',
     'Quiero colaborar en el catálogo.',
     '$2a$10$1PODhcZKKDlt/U53ogsOmOR6ygMDNxrXY.PumZG0OIHsHUS5HP7dO', now());

INSERT INTO element_requests (requested_by_user_id, element_type, message, request_payload, created_at)
SELECT id, 'FIXED_QUESTION', 'Propongo una pregunta de demostración.',
       jsonb_build_object(
           'category', 'PORTATIL',
           'question', jsonb_build_object(
               'statement', '¿Usas un portátil en clase?',
               'questionType', 'BOOLEAN', 'evaluationDirection', 'NEUTRAL',
               'settings', jsonb_build_object('trueLabel', 'Sí', 'falseLabel', 'No')
           )
       ), now()
FROM users WHERE email = 'editor01@formquid.example';

INSERT INTO element_requests (requested_by_user_id, element_type, message, request_payload, created_at)
SELECT id, 'BLOCK', 'Propongo un bloque de demostración.',
       jsonb_build_object(
           'category', 'TABLETA', 'name', 'Uso de tablets en el aula',
           'description', 'Bloque sugerido para el catálogo',
           'items', jsonb_build_array(jsonb_build_object(
               'itemType', 'QUESTION', 'position', 1, 'required', true,
               'question', jsonb_build_object(
                   'statement', '¿Usas una tablet en clase?',
                   'questionType', 'BOOLEAN', 'evaluationDirection', 'NEUTRAL',
                   'settings', jsonb_build_object('trueLabel', 'Sí', 'falseLabel', 'No')
               )
           ))
       ), now()
FROM users WHERE email = 'editor02@formquid.example';

INSERT INTO element_requests (requested_by_user_id, element_type, message, request_payload, created_at)
SELECT id, 'TEMPLATE', 'Propongo una plantilla de demostración.',
       jsonb_build_object(
           'category', 'ROBOTICA_EDUCATIVA', 'title', 'Primeros pasos en robótica',
           'description', 'Plantilla sugerida para el catálogo',
           'items', jsonb_build_array(jsonb_build_object(
               'itemType', 'QUESTION', 'position', 1, 'required', true,
               'question', jsonb_build_object(
                   'statement', '¿Qué haces con los robots de clase?',
                   'questionType', 'TEXT', 'evaluationDirection', 'NEUTRAL',
                   'settings', jsonb_build_object('maxLength', 200)
               )
           ))
       ), now()
FROM users WHERE email = 'editor03@formquid.example';

INSERT INTO collaboration_requests (survey_id, user_id, request_type, permission, message, created_at)
SELECT survey.id, candidate.id, cases.request_type, cases.permission,
       'Solicitud de colaboración de demostración', now()
FROM (VALUES
    (3, 5, 'INVITATION', 'EDIT'),
    (4, 6, 'INVITATION', 'VIEW'),
    (5, 7, 'ACCESS_REQUEST', 'EDIT'),
    (6, 8, 'ACCESS_REQUEST', 'VIEW')
) AS cases(survey_ordinal, editor_number, request_type, permission)
JOIN seed_topics topic ON topic.ordinal = cases.survey_ordinal
JOIN surveys survey ON survey.category = topic.code
                    AND survey.title = 'Encuesta sobre ' || topic.label
JOIN users candidate ON candidate.email =
    'editor' || lpad(cases.editor_number::text, 2, '0') || '@formquid.example';

DO $verify$
BEGIN
    IF (SELECT count(*) FROM users WHERE role = 'SUPERADMIN') <> 1
       OR (SELECT count(*) FROM users WHERE role = 'ADMIN') <> 2
       OR (SELECT count(*) FROM users WHERE role = 'EDITOR') <> 10
       OR (SELECT count(*) FROM questions WHERE scope = 'CATALOG_FIXED') <> 150
       OR EXISTS (
           SELECT 1 FROM categories category
           LEFT JOIN questions question ON question.category = category.code
                                       AND question.scope = 'CATALOG_FIXED'
           GROUP BY category.code
           HAVING count(question.id) <> CASE WHEN category.code = 'DATOS_PERSONALES' THEN 10 ELSE 7 END
       )
       OR (SELECT count(*) FROM surveys) <> 30
       OR (SELECT count(*) FROM surveys WHERE lifecycle_status = 'OPEN') <> 18
       OR (SELECT count(*) FROM surveys WHERE lifecycle_status = 'DRAFT') <> 8
       OR (SELECT count(*) FROM surveys WHERE lifecycle_status = 'CLOSED') <> 4
       OR (SELECT count(*) FROM surveys WHERE title LIKE 'Encuesta sobre %') <> 20
       OR (SELECT count(*) FROM surveys WHERE title LIKE 'Encuesta rápida %'
           AND lifecycle_status = 'OPEN') <> 10
       OR EXISTS (
           SELECT 1 FROM surveys survey
           WHERE NOT EXISTS (
               SELECT 1 FROM content_items item
               WHERE item.survey_id = survey.id AND item.item_type = 'QUESTION'
           )
       )
       OR EXISTS (
           SELECT 1 FROM questions question
           JOIN content_items item ON item.id = question.parent_item_id
           JOIN surveys survey ON survey.id = item.survey_id
           WHERE question.category <> survey.category
       )
       OR (SELECT count(*) FROM blocks WHERE scope = 'CATALOG') <> 6
       OR (SELECT count(*) FROM templates) <> 4
       OR (SELECT count(*) FROM registration_requests) <> 2
       OR (SELECT count(*) FROM element_requests) <> 3
       OR (SELECT count(*) FROM collaboration_requests) <> 4
       OR (SELECT count(*) FROM publication_requests) <> 2 THEN
        RAISE EXCEPTION 'La carga no produjo las cantidades esperadas; se deshace la transacción';
    END IF;
END;
$verify$;
COMMIT;
