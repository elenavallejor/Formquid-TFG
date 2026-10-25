# Documentación de FormQuid

Esta carpeta reúne las guías técnicas y de uso de FormQuid. El diseño relacional preparado al comienzo del TFG cambió durante el desarrollo. Por eso, el modelo de datos se ha contrastado con el código y las migraciones de Flyway.

Se recomienda seguir este recorrido:

1. [Sistema y datos](01_sistema_y_datos.md). Explica los componentes, las tablas y las diferencias con el diseño inicial.
2. [Uso de la aplicación](02_uso_de_la_aplicacion.md). Recorre las tareas disponibles para participantes, editores y administradores.
3. [Servicios de la API](03_api.md). Enumera las rutas que ofrecen los controladores.
4. [Entorno y desarrollo](04_entorno_y_desarrollo.md). Reúne instalación, variables, organización del código y pruebas.
5. [Contenedores y despliegue](05_contenedores.md). Describe Docker Compose y el perfil de producción del backend.

El archivo [openapi.yaml](openapi.yaml) recoge una selección de operaciones principales con sus esquemas de entrada.

Los [README del backend](../backend/README.md) y [frontend](../frontend/README.md) amplían las indicaciones específicas de cada aplicación. Los [scripts](../scripts/README.md) y las [pruebas de rendimiento](../performance/README.md) tienen guías propias.
