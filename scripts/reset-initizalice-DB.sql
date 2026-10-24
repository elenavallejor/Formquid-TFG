BEGIN;

DO $guard$
BEGIN
    IF current_setting('formquid.allow_dev_seed', true) IS DISTINCT FROM 'yes' THEN
        RAISE EXCEPTION 'Activa formquid.allow_dev_seed=yes para permitir esta carga destructiva';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '40' AND success) THEN
        RAISE EXCEPTION 'Es necesario aplicar las migraciones hasta V40';
    END IF;
    IF (SELECT count(*) FROM superadmin_guard) <> 1 THEN
        RAISE EXCEPTION 'Falta el superadministrador protegido';
    END IF;
END;
$guard$;

CREATE TEMP TABLE preserved_superadmin ON COMMIT DROP AS
SELECT id, role, email, name, institution, country, password_hash, created_at, updated_at
FROM users
WHERE id = (SELECT user_id FROM superadmin_guard WHERE singleton);

TRUNCATE TABLE
    password_reset_tokens, notifications, matrix_answer_values, answer_options,
    answers, participations, element_requests, matrix_rows, question_options,
    question_settings, questions, blocks, content_items, publication_requests,
    collaboration_requests, survey_collaborators, surveys, templates,
    registration_requests
RESTART IDENTITY;
DELETE FROM users WHERE id <> (SELECT user_id FROM superadmin_guard WHERE singleton);

DO $reset_user_ids$
DECLARE
    original record;
BEGIN
    SELECT * INTO STRICT original FROM preserved_superadmin;
    IF original.id <> 1 THEN
        INSERT INTO users (
            id, role, email, name, institution, country, password_hash, created_at, updated_at
        ) VALUES (
            1, original.role, format('reinicio-%s@formquid.invalid', original.id),
            original.name, original.institution, original.country, original.password_hash,
            original.created_at, original.updated_at
        );
        UPDATE superadmin_guard SET user_id = 1 WHERE singleton;
        DELETE FROM users WHERE id = original.id;
        UPDATE users SET email = original.email WHERE id = 1;
    END IF;
END;
$reset_user_ids$;
ALTER TABLE users ALTER COLUMN id RESTART WITH 2;

CREATE TEMP TABLE seed_users (
    role varchar(30) NOT NULL,
    ordinal integer NOT NULL,
    email varchar(255) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    institution varchar(200) NOT NULL,
    user_id bigint,
    PRIMARY KEY (role, ordinal)
) ON COMMIT DROP;
INSERT INTO seed_users (role, ordinal, email, name, institution) VALUES
    ('ADMIN', 1, 'lauraguirao@gmail.com', 'Laura Guirao Torrente', 'Universidad de Granada'),
    ('ADMIN', 2, 'anabelmena@gmail.com', 'Anabel Mena Meseguer', 'Universidad de Málaga'),
    ('EDITOR', 1, 'pablogarcia@gmail.com', 'Pablo García Roldán', 'Universidad de Sevilla'),
    ('EDITOR', 2, 'davidserrano@gmail.com', 'David Serrano Vega', 'Universidad de Córdoba'),
    ('EDITOR', 3, 'mariotorres@gmail.com', 'Mario Torres Gil', 'Universidad de Cádiz'),
    ('EDITOR', 4, 'cristinapaquet@gmail.com', 'Cristina Paquet Vidal', 'Universidad de Valencia'),
    ('EDITOR', 5, 'tonimartin@gmail.com', 'Toñi Martín Robles', 'Universidad de Alicante'),
    ('EDITOR', 6, 'manoloperez@gmail.com', 'Manolo Pérez Cruz', 'Universidad de Murcia'),
    ('EDITOR', 7, 'joseluisnavarro@gmail.com', 'José Luis Navarro Díaz', 'Universidad de Zaragoza'),
    ('EDITOR', 8, 'joseromero@gmail.com', 'José Romero Ortiz', 'Universidad de Salamanca'),
    ('EDITOR', 9, 'gerardoruiz@gmail.com', 'Gerardo Ruiz Molina', 'Universidad de Valladolid'),
    ('EDITOR', 10, 'sarapina@gmail.com', 'Sara Piña Munera', 'Universidad de Barcelona');

INSERT INTO users (role, email, name, institution, country, password_hash, created_at, updated_at)
SELECT role, email, name, institution, 'ES',
       '$2a$10$1PODhcZKKDlt/U53ogsOmOR6ygMDNxrXY.PumZG0OIHsHUS5HP7dO',
       now(), now()
FROM seed_users;
UPDATE seed_users seeded SET user_id = user_account.id
FROM users user_account WHERE user_account.email = seeded.email;
DO $seed_users_check$
BEGIN
    IF (SELECT count(*) FROM seed_users) <> 12
       OR EXISTS (SELECT 1 FROM seed_users WHERE user_id IS NULL)
       OR EXISTS (
           SELECT 1 FROM seed_users seeded
           JOIN users account ON account.id = seeded.user_id
           WHERE account.institution IS DISTINCT FROM seeded.institution
       ) THEN
        RAISE EXCEPTION 'No se pudieron crear todos los usuarios de demostración';
    END IF;
END;
$seed_users_check$;

