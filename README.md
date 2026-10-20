# SurveyLens - TFG

## Clave JWT del backend

Configura `JWT_SECRET` en el entorno antes de iniciar el backend. Debe ser una clave aleatoria de al menos 32 bytes; por ejemplo, puedes generar una con `openssl rand -hex 32`. La aplicación no arranca si falta, es demasiado corta o coincide con la antigua clave pública del proyecto.

Al cambiar `JWT_SECRET`, todos los tokens de sesión existentes dejan de ser válidos y los usuarios deben iniciar sesión de nuevo. Si se utilizó la antigua clave por defecto en algún despliegue, rota la clave configurada allí.

Al cambiar la contraseña de una cuenta, sus tokens anteriores dejan de ser válidos; la respuesta entrega una cookie nueva para conservar la sesión que hizo el cambio. Los tokens emitidos antes de esta protección también requieren iniciar sesión de nuevo una vez.

## Configuración previa al despliegue

Para preparar el backend con el perfil `prod`, combina `docker-compose.yml` con `docker-compose.prod.yml`. Antes de iniciarlo, configura en `.env` `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD`, `SUPERADMIN_NAME` y `SUPERADMIN_INSTITUTION`, y ejecuta `python3 check-prod-env.py`. La comprobación no muestra los valores de estas variables.

El perfil `prod` exige estas variables y rechaza valores vacíos o solo espacios al arrancar. `SuperadminInitializer` conserva su comportamiento: solo crea la cuenta cuando no existen usuarios. Cambiar `.env` después no actualiza la contraseña de una cuenta ya creada.
