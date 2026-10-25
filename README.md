# FormQuid

FormQuid es una aplicación web para crear, publicar y responder encuestas de forma colaborativa. Este repositorio recoge el desarrollo del Trabajo de Fin de Grado, desde la interfaz y la API hasta el modelo de datos, las pruebas y las herramientas de preparación del entorno.

La aplicación permite construir encuestas desde cero, utilizar elementos reutilizables del catálogo o importar una plantilla ODT. Una vez publicadas, las personas destinatarias pueden responder según el modo de acceso elegido. Los responsables de la encuesta disponen de resultados individuales y estadísticas agregadas.

## Funcionalidades

### Encuestas y participación

El editor puede organizar una encuesta con textos, preguntas y bloques. Hay preguntas de texto, numéricas, de opción única, de opción múltiple, de escala, de matriz de escala y de dos opciones. También puede añadir una portada, elegir la tipografía, clasificar la encuesta y asignarle etiquetas.

Cada encuesta pasa por los estados de borrador, abierta y cerrada. Al abrirla, puede ofrecer acceso público, exigir una contraseña o reservar la participación a usuarios autenticados. La visibilidad en los listados se configura por separado. Una encuesta abierta puede compartirse mediante su código público.

El formulario comprueba las respuestas obligatorias y evita que una misma identidad participe dos veces en la misma encuesta. Los resultados incluyen el detalle de cada participación y un resumen estadístico por pregunta. Cuando se configuran preguntas evaluables con un peso, también se calcula un análisis global ponderado.

### Catálogo y trabajo compartido

El catálogo reúne preguntas, bloques y plantillas que pueden utilizarse al preparar nuevas encuestas. Los editores pueden proponer elementos para el catálogo. Las personas administradoras revisan estas solicitudes y gestionan los elementos publicados.

Una encuesta puede contar con colaboradores con permiso de lectura o edición. La aplicación contempla invitaciones, solicitudes de acceso y solicitudes de publicación. Cuando un colaborador solicita la publicación, el propietario decide si acepta la petición. Las notificaciones informan de las acciones relacionadas con estas solicitudes y con la gestión de cuentas.

### Cuentas y administración

Las nuevas cuentas se solicitan desde la web y deben ser aprobadas por una persona administradora. Existen tres roles: editor, administrador y superadministrador. Los administradores supervisan las solicitudes y las encuestas de la plataforma. El superadministrador gestiona además las cuentas, sus roles y las categorías del catálogo.

Cada usuario puede consultar y modificar su perfil. La aplicación incluye recuperación de contraseña mediante correo electrónico.

## Organización del proyecto

La interfaz está desarrollada con React 19, React Router 7 y Vite 8. El servidor utiliza Java 21, Spring Boot 4, Spring Security y Spring Data JPA. Los datos se almacenan en PostgreSQL y el esquema se gestiona con Flyway. Docker Compose reúne el servidor, la interfaz de desarrollo y MailHog. La base de datos debe estar disponible por separado, ya que los archivos Compose de este repositorio no crean un contenedor PostgreSQL.

## Documentación

Los apartados principales tienen documentación propia:

- [Documentación de FormQuid](doc/README.md). Reúne el sistema y sus datos, el uso de la aplicación, la API, el entorno de desarrollo y los contenedores.
- [Backend](backend/README.md). Explica los módulos, la seguridad y las pruebas de la API.
- [Frontend](frontend/README.md). Describe las páginas, los componentes y los comandos de desarrollo.
- [Scripts](scripts/README.md). Documenta el reinicio y la carga de datos de desarrollo.
- [Rendimiento](performance/README.md). Explica los planes JMeter y la preparación de las mediciones.

## Puesta en marcha con Docker Compose

Se necesitan Docker Compose y una instancia de PostgreSQL a la que pueda conectarse el backend. Los perfiles `dev` y `prod` configuran la conexión con `sslmode=require`, por lo que esa instancia debe aceptar conexiones TLS.

En la raíz del repositorio hay que crear un archivo `.env` con los datos de conexión `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD`. También es obligatorio definir `JWT_SECRET`. Para que la primera cuenta no use los valores predeterminados de desarrollo, configura `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD`, `SUPERADMIN_NAME` y `SUPERADMIN_INSTITUTION`. El archivo `.env` está excluido de Git.

Con la base disponible y las variables configuradas, el entorno de desarrollo se inicia desde la raíz:

```bash
docker compose up --build
```

La interfaz queda en `http://localhost:5173`, la API en `http://localhost:8080/api/v1` y MailHog en `http://localhost:8025`. El envío de correo está desactivado de forma predeterminada. Para probarlo con el servicio de Compose se deben añadir `MAIL_ENABLED=true` y `MAIL_HOST=mailhog` al archivo `.env`.

Flyway aplica las migraciones al arrancar el backend con el perfil `dev`. El inicializador crea el primer superadministrador únicamente si la base no contiene usuarios. Cambiar después las variables de esa cuenta no modifica una cuenta ya existente. La carga de datos de demostración es una operación independiente y está explicada en el [README de scripts](scripts/README.md).