CREATE TEMP TABLE seed_topics (
    code varchar(64) PRIMARY KEY,
    label text NOT NULL,
    subject text NOT NULL,
    ordinal integer NOT NULL UNIQUE
) ON COMMIT DROP;
INSERT INTO seed_topics (code, label, subject, ordinal) VALUES
    ('ORDENADORES', 'Ordenadores', 'el ordenador', 1),
    ('TABLETA', 'Tabletas', 'una tableta', 2),
    ('TELEFONO_INTELIGENTE', 'Teléfonos móviles', 'un teléfono móvil', 3),
    ('GAFAS_INTELIGENTES', 'Gafas inteligentes', 'unas gafas inteligentes', 4),
    ('TELEVISION_INTELIGENTE', 'Televisión', 'la televisión', 5),
    ('PIZARRA_INTERACTIVA', 'Pizarras interactivas', 'la pizarra digital', 6),
    ('PROYECTOR_DIGITAL', 'Proyectores digitales', 'el proyector de clase', 7),
    ('PLATAFORMA_APRENDIZAJE', 'Plataformas de aprendizaje', 'la web de clase', 8),
    ('LIBROS_TEXTO_DIGITALES', 'Libros digitales', 'los libros digitales', 9),
    ('ROBOTICA_EDUCATIVA', 'Robots educativos', 'los robots de clase', 10),
    ('IMPRESION_3D', 'Impresoras 3D', 'una impresora 3D', 11),
    ('INTELIGENCIA_ARTIFICIAL', 'Herramientas de inteligencia artificial', 'una herramienta con inteligencia artificial', 12),
    ('ASISTENTES_VOZ', 'Asistentes de voz', 'un altavoz que responde a tu voz', 13),
    ('REALIDAD_VIRTUAL', 'Experiencias de realidad virtual', 'unas gafas de realidad virtual', 14),
    ('REALIDAD_AUMENTADA', 'Aplicaciones de realidad aumentada', 'una app que pone dibujos en la cámara', 15),
    ('REDES_SOCIALES', 'Redes sociales', 'las redes sociales', 16),
    ('VIDEOJUEGOS', 'Videojuegos', 'los videojuegos', 17),
    ('OTROS', 'Otras tecnologías', 'otras tecnologías', 18);

DO $seed_categories$
BEGIN
    IF EXISTS (
        SELECT 1 FROM seed_topics topic
        LEFT JOIN categories category ON category.code = topic.code
        WHERE category.code IS NULL
    ) THEN
        RAISE EXCEPTION 'Faltan categorías requeridas por la carga de desarrollo';
    END IF;
END;
$seed_categories$;

UPDATE categories category
SET name = topic.label, version = category.version + 1
FROM seed_topics topic
WHERE category.code = topic.code
  AND category.name IS DISTINCT FROM topic.label;

UPDATE categories
SET name = 'Datos personales', version = version + 1
WHERE code = 'DATOS_PERSONALES' AND name IS DISTINCT FROM 'Datos personales';

DELETE FROM categories
WHERE code <> 'DATOS_PERSONALES'
  AND code NOT IN (SELECT code FROM seed_topics);
DO $categories$
BEGIN
    IF (SELECT count(*) FROM categories) <> 19 THEN
        RAISE EXCEPTION 'El catálogo de categorías iniciales no coincide con el esperado';
    END IF;
END;
$categories$;

