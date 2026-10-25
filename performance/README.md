# Pruebas de rendimiento de Formquid

## Alcance

`plans/public-read.jmx` mide tres operaciones de lectura de la API: listado público paginado, detalle de una encuesta pública y categorías. El plan usa una pausa de un segundo antes de cada petición y verifica el código HTTP 200. No mide la interfaz del navegador ni las escrituras. La carga de desarrollo actual contiene 30 encuestas y ninguna participación; sirve para comprobar el plan, pero no para sacar conclusiones sobre consultas con tablas grandes, resultados o contención por escrituras.

La carpeta se organiza así: `plans/` contiene los planes JMeter, `data/` los CSV de entrada y `sql/` las consultas de observación. Los resultados generados permanecen en `resultados-rendimiento/` en la raíz del proyecto y están excluidos de Git.

## Preparación

1. Arrancar una instancia aislada del backend y PostgreSQL con las migraciones aplicadas. Si se utiliza `scripts/reset-initizalice-DB.sh --confirm-reset`, hacerlo **solo** sobre la base de desarrollo designada: el script elimina y recrea sus datos.
2. Verificar que `GET /api/v1/public/surveys/page?page=0&size=9` devuelve 200 y contenido. `performance/data/public-codes.csv` contiene los diez códigos de las encuestas públicas y abiertas que genera ese script. Para otra base, crear un CSV sin cabecera con códigos válidos y pasarlo mediante `-JcodesFile=/ruta/absoluta/codigos.csv`.
3. Usar una versión reciente de JMeter y ejecutar en modo CLI. El JMeter 2.13 instalado en este entorno es antiguo y no sirve para validar una campaña actual. Reservar CPU y memoria suficientes para el generador; si compite con el backend, ejecutarlo desde otra máquina.
4. Registrar antes de cada fase: commit, versión de JMeter, perfil de Spring, recursos de backend y PostgreSQL, tamaño del pool Hikari (`DB_POOL_MAX_SIZE`, por defecto 5), número de filas por tabla principal y configuración del generador. Mantener el mismo conjunto de datos al comparar ejecuciones.

Ejemplo desde la raíz del repositorio, con una instancia de pruebas en `localhost:8080`:

```bash
jmeter -n -t performance/plans/public-read.jmx \
  -Jhost=localhost -Jport=8080 -Jprotocol=http \
  -Jusers=1 -Jramp=1 -Jduration=60 \
  -l /ruta/de/resultados/humo.jtl -e -o /ruta/de/resultados/humo-html
```

Los archivos JTL y la carpeta HTML de cada ejecución deben ser nuevos. JMeter rechaza un directorio de informe que ya contiene archivos. Revisar el JTL antes de interpretar percentiles: si hay códigos inesperados o fallos de aserción, la medición no es válida.

## Escenarios adicionales

Los nuevos planes usan el mismo `host`, `port`, `protocol`, `users` y `ramp`. Ejecutar cada uno por separado y guardar su JTL e informe HTML con nombres diferentes.

| Plan | Operaciones | Datos necesarios |
| --- | --- | --- |
| `plans/public-filters.jmx` | Búsqueda pública y filtrado por categoría y acceso público | `data/public-filters.csv` de la carga de desarrollo |
| `plans/authenticated-read.jmx` | CSRF, login, encuestas propias, detalle, directorio, listado registrado y listado de resultados | `data/editor-emails.csv` y contraseña de las cuentas de prueba |
| `plans/participation-write.jmx` | CSRF, detalle y envío de una participación anónima válida | `data/public-codes.csv` y base aislada que pueda restaurarse |
| `plans/survey-edit.jmx` | CSRF, login, creación de borrador, estructura inicial, guardado de estructura, guardado de metadatos y lectura de verificación | `data/editor-emails.csv`, contraseña de las cuentas de prueba y base aislada |
| `plans/individual-results-read.jmx` | CSRF, login, apertura de resultados, estadísticas, paginación y detalle de una participación | Una encuesta con participaciones y la cuenta de su propietario (`data/results-owner-email.csv` en la carga de desarrollo) |

El plan de filtros comprueba que las búsquedas devuelven contenido además de HTTP 200. Los planes autenticado y de escritura usan claves de compilación distintas para cada script Groovy. Los tres se depuran primero con un usuario; la carga se ejecuta en CLI. No se usa un límite de duración por petición hasta definir el objetivo de rendimiento del proyecto.

