import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import JSZip from 'jszip';

const ODT_MIME_TYPE = 'application/vnd.oasis.opendocument.text';
const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const defaultOutput = resolve(
  scriptDirectory,
  '../../Plantilla importacion de encuestas FormQuid.odt',
);
const publicOutput = resolve(
  scriptDirectory,
  '../public/templates/plantilla-importacion-encuestas-formquid.odt',
);
const hasCustomOutput = Boolean(process.argv[2]);
const outputPath = hasCustomOutput
  ? resolve(process.cwd(), process.argv[2])
  : defaultOutput;

function escapeXml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;');
}

function paragraphs(value, styleName = 'TableValue') {
  return String(value).split('\n').map((line) => (
    line
      ? `<text:p text:style-name="${styleName}">${escapeXml(line)}</text:p>`
      : `<text:p text:style-name="${styleName}"/>`
  )).join('');
}

function paragraph(value, styleName = 'Body') {
  return `<text:p text:style-name="${styleName}">${escapeXml(value)}</text:p>`;
}

function heading(value, level = 1) {
  const styleName = level === 1 ? 'Heading1' : 'Heading2';
  return `<text:h text:style-name="${styleName}" text:outline-level="${level}">${escapeXml(value)}</text:h>`;
}

function title(value) {
  return `<text:h text:style-name="Title" text:outline-level="1">${escapeXml(value)}</text:h>`;
}

function numberedStep(number, value) {
  return `<text:p text:style-name="Step"><text:span text:style-name="StepNumber">${number}  </text:span>${escapeXml(value)}</text:p>`;
}

function pageBreak() {
  return '<text:p text:style-name="PageBreak"/>';
}

function spacer() {
  return '<text:p text:style-name="Spacer"/>';
}

function tableCell(value, styleName, span = 1) {
  const spanAttribute = span > 1 ? ` table:number-columns-spanned="${span}"` : '';
  const coveredCell = span > 1 ? '<table:covered-table-cell/>' : '';
  const paragraphStyle = styleName === 'QuestionHeadingCell'
    ? 'QuestionHeading'
    : styleName === 'LabelCell' ? 'TableLabel' : 'TableValue';
  return `<table:table-cell table:style-name="${styleName}" office:value-type="string"${spanAttribute}>${paragraphs(value, paragraphStyle)}</table:table-cell>${coveredCell}`;
}

function fieldRow(label, value) {
  return `<table:table-row>${tableCell(label, 'LabelCell')}${tableCell(value, 'ValueCell')}</table:table-row>`;
}

let tableIndex = 0;

function formTable(rows) {
  tableIndex += 1;
  return `<table:table table:name="Tabla${tableIndex}" table:style-name="FormTable"><table:table-column table:style-name="LabelColumn"/><table:table-column table:style-name="ValueColumn"/>${rows.map(([label, value]) => fieldRow(label, value)).join('')}</table:table>`;
}

function questionTable(number, type, rows) {
  tableIndex += 1;
  const allRows = [['Tipo de pregunta', type], ...rows];
  return `<table:table table:name="Pregunta${tableIndex}" table:style-name="FormTable"><table:table-column table:style-name="LabelColumn"/><table:table-column table:style-name="ValueColumn"/><table:table-row>${tableCell(`Pregunta ${number}`, 'QuestionHeadingCell', 2)}</table:table-row>${allRows.map(([label, value]) => fieldRow(label, value)).join('')}</table:table>`;
}