CREATE OR REPLACE FUNCTION pg_temp.seed_question(
    p_category varchar, p_scope varchar, p_parent bigint, p_creator bigint,
    p_statement text, p_type varchar, p_direction varchar DEFAULT 'NEUTRAL',
    p_weight numeric DEFAULT 1, p_options text[] DEFAULT NULL,
    p_max_value numeric DEFAULT NULL, p_left_label text DEFAULT NULL,
    p_right_label text DEFAULT NULL
) RETURNS bigint LANGUAGE plpgsql AS $function$
DECLARE
    question_id bigint;
    option_label text;
    option_position integer;
    choice_options text[];
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
        CASE WHEN p_type = 'TEXT' THEN 500 ELSE NULL END,
        CASE WHEN p_type = 'NUMBER' THEN 0 WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN 1 END,
        CASE WHEN p_type = 'NUMBER' THEN coalesce(p_max_value, 7)
             WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN coalesce(p_max_value, 5) END,
        CASE WHEN p_type IN ('NUMBER', 'SCALE', 'MATRIX_SCALE') THEN 1 END,
        CASE WHEN p_type = 'MULTIPLE_CHOICE' THEN 1 END,
        CASE WHEN p_type = 'MULTIPLE_CHOICE' THEN coalesce(array_length(p_options, 1), 4) END,
        CASE WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN coalesce(p_left_label, 'Nada') END,
        CASE WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN coalesce(p_right_label, 'Mucho') END,
        CASE WHEN p_type = 'BOOLEAN' THEN 'Sí' END,
        CASE WHEN p_type = 'BOOLEAN' THEN 'No' END,
        CASE WHEN p_type = 'BOOLEAN' AND p_direction <> 'NEUTRAL' THEN 1 END,
        CASE WHEN p_type = 'BOOLEAN' AND p_direction <> 'NEUTRAL' THEN 0 END,
        CASE WHEN p_type IN ('SCALE', 'MATRIX_SCALE') THEN 'NUMERIC' END
    );
    IF p_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE') THEN
        choice_options := coalesce(p_options,
            CASE WHEN p_type = 'SINGLE_CHOICE'
                 THEN ARRAY['Nunca', 'Alguna vez', 'Varias veces al mes',
                            'Varias veces a la semana', 'A diario']
                 ELSE ARRAY['En casa', 'En el centro educativo',
                            'En actividades fuera del centro', 'En otro lugar'] END);
        FOR option_label, option_position IN
            SELECT label, position::integer
            FROM unnest(choice_options) WITH ORDINALITY AS options(label, position)
        LOOP
            INSERT INTO question_options (question_id, label, position, is_other, evaluation_factor)
            VALUES (question_id, option_label, option_position, false,
                    CASE WHEN p_direction = 'NEUTRAL' THEN NULL
                         ELSE (option_position - 1)::numeric /
                              greatest(array_length(choice_options, 1) - 1, 1) END);
        END LOOP;
    END IF;
    IF p_type = 'MATRIX_SCALE' THEN
        INSERT INTO matrix_rows (question_id, label, position) VALUES
            (question_id, 'Facilidad de uso', 1),
            (question_id, 'Utilidad educativa', 2),
            (question_id, 'Seguridad percibida', 3);
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
    SELECT user_id INTO STRICT admin_id FROM seed_users
    WHERE role = 'ADMIN' AND ordinal = 1;
    FOR topic IN SELECT * FROM seed_topics ORDER BY ordinal LOOP
        FOR slot IN 1..7 LOOP
            question_type := (ARRAY[
                'TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE',
                'SCALE', 'MATRIX_SCALE', 'BOOLEAN'
            ])[slot];
            statement_text := CASE slot
                WHEN 1 THEN format('¿Qué experiencia has tenido al usar %s con menores?', topic.subject)
                WHEN 2 THEN format('¿Cuántos días de la última semana has usado %s junto a un menor?', topic.subject)
                WHEN 3 THEN format('¿Con qué frecuencia has visto a un menor usar %s?', topic.subject)
                WHEN 4 THEN format('¿En qué entornos has visto a menores usar %s? Puedes elegir varios.', topic.subject)
                WHEN 5 THEN format('¿Cuánto crees que %s puede ayudar al aprendizaje de un menor?', topic.subject)
                WHEN 6 THEN format('¿Cómo valoras el uso de %s por menores?', topic.subject)
                ELSE format('¿Has usado %s alguna vez?', topic.subject)
            END;
            PERFORM pg_temp.seed_question(topic.code, 'CATALOG_FIXED', NULL,
                                          admin_id, statement_text, question_type);
        END LOOP;
        IF topic.ordinal <= 6 THEN
            INSERT INTO blocks (scope, name, description, created_by_user_id,
                                category, created_at, updated_at)
            VALUES ('CATALOG', 'Bloque de ' || lower(left(topic.label, 1)) || substr(topic.label, 2),
                    'Preguntas cortas sobre el uso de esta tecnología.', admin_id,
                    topic.code, now(), now()) RETURNING id INTO block_id;
            INSERT INTO content_items (parent_block_id, item_type, position, is_required)
            VALUES (block_id, 'QUESTION', 1, true) RETURNING id INTO item_id;
            PERFORM pg_temp.seed_question(topic.code, 'LOCAL', item_id, admin_id,
                format('¿Has visto usar %s en clase?', topic.subject), 'BOOLEAN');
        END IF;
        IF topic.ordinal <= 4 THEN
            INSERT INTO templates (title, description, created_by_user_id,
                                   category, created_at, updated_at)
            VALUES ('Plantilla de ' || lower(left(topic.label, 1)) || substr(topic.label, 2),
                    'Preguntas sencillas para niños y niñas.',
                    admin_id, topic.code, now(), now()) RETURNING id INTO template_id;
            INSERT INTO content_items (template_id, item_type, position, is_required)
            VALUES (template_id, 'QUESTION', 1, true) RETURNING id INTO item_id;
            PERFORM pg_temp.seed_question(topic.code, 'FIXED_SNAPSHOT', item_id, admin_id,
                format('¿Con qué frecuencia has visto a un menor usar %s?', topic.subject), 'SINGLE_CHOICE');
            INSERT INTO content_items (template_id, item_type, position, text_body)
            VALUES (template_id, 'TEXT', 2, 'Gracias por contar lo que piensas.');
        END IF;
    END LOOP;

    FOR personal_question IN SELECT * FROM (VALUES
        ('¿Cuál es tu relación con los menores de los que habla esta encuesta?', 'SINGLE_CHOICE',
         ARRAY['Madre, padre o tutor/a', 'Abuelo o abuela', 'Docente o educador/a',
               'Otra persona cuidadora', 'Prefiero no responder']::text[], NULL::numeric),
        ('¿Qué edad aproximada tienen esos menores?', 'SINGLE_CHOICE',
         ARRAY['3 a 5 años', '6 a 8 años', '9 a 11 años', '12 a 14 años',
               '15 a 17 años', 'Prefiero no responder']::text[], NULL::numeric),
        ('¿Con qué frecuencia convives o trabajas con esos menores?', 'SINGLE_CHOICE',
         ARRAY['A diario', 'Varias veces a la semana', 'Algunas veces al mes',
               'Con menos frecuencia', 'Prefiero no responder']::text[], NULL::numeric),
        ('¿Cuál es el nivel de estudios más alto que has completado?', 'SINGLE_CHOICE',
         ARRAY['Primaria o menos', 'Secundaria o bachillerato', 'Formación profesional',
               'Estudios universitarios', 'Prefiero no responder']::text[], NULL::numeric),
        ('¿Cuántos años tienes?', 'NUMBER', NULL::text[], 100::numeric),
        ('¿Con qué frecuencia utilizas dispositivos digitales?', 'SINGLE_CHOICE',
         ARRAY['Nunca', 'Algunas veces al mes', 'Varias veces a la semana',
               'A diario', 'Prefiero no responder']::text[], NULL::numeric),
        ('¿Has utilizado alguna herramienta de inteligencia artificial?', 'BOOLEAN',
         NULL::text[], NULL::numeric),
        ('¿Qué dispositivo digital utilizas con más frecuencia?', 'SINGLE_CHOICE',
         ARRAY['Ordenador', 'Tableta', 'Teléfono móvil', 'Otro dispositivo',
               'Prefiero no responder']::text[], NULL::numeric),
        ('¿Cuántos menores tienes habitualmente a tu cargo?', 'NUMBER',
         NULL::text[], 40::numeric),
        ('¿Acompañas habitualmente a un menor cuando usa tecnología?', 'BOOLEAN',
         NULL::text[], NULL::numeric)
    ) AS personal(statement, type, options, max_value) LOOP
        personal_question_id := pg_temp.seed_question('DATOS_PERSONALES', 'CATALOG_FIXED', NULL,
                                                      admin_id, personal_question.statement,
                                                      personal_question.type, 'NEUTRAL', 1,
                                                      personal_question.options,
                                                      personal_question.max_value);
        IF personal_question.statement = '¿Cuántos años tienes?' THEN
            UPDATE question_settings SET min_value = 18
            WHERE question_id = personal_question_id;
        ELSIF personal_question.statement = '¿Cuántos menores tienes habitualmente a tu cargo?' THEN
            UPDATE question_settings SET min_value = 1
            WHERE question_id = personal_question_id;
        END IF;
    END LOOP;
END;
$seed_catalog$;

-- Una copia fija conserva el enunciado y las opciones de la pregunta de catálogo.
CREATE OR REPLACE FUNCTION pg_temp.seed_catalog_snapshot(
    p_category varchar, p_statement text, p_parent bigint, p_creator bigint
) RETURNS bigint LANGUAGE plpgsql AS $function$
DECLARE
    source_question record;
    source_options text[];