También se pueden iniciar ambos proyectos sin Compose. El [README del backend](backend/README.md) explica cómo arrancar la API y el [README del frontend](frontend/README.md) recoge la configuración de la interfaz.

## Pruebas

Las pruebas del backend se ejecutan desde `backend` con `./mvnw test`. Las pruebas que utilizan PostgreSQL arrancan un contenedor mediante Testcontainers, así que necesitan Docker disponible.

En `frontend`, `npm test` ejecuta Vitest y `npm run test:coverage` genera el informe de cobertura. El repositorio incluye además planes JMeter para estudiar distintas operaciones de lectura y escritura de la API. Su preparación y sus límites están documentados en [performance/README.md](performance/README.md).

## Configuración y mantenimiento

`docker-compose.prod.yml` modifica la configuración del backend al combinarse con `docker-compose.yml`. La interfaz incluida en Compose sigue usando el servidor de desarrollo de Vite. Las verificaciones previas, la persistencia de portadas y la actualización de bases existentes se describen a continuación.

### Clave JWT del backend

Configura `JWT_SECRET` en el entorno antes de iniciar el backend. Debe ser una clave aleatoria de al menos 32 bytes. Puedes generar una con `openssl rand -hex 32`. La aplicación no arranca si falta, es demasiado corta o coincide con la antigua clave pública del proyecto.

Al cambiar `JWT_SECRET`, todos los tokens de sesión existentes dejan de ser válidos y los usuarios deben iniciar sesión de nuevo. Si se utilizó la antigua clave por defecto en algún despliegue, rota la clave configurada allí.

Al cambiar la contraseña de una cuenta, sus tokens anteriores dejan de ser válidos. La respuesta entrega una cookie nueva para conservar la sesión que hizo el cambio. Los tokens emitidos antes de esta protección también requieren iniciar sesión de nuevo una vez.

### Recuperación de contraseña

El enlace «¿Has olvidado tu contraseña?» solicita un correo de recuperación. El enlace recibido contiene un token de un solo uso que caduca a los 30 minutos. Solo se guarda su hash en la base de datos. Una nueva solicitud invalida el enlace anterior. El formulario muestra la misma confirmación tanto si existe la cuenta como si no.

Para enviar estos correos, configura `MAIL_ENABLED=true` y los parámetros SMTP existentes. El enlace utiliza `FRONTEND_ORIGIN` o, si se necesita una URL distinta, `PASSWORD_RESET_BASE_URL`. En desarrollo, el valor predeterminado es `http://localhost:5173`. MailHog muestra los mensajes locales en `http://localhost:8025`.

### Configuración previa al despliegue

Para preparar el backend con el perfil `prod`, combina `docker-compose.yml` con `docker-compose.prod.yml`. Antes de iniciarlo, configura en `.env` `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD`, `SUPERADMIN_NAME`, `SUPERADMIN_INSTITUTION` y `FRONTEND_ORIGIN` (por ejemplo, `https://app.ejemplo.com`), y ejecuta `python3 check-prod-env.py`. La comprobación no muestra los valores de estas variables.

El perfil `prod` exige estas variables y rechaza valores vacíos o solo espacios al arrancar. `SuperadminInitializer` conserva su comportamiento: solo crea la cuenta cuando no existen usuarios. Cambiar `.env` después no actualiza la contraseña de una cuenta ya creada.

### Portadas de encuestas

Las portadas subidas desde un dispositivo se guardan en `/app/data/survey-covers` dentro del backend. `docker-compose.yml` monta ahí el volumen persistente `survey-covers`. El perfil de producción reutiliza ese montaje al combinar ambos archivos Compose. Incluye el volumen en las copias de seguridad junto con la base de datos. Al retirar el contenedor, conserva el volumen y evita `docker compose down -v`.

Si el backend se despliega sin Docker Compose, monta almacenamiento persistente y configura `SURVEY_COVER_STORAGE_PATH` con la ruta montada. Se admiten imágenes PNG y JPEG de hasta 3 MB y 4000 píxeles por lado. Las portadas de borradores y encuestas con acceso restringido requieren autorización para leerse.

### Migraciones de bases existentes

La migración `V9__allow_multiple_superadmins.sql` se modificó anteriormente para admitir una base sin usuarios. Antes de actualizar una base existente, realiza una copia de seguridad y comprueba su historial:

```sql
SELECT version, description, checksum, success
FROM flyway_schema_history
WHERE version = '9';
```

Si el arranque informa de un checksum distinto en V9, no modifiques de nuevo el archivo ni ejecutes una reparación a ciegas. El historial de Git contiene versiones anteriores con un comentario distinto y con una comprobación que exigía un superadministrador incluso en una base vacía. Compara la versión aplicada con la actual y comprueba que la V9 anterior terminó correctamente y que el esquema contiene los objetos esperados. Después de la copia y esa comprobación, actualiza el checksum registrado con `flyway repair` usando el mismo conjunto de migraciones que desplegarás. Si hay otras diferencias o la migración falló, investiga y corrige el esquema antes de reparar el historial.

La migración V33 protege la existencia de al menos un superadministrador. Se aplica normalmente después de resolver cualquier discrepancia previa del historial. En una base nueva, el inicializador crea el primer usuario y la base establece la garantía en la misma transacción.
