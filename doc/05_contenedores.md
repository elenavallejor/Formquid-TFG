# Contenedores y despliegue

El archivo `docker-compose.yml` reúne el backend, la interfaz de desarrollo y MailHog. La base de datos PostgreSQL se contrata o se ejecuta por separado. Debe ser accesible desde el contenedor del backend y aceptar conexiones TLS, porque los perfiles `dev` y `prod` utilizan `sslmode=require`.

Los servicios incluidos son:

- `backend`, que expone la API en `http://localhost:8080` con el perfil `dev`.
- `frontend`, que sirve la interfaz mediante Vite en `http://localhost:5173`.
- `mailhog`, cuyo buzón web está en `http://localhost:8025` y cuyo SMTP escucha en el puerto 1025.

El backend lee las variables del archivo `.env` de la raíz. La [guía del entorno](04_entorno_y_desarrollo.md#variables-de-entorno) indica cuáles necesita. Una vez preparada la base de datos y la configuración, se inicia el conjunto desde la raíz del repositorio.

```bash
docker compose up --build
```

Para enviar correos reales durante el desarrollo, `docker-compose.brevo.yml` configura el backend para Brevo sin alterar el Compose habitual. Requiere `BREVO_SMTP_LOGIN`, `BREVO_SMTP_KEY` y `BREVO_MAIL_FROM` en el `.env` ignorado por Git. La [guía de Brevo](04_entorno_y_desarrollo.md#correo-en-desarrollo-con-brevo) recoge la preparación y el comando de arranque.

El volumen `survey-covers` conserva las portadas que se suben desde la aplicación. Su ruta dentro del contenedor es `/app/data/survey-covers`. Conviene incluir tanto el volumen como la base de datos en las copias de seguridad. El correo está desactivado por defecto. Para probar mensajes con MailHog se configuran `MAIL_ENABLED=true` y `MAIL_HOST=mailhog`.

## Perfil de producción del backend

`docker-compose.prod.yml` cambia el perfil del backend a `prod` y exige la URL de origen de la interfaz y los datos del superadministrador inicial. Se combina con el archivo principal de este modo:

```bash
python3 check-prod-env.py
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build
```

La comprobación previa valida la configuración requerida sin imprimir las claves. El archivo adicional conserva el volumen de portadas. También mantiene el servicio `frontend` del Compose principal, que ejecuta Vite en modo de desarrollo. Por ello, este conjunto prepara el backend con su perfil de producción, pero no configura por sí solo una publicación de la interfaz estática.

El perfil `prod` activa las migraciones Flyway y desactiva la generación del esquema mediante JPA. También marca la cookie JWT como segura. La URL `FRONTEND_ORIGIN` se utiliza para CORS. El navegador y la API deben servirse con la configuración adecuada para esa cookie.

El inicializador solo crea el primer superadministrador cuando la base no contiene usuarios. Cambiar sus variables después no cambia una cuenta ya creada. Antes de aplicar migraciones a una base existente hay que revisar su historial y disponer de una copia de seguridad. La [guía de desarrollo](04_entorno_y_desarrollo.md#migraciones-de-bases-existentes) explica la precaución específica sobre la migración V9.
