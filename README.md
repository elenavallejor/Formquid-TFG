# SurveyLens - TFG

## Clave JWT del backend

Configura `JWT_SECRET` en el entorno antes de iniciar el backend. Debe ser una clave aleatoria de al menos 32 bytes; por ejemplo, puedes generar una con `openssl rand -hex 32`. La aplicación no arranca si falta, es demasiado corta o coincide con la antigua clave pública del proyecto.

Al cambiar `JWT_SECRET`, todos los tokens de sesión existentes dejan de ser válidos y los usuarios deben iniciar sesión de nuevo. Si se utilizó la antigua clave por defecto en algún despliegue, rota la clave configurada allí.

Al cambiar la contraseña de una cuenta, sus tokens anteriores dejan de ser válidos; la respuesta entrega una cookie nueva para conservar la sesión que hizo el cambio. Los tokens emitidos antes de esta protección también requieren iniciar sesión de nuevo una vez.

## Configuración previa al despliegue

Para preparar el backend con el perfil `prod`, combina `docker-compose.yml` con `docker-compose.prod.yml`. Antes de iniciarlo, configura en `.env` `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD`, `SUPERADMIN_NAME`, `SUPERADMIN_INSTITUTION` y `FRONTEND_ORIGIN` (por ejemplo, `https://app.ejemplo.com`), y ejecuta `python3 check-prod-env.py`. La comprobación no muestra los valores de estas variables.

El perfil `prod` exige estas variables y rechaza valores vacíos o solo espacios al arrancar. `SuperadminInitializer` conserva su comportamiento: solo crea la cuenta cuando no existen usuarios. Cambiar `.env` después no actualiza la contraseña de una cuenta ya creada.

## Migraciones de bases existentes

La migración `V9__allow_multiple_superadmins.sql` se modificó anteriormente para admitir una base sin usuarios. Antes de actualizar una base existente, realiza una copia de seguridad y comprueba su historial:

```sql
SELECT version, description, checksum, success
FROM flyway_schema_history
WHERE version = '9';
```

Si el arranque informa de un checksum distinto en V9, no modifiques de nuevo el archivo ni ejecutes una reparación a ciegas. El historial de Git contiene versiones anteriores con un comentario distinto y con una comprobación que exigía un superadministrador incluso en una base vacía. Compara la versión aplicada con la actual y comprueba que la V9 anterior terminó correctamente y que el esquema contiene los objetos esperados. Después de la copia y esa comprobación, actualiza el checksum registrado con `flyway repair` usando el mismo conjunto de migraciones que desplegarás. Si hay otras diferencias o la migración falló, investiga y corrige el esquema antes de reparar el historial.

La nueva migración V33 protege la existencia de al menos un superadministrador. Se aplica normalmente después de resolver cualquier discrepancia previa del historial. En una base nueva, el inicializador crea el primer usuario y la base establece la garantía en la misma transacción.
