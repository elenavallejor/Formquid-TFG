-- Solo cambian las etiquetas visibles. Los códigos siguen siendo las claves de
-- encuestas, preguntas, bloques y plantillas.
UPDATE categories category
SET name = labels.name,
    version = category.version + 1
FROM (VALUES
    ('ORDENADOR_SOBREMESA', 'Ordenadores de sobremesa'),
    ('PORTATIL', 'Ordenadores portátiles'),
    ('TABLETA', 'Tabletas'),
    ('TELEFONO_INTELIGENTE', 'Teléfonos móviles'),
    ('GAFAS_INTELIGENTES', 'Gafas inteligentes'),
    ('TELEVISION_INTELIGENTE', 'Televisores inteligentes'),
    ('PIZARRA_INTERACTIVA', 'Pizarras interactivas'),
    ('PROYECTOR_DIGITAL', 'Proyectores digitales'),
    ('PLATAFORMA_APRENDIZAJE', 'Plataformas de aprendizaje'),
    ('LIBROS_TEXTO_DIGITALES', 'Libros digitales'),
    ('ROBOTICA_EDUCATIVA', 'Robots educativos'),
    ('IMPRESION_3D', 'Impresoras 3D'),
    ('INTELIGENCIA_ARTIFICIAL', 'Herramientas de inteligencia artificial'),
    ('IA_GENERATIVA', 'Herramientas de IA generativa'),
    ('ASISTENTES_VOZ', 'Asistentes de voz'),
    ('REALIDAD_VIRTUAL', 'Experiencias de realidad virtual'),
    ('REALIDAD_AUMENTADA', 'Aplicaciones de realidad aumentada'),
    ('REDES_SOCIALES', 'Redes sociales'),
    ('VIDEOJUEGOS', 'Videojuegos'),
    ('OTROS', 'Otras tecnologías'),
    ('DATOS_PERSONALES', 'Datos personales')
) AS labels(code, name)
WHERE category.code = labels.code
  AND category.name IS DISTINCT FROM labels.name;
