# Contenedores y despliegue

FormQuid dispone de un entorno local con Docker Compose y de una configuración de publicación que separa la interfaz en Vercel y la API en Render. PostgreSQL se ejecuta por separado en ambos casos. Debe ser accesible desde el backend y aceptar conexiones TLS, porque los perfiles `dev` y `prod` utilizan `sslmode=require`.

## Entorno local con Docker Compose

El archivo `docker-compose.yml` reúne el backend, la interfaz de desarrollo y MailHog.

Los servicios incluidos son:

- `backend`, que expone la API en `http://localhost:8080` con el perfil `dev`.
- `frontend`, que sirve la interfaz mediante Vite en `http://localhost:5173`.
- `mailhog`, cuyo buzón web está en `http://localhost:8025` y cuyo SMTP escucha en el puerto 1025.

El backend lee las variables del archivo `.env` de la raíz. La [guía del entorno](04_entorno_y_desarrollo.md#variables-de-entorno) indica cuáles necesita. Una vez preparada la base de datos y la configuración, se inicia el conjunto desde la raíz del repositorio.

```bash
docker compose up --build
```

Para enviar correos reales durante el desarrollo, `docker-compose.brevo.yml` configura el backend para Brevo sin alterar el Compose habitual. Requiere `BREVO_SMTP_LOGIN`, `BREVO_SMTP_KEY` y `BREVO_MAIL_FROM` en el `.env` ignorado por Git. La [guía de Brevo](04_entorno_y_desarrollo.md#correo-en-desarrollo-con-brevo) recoge la preparación y el comando de arranque.

Las portadas nuevas se guardan en PostgreSQL. El volumen `survey-covers` permite leer portadas antiguas que sigan en `/app/data/survey-covers`; `profile-photos` conserva las fotos de perfil en `/app/data/profile-photos`. Las copias de seguridad deben incluir la base de datos y los volúmenes que contengan archivos antiguos o fotos. El correo está desactivado por defecto. Para probar mensajes con MailHog se configuran `MAIL_ENABLED=true` y `MAIL_HOST=mailhog`.

## Perfil de producción del backend

`docker-compose.prod.yml` cambia el perfil del backend a `prod` y exige la URL de origen de la interfaz y los datos del superadministrador inicial. Se combina con el archivo principal de este modo:

```bash
python3 check-prod-env.py
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build
```

La comprobación previa valida las variables obligatorias y exige que `FRONTEND_ORIGIN` sea un origen HTTPS exacto, sin imprimir las claves. El archivo adicional hereda los dos volúmenes del Compose principal. También mantiene el servicio `frontend`, que ejecuta Vite en modo de desarrollo. Por ello, este conjunto prepara el backend con su perfil de producción, pero no configura por sí solo una publicación de la interfaz estática.

El perfil `prod` activa las migraciones Flyway y desactiva la generación del esquema mediante JPA. También marca la cookie JWT como segura. La URL `FRONTEND_ORIGIN` se utiliza para CORS. El navegador y la API deben servirse con la configuración adecuada para esa cookie.

El inicializador solo crea el primer superadministrador cuando la base no contiene usuarios. Cambiar sus variables después no cambia una cuenta ya creada. Antes de aplicar migraciones a una base existente hay que revisar su historial y disponer de una copia de seguridad. La [guía de desarrollo](04_entorno_y_desarrollo.md#migraciones-de-bases-existentes) explica la precaución específica sobre la migración V9.

## Publicación de la aplicación

La configuración actual sitúa la interfaz en Vercel y el backend en Render. En una compilación de producción, el cliente usa `/api/v1` por defecto para llamar a la API. La regla de `frontend/vercel.json` reenvía `/api/*` a `https://formquid-backend.onrender.com/api/*`; otra regla devuelve `index.html` para las rutas de React Router. Si se define `VITE_API_BASE_URL` durante la compilación, su valor sustituye la ruta relativa y debe revisarse junto con esta reescritura. La configuración de Vercel indica que las respuestas de la API reenviadas no se almacenen en su caché de reescrituras.

En Render se configuran las variables del backend, incluidos `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `FRONTEND_ORIGIN` y los cuatro datos obligatorios del superadministrador inicial. `FRONTEND_ORIGIN` debe coincidir con el origen HTTPS desde el que se abre la interfaz. Si se habilita el correo real, sus variables SMTP se configuran también en Render; `docker-compose.brevo.yml` solo afecta a la ejecución local. La [guía del entorno](04_entorno_y_desarrollo.md#frontend-en-vercel-y-backend-en-render) recoge el ejemplo de configuración para Brevo.

La base de datos y las fotos de perfil deben persistir entre despliegues. Los volúmenes con nombre descritos antes pertenecen a Docker Compose y no configuran el almacenamiento de Render. Las portadas nuevas se guardan en PostgreSQL. `SURVEY_COVER_STORAGE_PATH` solo sirve para leer portadas antiguas si sus archivos todavía existen; `PROFILE_PHOTO_STORAGE_PATH` debe apuntar a un directorio persistente para conservar las fotos. Las portadas ya perdidas de un disco efímero deben volver a subirse.

El flujo `.github/workflows/record-deployments.yml` consulta el resultado comunicado por Vercel o Render para un commit y lo registra en GitHub. Este registro identifica qué versión ha sido desplegada y si el proveedor informó éxito o fallo; no sustituye una comprobación funcional de la aplicación publicada.