const content = [
  title('Plantilla para importar una encuesta'),
  paragraph(
    'Prepara tu encuesta en este documento ODT y súbela a FormQuid para obtener un borrador editable. Consulta los ejemplos y completa únicamente la plantilla vacía situada al final.',
    'Introduction',
  ),

  heading('Cómo utilizarla'),
  numberedStep(1, 'Ve a la plantilla vacía situada al final y completa el título y la descripción.'),
  numberedStep(2, 'Conserva los nombres de los campos, como Nombre del bloque, Enunciado, Tipo de pregunta y Obligatoria.'),
  numberedStep(3, 'Copia en la plantilla vacía la ficha completa del tipo de pregunta que necesites. Puedes crear tantos bloques y preguntas como quieras.'),
  numberedStep(4, 'Guarda el documento en formato ODT y súbelo a FormQuid. Podrás revisar el resultado antes de publicarlo.'),

  heading('Qué puedes incluir'),
  paragraph('Texto, Numérica, Opción única, Opción múltiple, Escala, Matriz de escala y Dos opciones. Utiliza exactamente uno de estos nombres en el campo Tipo de pregunta.'),
  paragraph('Los bloques son opcionales. Puedes añadir preguntas directamente a la encuesta o agruparlas dentro de bloques con nombre.'),

  heading('Antes de importar'),
  paragraph('Cada bloque debe tener un nombre. Cada pregunta debe tener enunciado y tipo de pregunta. Las preguntas de opción necesitan al menos dos opciones. En las escalas, el valor mínimo debe ser menor que el máximo.'),
  paragraph('Si el documento contiene algún problema, FormQuid indicará la pregunta que debes corregir y no creará una encuesta incompleta.'),

  pageBreak(),
  heading('Ejemplo completo para copiar y adaptar'),
  paragraph('Este ejemplo contiene todos los tipos de pregunta disponibles. Sustituye su contenido y elimina las preguntas que no necesites.', 'Introduction'),
  heading('Información de la encuesta', 2),
  formTable([
    ['Título', 'Encuesta de hábitos digitales y bienestar'],
    ['Descripción', 'Queremos conocer cómo utilizas las herramientas digitales y cómo influyen en tu experiencia de aprendizaje.'],
  ]),
  spacer(),

  heading('Bloque 1', 2),
  formTable([['Nombre del bloque', 'Perfil y hábitos']]),
  spacer(),
  paragraph('Texto informativo', 'TextLabel'),
  paragraph('Lee cada pregunta con atención. No incluyas datos personales en las respuestas abiertas.', 'Introduction'),
  questionTable(1, 'Texto', [
    ['Enunciado', '¿Qué aspecto de tu experiencia digital te gustaría mejorar?'],
    ['Obligatoria', 'Sí'],
    ['Longitud máxima', '500 caracteres'],
    ['Ayuda para responder', 'Describe brevemente una situación concreta.'],
  ]),
  spacer(),
  questionTable(2, 'Numérica', [
    ['Enunciado', '¿Cuántas interrupciones por notificaciones tienes durante una hora de estudio?'],
    ['Obligatoria', 'Sí'],
    ['Valor mínimo', '0'],
    ['Valor máximo', '20'],
    ['Incremento', '1'],
  ]),

  pageBreak(),
  heading('Bloque 1  Perfil y hábitos', 2),
  questionTable(3, 'Opción única', [
    ['Enunciado', '¿Con qué frecuencia utilizas herramientas digitales para estudiar?'],
    ['Obligatoria', 'Sí'],
    ['Opciones', 'Nunca\nMenos de una vez por semana\nVarias veces por semana\nTodos los días\nOtro'],
    ['Permitir otra respuesta', 'Sí'],
  ]),
  spacer(),
  questionTable(4, 'Opción múltiple', [
    ['Enunciado', '¿Qué recursos digitales utilizas habitualmente para aprender?'],
    ['Obligatoria', 'No'],
    ['Opciones', 'Vídeos educativos\nCursos en línea\nLibros o apuntes digitales\nAsistentes de inteligencia artificial\nOtro'],
    ['Selecciones mínimas', '1'],
    ['Selecciones máximas', '3'],
    ['Permitir otra respuesta', 'Sí'],
  ]),

  pageBreak(),
  heading('Bloque 2', 2),
  formTable([['Nombre del bloque', 'Opinión sobre la experiencia']]),
  spacer(),
  paragraph('Texto informativo', 'TextLabel'),
  paragraph('Elige la respuesta que mejor represente tu opinión.', 'Introduction'),
  questionTable(5, 'Escala', [
    ['Enunciado', '¿Qué utilidad tienen las herramientas digitales para tu aprendizaje?'],
    ['Obligatoria', 'Sí'],
    ['Escala', 'Del 1 al 5'],
    ['Incremento', '1'],
    ['Etiqueta izquierda', 'Nada útil'],
    ['Etiqueta derecha', 'Muy útil'],
    ['Apariencia', 'Números'],
  ]),

  pageBreak(),
  heading('Bloque 2  Opinión sobre la experiencia', 2),
  questionTable(6, 'Matriz de escala', [
    ['Enunciado', '¿Qué grado de satisfacción tienes con cada aspecto?'],
    ['Obligatoria', 'Sí'],
    ['Escala', 'Del 1 al 5'],
    ['Incremento', '1'],
    ['Etiqueta izquierda', 'Muy insatisfecho'],
    ['Etiqueta derecha', 'Muy satisfecho'],
    ['Apariencia', 'Caras'],
    ['Filas', 'Facilidad de uso\nClaridad de las instrucciones\nVelocidad de respuesta\nAccesibilidad'],
  ]),
  spacer(),
  questionTable(7, 'Dos opciones', [
    ['Enunciado', '¿Recomendarías esta plataforma a otra persona?'],
    ['Obligatoria', 'Sí'],
    ['Texto para Sí', 'Sí'],
    ['Texto para No', 'No'],
  ]),

  pageBreak(),
  heading('Plantilla vacía para completar'),
  paragraph('Rellena estos campos y duplica la ficha de pregunta cuando necesites añadir más.', 'Introduction'),
  heading('Información de la encuesta', 2),
  formTable([
    ['Título', ''],
    ['Descripción', ''],
  ]),
  spacer(),
  heading('Bloque opcional', 2),
  paragraph('Escribe un nombre únicamente si quieres agrupar la pregunta dentro de un bloque.', 'Introduction'),
  formTable([['Nombre del bloque', '']]),
  spacer(),
  questionTable(1, '', [
    ['Enunciado', ''],
    ['Obligatoria', ''],
  ]),
].join('');

