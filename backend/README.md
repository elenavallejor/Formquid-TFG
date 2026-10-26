# Backend de FormQuid

El backend expone la API REST de FormQuid y concentra las reglas de gestión de usuarios, encuestas, participación y resultados. Está desarrollado con Java 21 y Spring Boot 4. Utiliza Spring Security para controlar el acceso, Spring Data JPA para la persistencia y Flyway para mantener el esquema de PostgreSQL.

## Organización del código

El código principal está en `src/main/java/com/formquid`. Cada área funcional reúne sus controladores, servicios, entidades, repositorios y objetos de entrada o respuesta. Los paquetes `survey`, `content`, `question`, `block` y `template` resuelven la construcción de encuestas y la reutilización de elementos del catálogo. Los paquetes `participation` y `statistics` procesan las respuestas y ofrecen las consultas de resultados. `collaboration`, `registration`, `elementrequest` y `notification` cubren las solicitudes y los avisos asociados. `auth`, `user` y `config` contienen la autenticación, la gestión de cuentas y la configuración común.

La API comienza en `/api/v1`. Las rutas `/public/surveys` permiten consultar las encuestas visibles y responder sin iniciar sesión cuando el modo de acceso lo permite. Las rutas `/surveys` reúnen la creación, edición, importación y consulta de encuestas. `/catalog` ofrece preguntas, bloques y plantillas. Los controladores de administración están bajo `/admin`, mientras que `/users` y `/auth` gestionan las cuentas y la autenticación.

## Reglas principales

Una encuesta se crea como borrador y puede pasar a abierta o cerrada. El modo de acceso puede ser público, con contraseña o para usuarios autenticados. La visibilidad determina si aparece en los listados. El código público identifica la dirección de acceso a la encuesta.

El contenido admite textos, preguntas y bloques ordenados. Hay siete tipos de pregunta: texto, número, opción única, opción múltiple, escala, matriz de escala y dos opciones. Las plantillas y los elementos fijos del catálogo permiten reutilizar estructuras al crear encuestas. También se puede importar una encuesta desde la plantilla ODT incluida en el repositorio. El servidor valida el documento y genera primero una vista previa. El archivo admitido tiene un tamaño máximo de 5 MB.

Al recibir una participación, el servicio comprueba el acceso, la pertenencia de cada pregunta a la encuesta, las respuestas obligatorias y la identidad del participante. La base de datos incluye índices únicos para impedir participaciones repetidas. Los propietarios y usuarios con permiso pueden consultar las participaciones y las estadísticas por encuesta o por pregunta. Las preguntas evaluables pueden contribuir, según su peso, a un índice global ponderado.

Las cuentas nuevas se tramitan como solicitudes de registro. La aprobación crea un usuario con rol `EDITOR`. Los roles `ADMIN` y `SUPERADMIN` gestionan solicitudes y encuestas de la plataforma. El superadministrador puede gestionar usuarios y categorías. Las colaboraciones en encuestas distinguen permisos de lectura y edición.

## Base de datos y archivos

Las migraciones están en `src/main/resources/db/migration`. Los perfiles `dev` y `prod` ejecutan Flyway al arrancar. En `dev`, Hibernate valida el esquema resultante. El perfil `prod` deja los cambios de estructura a Flyway.

Las portadas subidas se guardan fuera de la imagen del contenedor. El servicio acepta PNG y JPEG de hasta 3 MB y 4000 píxeles por lado. Con Docker Compose, el volumen `survey-covers` conserva esos archivos en `/app/data/survey-covers`. La ruta puede cambiarse mediante `SURVEY_COVER_STORAGE_PATH` cuando el backend se ejecuta de otra forma.

La [guía de desarrollo](../doc/04_entorno_y_desarrollo.md#migraciones-de-bases-existentes) explica qué comprobar si una base antigua presenta una discrepancia de checksum en la migración V9. Para las copias de seguridad deben conservarse tanto PostgreSQL como el volumen de portadas.

## Configuración y arranque

Los perfiles `dev` y `prod` necesitan `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD`. La URL JDBC de ambos exige una conexión TLS. `JWT_SECRET` es obligatorio y debe contener al menos 32 bytes. El primer superadministrador se crea con los valores `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD`, `SUPERADMIN_NAME` y `SUPERADMIN_INSTITUTION` cuando la base todavía no contiene usuarios. En producción estos valores son obligatorios. `FRONTEND_ORIGIN` fija el origen permitido para la interfaz en ese perfil.

Con Java 21, PostgreSQL disponible y las variables exportadas en el entorno, el servidor puede iniciarse desde esta carpeta:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

También puede arrancarse desde la raíz con `docker compose up --build`. En ese caso, Docker Compose lee el archivo `.env` de la raíz. El backend atiende por defecto en el puerto 8080.

La entrega de correo se habilita con `MAIL_ENABLED=true`. El servidor SMTP se configura con `MAIL_HOST` y `MAIL_PORT`; `MAIL_USERNAME` y `MAIL_PASSWORD` aportan las credenciales cuando se necesita autenticación. `MAIL_SMTP_AUTH` y `MAIL_STARTTLS` controlan autenticación y STARTTLS obligatorio. El Compose habitual dirige los mensajes a MailHog; `docker-compose.brevo.yml` permite usar Brevo en desarrollo con una clave SMTP y un remitente verificado, sin modificar el envío existente. La recuperación de contraseña utiliza `FRONTEND_ORIGIN` o `PASSWORD_RESET_BASE_URL` para construir el enlace. Cada enlace es de un solo uso y caduca a los 30 minutos.

La sesión se identifica mediante un JWT almacenado en una cookie `HttpOnly`. Las operaciones que modifican datos utilizan protección CSRF y las contraseñas se almacenan con BCrypt. Cambiar `JWT_SECRET` invalida las sesiones emitidas con la clave anterior. La configuración de producción marca la cookie de sesión como segura.

## Pruebas

Las pruebas están en `src/test/java`. Cubren servicios, repositorios, controladores, seguridad, migraciones y flujos de participación. Las pruebas de integración levantan PostgreSQL con Testcontainers, por lo que necesitan Docker. Para ejecutarlas desde esta carpeta:

```bash
./mvnw test
```

La carga de datos de desarrollo se documenta en [scripts/README.md](../scripts/README.md) y los escenarios JMeter en [performance/README.md](../performance/README.md).