BEGIN
    SELECT * INTO STRICT source_question FROM questions
    WHERE scope = 'CATALOG_FIXED' AND category = p_category AND statement = p_statement;
    IF source_question.question_type <> 'SINGLE_CHOICE' THEN
        RAISE EXCEPTION 'La copia de catálogo requiere una pregunta de opción única: %', p_statement;
    END IF;
    SELECT array_agg(label ORDER BY position) INTO source_options
    FROM question_options WHERE question_id = source_question.id;
    RETURN pg_temp.seed_question(p_category, 'FIXED_SNAPSHOT', p_parent, p_creator,
        source_question.statement, source_question.question_type, 'NEUTRAL', 1, source_options);
END;
$function$;

CREATE TEMP TABLE seed_survey_specs (
    ordinal integer PRIMARY KEY,
    category varchar(64) NOT NULL,
    title varchar(200) NOT NULL UNIQUE,
    scenario text NOT NULL,
    benefit_question text NOT NULL,
    risk_question text NOT NULL,
    open_question text NOT NULL
) ON COMMIT DROP;
INSERT INTO seed_survey_specs VALUES
    (1, 'ORDENADORES', 'Ordenador compartido en el aula',
     'Una clase utiliza ordenadores compartidos para buscar información y preparar trabajos en grupo.',
     '¿Cuánto ayudaría el ordenador compartido a que el alumnado participe en el trabajo en grupo?',
     '¿Cuánto te preocupa que alguien vea archivos o cuentas de otros alumnos en ese ordenador?',
     '¿Qué norma pondrías para compartir el ordenador de forma segura?'),
    (2, 'ORDENADORES', 'Portátiles para hacer deberes en casa',
     'El centro presta portátiles para realizar tareas escolares fuera del aula.',
     '¿Cuánto facilitaría el portátil terminar las tareas cuando no hay ordenador propio en casa?',
     '¿Cuánto te preocupa que el uso del portátil dependa de la conexión disponible en cada hogar?',
     '¿Qué apoyo necesitarían las familias para utilizar el portátil prestado?'),
    (3, 'TABLETA', 'Tabletas para leer y crear en primaria',
     'Un grupo de primaria usa tabletas para leer cuentos y crear dibujos relacionados con ellos.',
     '¿Cuánto podría ayudar la tableta a que los niños participen en la actividad de lectura?',
     '¿Cuánto te preocupa que la tableta distraiga a los niños durante la lectura?',
     '¿Cómo combinarías esta actividad con libros y materiales físicos?'),
    (4, 'TELEFONO_INTELIGENTE', 'Primer teléfono móvil y autonomía',
     'Una familia valora dar un primer teléfono móvil a un menor para comunicarse al salir de casa.',
     '¿Cuánto ayudaría el teléfono a que el menor se comunique con su familia cuando lo necesite?',
     '¿Cuánto te preocupa que reciba mensajes de desconocidos o comparta datos personales?',
     '¿Qué acuerdos sobre el uso del teléfono propondrías a la familia?'),
    (5, 'GAFAS_INTELIGENTES', 'Gafas inteligentes para actividades escolares',
     'Un grupo prueba gafas que muestran información sobre objetos durante una actividad supervisada.',
     '¿Cuánto ayudarían las gafas a comprender lo que se observa en la actividad?',
     '¿Cuánto te preocupa que las gafas graben a otras personas sin su consentimiento?',
     '¿En qué actividad concreta probarías las gafas por primera vez?'),
    (6, 'TELEVISION_INTELIGENTE', 'Vídeos educativos en una televisión conectada',
     'Una familia utiliza una televisión conectada para ver vídeos educativos con los niños.',
     '¿Cuánto ayudaría comentar los vídeos en familia a comprender sus contenidos?',
     '¿Cuánto te preocupa que las recomendaciones lleven a contenidos inapropiados?',
     '¿Cómo elegirías los vídeos que pueden ver los niños?'),
    (7, 'PIZARRA_INTERACTIVA', 'Pizarra interactiva en una clase participativa',
     'El alumnado resuelve problemas en grupo usando una pizarra interactiva.',
     '¿Cuánto favorecería la pizarra que los alumnos expliquen sus ideas al resto?',
     '¿Cuánto te preocupa que solo participen quienes se acercan a la pizarra?',
     '¿Cómo organizarías los turnos para que participe toda la clase?'),
    (8, 'PROYECTOR_DIGITAL', 'Proyecciones accesibles para toda la clase',
     'Un docente proyecta imágenes y subtítulos para explicar un tema a la clase.',
     '¿Cuánto ayudaría la proyección a seguir la explicación desde cualquier lugar del aula?',
     '¿Cuánto te preocupa que el texto proyectado resulte difícil de leer para algunos alumnos?',
     '¿Qué cambiarías en la proyección para hacerla más accesible?'),
    (9, 'PLATAFORMA_APRENDIZAJE', 'Seguimiento de tareas en la plataforma escolar',
     'Un centro publica tareas y comentarios del profesorado en una plataforma de aprendizaje.',
     '¿Cuánto ayudaría la plataforma a que las familias conozcan las tareas pendientes?',
     '¿Cuánto te preocupa que algunas familias no puedan acceder con regularidad?',
     '¿Qué alternativa ofrecerías a una familia sin conexión estable?'),
    (10, 'LIBROS_TEXTO_DIGITALES', 'Libro digital y lectura en casa',
     'El alumnado puede consultar en casa un libro de texto digital con audio y ejercicios.',
     '¿Cuánto ayudarían el audio y los ejercicios a repasar una lección?',
     '¿Cuánto te preocupa que el libro no pueda consultarse sin conexión?',
     '¿Qué parte del libro digital te parece más útil para estudiar?'),
    (11, 'ROBOTICA_EDUCATIVA', 'Robots para aprender a programar',
     'Un grupo programa un pequeño robot para recorrer un circuito y corregir errores en equipo.',
     '¿Cuánto ayudaría el robot a comprender la secuencia de instrucciones?',
     '¿Cuánto te preocupa que algunos niños queden apartados del manejo del robot?',
     '¿Cómo repartirías las tareas dentro del equipo?'),
    (12, 'IMPRESION_3D', 'Diseño e impresión de piezas para clase',
     'El alumnado diseña una pieza sencilla y observa cómo se imprime en 3D con supervisión.',
     '¿Cuánto ayudaría imprimir la pieza a entender el diseño que hicieron?',
     '¿Cuánto te preocupa la seguridad al usar la impresora o manipular materiales calientes?',
     '¿Qué objeto útil para la clase diseñarías con el grupo?'),
    (13, 'INTELIGENCIA_ARTIFICIAL', 'IA para preparar actividades educativas',
     'Un docente utiliza una herramienta de IA para preparar ejercicios que después revisa antes de usarlos.',
     '¿Cuánto podría ayudar la IA a adaptar los ejercicios a distintos niveles?',
     '¿Cuánto te preocupa que la herramienta proponga información incorrecta?',
     '¿Qué revisarías antes de entregar un ejercicio creado con IA?'),
    (14, 'INTELIGENCIA_ARTIFICIAL', 'Imágenes generadas por IA para contar historias',
     'Un grupo crea imágenes con IA para acompañar un cuento escrito por los niños.',
     '¿Cuánto podrían ayudar las imágenes a compartir el cuento con otras personas?',
     '¿Cuánto te preocupa que las imágenes sustituyan ideas propias de los niños?',
     '¿Cómo darías prioridad al cuento creado por el grupo?'),
    (15, 'ASISTENTES_VOZ', 'Preguntas a un asistente de voz en familia',
     'Un menor hace preguntas a un asistente de voz mientras una persona adulta le acompaña.',
     '¿Cuánto ayudaría el asistente a despertar la curiosidad del menor?',
     '¿Cuánto te preocupa que el asistente escuche o guarde conversaciones familiares?',
     '¿Qué preguntas preferirías buscar con el menor en otras fuentes?'),
    (16, 'REALIDAD_VIRTUAL', 'Visita virtual a un museo',
     'Una clase realiza una visita breve a un museo con gafas de realidad virtual y apoyo docente.',
     '¿Cuánto ayudaría la visita virtual a conocer obras que no pueden verse en persona?',
     '¿Cuánto te preocupa que el uso de las gafas provoque mareo o incomodidad?',
     '¿Qué actividad harías después de la visita virtual?'),
    (17, 'REALIDAD_AUMENTADA', 'Mapa aumentado del barrio',
     'Durante un paseo supervisado, una aplicación muestra información sobre edificios del barrio.',
     '¿Cuánto ayudaría la aplicación a conocer la historia del barrio?',
     '¿Cuánto te preocupa que mirar la pantalla distraiga al caminar?',
     '¿Cómo organizarías las paradas para consultar la información con seguridad?'),
    (18, 'REDES_SOCIALES', 'Primer perfil en una red social',
     'Una familia habla con un adolescente sobre abrir un perfil y decidir qué puede publicar.',
     '¿Cuánto ayudaría acordar normas antes de abrir el perfil?',
     '¿Cuánto te preocupa que se compartan fotos o datos personales sin permiso?',
     '¿Qué normas sobre privacidad acordarías con el adolescente?'),
    (19, 'VIDEOJUEGOS', 'Videojuegos cooperativos y tiempo de juego',
     'Varios niños juegan juntos a un videojuego cooperativo con un límite de tiempo pactado.',
     '¿Cuánto podría favorecer el juego la colaboración entre los niños?',
     '¿Cuánto te preocupa que aparezcan discusiones al terminar el tiempo acordado?',
     '¿Cómo acordarías el tiempo y el final de la partida?'),
    (20, 'OTROS', 'Tecnología nueva en una actividad extraescolar',
     'Una asociación quiere probar un dispositivo digital nuevo en una actividad con menores.',
     '¿Cuánto podría ayudar una prueba supervisada a decidir si vale la pena usarlo?',
     '¿Cuánto te preocupa que no se conozcan todavía sus efectos sobre los menores?',
     '¿Qué información pedirías antes de autorizar la prueba?'),
    (21, 'GAFAS_INTELIGENTES', 'Gafas que detectan posibles situaciones de acoso',
     'Unas gafas analizan señales del entorno y avisan a un adulto ante posibles situaciones de acoso.',
     '¿Cuánto ayudarían estas alertas a detectar una situación preocupante a tiempo?',
     '¿Cuánto te preocupa que una alerta equivocada cause angustia al menor?',
     '¿Quién debería recibir una alerta y cómo debería comprobarla?'),
    (22, 'GAFAS_INTELIGENTES', 'Gafas que describen el entorno al caminar',
     'Unas gafas describen objetos y lugares a un menor mientras camina con supervisión.',
     '¿Cuánto ayudarían las descripciones a explorar un lugar nuevo?',
     '¿Cuánto te preocupa que las indicaciones distraigan al menor del tráfico?',
     '¿En qué recorrido probarías esta función por primera vez?'),
    (23, 'REALIDAD_AUMENTADA', 'Historias de los abuelos con imágenes aumentadas',
     'Mientras una persona mayor cuenta recuerdos, unas gafas muestran imágenes generadas sobre la historia.',
     '¿Cuánto ayudarían las imágenes a comprender los recuerdos familiares?',
     '¿Cuánto te preocupa que las imágenes no representen bien lo que se contó?',
     '¿Cómo conservarías la voz y el relato de la persona mayor como parte principal?'),
    (24, 'GAFAS_INTELIGENTES', 'Gafas que proponen juegos creativos',
     'Unas gafas observan objetos del entorno y proponen juegos que los niños pueden adaptar.',
     '¿Cuánto ayudarían las propuestas a iniciar juegos creativos?',
     '¿Cuánto te preocupa que el menor deje de inventar juegos por sí mismo?',
     '¿Qué juego propondrías sin necesidad de utilizar las gafas?'),
    (25, 'GAFAS_INTELIGENTES', 'Avisos sobre el tiempo de pantalla',
     'Unas gafas muestran al menor cuánto tiempo lleva usando pantallas y avisan al acercarse al límite.',
     '¿Cuánto ayudarían los avisos a respetar un límite de tiempo acordado?',
     '¿Cuánto te preocupa que los avisos generen conflictos en la familia?',
     '¿Cómo decidirías el límite de tiempo junto con el menor?'),
    (26, 'INTELIGENCIA_ARTIFICIAL', 'Conocimiento de IA entre personas cuidadoras',
     'Queremos conocer la experiencia de las personas adultas con herramientas como ChatGPT o asistentes de voz.',
     '¿Cuánto ayudaría conocer estas herramientas para acompañar a un menor que las usa?',
     '¿Cuánto te preocupa no reconocer una respuesta incorrecta de una IA?',
     '¿Qué te gustaría aprender sobre estas herramientas?'),
    (27, 'TELEFONO_INTELIGENTE', 'Uso del móvil por los menores de la familia',
     'Una familia revisa cuándo y para qué utilizan el móvil los menores a su cargo.',
     '¿Cuánto ayudaría hablar juntos de las aplicaciones que utilizan?',
     '¿Cuánto te preocupa que el móvil interrumpa el descanso del menor?',
     '¿Qué momento del día mantendrías libre de móvil?'),
    (28, 'TABLETA', 'Acompañamiento adulto al usar tabletas',
     'Una persona adulta acompaña a un menor mientras utiliza una tableta para aprender.',
     '¿Cuánto ayudaría conversar sobre la actividad al terminar?',
     '¿Cuánto te preocupa que el menor utilice aplicaciones que no conoces?',
     '¿Cómo seleccionarías una aplicación adecuada a su edad?'),
    (29, 'REDES_SOCIALES', 'Privacidad de fotos familiares en redes',
     'Una familia decide si compartir fotografías de menores en sus perfiles de redes sociales.',
     '¿Cuánto ayudaría pedir la opinión del menor antes de publicar una foto?',
     '¿Cuánto te preocupa que las fotos circulen fuera del grupo de personas previsto?',
     '¿Qué alternativa usarías para compartir una foto solo con familiares?'),
    (30, 'VIDEOJUEGOS', 'Acuerdos familiares sobre videojuegos',
     'Una familia acuerda con los menores horarios, contenidos y juego en línea.',
     '¿Cuánto ayudaría revisar juntos la clasificación por edades de un juego?',
     '¿Cuánto te preocupa que un desconocido contacte con el menor durante una partida?',
     '¿Qué acuerdo revisarías si el menor empieza a jugar en línea?');

