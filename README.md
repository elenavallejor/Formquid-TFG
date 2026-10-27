<p align="center">
  <img src="frontend/src/assets/images/formquid-logo-transparent.png" alt="Logo de FormQuid" width="120">
</p>

<h1 align="center">FormQuid</h1>

<p align="center">
  Crea encuestas, trabaja en equipo y conoce los resultados.
</p>

<p align="center">
  <a href="#presentación">Presentación</a> ·
  <a href="#funcionalidades">Funcionalidades</a> ·
  <a href="#puesta-en-marcha">Puesta en marcha</a> ·
  <a href="#documentación">Documentación</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/React-19-149eca?logo=react&logoColor=white" alt="React 19">
  <img src="https://img.shields.io/badge/Java-21-ed8b00" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-4-6db33f?logo=springboot&logoColor=white" alt="Spring Boot 4">
  <img src="https://img.shields.io/badge/PostgreSQL-4169e1?logo=postgresql&logoColor=white" alt="PostgreSQL">
</p>

<p align="center">
  <img src="frontend/src/assets/images/mainpage.png" alt="Página principal de FormQuid con el buscador y el listado de encuestas" width="100%">
</p>

## Presentación

FormQuid es una aplicación web para crear, publicar y responder encuestas de forma colaborativa. Este repositorio recoge el desarrollo del Trabajo de Fin de Grado, desde la interfaz y la API hasta el modelo de datos, las pruebas y las herramientas de preparación del entorno.

Las encuestas se pueden construir desde cero, a partir de elementos del catálogo o mediante una plantilla ODT. Una vez publicadas, las personas destinatarias responden según el modo de acceso elegido. Los responsables de la encuesta pueden consultar las respuestas individuales y las estadísticas agregadas.

**Contenido**

