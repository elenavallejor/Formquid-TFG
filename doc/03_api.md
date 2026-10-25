# Servicios de la API

El backend expone sus recursos bajo `/api/v1`. Esta guía recoge las rutas definidas en los controladores del proyecto. Los identificadores entre llaves son variables de la URL. `surveyId` es el identificador interno de una encuesta, mientras que `publicCode` es su código para compartir.

El archivo [openapi.yaml](openapi.yaml) describe con más detalle una selección de operaciones principales.

## Acceso a la API

El inicio de sesión devuelve una cookie `HttpOnly` con el JWT. El navegador debe enviar las cookies en las siguientes peticiones. Las operaciones que cambian datos utilizan protección CSRF, por lo que el cliente obtiene el token en `GET /auth/csrf` y lo envía en la cabecera correspondiente. Las rutas de `/public`, la solicitud de registro, el acceso a categorías y las rutas de autenticación indicadas por la configuración de seguridad pueden consultarse sin sesión. El resto requiere autenticación y puede exigir permisos adicionales.

La aplicación devuelve códigos HTTP acordes con el resultado de cada operación. Los controladores y manejadores de errores son la referencia para el formato exacto de las peticiones y respuestas.

## Sesión y cuentas

| Método | Ruta | Función |
| --- | --- | --- |
| `GET` | `/auth/csrf` | Obtener el token CSRF. |
| `POST` | `/auth/login` | Iniciar sesión. |
| `POST` | `/auth/logout` | Cerrar sesión. |
| `POST` | `/auth/password-reset/request` | Solicitar un enlace de recuperación. |
| `POST` | `/auth/password-reset/confirm` | Establecer una nueva contraseña con el token. |
| `POST` | `/registration-requests` | Presentar una solicitud de cuenta. |
| `GET` | `/users/me` | Consultar el perfil propio. |
| `PATCH` | `/users/me/account` | Modificar los datos de la cuenta. |
| `DELETE` | `/users/me` | Eliminar la cuenta propia según las reglas del servicio. |

## Encuestas y contenido

| Método | Ruta | Función |
| --- | --- | --- |
| `GET` | `/surveys`, `/surveys/results`, `/surveys/directory`, `/surveys/registered/page` | Listados para la gestión y participación. |
| `GET` | `/surveys/by-code/{publicCode}` | Localizar una encuesta mediante su código. |
| `POST` | `/surveys` | Crear un borrador. |
| `POST` | `/surveys/from-template/{templateId}` | Crear una encuesta desde una plantilla. |
| `POST` | `/surveys/import/preview` | Previsualizar un archivo ODT. |
| `POST` | `/surveys/from-import` | Crear la encuesta a partir de la importación revisada. |
| `GET`, `PUT`, `DELETE` | `/surveys/{surveyId}` | Consultar, actualizar o eliminar una encuesta. |
| `POST` | `/surveys/{surveyId}/cover` | Subir una portada. |
| `PATCH` | `/surveys/{surveyId}/lifecycle` | Cambiar el estado de la encuesta. |
| `GET` | `/surveys/{surveyId}/results` | Consultar los resultados. |
| `PUT` | `/surveys/{surveyId}/content` | Sustituir el contenido ordenado. |
| `POST` | `/surveys/{surveyId}/content/texts` | Añadir un texto. |
| `POST` | `/surveys/{surveyId}/content/questions/local`, `/surveys/{surveyId}/content/questions/fixed` | Añadir una pregunta local o del catálogo. |
| `POST` | `/surveys/{surveyId}/content/blocks` | Añadir un bloque. |
| `PUT` | `/surveys/{surveyId}/content/texts/{itemId}`, `/surveys/{surveyId}/content/questions/{itemId}` | Editar un texto o una pregunta. |
| `PATCH` | `/surveys/{surveyId}/content/questions/{itemId}/required`, `/surveys/{surveyId}/content/reorder` | Cambiar obligatoriedad u orden. |
| `DELETE` | `/surveys/{surveyId}/content/{itemId}` | Quitar un elemento. |

## Publicación y participación