CREATE OR REPLACE FUNCTION pg_temp.seed_survey_items(
    p_ordinal integer, p_survey bigint, p_owner bigint, p_category varchar, p_subject text,
    p_scenario text, p_benefit text, p_risk text, p_open_question text
) RETURNS void LANGUAGE plpgsql AS $function$
DECLARE
    item_id bigint;
BEGIN
    INSERT INTO content_items (survey_id, item_type, position, text_body)
    VALUES (p_survey, 'TEXT', 1, p_scenario || ' Nos interesa tu opinión sincera; no hay respuestas correctas.');

    INSERT INTO content_items (survey_id, item_type, position, is_required)
    VALUES (p_survey, 'QUESTION', 2, false) RETURNING id INTO item_id;
    PERFORM pg_temp.seed_catalog_snapshot('DATOS_PERSONALES',
        '¿Cuál es tu relación con los menores de los que habla esta encuesta?', item_id, p_owner);

    INSERT INTO content_items (survey_id, item_type, position, is_required)
    VALUES (p_survey, 'QUESTION', 3, false) RETURNING id INTO item_id;
    PERFORM pg_temp.seed_catalog_snapshot('DATOS_PERSONALES',
        '¿Qué edad aproximada tienen esos menores?', item_id, p_owner);

    INSERT INTO content_items (survey_id, item_type, position, is_required)
    VALUES (p_survey, 'QUESTION', 4, true) RETURNING id INTO item_id;
    PERFORM pg_temp.seed_catalog_snapshot(p_category,
        format('¿Con qué frecuencia has visto a un menor usar %s?', p_subject), item_id, p_owner);

    INSERT INTO content_items (survey_id, item_type, position, is_required)
    VALUES (p_survey, 'QUESTION', 5, true) RETURNING id INTO item_id;
    PERFORM pg_temp.seed_question(p_category, 'LOCAL', item_id, p_owner,
        p_benefit, 'SCALE', 'POSITIVE', 2, NULL, 10, 'Nada', 'Mucho');

    INSERT INTO content_items (survey_id, item_type, position, is_required)
    VALUES (p_survey, 'QUESTION', 6, true) RETURNING id INTO item_id;
    PERFORM pg_temp.seed_question(p_category, 'LOCAL', item_id, p_owner,
        p_risk, 'SCALE', 'NEGATIVE', 2, NULL, 10, 'Nada', 'Mucho');

    INSERT INTO content_items (survey_id, item_type, position, is_required)
    VALUES (p_survey, 'QUESTION', 7, false) RETURNING id INTO item_id;
    PERFORM pg_temp.seed_question(p_category, 'LOCAL', item_id, p_owner,
        p_open_question, 'TEXT');

    IF p_ordinal BETWEEN 21 AND 25 THEN
        INSERT INTO content_items (survey_id, item_type, position, is_required)
        VALUES (p_survey, 'QUESTION', 8, true) RETURNING id INTO item_id;
        PERFORM pg_temp.seed_question(p_category, 'LOCAL', item_id, p_owner,
            CASE p_ordinal
                WHEN 21 THEN '¿Qué tan adecuado te parece que un menor lleve estas gafas para detectar posibles situaciones de acoso?'
                WHEN 22 THEN '¿Qué tan adecuado te parece que un menor use estas gafas mientras camina?'
                WHEN 23 THEN '¿Qué tan adecuado te parece mostrar imágenes mientras una persona mayor cuenta sus recuerdos?'
                WHEN 24 THEN '¿Qué tan adecuado te parece que las gafas sugieran juegos a un menor?'
                ELSE '¿Qué tan adecuado te parece usar las gafas para controlar el tiempo de pantalla?'
            END, 'SCALE', 'POSITIVE', 1, NULL, 10, 'Nada adecuado', 'Muy adecuado');

        INSERT INTO content_items (survey_id, item_type, position, is_required)
        VALUES (p_survey, 'QUESTION', 9, true) RETURNING id INTO item_id;
        PERFORM pg_temp.seed_question(p_category, 'LOCAL', item_id, p_owner,
            '¿En qué situación te parecería más apropiado utilizar esta función?',
            'SINGLE_CHOICE', 'NEUTRAL', 1,
            ARRAY['En una actividad supervisada', 'En casa con la familia',
                  'En la calle o un espacio público', 'En ninguna situación', 'No lo sé']);

        INSERT INTO content_items (survey_id, item_type, position, is_required)
        VALUES (p_survey, 'QUESTION', 10, true) RETURNING id INTO item_id;
        PERFORM pg_temp.seed_question(p_category, 'LOCAL', item_id, p_owner,
            CASE p_ordinal
                WHEN 21 THEN 'Comparado con pedir ayuda solo a adultos, ¿cómo valorarías el sistema de detección?'
                WHEN 22 THEN 'Comparado con explorar el entorno sin gafas, ¿cómo valorarías este asistente?'
                WHEN 23 THEN 'Comparado con escuchar el relato sin imágenes, ¿cómo valorarías esta experiencia?'
                WHEN 24 THEN 'Comparado con elegir por sí mismo a qué jugar, ¿cómo valorarías las sugerencias?'
                ELSE 'Comparado con recordatorios de una persona adulta, ¿cómo valorarías estos avisos?'
            END, 'SINGLE_CHOICE', 'NEUTRAL', 1,
            ARRAY['Mucho peor', 'Peor', 'Igual', 'Mejor', 'Mucho mejor', 'No lo sé']);

        INSERT INTO content_items (survey_id, item_type, position, is_required)
        VALUES (p_survey, 'QUESTION', 11, false) RETURNING id INTO item_id;
        PERFORM pg_temp.seed_question(p_category, 'LOCAL', item_id, p_owner,
            CASE p_ordinal
                WHEN 21 THEN '¿Cuánto te preocupa que las gafas graben conversaciones privadas al detectar acoso?'
                WHEN 22 THEN '¿Cuánto te preocupa que las gafas almacenen información sobre personas cercanas?'
                WHEN 23 THEN '¿Cuánto te preocupa que las imágenes sustituyan la imaginación del menor?'
                WHEN 24 THEN '¿Cuánto te preocupa que el menor dependa de las gafas para decidir a qué jugar?'
                ELSE '¿Cuánto te preocupa que el menor se sienta vigilado constantemente?'
            END, 'SCALE', 'NEGATIVE', 1, NULL, 10, 'Nada', 'Mucho');
    END IF;
