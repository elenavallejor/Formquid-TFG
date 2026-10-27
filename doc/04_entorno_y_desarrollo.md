# Entorno y desarrollo

Para trabajar con FormQuid se necesitan estos componentes:

- PostgreSQL para conservar los datos.
- Java 21 para ejecutar el backend fuera de Docker.
- Node.js y npm para desarrollar la interfaz. El Dockerfile del frontend utiliza Node.js 22.
- Docker si se usa Compose o se ejecutan las pruebas de integración con Testcontainers.

El repositorio incluye Dockerfile y un archivo Compose para los servicios de la aplicación. PostgreSQL se ejecuta por separado.

## Preparar la base de datos

1. Crea una base PostgreSQL accesible desde el backend.
2. Anota su dirección, puerto, nombre, usuario y contraseña.
3. Comprueba que acepta TLS. Los perfiles `dev` y `prod` lo requieren.

En una base nueva, Flyway crea el esquema al arrancar. Los archivos SQL de migración no se ejecutan a mano.

## Opción 1. Docker Compose

En la raíz del repositorio, crea `.env` con los datos de base de datos y `JWT_SECRET`. Añade los datos de la cuenta inicial para evitar los valores predeterminados de desarrollo. Puedes generar una clave para `JWT_SECRET` con `openssl rand -hex 32`. El apartado de variables, más abajo, indica los nombres exactos.

Después, inicia los contenedores.

```bash
docker compose up --build
```

Abre `http://localhost:5173` para usar la interfaz. La API responde bajo `http://localhost:8080/api/v1`. Si activas el correo de pruebas con `MAIL_ENABLED=true` y `MAIL_HOST=mailhog`, los mensajes se consultan en `http://localhost:8025`.

## Opción 2. Ejecución local

Configura en el entorno del proceso Java las variables de base de datos y `JWT_SECRET`. Para el perfil `dev` la conexión a PostgreSQL sigue requiriendo TLS. Desde `backend`, arranca la API con el wrapper de Maven.

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

En otra terminal, instala las dependencias de la interfaz y ejecuta Vite.

```bash
cd frontend
npm ci
npm run dev
```

Si la API no está en `http://localhost:8080/api/v1`, configura `VITE_API_BASE_URL` antes de iniciar Vite. Los pasos particulares de cada módulo se amplían en los [README del backend](../backend/README.md) y [frontend](../frontend/README.md).

## Correo en desarrollo con Brevo

FormQuid conserva su envío SMTP y sus plantillas. Para entregar mensajes reales mediante Brevo necesitas la plataforma de correo transaccional activada en tu cuenta, un remitente verificado (o un dominio autenticado), el **login SMTP** que muestra Brevo y una **clave SMTP**, distinta de la clave API y de la contraseña de la cuenta. Guarda los valores en el `.env` de la raíz, que Git ignora:

```dotenv
BREVO_SMTP_LOGIN=tu-login-smtp
BREVO_SMTP_KEY=tu-clave-smtp
BREVO_MAIL_FROM=notificaciones@tu-dominio.example
```

Con Compose, arranca la variante opcional:

```bash
docker compose -f docker-compose.yml -f docker-compose.brevo.yml up --build
```

Esta variante activa el correo, usa `smtp-relay.brevo.com` por el puerto 587 y exige autenticación y STARTTLS. El Compose normal continúa usando MailHog. El servicio MailHog puede seguir arrancado, pero el backend envía por Brevo mientras se use el archivo adicional. Para volver a MailHog, arranca de nuevo solo con `docker compose up --build`.