El plan autenticado lee la contraseña de la propiedad JMeter `password`; no se guarda en Git. Crear un archivo de propiedades **fuera del repositorio** con permisos solo para el propietario y una línea `password=CONTRASEÑA_DE_PRUEBA`. Pasarlo a JMeter con `-q /ruta/privada/jmeter.properties`; borrarlo al terminar. El CSV contiene solo correos de los editores de desarrollo. Cada usuario virtual inicia sesión una vez y conserva sus cookies durante el test. El listado de resultados estará vacío hasta que existan participaciones.

`survey-edit.jmx` usa la misma propiedad `password`. Cada usuario virtual crea un borrador distinto; luego guarda una estructura inicial y mide su sustitución, la actualización de metadatos y la lectura que comprueba que los cambios persisten. El plan actualiza el token CSRF cuando el servidor renueva la cookie tras una escritura. La preparación tiene etiquetas separadas de las operaciones medidas. Ejecutar con `-Jiterations=1` para guardar una vez por borrador. Por ejemplo:

```bash
jmeter -n -t performance/plans/survey-edit.jmx \
  -q /ruta/privada/jmeter.properties \
  -Jusers=10 -Jramp=1 -Jiterations=1 -Jthink=0 \
  -l resultados-rendimiento/edicion-10-r1.jtl \
  -e -o resultados-rendimiento/edicion-10-r1-html
```

La creación y el guardado persisten encuestas de prueba: restaurar la base aislada al acabar. `-Jthink` expresa en milisegundos la pausa entre peticiones; por defecto es 1000 y en el ejemplo se desactiva para medir una ráfaga. Usar nombres nuevos para cada JTL e informe.

`individual-results-read.jmx` reproduce la consulta de resultados de un editor. Alterna entre las páginas 0, 1, 10, 50 y 90, de diez participaciones, y abre un detalle elegido dentro de la página devuelta. Valida el identificador de la encuesta, el número total de participaciones y el contenido del detalle. Preparar primero una encuesta con al menos 910 participaciones; el caso de referencia usa la encuesta 21 de la carga de desarrollo con 1000. Indicar el identificador real mediante `-JsurveyId` si es distinto. Ejemplo:

```bash
jmeter -n -t performance/plans/individual-results-read.jmx \
  -q /ruta/privada/jmeter.properties \
  -JsurveyId=21 -JexpectedCount=1000 \
  -Jusers=10 -Jramp=30 -Jduration=180 -Jthink=1000 \
  -l resultados-rendimiento/resultados-individuales-10-r1.jtl \
  -e -o resultados-rendimiento/resultados-individuales-10-r1-html
```

El CSV debe contener una cuenta con permiso para ver los resultados de la encuesta. El plan permite cambiarlo con `-JusersFile` y el tamaño de página con `-JpageSize`; al cambiar el tamaño, ajustar también las páginas del plan para que todas existan.

Comenzar cada plan con un usuario y revisar que todas las muestras devuelven el HTTP esperado. Para filtros y lecturas autenticadas, usar `-Jduration=60` en el humo; después se pueden repetir las fases de 10 y 25 usuarios. En `participation-write.jmx`, cada usuario virtual envía una participación por iteración; el valor inicial es `-Jiterations=1`. **Ejecutarlo solo contra una base de pruebas aislada:** cada POST persiste datos. Para aumentar la carga, subir `-Jusers` y `-Jiterations` de forma controlada, registrar el número de filas antes y después, y restaurar la base entre repeticiones comparables. El plan limpia la cookie del participante al inicio de cada iteración para crear identidades de prueba distintas.

Ejemplo de humo para filtros:

```bash
jmeter -n -t performance/plans/public-filters.jmx -Jusers=1 -Jramp=1 -Jduration=60 \
  -l resultados-rendimiento/filtros-humo.jtl -e -o resultados-rendimiento/filtros-humo-html
```

Para los otros planes, cambiar `-t`, los nombres de salida y, en el autenticado, añadir `-q /ruta/privada/jmeter.properties`. Para participaciones, usar `-Jiterations=1` en lugar de `-Jduration`. Se pueden cambiar los CSV con `-JfiltersFile`, `-JusersFile` y `-JcodesFile` respectivamente; el archivo de códigos debe contener encuestas abiertas con acceso público.

