-- Category codes stay as strings in the API. The initial Spanish vocabulary is seeded here;
-- additional categories can be inserted without changing Java code or database constraints.
CREATE TABLE categories (
    code varchar(64) PRIMARY KEY,
    name varchar(100) NOT NULL,
    personal_data boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_categories_code CHECK (code ~ '^[A-Z][A-Z0-9_]{1,63}$')
);

CREATE TEMP TABLE category_translation (
    old_code varchar(50) PRIMARY KEY,
    code varchar(64) NOT NULL UNIQUE,
    name varchar(100) NOT NULL
) ON COMMIT DROP;

INSERT INTO category_translation (old_code, code, name) VALUES
    ('DESKTOP_COMPUTER', 'ORDENADOR_SOBREMESA', 'Ordenador de sobremesa'),
    ('LAPTOP', 'PORTATIL', 'Portátil'),
    ('TABLET', 'TABLETA', 'Tableta'),
    ('SMARTPHONE', 'TELEFONO_INTELIGENTE', 'Teléfono inteligente'),
    ('SMARTWATCH', 'RELOJ_INTELIGENTE', 'Reloj inteligente'),
    ('SMART_TV', 'TELEVISION_INTELIGENTE', 'Televisión inteligente'),
    ('VIDEO_GAME_CONSOLE', 'CONSOLA_VIDEOJUEGOS', 'Consola de videojuegos'),
    ('E_BOOK_READER', 'LECTOR_LIBROS_ELECTRONICOS', 'Lector de libros electrónicos'),
    ('INTERACTIVE_WHITEBOARD', 'PIZARRA_INTERACTIVA', 'Pizarra interactiva'),
    ('DIGITAL_PROJECTOR', 'PROYECTOR_DIGITAL', 'Proyector digital'),
    ('LEARNING_MANAGEMENT_SYSTEM', 'PLATAFORMA_APRENDIZAJE', 'Plataforma de aprendizaje'),
    ('EDUCATIONAL_APPS', 'APLICACIONES_EDUCATIVAS', 'Aplicaciones educativas'),
    ('DIGITAL_TEXTBOOKS', 'LIBROS_TEXTO_DIGITALES', 'Libros de texto digitales'),
    ('EDUCATIONAL_ROBOTICS', 'ROBOTICA_EDUCATIVA', 'Robótica educativa'),
    ('THREE_D_PRINTING', 'IMPRESION_3D', 'Impresión 3D'),
    ('ARTIFICIAL_INTELLIGENCE', 'INTELIGENCIA_ARTIFICIAL', 'Inteligencia artificial'),
    ('GENERATIVE_AI', 'IA_GENERATIVA', 'IA generativa'),
    ('CHATBOTS', 'BOTS_CONVERSACION', 'Bots de conversación'),
    ('VOICE_ASSISTANTS', 'ASISTENTES_VOZ', 'Asistentes de voz'),
    ('VIRTUAL_REALITY', 'REALIDAD_VIRTUAL', 'Realidad virtual'),
    ('AUGMENTED_REALITY', 'REALIDAD_AUMENTADA', 'Realidad aumentada'),
    ('SOCIAL_MEDIA', 'REDES_SOCIALES', 'Redes sociales'),
    ('INSTANT_MESSAGING', 'MENSAJERIA_INSTANTANEA', 'Mensajería instantánea'),
    ('VIDEO_CONFERENCING', 'VIDEOCONFERENCIAS', 'Videoconferencias'),
    ('VIDEO_GAMES', 'VIDEOJUEGOS', 'Videojuegos'),
    ('STREAMING_PLATFORMS', 'PLATAFORMAS_TRANSMISION', 'Plataformas de transmisión'),
    ('OTHER', 'OTROS', 'Otros'),
    ('PERSONAL_DATA', 'DATOS_PERSONALES', 'Datos personales');

INSERT INTO categories (code, name, personal_data)
SELECT code, name, code = 'DATOS_PERSONALES' FROM category_translation;

ALTER TABLE surveys DROP CONSTRAINT ck_surveys_category;
ALTER TABLE templates DROP CONSTRAINT ck_templates_category;
ALTER TABLE blocks DROP CONSTRAINT ck_blocks_category;
ALTER TABLE questions DROP CONSTRAINT ck_questions_category;

UPDATE surveys s SET category = t.code FROM category_translation t WHERE s.category = t.old_code;
UPDATE templates x SET category = t.code FROM category_translation t WHERE x.category = t.old_code;
UPDATE blocks b SET category = t.code FROM category_translation t WHERE b.category = t.old_code;
UPDATE questions q SET category = t.code FROM category_translation t WHERE q.category = t.old_code;

ALTER TABLE surveys ALTER COLUMN category TYPE varchar(64), ALTER COLUMN category SET DEFAULT 'OTROS';
ALTER TABLE templates ALTER COLUMN category TYPE varchar(64), ALTER COLUMN category SET DEFAULT 'OTROS';
ALTER TABLE blocks ALTER COLUMN category TYPE varchar(64), ALTER COLUMN category SET DEFAULT 'OTROS';
ALTER TABLE questions ALTER COLUMN category TYPE varchar(64), ALTER COLUMN category SET DEFAULT 'OTROS';

ALTER TABLE surveys ADD CONSTRAINT fk_surveys_category
    FOREIGN KEY (category) REFERENCES categories(code) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE templates ADD CONSTRAINT fk_templates_category
    FOREIGN KEY (category) REFERENCES categories(code) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE blocks ADD CONSTRAINT fk_blocks_category
    FOREIGN KEY (category) REFERENCES categories(code) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE questions ADD CONSTRAINT fk_questions_category
    FOREIGN KEY (category) REFERENCES categories(code) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE surveys ADD CONSTRAINT ck_surveys_category CHECK (category <> 'DATOS_PERSONALES');
