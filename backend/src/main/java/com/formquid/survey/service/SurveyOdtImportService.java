package com.formquid.survey.service;

import com.formquid.block.dto.CreateBlockItemRequestDto;
import com.formquid.block.dto.CreateBlockRequestDto;
import com.formquid.block.service.BlockService;
import com.formquid.common.exception.ApiException;
import com.formquid.content.dto.SurveyContentItemRequestDto;
import com.formquid.content.dto.UpdateSurveyContentRequestDto;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.service.ContentService;
import com.formquid.question.dto.MatrixRowDto;
import com.formquid.question.dto.QuestionDraftDto;
import com.formquid.question.dto.QuestionOptionDto;
import com.formquid.question.dto.QuestionSettingsDto;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.entity.ScaleDisplayType;
import com.formquid.question.service.QuestionService;
import com.formquid.survey.dto.CreateImportedSurveyRequestDto;
import com.formquid.survey.dto.SurveyImportPreviewDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.entity.Survey;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Valida la plantilla ODT de FormQuid y la convierte en una encuesta editable. */
@Service
@RequiredArgsConstructor
public class SurveyOdtImportService {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final int MAX_DOCUMENT_XML_SIZE = 8 * 1024 * 1024;
    private static final String CONTENT_XML = "content.xml";
    private static final String OFFICE_NAMESPACE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
    private static final String EMPTY_TEMPLATE_MARKER = "Plantilla vacía para completar";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:[.,]\\d+)?");

    private final SurveyService surveyService;
    private final ContentService contentService;
    private final QuestionService questionService;
    private final BlockService blockService;

    public SurveyImportPreviewDto preview(MultipartFile file) {
        validateFile(file);
        try {
            return parseDocument(extractContentXml(file));
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException | ParserConfigurationException | SAXException exception) {
            throw invalidDocument("No se ha podido leer el documento ODT");
        }
    }

    @Transactional
    public SurveyResponseDto createImported(CreateImportedSurveyRequestDto request, User owner) {
        SurveyResponseDto created = surveyService.create(request.survey(), owner);
        Survey survey = surveyService.getSurvey(created.id());
        contentService.replaceSurveyContent(survey, request.content(), owner);
        return surveyService.getSurveyForUser(created.id(), owner);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalidDocument("Selecciona un documento ODT");
        if (file.getSize() > MAX_FILE_SIZE) throw invalidDocument("El documento no puede superar 5 MB");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".odt")) {
            throw invalidDocument("El archivo debe tener formato .odt");
        }
    }

    private byte[] extractContentXml(MultipartFile file) throws IOException {
        try (ZipInputStream input = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = input.getNextEntry()) != null) {
                if (++entryCount > 1_000) throw invalidDocument("El documento contiene demasiados elementos");
                if (CONTENT_XML.equals(entry.getName())) return readLimited(input);
            }
        }
        throw invalidDocument("El archivo no contiene un documento ODT válido");
    }

    private byte[] readLimited(ZipInputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8_192];
        int read;
        int total = 0;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_DOCUMENT_XML_SIZE) {
                throw invalidDocument("El contenido del documento es demasiado grande");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private SurveyImportPreviewDto parseDocument(byte[] xml)
        throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        NodeList textBodies = document.getElementsByTagNameNS(OFFICE_NAMESPACE, "text");
        if (textBodies.getLength() == 0) {
            throw invalidDocument("No se ha encontrado el contenido de la plantilla");
        }

        List<Node> elements = childElements(textBodies.item(0));
        String title = null;
        String description = null;
        List<RootDraft> rootItems = new ArrayList<>();
        BlockDraft currentBlock = null;
        boolean expectsInformativeText = false;
        int questionCount = 0;

        for (int index = importStartIndex(elements); index < elements.size(); index += 1) {
            Node element = elements.get(index);
            if ("table".equals(element.getLocalName())) {
                List<List<String>> rows = tableRows((Element) element);
                if (isSurveyInformation(rows)) {
                    title = valueFor(rows, "Título");
                    if (title == null || title.isBlank()) {
                        throw invalidDocument("Completa el título de la encuesta en la plantilla vacía");
                    }
                    if (title.trim().length() > 200) {
                        throw invalidDocument("El título de la encuesta no puede superar 200 caracteres");
                    }
                    description = blankToNull(valueFor(rows, "Descripción"));
                } else if (hasLabel(rows, "Nombre del bloque")) {
                    String blockName = valueFor(rows, "Nombre del bloque");
                    if (blockName == null || blockName.isBlank()) {
                        currentBlock = null;
                        continue;
                    }
                    currentBlock = new BlockDraft(blockName.trim());
                    rootItems.add(RootDraft.block(currentBlock));
                } else if (isQuestionTable(rows)) {
                    if (isBlankQuestion(rows)) continue;
                    questionCount += 1;
                    try {
                        CreateBlockItemRequestDto question = questionItem(
                            rows,
                            currentBlock == null ? rootItems.size() + 1 : currentBlock.items.size() + 1
                        );
                        if (currentBlock == null) {
                            rootItems.add(RootDraft.item(question));
                        } else {
                            currentBlock.items.add(question);
                        }
                    } catch (ApiException exception) {
                        String context = currentBlock == null
                            ? "Pregunta " + questionCount
                            : "Pregunta " + questionCount + " del bloque «" + currentBlock.name + "»";
                        throw invalidDocument(context + ": " + exception.getMessage());
                    }
                }
                continue;
            }

            if (!"p".equals(element.getLocalName())) continue;
            String text = paragraphText(element).trim();
            if (text.equalsIgnoreCase("Texto informativo")) {
                expectsInformativeText = true;
            } else if (expectsInformativeText && !text.isBlank()) {
                CreateBlockItemRequestDto textItem = new CreateBlockItemRequestDto(
                    ContentItemType.TEXT,
                    currentBlock == null ? rootItems.size() + 1 : currentBlock.items.size() + 1,
                    null,
                    null,
                    null,
                    text
                );
                if (currentBlock == null) {
                    rootItems.add(RootDraft.item(textItem));
                } else {
                    currentBlock.items.add(textItem);
                }
                expectsInformativeText = false;
            }
        }

        if (title == null) {
            throw invalidDocument("Completa el título de la encuesta en la plantilla vacía");
        }
        List<SurveyContentItemRequestDto> contentItems = new ArrayList<>();
        int blockCount = 0;
        for (int index = 0; index < rootItems.size(); index += 1) {
            RootDraft rootItem = rootItems.get(index);
            if (rootItem.block != null) {
                BlockDraft block = rootItem.block;
                CreateBlockRequestDto blockRequest = new CreateBlockRequestDto(block.name, null, block.items, null);
                blockService.validateDraft(blockRequest);
                contentItems.add(new SurveyContentItemRequestDto(
                    ContentItemType.BLOCK,
                    index + 1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    blockRequest
                ));
                blockCount += 1;
                continue;
            }

            CreateBlockItemRequestDto item = rootItem.item;
            contentItems.add(new SurveyContentItemRequestDto(
                item.itemType(),
                index + 1,
                item.required(),
                item.question(),
                item.textBody(),
                item.fixedQuestionId(),
                null,
                null
            ));
        }

        return new SurveyImportPreviewDto(
            title.trim(),
            description,
            new UpdateSurveyContentRequestDto(contentItems),
            blockCount,
            questionCount
        );
    }

    private int importStartIndex(List<Node> elements) {
        for (int index = 0; index < elements.size(); index += 1) {
            Node element = elements.get(index);
            if (("p".equals(element.getLocalName()) || "h".equals(element.getLocalName()))
                && EMPTY_TEMPLATE_MARKER.equalsIgnoreCase(paragraphText(element).trim())) {
                return index + 1;
            }
        }
        throw invalidDocument("No se ha encontrado la plantilla vacía de FormQuid");
    }

    private CreateBlockItemRequestDto questionItem(List<List<String>> rows, int position) {
        Map<String, String> fields = fields(rows);
        String statement = required(fields, "Enunciado", "Cada pregunta necesita un enunciado");
        QuestionType type = questionType(required(
            fields,
            "Tipo de pregunta",
            "Cada pregunta necesita un tipo de pregunta"
        ));
        boolean isRequired = yesNo(required(fields, "Obligatoria", "Indica si la pregunta es obligatoria"));
        QuestionDraftDto question = new QuestionDraftDto(
            statement,
            blankToNull(fields.get("Ayuda para responder")),
            type,
            EvaluationDirection.NEUTRAL,
            null,
            settings(type, fields),
            options(type, fields),
            matrixRows(type, fields)
        );
        questionService.validateQuestionDraft(question);
        return new CreateBlockItemRequestDto(
            ContentItemType.QUESTION,
            position,
            isRequired,
            question,
            null,
            null
        );
    }

    private QuestionSettingsDto settings(QuestionType type, Map<String, String> fields) {
        Integer maxLength = type == QuestionType.TEXT
            ? integerValue(fields.getOrDefault("Longitud máxima", "300"), "Longitud máxima")
            : null;
        BigDecimal minValue = null;
        BigDecimal maxValue = null;
        BigDecimal step = null;
        Integer minSelections = null;
        Integer maxSelections = null;
        String leftLabel = null;
        String rightLabel = null;
        String trueLabel = null;
        String falseLabel = null;
        ScaleDisplayType displayType = null;

        if (type == QuestionType.NUMBER) {
            minValue = decimalValue(required(fields, "Valor mínimo", "Indica el valor mínimo"), "Valor mínimo");
            maxValue = decimalValue(required(fields, "Valor máximo", "Indica el valor máximo"), "Valor máximo");
            step = decimalValue(required(fields, "Incremento", "Indica el incremento"), "Incremento");
        } else if (type == QuestionType.SCALE || type == QuestionType.MATRIX_SCALE) {
            List<BigDecimal> range = scaleRange(required(fields, "Escala", "Indica el rango de la escala"));
            minValue = range.get(0);
            maxValue = range.get(1);
            step = decimalValue(required(fields, "Incremento", "Indica el incremento"), "Incremento");
            leftLabel = blankToNull(fields.get("Etiqueta izquierda"));
            rightLabel = blankToNull(fields.get("Etiqueta derecha"));
            displayType = displayType(fields.get("Apariencia"));
        } else if (type == QuestionType.MULTIPLE_CHOICE) {
            minSelections = integerValue(
                required(fields, "Selecciones mínimas", "Indica las selecciones mínimas"),
                "Selecciones mínimas"
            );
            maxSelections = integerValue(
                required(fields, "Selecciones máximas", "Indica las selecciones máximas"),
                "Selecciones máximas"
            );
        } else if (type == QuestionType.BOOLEAN) {
            trueLabel = required(fields, "Texto para Sí", "Indica el texto de la primera opción");
            falseLabel = required(fields, "Texto para No", "Indica el texto de la segunda opción");
        }

        return new QuestionSettingsDto(
            maxLength,
            minValue,
            maxValue,
            step,
            minSelections,
            maxSelections,
            leftLabel,
            rightLabel,
            trueLabel,
            falseLabel,
            null,
            null,
            displayType
        );
    }

    private List<QuestionOptionDto> options(QuestionType type, Map<String, String> fields) {
        if (type != QuestionType.SINGLE_CHOICE && type != QuestionType.MULTIPLE_CHOICE) return List.of();
        List<String> labels = lines(required(fields, "Opciones", "Añade al menos dos opciones"));
        if (labels.size() < 2) throw invalidDocument("Las preguntas de opción necesitan al menos dos opciones");
        boolean allowOther = yesNo(fields.getOrDefault("Permitir otra respuesta", "No"));
        List<QuestionOptionDto> options = new ArrayList<>();
        for (int index = 0; index < labels.size(); index += 1) {
            String label = labels.get(index);
            boolean other = allowOther && index == labels.size() - 1 && label.equalsIgnoreCase("Otro");
            options.add(new QuestionOptionDto(null, label, index + 1, other, null));
        }
        if (allowOther && options.stream().noneMatch(QuestionOptionDto::other)) {
            options.add(new QuestionOptionDto(null, "Otro", options.size() + 1, true, null));
        }
        return options;
    }

    private List<MatrixRowDto> matrixRows(QuestionType type, Map<String, String> fields) {
        if (type != QuestionType.MATRIX_SCALE) return List.of();
        List<String> labels = lines(required(fields, "Filas", "Añade al menos una fila a la matriz"));
        List<MatrixRowDto> rows = new ArrayList<>();
        for (int index = 0; index < labels.size(); index += 1) {
            rows.add(new MatrixRowDto(null, labels.get(index), index + 1));
        }
        return rows;
    }

    private QuestionType questionType(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "texto" -> QuestionType.TEXT;
            case "numérica", "numerica" -> QuestionType.NUMBER;
            case "opción única", "opcion unica" -> QuestionType.SINGLE_CHOICE;
            case "opción múltiple", "opcion multiple" -> QuestionType.MULTIPLE_CHOICE;
            case "escala" -> QuestionType.SCALE;
            case "matriz de escala" -> QuestionType.MATRIX_SCALE;
            case "dos opciones" -> QuestionType.BOOLEAN;
            default -> throw invalidDocument("Tipo de pregunta no reconocido: " + value);
        };
    }

    private ScaleDisplayType displayType(String value) {
        if (value == null || value.isBlank()) return ScaleDisplayType.NUMERIC;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "números", "numeros", "numérica", "numerica" -> ScaleDisplayType.NUMERIC;
            case "estrellas" -> ScaleDisplayType.STARS;
            case "caras" -> ScaleDisplayType.FACES;
            default -> throw invalidDocument("Apariencia de escala no reconocida: " + value);
        };
    }

    private List<BigDecimal> scaleRange(String value) {
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        List<BigDecimal> numbers = new ArrayList<>();
        while (matcher.find()) numbers.add(new BigDecimal(matcher.group().replace(',', '.')));
        if (numbers.size() < 2) {
            throw invalidDocument("La escala debe indicar un valor mínimo y uno máximo");
        }
        return List.of(numbers.get(0), numbers.get(1));
    }

    private BigDecimal decimalValue(String value, String label) {
        Matcher matcher = NUMBER_PATTERN.matcher(value == null ? "" : value);
        if (!matcher.find()) throw invalidDocument(label + " debe ser un número");
        return new BigDecimal(matcher.group().replace(',', '.'));
    }

    private Integer integerValue(String value, String label) {
        try {
            return decimalValue(value, label).intValueExact();
        } catch (ArithmeticException exception) {
            throw invalidDocument(label + " debe ser un número entero");
        }
    }

    private boolean yesNo(String value) {
        if (value == null) throw invalidDocument("Indica Sí o No");
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "sí", "si" -> true;
            case "no" -> false;
            default -> throw invalidDocument("El valor «" + value + "» debe ser Sí o No");
        };
    }

    private String required(Map<String, String> fields, String label, String message) {
        String value = fields.get(label);
        if (value == null || value.isBlank()) throw invalidDocument(message);
        return value.trim();
    }

    private boolean isSurveyInformation(List<List<String>> rows) {
        return hasLabel(rows, "Título");
    }

    private boolean isQuestionTable(List<List<String>> rows) {
        return hasLabel(rows, "Tipo de pregunta") && hasLabel(rows, "Enunciado");
    }

    private boolean isBlankQuestion(List<List<String>> rows) {
        return rows.stream()
            .filter(row -> row.size() > 1)
            .map(row -> row.get(1))
            .allMatch(value -> value == null || value.isBlank());
    }

    private boolean hasLabel(List<List<String>> rows, String label) {
        return rows.stream().anyMatch(row -> !row.isEmpty() && row.get(0).equalsIgnoreCase(label));
    }

    private String valueFor(List<List<String>> rows, String label) {
        return rows.stream()
            .filter(row -> row.size() > 1 && row.get(0).equalsIgnoreCase(label))
            .map(row -> row.get(1))
            .findFirst()
            .orElse("");
    }

    private Map<String, String> fields(List<List<String>> rows) {
        Map<String, String> fields = new LinkedHashMap<>();
        rows.stream()
            .filter(row -> row.size() > 1)
            .forEach(row -> fields.put(row.get(0).trim(), row.get(1).trim()));
        return fields;
    }

    private List<List<String>> tableRows(Element table) {
        List<List<String>> rows = new ArrayList<>();
        NodeList rowNodes = table.getElementsByTagNameNS("*", "table-row");
        for (int index = 0; index < rowNodes.getLength(); index += 1) {
            Element rowNode = (Element) rowNodes.item(index);
            NodeList cellNodes = rowNode.getElementsByTagNameNS("*", "table-cell");
            List<String> cells = new ArrayList<>();
            for (int cellIndex = 0; cellIndex < cellNodes.getLength(); cellIndex += 1) {
                cells.add(cellText((Element) cellNodes.item(cellIndex)));
            }
            rows.add(cells);
        }
        return rows;
    }

    private String cellText(Element cell) {
        NodeList paragraphs = cell.getElementsByTagNameNS("*", "p");
        List<String> values = new ArrayList<>();
        for (int index = 0; index < paragraphs.getLength(); index += 1) {
            String value = paragraphText(paragraphs.item(index)).trim();
            if (!value.isEmpty()) values.add(value);
        }
        return String.join("\n", values).trim();
    }

    private String paragraphText(Node paragraph) {
        StringBuilder text = new StringBuilder();
        appendText(paragraph, text);
        return text.toString();
    }

    private void appendText(Node node, StringBuilder text) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            text.append(node.getNodeValue());
            return;
        }
        if ("line-break".equals(node.getLocalName())) {
            text.append('\n');
            return;
        }
        if ("tab".equals(node.getLocalName())) {
            text.append('\t');
            return;
        }
        if ("s".equals(node.getLocalName())) {
            Node count = node.getAttributes() == null ? null : node.getAttributes().getNamedItemNS(
                "urn:oasis:names:tc:opendocument:xmlns:text:1.0",
                "c"
            );
            int spaces = 1;
            if (count != null) {
                try {
                    spaces = Math.min(1_000, Math.max(1, Integer.parseInt(count.getNodeValue())));
                } catch (NumberFormatException ignored) {
                    spaces = 1;
                }
            }
            text.append(" ".repeat(spaces));
            return;
        }
        Node child = node.getFirstChild();
        while (child != null) {
            appendText(child, text);
            child = child.getNextSibling();
        }
    }

    private List<Node> childElements(Node parent) {
        List<Node> children = new ArrayList<>();
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) children.add(child);
            child = child.getNextSibling();
        }
        return children;
    }

    private List<String> lines(String value) {
        return value.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final class BlockDraft {
        private final String name;
        private final List<CreateBlockItemRequestDto> items = new ArrayList<>();

        private BlockDraft(String name) {
            this.name = name;
        }
    }

    private static final class RootDraft {
        private final BlockDraft block;
        private final CreateBlockItemRequestDto item;

        private RootDraft(BlockDraft block, CreateBlockItemRequestDto item) {
            this.block = block;
            this.item = item;
        }

        private static RootDraft block(BlockDraft block) {
            return new RootDraft(block, null);
        }

        private static RootDraft item(CreateBlockItemRequestDto item) {
            return new RootDraft(null, item);
        }
    }

    private ApiException invalidDocument(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