END;
$function$;

CREATE TEMP TABLE seed_main_surveys (
    ordinal integer PRIMARY KEY,
    survey_id bigint NOT NULL UNIQUE
) ON COMMIT DROP;

DO $seed_surveys$
DECLARE
    topic record;
    spec record;
    survey_id bigint;
    owner_id bigint;
    collaborator_id bigint;
    lifecycle varchar;
    access_mode varchar;
    extra_number integer;
BEGIN
    FOR spec IN SELECT * FROM seed_survey_specs WHERE ordinal <= 20 ORDER BY ordinal LOOP
        SELECT * INTO STRICT topic FROM seed_topics WHERE code = spec.category;
        SELECT user_id INTO STRICT owner_id FROM seed_users
        WHERE role = 'EDITOR' AND ordinal = ((spec.ordinal - 1) % 10) + 1;
        lifecycle := CASE WHEN spec.ordinal <= 8 THEN 'DRAFT'
                          WHEN spec.ordinal <= 16 THEN 'OPEN' ELSE 'CLOSED' END;
        access_mode := CASE spec.ordinal % 3
            WHEN 0 THEN 'PUBLIC' WHEN 1 THEN 'PASSWORD_PROTECTED'
            ELSE 'AUTHENTICATED' END;
        INSERT INTO surveys (
            public_code, owner_id, title, description, lifecycle_status,
            access_mode, visibility, access_password_hash, font_family, tags,
            category, created_at, updated_at
        ) VALUES (
            CASE WHEN lifecycle = 'DRAFT' THEN NULL
                 ELSE upper(substr(md5('survey:' || spec.ordinal::text), 1, 12)) END,
            owner_id, spec.title, spec.scenario,
            lifecycle, access_mode,
            CASE WHEN lifecycle = 'DRAFT' OR spec.ordinal % 4 = 0
                 THEN 'UNLISTED' ELSE 'LISTED' END,
            CASE WHEN access_mode = 'PASSWORD_PROTECTED'
                 THEN '$2a$10$1PODhcZKKDlt/U53ogsOmOR6ygMDNxrXY.PumZG0OIHsHUS5HP7dO'
                 ELSE NULL END,
            'LEXEND', ARRAY['infancia', lower(replace(topic.code, '_', '-'))],
            topic.code, now(), now()
        ) RETURNING id INTO survey_id;

        INSERT INTO seed_main_surveys VALUES (spec.ordinal, survey_id);
        PERFORM pg_temp.seed_survey_items(spec.ordinal, survey_id, owner_id, topic.code, topic.subject,
            spec.scenario, spec.benefit_question, spec.risk_question, spec.open_question);

        IF spec.ordinal IN (1, 2) THEN
            SELECT user_id INTO STRICT collaborator_id FROM seed_users
            WHERE role = 'EDITOR' AND ordinal = spec.ordinal + 2;
            INSERT INTO survey_collaborators (survey_id, user_id, permission, joined_at)
            VALUES (survey_id, collaborator_id, 'EDIT', now());
            INSERT INTO publication_requests (survey_id, requested_by_user_id, message, created_at)
            VALUES (survey_id, collaborator_id,
                    'Solicito publicar esta encuesta de demostración.', now());
        END IF;
    END LOOP;

    FOR extra_number IN 1..10 LOOP
        SELECT * INTO STRICT spec FROM seed_survey_specs WHERE ordinal = extra_number + 20;
        SELECT * INTO STRICT topic FROM seed_topics WHERE code = spec.category;
        SELECT user_id INTO STRICT owner_id FROM seed_users
        WHERE role = 'EDITOR' AND ordinal = extra_number;
        INSERT INTO surveys (
            public_code, owner_id, title, description, lifecycle_status,
            access_mode, visibility, font_family, tags, category, created_at, updated_at
        ) VALUES (
            upper(substr(md5('short:' || extra_number::text), 1, 12)),
            owner_id, spec.title, spec.scenario, 'OPEN', 'PUBLIC',
            'LISTED', 'ARIAL', ARRAY['infancia', lower(replace(topic.code, '_', '-'))],
            topic.code, now(), now()
        ) RETURNING id INTO survey_id;
        PERFORM pg_temp.seed_survey_items(spec.ordinal, survey_id, owner_id, topic.code, topic.subject,
            spec.scenario, spec.benefit_question, spec.risk_question, spec.open_question);
    END LOOP;