Si ejecutas el backend fuera de Compose, exporta `MAIL_ENABLED=true`, `MAIL_HOST=smtp-relay.brevo.com`, `MAIL_PORT=587`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`, `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM` antes de iniciar Java. Si tu red bloquea el puerto 587, Brevo admite 2525 con la misma configuración STARTTLS. El enlace de recuperación usa `PASSWORD_RESET_BASE_URL` o `FRONTEND_ORIGIN`; revisa que apunte a la interfaz desde la que el destinatario abrirá el correo.

Puedes verificar la entrega solicitando un restablecimiento de contraseña para una cuenta existente. El envío se programa después de confirmar la transacción y se reintenta si falla; la respuesta HTTP de la solicitud no confirma la entrega final. Consulta los registros del backend y el historial transaccional de Brevo si el mensaje no llega. [Configuración SMTP de Brevo](https://help.brevo.com/hc/en-us/articles/7924908994450-Send-transactional-emails-using-Brevo-SMTP).

### Frontend en Vercel y backend en Render

La configuración actual de Vercel sirve el frontend y reenvía `/api` al backend de Render. El correo sale del backend: configura estas variables en **Render**, no en Vercel ni como variables `VITE_` del frontend:

```dotenv
MAIL_ENABLED=true
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=2525
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
MAIL_USERNAME=tu-login-smtp
MAIL_PASSWORD=tu-clave-smtp
MAIL_FROM=notificaciones@tu-dominio.example
FRONTEND_ORIGIN=https://tu-frontend.vercel.app
```

Usa `MAIL_PORT=2525` si el backend es un servicio gratuito de Render: este plan bloquea las conexiones SMTP salientes a los puertos 25, 465 y 587. Brevo admite 2525 como alternativa con STARTTLS. En un servicio que permita 587 puedes usar ese puerto. El archivo `docker-compose.brevo.yml` solo se aplica a ejecuciones locales con Compose; no configura las variables de Render. Comprueba el envío real y los registros tras desplegar, ya que la configuración no garantiza por sí sola la entrega. [Límites del plan gratuito de Render](https://render.com/docs/free#other-limitations) · [Puertos SMTP de Brevo](https://help.brevo.com/hc/en-us/articles/10905415650322-Which-SMTP-port-should-I-use-Port-587-465-or-2525).

## Primera entrada

En una base vacía, el inicializador crea la cuenta de superadministrador con los datos configurados. Después se puede entrar con ese correo y contraseña. Las nuevas cuentas de editor se solicitan desde la página de registro y una persona administradora debe aprobarlas. Las encuestas se crean como borradores y se abren cuando tienen contenido válido.

## Variables de entorno

El backend lee `application.properties` y el archivo del perfil activo. Compose carga `.env` desde la raíz. Ese archivo está excluido de Git. Las variables principales son:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` identifican la base PostgreSQL. Los perfiles `dev` y `prod` construyen una conexión con `sslmode=require`.
- `JWT_SECRET` firma los tokens. Debe contener al menos 32 bytes y no puede ser la antigua clave pública del proyecto. `JWT_EXPIRATION_MS` cambia su duración, que por defecto es de 86400000 milisegundos.
- `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD`, `SUPERADMIN_NAME` y `SUPERADMIN_INSTITUTION` definen la primera cuenta. `SUPERADMIN_COUNTRY` es opcional. El perfil `prod` exige los cuatro primeros valores. El inicializador los utiliza solo cuando aún no hay usuarios.
- `FRONTEND_ORIGIN` indica el origen de la interfaz en `prod`. En la configuración común, `CORS_ALLOWED_ORIGINS` tiene como valor predeterminado `http://localhost:5173`.
- `VITE_API_BASE_URL` indica al frontend dónde está la API. Por defecto utiliza `http://localhost:8080/api/v1`. Es una variable visible en el navegador y no debe contener secretos.

Las opciones de correo y almacenamiento son:

