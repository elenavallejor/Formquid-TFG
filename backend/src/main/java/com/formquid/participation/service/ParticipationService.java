package com.formquid.participation.service;

import com.formquid.block.repository.BlockRepository;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.common.exception.ApiException;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.participation.dto.AnswerResponseDto;
import com.formquid.participation.dto.MatrixAnswerRequestDto;
import com.formquid.participation.dto.MatrixAnswerValueResponseDto;
import com.formquid.participation.dto.OptionAnswerRequestDto;
import com.formquid.participation.dto.ParticipationAnswerResponseDto;
import com.formquid.participation.dto.ParticipationDetailResponseDto;
import com.formquid.participation.dto.ParticipationResponseDto;
import com.formquid.participation.dto.ParticipationSummaryResponseDto;
import com.formquid.participation.dto.SelectedOptionResponseDto;
import com.formquid.participation.dto.SubmitAnswerRequestDto;
import com.formquid.participation.dto.SubmitParticipationRequestDto;
import com.formquid.participation.entity.Answer;
import com.formquid.participation.entity.AnswerOption;
import com.formquid.participation.entity.AnswerOptionId;
import com.formquid.participation.entity.MatrixAnswerValue;
import com.formquid.participation.entity.MatrixAnswerValueId;
import com.formquid.participation.entity.Participation;
import com.formquid.participation.repository.AnswerRepository;
import com.formquid.participation.repository.AnswerOptionRepository;
import com.formquid.participation.repository.MatrixAnswerValueRepository;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.question.entity.MatrixRow;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionOption;
import com.formquid.question.entity.QuestionSettings;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.MatrixRowRepository;
import com.formquid.question.repository.QuestionOptionRepository;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    public static final String ALREADY_SUBMITTED_CODE = "PARTICIPATION_ALREADY_SUBMITTED";
    private static final int MAX_PAGE_SIZE = 50;

    private final SurveyRepository surveyRepository;
    private final ContentItemRepository contentItemRepository;
    private final BlockRepository blockRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final MatrixRowRepository matrixRowRepository;
    private final ParticipationRepository participationRepository;
    private final AnswerRepository answerRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final MatrixAnswerValueRepository matrixAnswerValueRepository;
    private final PasswordEncoder passwordEncoder;
    private final SurveyService surveyService;

    /**
     * Recibe encuesta abierta y respuestas, devuelve la participación guardada.
     */
    @Transactional
    public ParticipationResponseDto submit(
        String publicCode,
        SubmitParticipationRequestDto request,
        User authenticatedUser,
        String respondentTokenHash
    ) {
        Survey survey = surveyRepository.findByPublicCode(normalizePublicCode(publicCode))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
        validateSurveyAccess(survey, request.accessPassword(), authenticatedUser);
        validateRespondent(survey.getId(), authenticatedUser, respondentTokenHash);

        Map<Long, Boolean> requiredByQuestion = loadSurveyQuestions(survey.getId());
        Set<Long> receivedQuestionIds = new HashSet<>();
        for (SubmitAnswerRequestDto answer : request.answers()) {
            if (!receivedQuestionIds.add(answer.questionId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La petición contiene respuestas duplicadas");
            }
        }

        Map<Long, SubmitAnswerRequestDto> answersByQuestion = request.answers().stream()
            .collect(Collectors.toMap(SubmitAnswerRequestDto::questionId, answer -> answer, (a, b) -> b, LinkedHashMap::new));

        for (Long questionId : answersByQuestion.keySet()) {
            if (!requiredByQuestion.containsKey(questionId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta " + questionId + " no pertenece a la encuesta");
            }
        }

        requiredByQuestion.forEach((questionId, required) -> {
            if (Boolean.TRUE.equals(required) && isEmpty(answersByQuestion.get(questionId))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Falta una pregunta obligatoria: " + questionId);
            }
        });

        Participation participation = saveParticipation(survey, authenticatedUser, respondentTokenHash);
        for (SubmitAnswerRequestDto dto : answersByQuestion.values()) {
            if (!isEmpty(dto)) {
                saveAnswer(participation, dto);
            }
        }
        return toResponse(participationRepository.findById(participation.getId()).orElse(participation));
    }

    @Transactional(readOnly = true)
    public PageResponseDto<ParticipationSummaryResponseDto> findParticipationPage(
        Long surveyId,
        User actor,
        int page,
        int size
    ) {
        validatePagination(page, size);
        Survey survey = getSurveyWithResultsAccess(surveyId, actor);
        Page<ParticipationSummaryResponseDto> participations = participationRepository.findSummariesBySurveyId(
            survey.getId(),
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id"))
            )
        );
        return new PageResponseDto<>(
            participations.getContent(),
            participations.getNumber(),
            participations.getSize(),
            participations.getTotalElements(),
            participations.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ParticipationDetailResponseDto getParticipationDetail(
        Long surveyId,
        Long participationId,
        User actor
    ) {
        Survey survey = getSurveyWithResultsAccess(surveyId, actor);
        Participation participation = participationRepository
            .findDetailByIdAndSurveyId(participationId, survey.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Participación no encontrada"));
        List<Answer> answers = answerRepository.findDetailsByParticipationId(participation.getId());
        List<Long> answerIds = answers.stream().map(Answer::getId).toList();

        Map<Long, List<SelectedOptionResponseDto>> optionsByAnswer = new LinkedHashMap<>();
        Map<Long, List<MatrixAnswerValueResponseDto>> matrixValuesByAnswer = new LinkedHashMap<>();
        if (!answerIds.isEmpty()) {
            answerOptionRepository.findByAnswerIdsWithOption(answerIds).forEach(selected ->
                optionsByAnswer.computeIfAbsent(selected.getAnswer().getId(), ignored -> new ArrayList<>())
                    .add(new SelectedOptionResponseDto(
                        selected.getOption().getId(),
                        selected.getOption().getLabel(),
                        selected.getOtherText()
                    ))
            );
            matrixAnswerValueRepository.findByAnswerIdsWithRow(answerIds).forEach(value ->
                matrixValuesByAnswer.computeIfAbsent(value.getAnswer().getId(), ignored -> new ArrayList<>())
                    .add(new MatrixAnswerValueResponseDto(
                        value.getMatrixRow().getId(),
                        value.getMatrixRow().getLabel(),
                        value.getNumericValue()
                    ))
            );
        }

        List<ParticipationAnswerResponseDto> responseAnswers = answers.stream()
            .map(answer -> new ParticipationAnswerResponseDto(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getQuestion().getStatement(),
                answer.getQuestion().getQuestionType(),
                answer.getTextValue(),
                answer.getNumericValue(),
                answer.getBooleanValue(),
                answer.getQuestion().getSettings() == null
                    ? null
                    : answer.getQuestion().getSettings().getTrueLabel(),
                answer.getQuestion().getSettings() == null
                    ? null
                    : answer.getQuestion().getSettings().getFalseLabel(),
                List.copyOf(optionsByAnswer.getOrDefault(answer.getId(), List.of())),
                List.copyOf(matrixValuesByAnswer.getOrDefault(answer.getId(), List.of()))
            ))
            .toList();

        return new ParticipationDetailResponseDto(
            participation.getId(),
            survey.getId(),
            survey.getTitle(),
            participation.getSubmittedAt(),
            responseAnswers
        );
    }

    private Survey getSurveyWithResultsAccess(Long surveyId, User actor) {
        Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada"));
        surveyService.ensureCanViewResults(survey, actor);
        return survey;
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "La página no puede ser negativa y su tamaño debe estar entre 1 y " + MAX_PAGE_SIZE
            );
        }
    }

    private void validateRespondent(Long surveyId, User authenticatedUser, String respondentTokenHash) {
        if (authenticatedUser == null && (respondentTokenHash == null || respondentTokenHash.isBlank())) {
            throw new IllegalStateException("La participación no tiene una identidad asociada");
        }
        boolean alreadySubmitted = authenticatedUser != null
            ? participationRepository.existsBySurveyIdAndRespondentUserId(surveyId, authenticatedUser.getId())
            : participationRepository.existsBySurveyIdAndRespondentTokenHash(surveyId, respondentTokenHash);
        if (alreadySubmitted) {
            throw duplicateParticipation();
        }
    }

    private Participation saveParticipation(Survey survey, User authenticatedUser, String respondentTokenHash) {
        try {
            return participationRepository.saveAndFlush(Participation.builder()
                .survey(survey)
                .respondentUser(authenticatedUser)
                .respondentTokenHash(authenticatedUser == null ? respondentTokenHash : null)
                .build());
        } catch (DataIntegrityViolationException exception) {
            throw duplicateParticipation();
        }
    }

    private ApiException duplicateParticipation() {
        return new ApiException(
            HttpStatus.CONFLICT,
            ALREADY_SUBMITTED_CODE,
            "Ya respondiste esta encuesta. Solo se puede enviar una vez."
        );
    }

    private void validateSurveyAccess(Survey survey, String accessPassword, User authenticatedUser) {
        if (survey.getLifecycleStatus() != SurveyLifecycle.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "La encuesta no admite respuestas");
        }
        if (survey.getAccessMode() == SurveyAccessMode.AUTHENTICATED) {
            if (authenticatedUser == null) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Esta encuesta requiere una cuenta registrada");
            }
            return;
        }
        if (survey.getAccessMode() == SurveyAccessMode.PASSWORD_PROTECTED
            && (accessPassword == null || !passwordEncoder.matches(accessPassword, survey.getAccessPasswordHash()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Contraseña de acceso incorrecta");
        }
    }

    private Map<Long, Boolean> loadSurveyQuestions(Long surveyId) {
        Map<Long, Boolean> questions = new LinkedHashMap<>();
        for (ContentItem item : contentItemRepository.findBySurveyIdOrderByPositionAsc(surveyId)) {
            if (item.getItemType() == ContentItemType.QUESTION) {
                Question question = questionRepository.findByParentItemId(item.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "La encuesta contiene una pregunta incompleta"));
                questions.put(question.getId(), item.getRequired());
            } else if (item.getItemType() == ContentItemType.BLOCK) {
                var block = blockRepository.findByParentItemId(item.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "La encuesta contiene un bloque incompleto"));
                contentItemRepository.findByParentBlockIdOrderByPositionAsc(block.getId()).forEach(blockItem -> {
                    if (blockItem.getItemType() == ContentItemType.QUESTION) {
                        Question question = questionRepository.findByParentItemId(blockItem.getId())
                            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "El bloque contiene una pregunta incompleta"));
                        questions.put(question.getId(), blockItem.getRequired());
                    } else if (blockItem.getItemType() != ContentItemType.TEXT) {
                        throw new ApiException(HttpStatus.CONFLICT, "El bloque contiene un tipo de elemento no permitido");
                    }
                });
            }
        }
        return questions;
    }

    private void saveAnswer(Participation participation, SubmitAnswerRequestDto dto) {
        Question question = questionRepository.findById(dto.questionId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pregunta no encontrada"));
        validateCompatible(question, dto);
        Answer answer = answerRepository.save(Answer.builder()
            .participation(participation)
            .question(question)
            .textValue(textValue(question, dto))
            .numericValue(numericValue(question, dto))
            .booleanValue(booleanValue(question, dto))
            .build());

        Set<Long> selectedOptionIds = new HashSet<>();
        for (OptionAnswerRequestDto optionDto : safeOptions(dto.selectedOptions())) {
            if (!selectedOptionIds.add(optionDto.optionId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La respuesta contiene opciones duplicadas");
            }
            QuestionOption option = optionRepository.findById(optionDto.optionId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Opción no encontrada"));
            if (!option.getQuestion().getId().equals(question.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La opción no pertenece a la pregunta");
            }
            validateOtherOption(option, optionDto.otherText());
            answer.getSelectedOptions().add(AnswerOption.builder()
                .id(new AnswerOptionId(answer.getId(), option.getId()))
                .answer(answer)
                .option(option)
                .otherText(optionDto.otherText())
                .build());
        }

        Set<Long> matrixRowIds = new HashSet<>();
        for (MatrixAnswerRequestDto matrixDto : safeMatrixValues(dto.matrixValues())) {
            if (!matrixRowIds.add(matrixDto.matrixRowId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La matriz contiene filas duplicadas");
            }
            MatrixRow row = matrixRowRepository.findById(matrixDto.matrixRowId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Fila de matriz no encontrada"));
            if (!row.getQuestion().getId().equals(question.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La fila de matriz no pertenece a la pregunta");
            }
            validateNumericValue(question, matrixDto.numericValue());
            answer.getMatrixValues().add(MatrixAnswerValue.builder()
                .id(new MatrixAnswerValueId(answer.getId(), row.getId()))
                .answer(answer)
                .matrixRow(row)
                .numericValue(matrixDto.numericValue())
                .build());
        }
        answerRepository.save(answer);
    }

    private void validateCompatible(Question question, SubmitAnswerRequestDto dto) {
        QuestionType type = question.getQuestionType();
        if (type == QuestionType.TEXT ) {
            requireOnlyText(question, dto);
        } else if (type == QuestionType.NUMBER || type == QuestionType.SCALE) {
            requireOnlyNumeric(question, dto);
        } else if (type == QuestionType.BOOLEAN) {
            requireOnlyBoolean(dto);
        } else if (type == QuestionType.SINGLE_CHOICE) {
            requireOnlyOptions(dto);
            if (safeOptions(dto.selectedOptions()).size() != 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La selección única necesita exactamente una opción");
            }
        } else if (type == QuestionType.MULTIPLE_CHOICE) {
            requireOnlyOptions(dto);
            validateMultipleChoice(question, dto);
        } else if (type == QuestionType.MATRIX_SCALE) {
            requireOnlyMatrix(question, dto);
        }
    }

    private void requireOnlyText(Question question, SubmitAnswerRequestDto dto) {
        reject(dto.numericValue() != null || dto.booleanValue() != null || hasOptions(dto) || hasMatrix(dto));

        if (dto.textValue() == null || dto.textValue().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta de texto necesita textValue");
        }

        int maxLength = question.getSettings() != null && question.getSettings().getMaxLength() != null
            ? question.getSettings().getMaxLength()
            : 5000;

        if (dto.textValue().length() > maxLength) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La respuesta de texto supera la longitud máxima");
        }
    }

    private void requireOnlyNumeric(Question question, SubmitAnswerRequestDto dto) {
        reject(dto.textValue() != null || dto.booleanValue() != null || hasOptions(dto) || hasMatrix(dto));
        if (dto.numericValue() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta numérica necesita numericValue");
        }
        validateNumericValue(question, dto.numericValue());
    }

    private void requireOnlyBoolean(SubmitAnswerRequestDto dto) {
        reject(dto.textValue() != null || dto.numericValue() != null || hasOptions(dto) || hasMatrix(dto));
        if (dto.booleanValue() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La pregunta booleana necesita booleanValue");
        }
    }

    private void requireOnlyOptions(SubmitAnswerRequestDto dto) {
        reject(dto.textValue() != null || dto.numericValue() != null || dto.booleanValue() != null || hasMatrix(dto));
    }

    private void requireOnlyMatrix(Question question, SubmitAnswerRequestDto dto) {
        reject(dto.textValue() != null || dto.numericValue() != null || dto.booleanValue() != null || hasOptions(dto));
        List<MatrixRow> rows = matrixRowRepository.findByQuestionIdOrderByPositionAsc(question.getId());
        if (safeMatrixValues(dto.matrixValues()).size() != rows.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La matriz debe incluir todas sus filas");
        }
        Set<Long> expectedRows = rows.stream().map(MatrixRow::getId).collect(Collectors.toSet());
        Set<Long> receivedRows = safeMatrixValues(dto.matrixValues()).stream()
            .map(MatrixAnswerRequestDto::matrixRowId)
            .collect(Collectors.toSet());
        if (!expectedRows.equals(receivedRows)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La matriz contiene filas no válidas o incompletas");
        }
    }

    private void validateMultipleChoice(Question question, SubmitAnswerRequestDto dto) {
        int selected = safeOptions(dto.selectedOptions()).size();
        QuestionSettings settings = question.getSettings();
        int min = settings == null || settings.getMinSelections() == null ? 1 : settings.getMinSelections();
        Integer max = settings == null ? null : settings.getMaxSelections();
        if (selected < min) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La selección múltiple no alcanza el mínimo de opciones");
        }
        if (max != null && selected > max) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La selección múltiple supera el máximo de opciones");
        }
    }

    private void validateNumericValue(Question question, java.math.BigDecimal value) {
        QuestionSettings settings = question.getSettings();
        if (settings == null) {
            return;
        }
        if (settings.getMinValue() != null && value.compareTo(settings.getMinValue()) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El valor está por debajo del mínimo permitido");
        }
        if (settings.getMaxValue() != null && value.compareTo(settings.getMaxValue()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El valor supera el máximo permitido");
        }
        if (settings.getStep() != null) {
            java.math.BigDecimal base = settings.getMinValue() == null ? java.math.BigDecimal.ZERO : settings.getMinValue();
            java.math.BigDecimal remainder = value.subtract(base).remainder(settings.getStep()).abs();
            if (remainder.compareTo(java.math.BigDecimal.ZERO) != 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El valor no respeta el step configurado");
            }
        }
    }

    private void validateOtherOption(QuestionOption option, String otherText) {
        if (option.isOther()) {
            if (otherText == null || otherText.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La opción OTHER requiere texto adicional");
            }
            if (otherText.length() > 500) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El texto de OTHER supera la longitud máxima");
            }
            return;
        }
        if (otherText != null && !otherText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo las opciones OTHER admiten texto adicional");
        }
    }

    private void reject(boolean incompatible) {
        if (incompatible) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La respuesta contiene campos incompatibles con el tipo de pregunta");
        }
    }

    private boolean hasOptions(SubmitAnswerRequestDto dto) {
        return !safeOptions(dto.selectedOptions()).isEmpty();
    }

    private boolean hasMatrix(SubmitAnswerRequestDto dto) {
        return !safeMatrixValues(dto.matrixValues()).isEmpty();
    }

    private boolean isEmpty(SubmitAnswerRequestDto dto) {
        return dto == null
            || (dto.textValue() == null
            && dto.numericValue() == null
            && dto.booleanValue() == null
            && safeOptions(dto.selectedOptions()).isEmpty()
            && safeMatrixValues(dto.matrixValues()).isEmpty());
    }

    private String textValue(Question question, SubmitAnswerRequestDto dto) {
        return question.getQuestionType() == QuestionType.TEXT
            ? dto.textValue()
            : null;
    }

    private java.math.BigDecimal numericValue(Question question, SubmitAnswerRequestDto dto) {
        return question.getQuestionType() == QuestionType.NUMBER
            || question.getQuestionType() == QuestionType.SCALE
            ? dto.numericValue()
            : null;
    }

    private Boolean booleanValue(Question question, SubmitAnswerRequestDto dto) {
        return question.getQuestionType() == QuestionType.BOOLEAN ? dto.booleanValue() : null;
    }

    private ParticipationResponseDto toResponse(Participation participation) {
        List<AnswerResponseDto> answers = answerRepository.findByParticipationId(participation.getId())
            .stream()
            .map(answer -> new AnswerResponseDto(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getTextValue(),
                answer.getNumericValue(),
                answer.getBooleanValue(),
                answer.getSelectedOptions().stream().map(selected -> selected.getOption().getId()).toList(),
                answer.getMatrixValues().stream().map(value -> value.getMatrixRow().getId()).toList()
            ))
            .toList();
        return new ParticipationResponseDto(
            participation.getId(),
            participation.getSurvey().getPublicCode(),
            participation.getSubmittedAt(),
            answers
        );
    }

    private List<OptionAnswerRequestDto> safeOptions(List<OptionAnswerRequestDto> values) {
        return values == null ? List.of() : values;
    }

    private List<MatrixAnswerRequestDto> safeMatrixValues(List<MatrixAnswerRequestDto> values) {
        return values == null ? List.of() : values;
    }

    private String normalizePublicCode(String publicCode) {
        if (publicCode == null || publicCode.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada");
        }
        return publicCode.trim().toUpperCase(Locale.ROOT);
    }
}