- [Funcionalidades](#funcionalidades).
- [Tecnologías y organización](#tecnologías-y-organización).
- [Puesta en marcha](#puesta-en-marcha).
- [Pruebas](#pruebas).
- [Configuración y mantenimiento](#configuración-y-mantenimiento).
- [Documentación](#documentación).

## Funcionalidades

### Crear y publicar encuestas

- **Contenido.** Textos, preguntas y bloques que se pueden ordenar en el formulario.
- **Tipos de pregunta.** Texto, número, opción única, opción múltiple, escala, matriz de escala y dos opciones.
- **Presentación.** Portada, tipografía, categoría y etiquetas.
- **Estados.** Borrador, abierta y cerrada.
- **Acceso.** Público, con contraseña o reservado a usuarios autenticados.
- **Visibilidad.** Se configura por separado para decidir si la encuesta aparece en los listados.
- **Difusión.** Cada encuesta abierta puede compartirse mediante su código público.

### Responder y consultar resultados

- El formulario comprueba las respuestas obligatorias.
- Se evita que una misma identidad participe dos veces en la misma encuesta.
- Los resultados incluyen el detalle de cada participación y estadísticas por pregunta.
- Las preguntas evaluables con peso permiten calcular un análisis global ponderado.

### Reutilizar contenido y trabajar en equipo

- El catálogo reúne preguntas, bloques y plantillas para preparar nuevas encuestas.
- Los editores pueden proponer elementos. Las personas administradoras revisan las propuestas y gestionan el catálogo publicado.
- Los colaboradores pueden tener permiso de lectura o edición.
- La aplicación permite enviar invitaciones, solicitudes de acceso y solicitudes de publicación.
- Cuando un colaborador solicita publicar, el propietario decide si acepta la petición.
- Las notificaciones informan de estas solicitudes y de las acciones relacionadas con las cuentas.

### Gestionar cuentas

Las cuentas nuevas se solicitan desde la web y necesitan aprobación administrativa. Cada usuario puede consultar y modificar su perfil, además de recuperar su contraseña por correo electrónico.

| Rol | Funciones de gestión |
| --- | --- |
| Editor | Crear encuestas, colaborar y proponer elementos para el catálogo. |
| Administrador | Revisar solicitudes, gestionar el catálogo y supervisar las encuestas de la plataforma. |
| Superadministrador | Gestionar además las cuentas, los roles y las categorías. |

## Tecnologías y organización

| Componente | Tecnologías | Ubicación |
| --- | --- | --- |
| Interfaz | React 19, React Router 7 y Vite 8 | [frontend](frontend/README.md) |
| API | Java 21, Spring Boot 4, Spring Security y Spring Data JPA | [backend](backend/README.md) |
| Datos | PostgreSQL y migraciones Flyway | [Modelo de datos](doc/01_sistema_y_datos.md#modelo-de-datos) |
| Entorno de desarrollo | Docker Compose y MailHog | [Contenedores](doc/05_contenedores.md) |
| Preparación de datos | Scripts de desarrollo | [scripts](scripts/README.md) |
| Rendimiento | Planes JMeter | [performance](performance/README.md) |

Docker Compose reúne el backend, la interfaz de desarrollo y MailHog. **PostgreSQL debe estar disponible por separado**, porque los archivos Compose del repositorio no crean un contenedor para la base de datos.

## Puesta en marcha

### 1. Preparar el entorno

Se necesitan Docker Compose y una instancia de PostgreSQL accesible desde el backend. Los perfiles `dev` y `prod` utilizan `sslmode=require`, por lo que la base debe aceptar conexiones TLS.

Crea un archivo `.env` en la raíz del repositorio. Está excluido de Git. Configura estas variables:

| Grupo | Variables |
| --- | --- |
| Conexión a PostgreSQL | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` |
| Firma de sesiones | `JWT_SECRET`, obligatorio y de al menos 32 bytes |
| Primera cuenta | `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD`, `SUPERADMIN_NAME`, `SUPERADMIN_INSTITUTION` |

Los datos de la primera cuenta permiten sustituir los valores predeterminados de desarrollo. Para generar una clave JWT aleatoria puedes utilizar:

```bash
openssl rand -hex 32
```

### 2. Iniciar los servicios

Desde la raíz del repositorio:

```bash
docker compose up --build
```

| Servicio | Dirección local |
| --- | --- |
| Interfaz | [localhost:5173](http://localhost:5173) |
| API | `http://localhost:8080/api/v1` |
| Buzón de pruebas MailHog | [localhost:8025](http://localhost:8025) |

Durante el arranque:

- Flyway aplica las migraciones con el perfil `dev`.
- El inicializador crea el primer superadministrador si la base todavía no contiene usuarios.
- Cambiar después las variables de esa cuenta no modifica una cuenta existente.
- Los datos de demostración se cargan por separado, siguiendo el [README de scripts](scripts/README.md).

### 3. Probar el correo o ejecutar en local

El envío de correo está desactivado por defecto. Para probarlo con MailHog, añade a `.env`:

```dotenv
MAIL_ENABLED=true
MAIL_HOST=mailhog
```

Para enviar correos reales en desarrollo con Brevo, crea un remitente verificado en Brevo y una clave **SMTP**. Añade a `.env` (excluido de Git):

```dotenv
BREVO_SMTP_LOGIN=tu-login-smtp
BREVO_SMTP_KEY=tu-clave-smtp
BREVO_MAIL_FROM=notificaciones@tu-dominio.example
```

Arranca con el archivo opcional de Brevo:

```bash
docker compose -f docker-compose.yml -f docker-compose.brevo.yml up --build
```

Este archivo configura el backend para `smtp-relay.brevo.com:587`, con autenticación y STARTTLS obligatorio. El arranque habitual con `docker compose up --build` sigue usando la configuración local de MailHog. El login SMTP y la clave SMTP se obtienen en **Settings → SMTP & API** de Brevo; una clave API no sirve como contraseña SMTP. Consulta la [guía del entorno](doc/04_entorno_y_desarrollo.md#correo-en-desarrollo-con-brevo) para la ejecución fuera de Compose.

También se pueden arrancar ambos proyectos sin Compose. Los README del [backend](backend/README.md#configuración-y-arranque) y del [frontend](frontend/README.md#desarrollo-local) explican los comandos. La [guía del entorno](doc/04_entorno_y_desarrollo.md) reúne la instalación y todas las variables.

## Pruebas

### Backend

Desde `backend`:

```bash
./mvnw test
```

Las pruebas que utilizan PostgreSQL levantan un contenedor mediante Testcontainers y necesitan Docker disponible.

### Frontend

Desde `frontend`:

```bash
npm test
npm run test:coverage
npm run lint
npm run build
```

- `npm test` ejecuta las pruebas con Vitest.
- `npm run test:coverage` genera el informe de cobertura.
- `npm run lint` comprueba el código con ESLint.
- `npm run build` genera la compilación de la interfaz.

### Rendimiento

El repositorio incluye planes JMeter para operaciones de lectura y escritura de la API. La [guía de rendimiento](performance/README.md) explica cómo preparar los datos y cómo interpretar las mediciones y sus límites.

## Configuración y mantenimiento

Los siguientes apartados reúnen la información necesaria para configurar sesiones, correo, despliegues y bases existentes.

<details>
<summary><strong>Clave JWT y cambios de contraseña</strong></summary>

- `JWT_SECRET` debe contener una clave aleatoria de al menos 32 bytes.
- El backend no arranca si la clave falta, es demasiado corta o coincide con la antigua clave pública del proyecto.
- Cambiar la clave invalida los tokens de sesión existentes. Los usuarios deben iniciar sesión de nuevo.
- Si un despliegue utilizó la antigua clave predeterminada, hay que sustituirla.
- Cambiar la contraseña de una cuenta invalida sus tokens anteriores. La respuesta entrega una cookie nueva para conservar la sesión desde la que se hizo el cambio.
- Los tokens emitidos antes de incorporar esa protección también requieren un nuevo inicio de sesión.

</details>

<details>
<summary><strong>Recuperación de contraseña</strong></summary>

El enlace «¿Has olvidado tu contraseña?» permite solicitar un correo de recuperación.

- El token es de un solo uso y caduca a los 30 minutos.
- La base de datos guarda únicamente su hash.
- Una nueva solicitud invalida el enlace anterior.
- El formulario muestra la misma confirmación tanto si la cuenta existe como si no.

Para enviar los mensajes:

1. Activa `MAIL_ENABLED=true` y configura los parámetros SMTP.
2. Define `FRONTEND_ORIGIN` o utiliza `PASSWORD_RESET_BASE_URL` si el enlace necesita una URL distinta.
3. En desarrollo, la dirección predeterminada es `http://localhost:5173`. Los mensajes locales se consultan en MailHog, en `http://localhost:8025`.

</details>

<details>
<summary><strong>Preparación del perfil de producción</strong></summary>

`docker-compose.prod.yml` modifica el backend al combinarse con `docker-compose.yml`. La interfaz de ese conjunto sigue usando el servidor de desarrollo de Vite.

1. Configura en `.env` los cuatro datos de `SUPERADMIN` y `FRONTEND_ORIGIN`, por ejemplo, `https://app.ejemplo.com`.
2. Ejecuta la comprobación previa. No imprime los valores de las variables.

   ```bash
   python3 check-prod-env.py
   ```

3. Combina los dos archivos Compose para iniciar el backend con el perfil `prod`.

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build
   ```

El perfil `prod` exige esas variables y rechaza valores vacíos o compuestos solo por espacios. El inicializador mantiene su comportamiento: crea la cuenta únicamente si no existen usuarios. Cambiar `.env` después no actualiza su contraseña.

La [guía de contenedores](doc/05_contenedores.md) amplía la configuración del despliegue.

</details>

<details>
<summary><strong>Portadas y copias de seguridad</strong></summary>

- Las portadas subidas se guardan en `/app/data/survey-covers` y las fotos de perfil en `/app/data/profile-photos` dentro del backend.
- Docker Compose monta los volúmenes persistentes `survey-covers` y `profile-photos` en esas rutas. La configuración de producción reutiliza los montajes.
- Las copias de seguridad deben incluir PostgreSQL y el volumen de portadas.
- Al retirar los contenedores, conserva el volumen. El comando `docker compose down -v` también lo elimina.
- Fuera de Compose, monta almacenamiento persistente y configura sus rutas en `SURVEY_COVER_STORAGE_PATH` y `PROFILE_PHOTO_STORAGE_PATH`.
- Se admiten imágenes PNG y JPEG de hasta 3 MB y 4000 píxeles por lado.
- Las portadas de borradores y de encuestas con acceso restringido requieren autorización para leerse.

</details>

<details>
<summary><strong>Migraciones de bases existentes</strong></summary>

La migración `V9__allow_multiple_superadmins.sql` se modificó anteriormente para admitir una base sin usuarios. Antes de actualizar una base existente:

1. Realiza una copia de seguridad.
2. Comprueba el historial de V9.

   ```sql
   SELECT version, description, checksum, success
   FROM flyway_schema_history
   WHERE version = '9';
   ```

3. Si el arranque informa de un checksum distinto, compara la versión aplicada con la actual. El historial de Git conserva versiones con un comentario distinto y con una comprobación que exigía un superadministrador incluso en una base vacía.
4. Confirma que la V9 anterior terminó correctamente y que el esquema contiene los objetos esperados.
5. Después de la copia y esas comprobaciones, actualiza el checksum registrado mediante `flyway repair`, usando el mismo conjunto de migraciones que desplegarás.

Si hay otras diferencias o la migración falló, investiga y corrige el esquema antes de reparar el historial. Evita volver a modificar V9 o reparar sin haber comprobado la causa.

La migración V33 protege la existencia de al menos un superadministrador. Se aplica normalmente después de resolver las discrepancias previas del historial. En una base nueva, el inicializador crea el primer usuario y la base establece la garantía en la misma transacción.

</details>

## Documentación

| Guía | Contenido |
| --- | --- |
| [Sistema y datos](doc/01_sistema_y_datos.md) | Arquitectura, tablas, relaciones y evolución del diseño inicial. |
| [Uso de la aplicación](doc/02_uso_de_la_aplicacion.md) | Recorrido por las funciones de participantes, editores y administradores. |
| [API](doc/03_api.md) | Rutas de los controladores y operaciones disponibles. |
| [Entorno y desarrollo](doc/04_entorno_y_desarrollo.md) | Instalación, variables, organización del código y pruebas. |
| [Contenedores y despliegue](doc/05_contenedores.md) | Servicios Compose y perfil de producción del backend. |
| [OpenAPI](doc/openapi.yaml) | Especificación resumida de las operaciones principales. |

También puedes consultar el [índice de documentación](doc/README.md) y los README específicos de [backend](backend/README.md), [frontend](frontend/README.md), [scripts](scripts/README.md) y [rendimiento](performance/README.md).