| Método | Ruta | Función |
| --- | --- | --- |
| `GET` | `/public/surveys/page` | Listar encuestas públicas visibles. |
| `GET` | `/public/surveys/{publicCode}` | Obtener el formulario público. |
| `POST` | `/public/surveys/{publicCode}/access` | Comprobar la contraseña de acceso cuando se necesita. |
| `GET` | `/public/surveys/{surveyId}/cover/{filename}` | Obtener una portada según las reglas de acceso. |
| `POST` | `/public/surveys/{publicCode}/participations` | Enviar una participación desde el recorrido público. |
| `POST` | `/surveys/{publicCode}/participations` | Enviar una participación con sesión. |
| `GET` | `/surveys/{surveyId}/participations` | Consultar las participaciones de la encuesta. |
| `GET` | `/surveys/{surveyId}/participations/{participationId}` | Consultar una participación concreta. |
| `GET` | `/surveys/{surveyId}/statistics` | Obtener estadísticas generales. |
| `GET` | `/surveys/{surveyId}/statistics/questions/{questionId}` | Obtener estadísticas de una pregunta. |

## Colaboración y catálogo

| Método | Ruta | Función |
| --- | --- | --- |
| `GET` | `/surveys/{surveyId}/collaborators`, `/surveys/{surveyId}/collaboration-requests` | Consultar colaboradores o solicitudes de la encuesta. |
| `POST` | `/surveys/{surveyId}/collaboration-requests` | Crear una invitación o solicitud de acceso. |
| `POST` | `/surveys/{surveyId}/collaboration-requests/{requestId}/accept` | Aceptar una solicitud. |
| `DELETE` | `/surveys/{surveyId}/collaboration-requests/{requestId}` | Retirar una solicitud. |
| `PATCH`, `DELETE` | `/surveys/{surveyId}/collaborators/{collaboratorId}` | Cambiar permisos o retirar a un colaborador. |
| `GET`, `POST` | `/surveys/{surveyId}/publication-requests` | Consultar o presentar solicitudes de publicación. |
| `POST` | `/surveys/{surveyId}/publication-requests/{requestId}/accept` | Aceptar una solicitud de publicación. |
| `DELETE` | `/surveys/{surveyId}/publication-requests/{requestId}` | Retirar una solicitud de publicación. |
| `GET` | `/collaboration-requests` | Consultar solicitudes dirigidas al usuario. |
| `GET`, `POST` | `/catalog/questions`, `/catalog/blocks`, `/catalog/templates` | Listar o crear elementos del catálogo. |
| `GET` | `/catalog/questions/builder`, `/catalog/blocks/builder` | Obtener elementos para el editor. |
| `GET`, `PUT`, `DELETE` | `/catalog/questions/{questionId}`, `/catalog/blocks/{blockId}`, `/catalog/templates/{templateId}` | Consultar, modificar o eliminar elementos. |
| `POST` | `/element-requests` | Proponer un elemento del catálogo. |
| `GET` | `/categories` | Consultar las categorías. |

## Administración y avisos

| Método | Ruta | Función |
| --- | --- | --- |
| `GET` | `/admin/dashboard`, `/admin/surveys` | Consultar el panel y las encuestas administradas. |
| `PATCH` | `/admin/surveys/{surveyId}/lifecycle`, `/admin/surveys/{surveyId}/visibility` | Cambiar estado o visibilidad desde administración. |
| `DELETE` | `/admin/surveys/{surveyId}` | Eliminar una encuesta desde administración. |
| `GET` | `/admin/registration-requests`, `/admin/element-requests` | Consultar solicitudes pendientes. |
| `POST` | `/admin/registration-requests/{requestId}/approve`, `/admin/element-requests/{requestId}/approve` | Aprobar solicitudes. |
| `DELETE` | `/admin/registration-requests/{requestId}`, `/admin/element-requests/{requestId}` | Rechazar solicitudes. |
| `GET` | `/users` | Listar usuarios. |
| `PATCH` | `/users/{userId}/role` | Cambiar el rol de un usuario. |
| `DELETE` | `/users/{userId}` | Eliminar un usuario. |
| `POST` | `/users/surveys/{surveyId}/transfer-ownership` | Transferir la propiedad de una encuesta. |
| `POST` | `/categories` | Crear una categoría. |
| `PATCH` | `/categories/{code}/icon` | Actualizar su icono. |
| `GET` | `/notifications`, `/notifications/unread-count`, `/notifications/{notificationId}/destination` | Consultar avisos y su destino. |
| `PATCH` | `/notifications/{notificationId}/read`, `/notifications/read-all` | Marcar avisos como leídos. |
| `DELETE` | `/notifications/{notificationId}` | Eliminar un aviso. |

Las comprobaciones de rol y propiedad se encuentran en los controladores y servicios. Para integrar un cliente nuevo conviene revisar los DTO de cada paquete, porque esta guía está orientada a localizar las operaciones y no sustituye sus contratos Java.