const contentXml = `<?xml version="1.0" encoding="UTF-8"?>
<office:document-content
  xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
  xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
  xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
  xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
  xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
  xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
  office:version="1.3">
  <office:font-face-decls><style:font-face style:name="Arial" svg:font-family="Arial"/></office:font-face-decls>
  <office:automatic-styles>
    <style:style style:name="Title" style:family="paragraph"><style:paragraph-properties fo:margin-bottom="0.3cm"/><style:text-properties style:font-name="Arial" fo:font-family="Arial" fo:font-size="24pt" fo:font-weight="bold"/></style:style>
    <style:style style:name="Heading1" style:family="paragraph"><style:paragraph-properties fo:margin-top="0.45cm" fo:margin-bottom="0.2cm" fo:keep-with-next="always"/><style:text-properties style:font-name="Arial" fo:font-family="Arial" fo:font-size="16pt" fo:font-weight="bold"/></style:style>
    <style:style style:name="Heading2" style:family="paragraph"><style:paragraph-properties fo:margin-top="0.3cm" fo:margin-bottom="0.18cm" fo:keep-with-next="always"/><style:text-properties style:font-name="Arial" fo:font-family="Arial" fo:font-size="13pt" fo:font-weight="bold"/></style:style>
    <style:style style:name="Body" style:family="paragraph"><style:paragraph-properties fo:margin-bottom="0.2cm" fo:line-height="130%"/><style:text-properties style:font-name="Arial" fo:font-family="Arial" fo:font-size="10.5pt"/></style:style>
    <style:style style:name="Introduction" style:family="paragraph" style:parent-style-name="Body"><style:text-properties fo:color="#404040"/></style:style>
    <style:style style:name="Step" style:family="paragraph" style:parent-style-name="Body"><style:paragraph-properties fo:margin-left="0.2cm" fo:margin-bottom="0.18cm"/></style:style>
    <style:style style:name="StepNumber" style:family="text"><style:text-properties fo:font-weight="bold"/></style:style>
    <style:style style:name="TextLabel" style:family="paragraph" style:parent-style-name="Body"><style:paragraph-properties fo:margin-bottom="0.08cm"/><style:text-properties fo:font-weight="bold"/></style:style>
    <style:style style:name="PageBreak" style:family="paragraph"><style:paragraph-properties fo:break-before="page"/></style:style>
    <style:style style:name="Spacer" style:family="paragraph"><style:paragraph-properties fo:margin-bottom="0.12cm"/></style:style>
    <style:style style:name="TableLabel" style:family="paragraph"><style:paragraph-properties fo:line-height="120%"/><style:text-properties style:font-name="Arial" fo:font-family="Arial" fo:font-size="10pt" fo:font-weight="bold"/></style:style>
    <style:style style:name="TableValue" style:family="paragraph"><style:paragraph-properties fo:line-height="120%"/><style:text-properties style:font-name="Arial" fo:font-family="Arial" fo:font-size="10.5pt"/></style:style>
    <style:style style:name="QuestionHeading" style:family="paragraph"><style:text-properties style:font-name="Arial" fo:font-family="Arial" fo:font-size="11pt" fo:font-weight="bold"/></style:style>
    <style:style style:name="FormTable" style:family="table"><style:table-properties table:align="center" style:width="17cm"/></style:style>
    <style:style style:name="LabelColumn" style:family="table-column"><style:table-column-properties style:column-width="4.3cm"/></style:style>
    <style:style style:name="ValueColumn" style:family="table-column"><style:table-column-properties style:column-width="12.7cm"/></style:style>
    <style:style style:name="LabelCell" style:family="table-cell"><style:table-cell-properties fo:background-color="#F3F4F6" fo:border="0.5pt solid #D9D9D9" fo:padding="0.18cm" style:vertical-align="middle"/></style:style>
    <style:style style:name="ValueCell" style:family="table-cell"><style:table-cell-properties fo:border="0.5pt solid #D9D9D9" fo:padding="0.18cm" style:vertical-align="middle"/></style:style>
    <style:style style:name="QuestionHeadingCell" style:family="table-cell"><style:table-cell-properties fo:background-color="#EAF2F8" fo:border="0.5pt solid #D9D9D9" fo:padding="0.2cm" style:vertical-align="middle"/></style:style>
  </office:automatic-styles>
  <office:body><office:text>${content}</office:text></office:body>
</office:document-content>`;