END;
$seed_surveys$;

INSERT INTO registration_requests (
    email, name, institution, country, motivation, password_hash, created_at
) VALUES
    ('palomarodriguez@gmail.com', 'Paloma Rodríguez Bravo', 'Centro de demostración', 'ES',
     'Quiero preparar encuestas educativas.',
     '$2a$10$1PODhcZKKDlt/U53ogsOmOR6ygMDNxrXY.PumZG0OIHsHUS5HP7dO', now()),
    ('solicitud02@formquid.example', 'Luis Prueba', 'Centro de demostración', 'ES',
     'Quiero colaborar en el catálogo.',
     '$2a$10$1PODhcZKKDlt/U53ogsOmOR6ygMDNxrXY.PumZG0OIHsHUS5HP7dO', now());

INSERT INTO element_requests (requested_by_user_id, element_type, message, request_payload, created_at)
SELECT user_id, 'FIXED_QUESTION', 'Propongo una pregunta de demostración.',
       jsonb_build_object(
           'category', 'ORDENADORES',
           'question', jsonb_build_object(
               'statement', '¿Usas un portátil en clase?',
               'questionType', 'BOOLEAN', 'evaluationDirection', 'NEUTRAL',
               'settings', jsonb_build_object('trueLabel', 'Sí', 'falseLabel', 'No')
           )
       ), now()
