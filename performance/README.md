# Pruebas de rendimiento de FormQuid

## Objetivo y preparación

Medir la API y observar PostgreSQL durante la carga. Usar una instancia aislada con datos sintéticos de volumen y distribución representativos. Registrar versión del backend, esquema, número de filas, CPU, memoria y tamaño del pool Hikari; mantenerlos constantes entre ejecuciones.

## Campaña inicial

El plan `public-read.jmx` consulta el listado público, el detalle de una encuesta publicada y las categorías. Para que el detalle sea válido, indicar un `publicCode` real de la base de pruebas. Comenzar con una prueba de humo de un usuario. Después ejecutar 10 usuarios durante 5 minutos y 25 usuarios durante 15 minutos, con entrada gradual. Repetir cada fase tres veces y ajustar la carga a la capacidad del entorno.

Ejemplo con JMeter actual, desde la raíz del repositorio:

```bash
jmeter -n -t performance/public-read.jmx -Jhost=localhost -Jport=8080 -JpublicCode=CODIGO_DE_PRUEBA -Jusers=10 -Jramp=60 -Jduration=300 -l resultados.jtl -e -o informe-jmeter
```

Cambiar los nombres de salida en cada ejecución. Ejecutar la carga desde otra máquina si el generador compite por CPU o memoria con el backend.

## Métricas y diagnóstico

Registrar solicitudes por segundo, mediana, p95, p99, tasa de error y códigos HTTP por endpoint. En la misma línea temporal observar CPU, memoria e I/O de PostgreSQL, conexiones activas y esperas, Hikari en uso y pendientes, y consultas costosas con `pg_stat_statements` si está disponible. Inspeccionar `EXPLAIN (ANALYZE, BUFFERS)` para consultas lentas de lectura. Definir umbrales de aceptación según objetivos de uso y una línea base medida en este entorno.

## Ampliación de escenarios

- Variar códigos, búsquedas, categorías, páginas y tamaños para evitar que una sola entrada sesgue las medidas.
- Sesiones autenticadas: listados propios, resultados y detalle. Obtener CSRF, hacer login por usuario de prueba, conservar cookies y enviar `X-XSRF-TOKEN` en el POST.
- Escrituras: participaciones y creación/edición de encuestas con datos únicos por usuario virtual, solo en base aislada y restaurada tras cada ejecución.
- Resistencia: mantener al menos una hora de carga estable para detectar degradación acumulada.

El JMeter instalado aquí es 2.13; el plan debe verificarse con una versión actual antes de una campaña real. En este entorno tampoco hay acceso al daemon de Docker ni una instancia de la aplicación disponible, por lo que aún no hay mediciones.

## Referencias

- https://jmeter.apache.org/usermanual/
- https://assertqa.com/blog/database-performance-testing
- https://qapractices.com/checklists/database-performance-testing-checklist/
- https://www.blazemeter.com/blog/jmeter-tutorial
- https://innakozak.medium.com/rest-api-testing-using-apache-jmeter-step-by-step-guide-e2122ece1ea0
