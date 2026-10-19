-- Comprueba las referencias a categorías y reserva Datos personales para los elementos del catálogo.

ALTER TABLE surveys VALIDATE CONSTRAINT fk_surveys_category;
ALTER TABLE templates VALIDATE CONSTRAINT fk_templates_category;
ALTER TABLE blocks VALIDATE CONSTRAINT fk_blocks_category;
ALTER TABLE questions VALIDATE CONSTRAINT fk_questions_category;
ALTER TABLE surveys ADD CONSTRAINT ck_surveys_category CHECK (category <> 'DATOS_PERSONALES');
