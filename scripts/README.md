# Datos de demostración

[`seed-dev.sh`](seed-dev.sh) prepara una base PostgreSQL **de desarrollo** mediante [`seed-dev.sql`](seed-dev.sql). La carga es manual: no forma parte de Flyway ni se ejecuta al arrancar la aplicación.

## Qué elimina y qué conserva

- Elimina encuestas, respuestas, contenido, elementos del catálogo, solicitudes, notificaciones, tokens y usuarios anteriores.
- Conserva el superadministrador protegido por `superadmin_guard`.
- Conserva las 21 categorías iniciales y elimina las categorías creadas manualmente.
- Usa una transacción; si falla una inserción o una comprobación, se revierte toda la carga.

## Qué crea

| Datos | Cantidad |
| --- | ---: |
| Administradores | 2 |
| Editores | 10 |
| Preguntas fijas del catálogo | 150: siete por categoría general y diez de datos personales |
| Bloques y plantillas del catálogo | 6 bloques y 4 plantillas |
| Encuestas por categoría | 20: ocho borradores, ocho abiertas y cuatro cerradas |
| Encuestas adicionales | 10 abiertas y cortas |
| Solicitudes de registro | 2 |
| Solicitudes de elementos | 3: pregunta, bloque y plantilla |
| Solicitudes de colaboración | 4: invitación y acceso, con permisos de lectura y edición |
| Solicitudes de publicación | 2 |

Las encuestas combinan preguntas del catálogo y preguntas propias. Algunas incluyen copias de bloques o contenido de plantillas. Las preguntas están redactadas para niños y niñas con frases breves y opciones sencillas. Las diez preguntas de datos personales incluyen dos identificadas para una persona adulta. Se incluyen los siete tipos de pregunta admitidos y evaluaciones positivas, negativas y neutras. No se crean respuestas de participantes.

## Requisitos para ejecutarlo después de la revisión

La base debe tener las migraciones aplicadas hasta V38 y un superadministrador. Se necesita una cuenta de PostgreSQL con permisos de escritura y `psql` instalado o Docker en funcionamiento. Si se usa Docker, la imagen `postgres:16-alpine` debe estar disponible o poder descargarse. Las cuentas nuevas y las encuestas protegidas usan la contraseña de demostración `FormQuidDemo2026!`; el superadministrador conserva su contraseña.

Para ejecutarlo **solo cuando se autorice**, usa `./scripts/seed-dev.sh --confirm-reset` desde el proyecto. El `.sh` lee `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` del `.env` de la raíz. Las variables de entorno ya exportadas tienen prioridad. El archivo `.env` se lee como datos: no se ejecuta su contenido ni se muestra la contraseña. Usa TLS por defecto; `DB_SSLMODE` permite ajustar ese modo si la base de desarrollo lo necesita.

El `.sh` comprueba que el SQL se puede leer y usa `psql` local cuando está disponible; en caso contrario ejecuta el cliente PostgreSQL en Docker con acceso a la red del equipo. Para esta alternativa, Docker debe estar en funcionamiento en el mismo equipo donde se ejecuta el script. Envía el SQL por la entrada estándar, sin montar archivos en el contenedor. Activa la variable de sesión que exige el SQL y usa `ON_ERROR_STOP=1`, de modo que un fallo detiene la carga y revierte la transacción.