## Campaña inicial

| Fase | Usuarios | Entrada gradual | Duración | Propósito |
| --- | ---: | ---: | ---: | --- |
| Humo | 1 | 1 s | 1 min | Confirmar rutas, CSV y respuestas 200 |
| Carga baja | 10 | 60 s | 5 min | Establecer línea base |
| Carga media | 25 | 120 s | 15 min | Observar saturación y latencia |

Repetir las fases de carga tres veces. Cambiar `-Jusers`, `-Jramp`, `-Jduration`, el JTL y el directorio HTML en cada ejecución. La duración configurada incluye la entrada gradual; separar ese periodo del tramo estable al comparar resultados. Ajustar la concurrencia según la capacidad del entorno y la demanda esperada. No fijar un umbral de aceptación hasta acordar objetivos medibles para p95, tasa de error y rendimiento.

El listado alterna entre las páginas 0 y 1, de tamaño 9, y cada iteración lee otro código del CSV compartido. Con la carga de desarrollo, ambas páginas tienen resultados. En otra base, confirmar que existen al menos diez encuestas visibles en el listado; si no, adaptar el rango de páginas del JMX. Mantener varios códigos válidos en el CSV para evitar que una sola encuesta domine el detalle.

## Datos y observación de PostgreSQL

Antes y después de cada fase, registrar los recuentos de `surveys`, `content_items` y `participations`. `performance/sql/db-snapshot.sql` ofrece una captura de recuentos, estadísticas acumuladas, conexiones y esperas de bloqueo; ejecutarlo con `psql -f performance/sql/db-snapshot.sql` contra la base de pruebas y guardar la salida con fecha y hora. Durante la carga, recoger con la misma referencia temporal que el JTL: CPU, memoria e I/O del servidor PostgreSQL; conexiones activas y esperas (`pg_stat_activity`); conexiones Hikari activas, inactivas y pendientes si las métricas están expuestas; y consultas de mayor tiempo total mediante `pg_stat_statements` si la extensión está habilitada. Si no lo está, anotarlo en el informe en lugar de atribuir el tiempo a consultas concretas.

Para consultas lentas, ejecutar `EXPLAIN (ANALYZE, BUFFERS)` sobre una copia de pruebas con parámetros representativos. Guardar el plan, número de filas y estadísticas junto con el JTL. No cambiar índices, pool ni datos entre una medición de referencia y su repetición de comparación.

Informar por endpoint de solicitudes por segundo, mediana, p95, p99, porcentaje de errores y códigos HTTP. Incluir gráfica temporal de latencia y concurrencia, más la observación de PostgreSQL, para distinguir un cuello de botella de consultas, bloqueos o conexiones. JMeter mide la respuesta de la API; el diagnóstico de base de datos requiere estas observaciones adicionales.

## Ampliaciones pendientes

- Generar un conjunto sintético con volumen y distribución representativos. El reinicio de desarrollo actual deja `participations` vacía; el plan de escritura permite crearlas en una base aislada.
- Añadir etiquetas, distintas profundidades de página y tamaños a los filtros públicos.
- Medir resultados en otras distribuciones de participaciones si difieren de la encuesta de referencia.
- Ampliar la edición a otros tipos de preguntas, bloques y cargas de imágenes si son representativos de los usuarios previstos.
- Tras establecer la línea base, ejecutar una prueba estable de al menos una hora para detectar degradación acumulada.


## Referencias

- [Manual oficial de Apache JMeter](https://jmeter.apache.org/usermanual/)
- [Buenas prácticas de JMeter](https://jmeter.apache.org/usermanual/best-practices.html)
- [AssertQA: pruebas de rendimiento de bases de datos](https://assertqa.com/blog/database-performance-testing)
- [Checklist de QA Practices](https://qapractices.com/checklists/database-performance-testing-checklist/)
- [Tutorial de BlazeMeter](https://www.blazemeter.com/blog/jmeter-tutorial)
- [Guía de Inna Kozak para API REST](https://innakozak.medium.com/rest-api-testing-using-apache-jmeter-step-by-step-guide-e2122ece1ea0)
- [Baeldung: introducción a JMeter](https://www.baeldung.com/jmeter)
- [Biswajit Tripathy: JMeter y Spring Boot](https://medium.com/@biswatrips/performance-test-java-code-jmeter-spring-boot-af8407779baf)