FROM seed_users WHERE role = 'EDITOR' AND ordinal = 1;

INSERT INTO element_requests (requested_by_user_id, element_type, message, request_payload, created_at)
SELECT user_id, 'BLOCK', 'Propongo un bloque de demostración.',
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
FROM seed_users WHERE role = 'EDITOR' AND ordinal = 2;

INSERT INTO element_requests (requested_by_user_id, element_type, message, request_payload, created_at)
SELECT user_id, 'TEMPLATE', 'Propongo una plantilla de demostración.',
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
FROM seed_users WHERE role = 'EDITOR' AND ordinal = 3;

INSERT INTO collaboration_requests (survey_id, user_id, request_type, permission, message, created_at)
SELECT survey.id, candidate.user_id, cases.request_type, cases.permission,
       'Solicitud de colaboración de demostración', now()
FROM (VALUES
    (3, 5, 'INVITATION', 'EDIT'),
    (4, 6, 'INVITATION', 'VIEW'),
    (5, 7, 'ACCESS_REQUEST', 'EDIT'),
    (6, 8, 'ACCESS_REQUEST', 'VIEW')
) AS cases(survey_ordinal, editor_number, request_type, permission)
JOIN seed_main_surveys seeded ON seeded.ordinal = cases.survey_ordinal
JOIN surveys survey ON survey.id = seeded.survey_id
JOIN seed_users candidate ON candidate.role = 'EDITOR'
                         AND candidate.ordinal = cases.editor_number;

DO $verify$
BEGIN
    IF (SELECT count(*) FROM users WHERE role = 'SUPERADMIN') <> 1
       OR (SELECT count(*) FROM preserved_superadmin) <> 1
       OR (SELECT user_id FROM superadmin_guard WHERE singleton) <> 1
       OR (SELECT count(*) FROM users WHERE id BETWEEN 1 AND 13) <> 13
       OR EXISTS (
           SELECT 1 FROM preserved_superadmin original
           JOIN users super_account ON super_account.id = 1
           WHERE super_account.email IS DISTINCT FROM original.email
              OR super_account.password_hash IS DISTINCT FROM original.password_hash
              OR super_account.name IS DISTINCT FROM original.name
              OR super_account.institution IS DISTINCT FROM original.institution
       )
       OR (SELECT count(*) FROM users WHERE role = 'ADMIN') <> 2
       OR (SELECT count(*) FROM users WHERE role = 'EDITOR') <> 10
       OR (SELECT count(*) FROM questions WHERE scope = 'CATALOG_FIXED') <> 136
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
       OR (SELECT count(*) FROM seed_main_surveys) <> 20
       OR (SELECT count(*) FROM seed_survey_specs) <> 30
       OR EXISTS (
           SELECT 1 FROM surveys survey
           LEFT JOIN seed_survey_specs spec ON spec.title = survey.title
           LEFT JOIN content_items item ON item.survey_id = survey.id
           LEFT JOIN questions question ON question.parent_item_id = item.id
           GROUP BY survey.id, spec.ordinal
           HAVING spec.ordinal IS NULL
               OR count(question.id) <> CASE WHEN spec.ordinal BETWEEN 21 AND 25 THEN 10 ELSE 6 END
               OR count(question.id) FILTER (WHERE question.scope = 'FIXED_SNAPSHOT') <> 3
               OR count(question.id) FILTER (WHERE question.scope = 'LOCAL') <>
                  CASE WHEN spec.ordinal BETWEEN 21 AND 25 THEN 7 ELSE 3 END
               OR count(question.id) FILTER (WHERE question.category = 'DATOS_PERSONALES') <> 2
       )
       OR EXISTS (
           SELECT 1 FROM questions question
           JOIN content_items item ON item.id = question.parent_item_id
           JOIN surveys survey ON survey.id = item.survey_id
           WHERE question.category <> survey.category
             AND question.category <> 'DATOS_PERSONALES'
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

-- En una conexión reutilizada por el pooler, las funciones de pg_temp pueden
-- seguir presentes tras COMMIT. Se eliminan al terminar la carga.
DROP FUNCTION IF EXISTS pg_temp.seed_survey_items(integer, bigint, bigint, varchar, text, text, text, text, text);
DROP FUNCTION IF EXISTS pg_temp.seed_catalog_snapshot(varchar, text, bigint, bigint);
DROP FUNCTION IF EXISTS pg_temp.seed_question(varchar, varchar, bigint, bigint, text, varchar, varchar, numeric, text[], numeric, text, text);
COMMIT;