- `MAIL_ENABLED` activa el envío de mensajes. Por defecto es `false`. `MAIL_FROM` define el remitente y tiene como valor inicial `notifications@formquid.local`.
- `MAIL_HOST` y `MAIL_PORT` indican el servidor SMTP. Sus valores iniciales son `localhost` y `1025`. `MAIL_USERNAME` y `MAIL_PASSWORD` son opcionales para servidores sin autenticación. `MAIL_SMTP_AUTH` y `MAIL_STARTTLS` están desactivadas por defecto; al activar `MAIL_STARTTLS`, también se exige que el servidor acepte STARTTLS. Con MailHog en Compose se utiliza `MAIL_HOST=mailhog`.
- `PASSWORD_RESET_BASE_URL` fija la dirección de los enlaces de recuperación. Si no se indica, toma `FRONTEND_ORIGIN` o `http://localhost:5173`.
- `SURVEY_COVER_STORAGE_PATH` indica dónde se guardan las portadas. Por defecto es `./data/survey-covers`. Compose utiliza `/app/data/survey-covers`.
- `PROFILE_PHOTO_STORAGE_PATH` indica dónde se guardan las fotos de perfil. Por defecto es `./data/profile-photos`. Compose utiliza `/app/data/profile-photos`.

`SERVER_PORT` cambia el puerto 8080 del backend. Las variables `DB_POOL_MAX_SIZE` y `DB_POOL_MIN_IDLE` ajustan el tamaño del grupo de conexiones, con valores iniciales de 5 y 1. Los tiempos se configuran con `DB_POOL_MAX_LIFETIME_MS`, `DB_POOL_IDLE_TIMEOUT_MS`, `DB_POOL_KEEPALIVE_MS`, `DB_POOL_CONNECTION_TIMEOUT_MS` y `DB_POOL_VALIDATION_TIMEOUT_MS`.

## Organización del código

- `backend/src/main/java/com/formquid` agrupa el código por áreas funcionales. En cada una se pueden seguir los controladores, servicios, entidades, repositorios y DTO.
- `backend/src/main/resources/db/migration` contiene las migraciones Flyway. Los cambios de estructura se añaden como archivos nuevos para poder actualizar bases existentes.
- `frontend/src/app/router.jsx` define las rutas y sus restricciones. Las pantallas están en `frontend/src/app/routes`, los componentes compartidos en `frontend/src/components` y las llamadas al servidor en `frontend/src/api`.
- `scripts` reúne las tareas de datos de desarrollo. `performance` contiene los planes JMeter. Ambos directorios tienen su propio README.

El perfil `dev` valida con JPA el esquema que ha construido Flyway. La actualización de una base que ya aplicó migraciones requiere revisar su historial antes de arrancar una versión nueva.

### Migraciones de bases existentes

La migración `V9__allow_multiple_superadmins.sql` se modificó para admitir una base sin usuarios. Una base que aplicó una versión anterior puede presentar una diferencia de checksum. Antes de actualizarla:

1. Haz una copia de seguridad de la base y de las portadas.
2. Consulta la entrada de V9 en `flyway_schema_history`.

   ```sql
   SELECT version, description, checksum, success
   FROM flyway_schema_history
   WHERE version = '9';
   ```

3. Si Flyway informa de un checksum distinto, compara la migración aplicada con la actual. El historial de Git conserva las versiones anteriores. Comprueba que la versión aplicada terminó correctamente y que el esquema contiene los objetos esperados.
4. Después de esas comprobaciones, actualiza el checksum con `flyway repair` usando el mismo conjunto de migraciones que se desplegará.

No modifiques otra vez V9 ni repares el historial sin investigar las diferencias. Si la migración falló o hay otros cambios en el esquema, corrígelos antes. La migración V33 protege la existencia de al menos un superadministrador.

## Pruebas y mantenimiento

Desde `backend`, `./mvnw test` ejecuta las pruebas Java. Las pruebas de integración utilizan Testcontainers y necesitan Docker. En `frontend` están disponibles:

```bash
npm test
npm run test:coverage
npm run lint
npm run build
```

Las pruebas de interfaz utilizan Vitest, React Testing Library y MSW. Los planes de carga están explicados en [performance/README.md](../performance/README.md). La carga y limpieza de datos de desarrollo se documenta en [scripts/README.md](../scripts/README.md).

La plantilla ODT descargable está en `frontend/public/templates`. `npm run generate:survey-template` la genera mediante `frontend/scripts/generate-survey-import-template-odt.mjs`. Cuando cambie una ruta, una variable o una regla de negocio, también debe revisarse la guía correspondiente de esta carpeta.