const stylesXml = `<?xml version="1.0" encoding="UTF-8"?>
<office:document-styles
  xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
  xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
  xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
  xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
  office:version="1.3">
  <office:font-face-decls><style:font-face style:name="Arial" svg:font-family="Arial"/></office:font-face-decls>
  <office:styles>
    <style:default-style style:family="paragraph"><style:paragraph-properties fo:line-height="130%"/><style:text-properties style:font-name="Arial" fo:font-family="Arial" fo:font-size="10.5pt"/></style:default-style>
    <style:style style:name="Standard" style:family="paragraph" style:class="text"/>
  </office:styles>
  <office:automatic-styles>
    <style:page-layout style:name="PageLayout"><style:page-layout-properties fo:page-width="21cm" fo:page-height="29.7cm" style:print-orientation="portrait" fo:margin-top="1.6cm" fo:margin-bottom="1.6cm" fo:margin-left="1.8cm" fo:margin-right="1.8cm"/></style:page-layout>
  </office:automatic-styles>
  <office:master-styles><style:master-page style:name="Standard" style:page-layout-name="PageLayout"/></office:master-styles>
</office:document-styles>`;

const metaXml = `<?xml version="1.0" encoding="UTF-8"?>
<office:document-meta
  xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
  xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  office:version="1.3">
  <office:meta>
    <meta:generator>FormQuid</meta:generator>
    <dc:title>Plantilla para importar una encuesta en FormQuid</dc:title>
    <dc:subject>Plantilla de encuesta editable y fácil de completar</dc:subject>
    <dc:description>Ejemplo de los tipos de pregunta que FormQuid puede importar desde ODT.</dc:description>
  </office:meta>
</office:document-meta>`;

const settingsXml = `<?xml version="1.0" encoding="UTF-8"?>
<office:document-settings xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" office:version="1.3"><office:settings/></office:document-settings>`;

const manifestXml = `<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.3">
  <manifest:file-entry manifest:full-path="/" manifest:version="1.3" manifest:media-type="${ODT_MIME_TYPE}"/>
  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="settings.xml" manifest:media-type="text/xml"/>
</manifest:manifest>`;

const archive = new JSZip();
archive.file('mimetype', ODT_MIME_TYPE, { compression: 'STORE' });
archive.file('content.xml', contentXml);
archive.file('styles.xml', stylesXml);
archive.file('meta.xml', metaXml);
archive.file('settings.xml', settingsXml);
archive.folder('META-INF').file('manifest.xml', manifestXml);

const documentBuffer = await archive.generateAsync({
  type: 'nodebuffer',
  compression: 'DEFLATE',
  compressionOptions: { level: 9 },
  mimeType: ODT_MIME_TYPE,
});

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, documentBuffer);
console.log(`Plantilla generada en ${outputPath}`);

if (!hasCustomOutput) {
  await mkdir(dirname(publicOutput), { recursive: true });
  await writeFile(publicOutput, documentBuffer);
  console.log(`Copia descargable generada en ${publicOutput}`);
}
