package com.formquid.survey.service;

import com.formquid.common.exception.ApiException;
import com.formquid.block.service.BlockService;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.service.ContentService;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.dto.QuestionDraftDto;
import com.formquid.question.service.QuestionService;
import com.formquid.survey.dto.SurveyImportPreviewDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SurveyOdtImportServiceTest {

    @Mock
    private SurveyService surveyService;

    @Mock
    private ContentService contentService;

    @Mock
    private QuestionService questionService;

    @Mock
    private BlockService blockService;

    @InjectMocks
    private SurveyOdtImportService importService;

    @Test
    void parsesOnlyTheCompletedTemplateAndUsesNeutralEvaluation() throws Exception {
        SurveyImportPreviewDto preview = importService.preview(documentWithTitle("Encuesta de satisfacción"));

        assertThat(preview.title()).isEqualTo("Encuesta de satisfacción");
        assertThat(preview.description()).isEqualTo("Una encuesta breve");
        assertThat(preview.blockCount()).isEqualTo(1);
        assertThat(preview.questionCount()).isEqualTo(1);

        var block = preview.content().items().getFirst().block();
        assertThat(block.name()).isEqualTo("Experiencia general");
        assertThat(block.items()).hasSize(1);
        assertThat(block.items().getFirst().itemType()).isEqualTo(ContentItemType.QUESTION);
        assertThat(block.items().getFirst().question().questionType()).isEqualTo(QuestionType.TEXT);
        assertThat(block.items().getFirst().question().evaluationDirection())
            .isEqualTo(EvaluationDirection.NEUTRAL);
        assertThat(block.items().getFirst().question().evaluationWeight()).isNull();
    }

    @Test
    void rejectsAnUncompletedTemplateWithoutCreatingPartialContent() throws Exception {
        assertThatThrownBy(() -> importService.preview(documentWithTitle("")))
            .isInstanceOf(ApiException.class)
            .hasMessage("Completa el título de la encuesta en la plantilla vacía");
    }

    @Test
    void rejectsTheOldDocxFormat() throws Exception {
        MockMultipartFile odt = documentWithTitle("Encuesta");
        MockMultipartFile docx = new MockMultipartFile(
            "file",
            "encuesta.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            odt.getBytes()
        );

        assertThatThrownBy(() -> importService.preview(docx))
            .isInstanceOf(ApiException.class)
            .hasMessage("El archivo debe tener formato .odt");
    }

    @Test
    void ignoresUnrelatedTablesWithoutBlockingTheImport() throws Exception {
        SurveyImportPreviewDto preview = importService.preview(documentWithContent(
            table(row("Título", "Encuesta"), row("Descripción", "Descripción")),
            table(row("Contenido auxiliar", "Esta tabla no forma parte de la encuesta")),
            questionFieldsTable(
                row("Tipo de pregunta", "Texto"),
                row("Enunciado", "Pregunta válida"),
                row("Obligatoria", "Sí")
            )
        ));

        assertThat(preview.blockCount()).isZero();
        assertThat(preview.questionCount()).isEqualTo(1);
    }

    @Test
    void importsAQuestionWithoutABlockOrVisualQuestionHeader() throws Exception {
        SurveyImportPreviewDto preview = importService.preview(documentWithContent(
            table(row("Título", "Encuesta directa"), row("Descripción", "Sin bloques")),
            questionFieldsTable(
                row("Tipo de pregunta", "Opción única"),
                row("Enunciado", "¿Qué opción prefieres?"),
                row("Obligatoria", "Sí"),
                row("Opciones", "A\nB"),
                row("Permitir otra respuesta", "No")
            )
        ));

        assertThat(preview.blockCount()).isZero();
        assertThat(preview.questionCount()).isEqualTo(1);
        assertThat(preview.content().items()).singleElement().satisfies(item -> {
            assertThat(item.itemType()).isEqualTo(ContentItemType.QUESTION);
            assertThat(item.question().questionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        });
    }

    @Test
    void acceptsOnlySurveyInformationWhenOptionalStructureIsLeftBlank() throws Exception {
        SurveyImportPreviewDto preview = importService.preview(documentWithContent(
            table(row("Título", "Borrador vacío"), row("Descripción", "")),
            table(row("Nombre del bloque", "")),
            questionFieldsTable(
                row("Tipo de pregunta", ""),
                row("Enunciado", ""),
                row("Obligatoria", "")
            )
        ));

        assertThat(preview.blockCount()).isZero();
        assertThat(preview.questionCount()).isZero();
        assertThat(preview.content().items()).isEmpty();
    }

    @Test
    void acceptsSurveyInformationWithoutTheOptionalDescriptionRow() throws Exception {
        SurveyImportPreviewDto preview = importService.preview(documentWithContent(
            table(row("Título", "Solo título"))
        ));

        assertThat(preview.title()).isEqualTo("Solo título");
        assertThat(preview.description()).isNull();
        assertThat(preview.content().items()).isEmpty();
    }

    @Test
    void parsesEveryQuestionTypeWithoutImportingEvaluationValues() throws Exception {
        SurveyImportPreviewDto preview = importService.preview(documentWithQuestions(
            questionTable("Pregunta 1", row("Tipo de pregunta", "Texto"), row("Enunciado", "Texto"), row("Obligatoria", "Sí")),
            questionTable("Pregunta 2", row("Tipo de pregunta", "Numérica"), row("Enunciado", "Número"), row("Obligatoria", "No"), row("Valor mínimo", "0"), row("Valor máximo", "10"), row("Incremento", "1")),
            questionTable("Pregunta 3", row("Tipo de pregunta", "Opción única"), row("Enunciado", "Una opción"), row("Obligatoria", "Sí"), row("Opciones", "A\nB"), row("Permitir otra respuesta", "No")),
            questionTable("Pregunta 4", row("Tipo de pregunta", "Opción múltiple"), row("Enunciado", "Varias opciones"), row("Obligatoria", "Sí"), row("Opciones", "A\nB\nOtro"), row("Selecciones mínimas", "1"), row("Selecciones máximas", "2"), row("Permitir otra respuesta", "Sí")),
            questionTable("Pregunta 5", row("Tipo de pregunta", "Escala"), row("Enunciado", "Escala"), row("Obligatoria", "Sí"), row("Escala", "Del 1 al 5"), row("Incremento", "1"), row("Apariencia", "Estrellas")),
            questionTable("Pregunta 6", row("Tipo de pregunta", "Matriz de escala"), row("Enunciado", "Matriz"), row("Obligatoria", "Sí"), row("Escala", "Del 1 al 5"), row("Incremento", "1"), row("Apariencia", "Caras"), row("Filas", "Fila A\nFila B")),
            questionTable("Pregunta 7", row("Tipo de pregunta", "Dos opciones"), row("Enunciado", "Booleano"), row("Obligatoria", "Sí"), row("Texto para Sí", "Sí"), row("Texto para No", "No"))
        ));

        List<QuestionDraftDto> questions = preview.content().items().getFirst()
            .block().items().stream().map(item -> item.question()).toList();
        assertThat(questions).extracting(question -> question.questionType()).containsExactly(
            QuestionType.TEXT,
            QuestionType.NUMBER,
            QuestionType.SINGLE_CHOICE,
            QuestionType.MULTIPLE_CHOICE,
            QuestionType.SCALE,
            QuestionType.MATRIX_SCALE,
            QuestionType.BOOLEAN
        );
        assertThat(questions).allSatisfy(question -> {
            assertThat(question.evaluationDirection()).isEqualTo(EvaluationDirection.NEUTRAL);
            assertThat(question.evaluationWeight()).isNull();
            assertThat(question.options()).allSatisfy(option -> assertThat(option.evaluationFactor()).isNull());
        });
        assertThat(questions.getLast().settings().trueEvaluationFactor()).isNull();
        assertThat(questions.getLast().settings().falseEvaluationFactor()).isNull();
    }

    private MockMultipartFile documentWithTitle(String title) throws Exception {
        return documentWithContent(
            table(row("Título", title), row("Descripción", "Una encuesta breve")),
            table(row("Nombre del bloque", "Experiencia general")),
            questionTable(
                "Pregunta 1",
                row("Tipo de pregunta", "Texto"),
                row("Enunciado", "¿Cómo describirías tu experiencia?"),
                row("Obligatoria", "Sí"),
                row("Longitud máxima", "500")
            )
        );
    }

    private MockMultipartFile documentWithQuestions(String... questions) throws Exception {
        return documentWithContent(
            table(row("Título", "Encuesta completa"), row("Descripción", "Todos los tipos")),
            table(row("Nombre del bloque", "Preguntas")),
            String.join("", questions)
        );
    }

    private MockMultipartFile documentWithContent(String... content) throws Exception {
        String contentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <office:document-content
              xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
              xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
              xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0">
              <office:body><office:text>
                <text:p>Ejemplo que debe ignorarse</text:p>
                <text:h text:outline-level="1">Plantilla vacía para completar</text:h>
                %s
              </office:text></office:body>
            </office:document-content>
            """.formatted(String.join("", content));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("content.xml"));
            zip.write(contentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return new MockMultipartFile(
            "file",
            "encuesta.odt",
            "application/vnd.oasis.opendocument.text",
            bytes.toByteArray()
        );
    }

    private String table(String... rows) {
        return "<table:table>" + String.join("", rows) + "</table:table>";
    }

    private String questionTable(String heading, String... rows) {
        return "<table:table><table:table-row>" + cell(heading) + "</table:table-row>"
            + String.join("", rows) + "</table:table>";
    }

    private String questionFieldsTable(String... rows) {
        return "<table:table>" + String.join("", rows) + "</table:table>";
    }

    private String row(String label, String value) {
        return "<table:table-row>" + cell(label) + cell(value) + "</table:table-row>";
    }

    private String cell(String value) {
        return "<table:table-cell><text:p>" + value.replace("\n", "</text:p><text:p>")
            + "</text:p></table:table-cell>";
    }
}
