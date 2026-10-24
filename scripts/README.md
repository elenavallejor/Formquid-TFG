# Reiniciar los datos de desarrollo

`reset-initizalice-DB.sh` borra los datos de prueba y vuelve a cargar usuarios, catálogo y encuestas. Úsalo solo con una base de desarrollo. El script SQL es independiente de Flyway y no se ejecuta al arrancar el backend.

## Antes de ejecutarlo

La base debe tener las migraciones aplicadas hasta V40 y un superadministrador registrado en `superadmin_guard`. Necesitas una cuenta de PostgreSQL con permisos de escritura y `psql` o Docker.

Comprueba a qué base apuntan `DB_HOST`, `DB_PORT` y `DB_NAME` en el `.env` de la raíz. El script también necesita `DB_USERNAME` y `DB_PASSWORD`. Si ya has exportado alguna de esas variables, su valor tiene prioridad sobre el del archivo.

## Ejecutarlo

Desde la raíz del proyecto:

```bash
./scripts/reset-initizalice-DB.sh --confirm-reset
```

El script usa `psql` si está instalado. Si no, ejecuta el cliente de PostgreSQL en Docker. Envía toda la carga en una transacción: si falla una consulta o una comprobación final, PostgreSQL deshace los cambios.

## Qué datos reemplaza

Se eliminan las encuestas y respuestas anteriores, el contenido del catálogo, las solicitudes, las notificaciones y los tokens de restablecimiento. El superadministrador se recrea con ID 1, pero conserva nombre, institución, correo y contraseña. Los demás usuarios reciben los IDs del 2 al 13.

La carga crea estos datos:

| Dato | Cantidad |
| --- | ---: |
| Administradores | 2 |
| Editores | 10 |
| Preguntas fijas del catálogo | 136 |
| Bloques y plantillas del catálogo | 6 bloques y 4 plantillas |
| Encuestas | 30: 8 borradores, 18 abiertas y 4 cerradas |
| Solicitudes de registro | 2 |
| Solicitudes de elementos | 3 |
| Solicitudes de colaboración | 4 |
| Solicitudes de publicación | 2 |

Los doce usuarios nuevos tienen correos `@gmail.com` e instituciones de distintas provincias españolas, como la Universidad de Granada o la Universidad de Sevilla. Su contraseña de prueba es `FormQuidDemo2026!`

Cada encuesta incluye preguntas del catálogo, preguntas propias y dos preguntas personales opcionales. Las cinco encuestas sobre gafas inteligentes siguen los escenarios de los formularios de referencia y tienen diez preguntas, las otras 25 tienen seis. No se cargan respuestas de participantes.
