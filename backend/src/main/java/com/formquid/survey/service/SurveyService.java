package com.formquid.survey.service;

import com.formquid.block.entity.Block;
import com.formquid.collaboration.entity.SurveyCollaborator;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.block.entity.BlockStatus;
import com.formquid.block.repository.BlockRepository;
import com.formquid.fixedquestion.dto.MatrixRowRequestDto;
import com.formquid.fixedquestion.dto.MatrixRowResponseDto;
import com.formquid.fixedquestion.dto.OptionConfigRequestDto;
import com.formquid.fixedquestion.dto.OptionConfigResponseDto;
import com.formquid.fixedquestion.entity.EvaluationDirection;
import com.formquid.fixedquestion.entity.FixedQuestion;
import com.formquid.fixedquestion.entity.FixedQuestionStatus;
import com.formquid.fixedquestion.entity.MatrixRowConfig;
import com.formquid.fixedquestion.entity.OptionConfig;
import com.formquid.fixedquestion.entity.QuestionType;
import com.formquid.fixedquestion.repository.FixedQuestionRepository;
import com.formquid.survey.dto.ChangeSurveyStatusRequestDto;
import com.formquid.survey.dto.CreateSurveyFromTemplateRequestDto;
import com.formquid.survey.dto.CreateSurveyRequestDto;
import com.formquid.survey.dto.SurveyBlockRequestDto;
import com.formquid.survey.dto.SurveyBlockResponseDto;
import com.formquid.survey.dto.SurveyQuestionRequestDto;
import com.formquid.survey.dto.SurveyQuestionResponseDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.dto.SurveySummaryResponseDto;
import com.formquid.survey.dto.UpdateSurveyRequestDto;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyBlock;
import com.formquid.survey.entity.SurveyQuestion;
import com.formquid.survey.entity.SurveyQuestionSource;
import com.formquid.survey.entity.SurveyStatus;
import com.formquid.survey.repository.SurveyQuestionRepository;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.tag.service.TagService;
import com.formquid.template.entity.SurveyTemplate;
import com.formquid.template.entity.TemplateBlock;
import com.formquid.template.entity.TemplateQuestion;
import com.formquid.template.entity.TemplateQuestionSource;
import com.formquid.template.entity.TemplateStatus;
import com.formquid.template.repository.SurveyTemplateRepository;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import com.formquid.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio principal de gestión autenticada de encuestas.
 *
 * Centraliza las operaciones realizadas por editores y administradores:
 * creación, edición, consultas privadas, cambios de estado, listados,
 * contadores y validaciones de permisos.
 *
 * El acceso anónimo de participantes se mantiene separado en
 * PublicSurveyService.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final FixedQuestionRepository fixedQuestionRepository;
    private final SurveyTemplateRepository surveyTemplateRepository;
    private final SurveyCollaboratorRepository surveyCollaboratorRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final BlockRepository blockRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crea una encuesta desde cero.
     *
     * La encuesta se crea inicialmente como borrador y queda asociada
     * al usuario autenticado como propietario.
     */
    @Transactional
    public SurveyResponseDto create(
        CreateSurveyRequestDto request,
        String ownerEmail
    ) {
        User owner = getAuthenticatedUser(ownerEmail);

        Survey survey = Survey.builder()
            .publicCode(generatePublicCode())
            .title(request.title().trim())
            .description(cleanOptional(request.description()))
            .imageUrl(cleanOptional(request.imageUrl()))
            .status(SurveyStatus.DRAFT)
            .owner(owner)
            .build();

        SurveyBuildContext context = buildContext(request.blocks());

        setTags(survey, request.tagNames(), owner.getId());
        buildBlocks(survey, request.blocks(), context);

        return toResponse(surveyRepository.save(survey));
    }

    /**
     * Crea una encuesta copiando la estructura de una plantilla activa.
     *
     * Las preguntas se copian en la encuesta para que futuros cambios en la
     * plantilla no alteren encuestas ya creadas.
     */
    @Transactional
    public SurveyResponseDto createFromTemplate(
        CreateSurveyFromTemplateRequestDto request,
        String ownerEmail
    ) {
        User owner = getAuthenticatedUser(ownerEmail);
        SurveyTemplate template = getActiveTemplate(request.templateId());

        validateTemplateHasContent(template);

        Survey survey = Survey.builder()
            .publicCode(generatePublicCode())
            .title(request.title().trim())
            .description(cleanOptional(request.description()))
            .imageUrl(cleanOptional(request.imageUrl()))
            .status(SurveyStatus.DRAFT)
            .owner(owner)
            .build();

        setTags(survey, request.tagNames(), owner.getId());
        copyTemplateBlocks(template, survey);

        return toResponse(surveyRepository.save(survey));
    }

    /**
     * Lista las encuestas creadas por el usuario autenticado.
     */
    public List<SurveyResponseDto> findMine(String ownerEmail) {
        User owner = getAuthenticatedUser(ownerEmail);

        return surveyRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Lista las encuestas en las que el usuario autenticado participa
     * como colaborador.
     */
    public List<SurveySummaryResponseDto> findCollaboratingSurveys(
        String editorEmail
    ) {
        User editor = getAuthenticatedUser(editorEmail);

        return surveyCollaboratorRepository.findByEditorId(editor.getId())
            .stream()
            .map(SurveyCollaborator::getSurvey)
            .map(this::toSummary)
            .toList();
    }

    /**
     * Lista todas las encuestas del sistema para administración.
     *
     * Si status es null devuelve todas; en caso contrario filtra por estado.
     */
    public List<SurveySummaryResponseDto> findAllForAdmin(
        SurveyStatus status
    ) {
        List<Survey> surveys = status == null
            ? surveyRepository.findAllByOrderByCreatedAtDesc()
            : surveyRepository.findByStatusOrderByCreatedAtDesc(status);

        return surveys.stream()
            .map(this::toSummary)
            .toList();
    }

    /**
     * Cuenta las encuestas del sistema.
     *
     * Si status es null devuelve el total.
     */
    public long count(SurveyStatus status) {
        return status == null
            ? surveyRepository.count()
            : surveyRepository.countByStatus(status);
    }

    /**
     * Obtiene una encuesta privada por id aplicando permisos.
     *
     * Puede consultarla el propietario, un colaborador, un administrador
     * o un superadministrador.
     */
    public SurveyResponseDto findById(
        Long surveyId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        assertCanViewSurvey(
            survey,
            user.getId(),
            user.getRole()
        );

        return toResponse(survey);
    }

    /**
     * Actualiza una encuesta en borrador.
     *
     * Puede editarla el propietario, un colaborador, un administrador
     * o un superadministrador. La encuesta debe estar en estado DRAFT.
     */
    @Transactional
    public SurveyResponseDto update(
        Long surveyId,
        UpdateSurveyRequestDto request,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        assertCanEditSurvey(
            survey,
            user.getId(),
            user.getRole()
        );
        validateDraft(survey);

        survey.setTitle(request.title().trim());
        survey.setDescription(cleanOptional(request.description()));
        survey.setImageUrl(cleanOptional(request.imageUrl()));

        setTags(survey, request.tagNames(), user.getId());

        if (request.blocks() != null) {
            replaceBlocks(survey, request.blocks());
        }

        return toResponse(survey);
    }

    /**
     * Cambia el estado de una encuesta.
     *
     * Solo pueden gestionar el estado el propietario, un administrador
     * o un superadministrador. Los colaboradores no pueden publicar,
     * ocultar ni cambiar la visibilidad global de la encuesta.
     */
    @Transactional
    public SurveyResponseDto changeStatus(
        Long surveyId,
        ChangeSurveyStatusRequestDto request,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Survey survey = getSurvey(surveyId);

        assertCanManageSurvey(
            survey,
            user.getId(),
            user.getRole()
        );

        SurveyStatus newStatus = request.status();

        if (
            newStatus == SurveyStatus.PUBLIC
            || newStatus == SurveyStatus.PRIVATE
        ) {
            validateReadyToPublish(survey.getId());
        }

        applyStatus(
            survey,
            newStatus,
            request.accessPassword()
        );

        return toResponse(survey);
    }

    /**
     * Recupera al usuario autenticado mediante el correo utilizado
     * como identidad en Spring Security.
     */
    private User getAuthenticatedUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario autenticado no encontrado"
            ));
    }

    /**
     * Comprueba si el usuario puede consultar una encuesta privada.
     */
    public void assertCanViewSurvey(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        validateAuthenticatedUser(userId, role);

        if (!canViewSurvey(survey, userId, role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para consultar esta encuesta"
            );
        }
    }

    /**
     * Comprueba si el usuario puede modificar el contenido de una encuesta.
     */
    public void assertCanEditSurvey(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        validateAuthenticatedUser(userId, role);

        if (!canEditSurvey(survey, userId, role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para modificar esta encuesta"
            );
        }
    }

    /**
     * Comprueba si el usuario puede gestionar aspectos sensibles,
     * como estado, visibilidad o colaboradores.
     */
    public void assertCanManageSurvey(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        validateAuthenticatedUser(userId, role);

        if (!canManageSurvey(survey, userId, role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para gestionar esta encuesta"
            );
        }
    }

    /**
     * Comprueba si el usuario puede consultar resultados.
     */
    public void assertCanViewResults(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        validateAuthenticatedUser(userId, role);

        if (!canViewResults(survey, userId, role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para consultar los resultados de esta encuesta"
            );
        }
    }

    /**
     * Indica si el usuario puede consultar una encuesta.
     */
    public boolean canViewSurvey(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        if (!isValidAccessCheck(survey, userId, role)) {
            return false;
        }

        return isPlatformManager(role)
            || isOwner(survey, userId)
            || isCollaborator(survey.getId(), userId);
    }

    /**
     * Indica si el usuario puede editar una encuesta.
     */
    public boolean canEditSurvey(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        return canViewSurvey(survey, userId, role);
    }

    /**
     * Indica si el usuario puede gestionar una encuesta.
     *
     * Los colaboradores no pueden cambiar estado ni administrar permisos.
     */
    public boolean canManageSurvey(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        if (!isValidAccessCheck(survey, userId, role)) {
            return false;
        }

        return isPlatformManager(role)
            || isOwner(survey, userId);
    }

    /**
     * Indica si el usuario puede consultar resultados.
     */
    public boolean canViewResults(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        return canViewSurvey(survey, userId, role);
    }

    /**
     * Indica si el rol tiene permisos globales de administración.
     */
    public boolean isPlatformManager(UserRole role) {
        return role == UserRole.ADMIN
            || role == UserRole.SUPERADMIN;
    }

    /**
     * Comprueba si el usuario es propietario de la encuesta.
     */
    private boolean isOwner(
        Survey survey,
        Long userId
    ) {
        return survey.getOwner() != null
            && survey.getOwner().getId() != null
            && survey.getOwner().getId().equals(userId);
    }

    /**
     * Comprueba si el usuario es colaborador de la encuesta.
     */
    private boolean isCollaborator(
        Long surveyId,
        Long userId
    ) {
        if (surveyId == null || userId == null) {
            return false;
        }

        return surveyCollaboratorRepository
            .existsBySurveyIdAndEditorId(
                surveyId,
                userId
            );
    }

    /**
     * Valida los datos de autenticación usados en las comprobaciones.
     */
    private void validateAuthenticatedUser(
        Long userId,
        UserRole role
    ) {
        if (userId == null || role == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }
    }

    /**
     * Comprueba que una validación de acceso disponga de datos mínimos.
     */
    private boolean isValidAccessCheck(
        Survey survey,
        Long userId,
        UserRole role
    ) {
        return survey != null
            && userId != null
            && role != null;
    }

    /**
     * Obtiene una encuesta interna por id.
     */
    private Survey getSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la encuesta indicada"
            ));
    }

    /**
     * Obtiene una plantilla activa.
     */
    private SurveyTemplate getActiveTemplate(Long templateId) {
        SurveyTemplate template = surveyTemplateRepository.findById(templateId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe la plantilla indicada"
            ));

        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se pueden usar plantillas activas"
            );
        }

        return template;
    }

    /**
     * Comprueba que la encuesta esté en borrador.
     */
    private void validateDraft(Survey survey) {
        if (!survey.isEditable()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se pueden modificar encuestas en borrador"
            );
        }
    }

    /**
     * Comprueba que una encuesta tenga preguntas sin cargar toda su estructura.
     */
    private void validateReadyToPublish(Long surveyId) {
        if (!surveyQuestionRepository.existsBySurveyId(surveyId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La encuesta debe contener al menos una pregunta antes de publicarse"
            );
        }
    }

    /**
     * Valida que una plantilla tenga estructura antes de copiarla.
     */
    private void validateTemplateHasContent(SurveyTemplate template) {
        if (template.getBlocks() == null || template.getBlocks().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La plantilla no contiene bloques o grupos de preguntas"
            );
        }
    }

    /**
     * Aplica el cambio de estado y gestiona la contraseña privada si procede.
     */
    private void applyStatus(
        Survey survey,
        SurveyStatus newStatus,
        String accessPassword
    ) {
        if (newStatus == SurveyStatus.PRIVATE) {
            applyPrivateStatus(survey, accessPassword);
        } else {
            survey.setAccessPassword(null);
        }

        survey.setStatus(newStatus);

        if ((newStatus == SurveyStatus.PUBLIC || newStatus == SurveyStatus.PRIVATE)
            && survey.getPublishedAt() == null) {
            survey.setPublishedAt(LocalDateTime.now());
        }

        if (newStatus == SurveyStatus.DRAFT) {
            survey.setPublishedAt(null);
        }
    }

    /**
     * Configura una encuesta como privada.
     *
     * Si ya tenía contraseña y no se envía una nueva, conserva la existente.
     */
    private void applyPrivateStatus(
        Survey survey,
        String accessPassword
    ) {
        if (!StringUtils.hasText(accessPassword)
            && !StringUtils.hasText(survey.getAccessPassword())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contraseña es obligatoria para una encuesta privada"
            );
        }

        if (StringUtils.hasText(accessPassword)) {
            survey.setAccessPassword(passwordEncoder.encode(accessPassword.trim()));
        }
    }

    /**
     * Asocia etiquetas libres a una encuesta.
     */
    private void setTags(
        Survey survey,
        List<String> tagNames,
        Long userId
    ) {
        survey.getTags().clear();
        survey.getTags().addAll(tagService.getOrCreateTags(tagNames, userId));
    }

    /**
     * Reemplaza la estructura completa de bloques y preguntas.
     *
     * Se fuerza el borrado de la estructura anterior antes de insertar las
     * nuevas posiciones para evitar conflictos con restricciones únicas.
     */
    private void replaceBlocks(
        Survey survey,
        List<SurveyBlockRequestDto> blockRequests
    ) {
        SurveyBuildContext context = buildContext(blockRequests);

        survey.getBlocks().clear();
        surveyRepository.flush();

        buildBlocks(survey, blockRequests, context);
    }

    /**
     * Construye un contexto de creación resolviendo bloques y preguntas fijas
     * en lote, antes de entrar en los bucles de construcción.
     */
    private SurveyBuildContext buildContext(List<SurveyBlockRequestDto> blockRequests) {
        Set<Long> blockIds = collectSourceBlockIds(blockRequests);
        Set<Long> fixedQuestionIds = collectFixedQuestionIds(blockRequests);

        return new SurveyBuildContext(
            resolveUsableBlocks(blockIds),
            resolveActiveFixedQuestions(fixedQuestionIds)
        );
    }

    /**
     * Extrae los identificadores de bloques reutilizables usados en el DTO.
     */
    private Set<Long> collectSourceBlockIds(List<SurveyBlockRequestDto> blockRequests) {
        Set<Long> blockIds = new LinkedHashSet<>();

        if (blockRequests == null) {
            return blockIds;
        }

        for (SurveyBlockRequestDto blockRequest : blockRequests) {
            if (blockRequest.sourceBlockId() != null) {
                blockIds.add(blockRequest.sourceBlockId());
            }
        }

        return blockIds;
    }

    /**
     * Extrae los identificadores de preguntas fijas usadas en el DTO.
     */
    private Set<Long> collectFixedQuestionIds(List<SurveyBlockRequestDto> blockRequests) {
        Set<Long> questionIds = new LinkedHashSet<>();

        if (blockRequests == null) {
            return questionIds;
        }

        for (SurveyBlockRequestDto blockRequest : blockRequests) {
            if (blockRequest.questions() == null) {
                continue;
            }

            for (SurveyQuestionRequestDto questionRequest : blockRequest.questions()) {
                if (questionRequest.source() == SurveyQuestionSource.FIXED
                    && questionRequest.sourceFixedQuestionId() != null) {
                    questionIds.add(questionRequest.sourceFixedQuestionId());
                }
            }
        }

        return questionIds;
    }

    /**
     * Recupera y valida en lote los bloques reutilizables usados.
     */
    private Map<Long, Block> resolveUsableBlocks(Collection<Long> blockIds) {
        Map<Long, Block> blocksById = new HashMap<>();

        if (blockIds.isEmpty()) {
            return blocksById;
        }

        for (Block block : blockRepository.findByIdIn(blockIds)) {
            blocksById.put(block.getId(), block);
        }

        Set<Long> blockIdsWithActiveQuestions =
            fixedQuestionRepository.findBlockIdsWithActiveQuestions(
                blockIds,
                FixedQuestionStatus.ACTIVE
            );

        for (Long blockId : blockIds) {
            Block block = blocksById.get(blockId);

            if (block == null) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe el bloque indicado"
                );
            }

            if (block.getStatus() != BlockStatus.ACTIVE) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden usar bloques activos"
                );
            }

            if (!blockIdsWithActiveQuestions.contains(blockId)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El bloque indicado debe contener al menos una pregunta fija activa"
                );
            }
        }

        return blocksById;
    }

    /**
     * Recupera y valida en lote las preguntas fijas usadas.
     */
    private Map<Long, FixedQuestion> resolveActiveFixedQuestions(Collection<Long> questionIds) {
        Map<Long, FixedQuestion> questionsById = new HashMap<>();

        if (questionIds.isEmpty()) {
            return questionsById;
        }

        for (FixedQuestion question : fixedQuestionRepository.findByIdIn(questionIds)) {
            questionsById.put(question.getId(), question);
        }

        for (Long questionId : questionIds) {
            FixedQuestion question = questionsById.get(questionId);

            if (question == null) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe la pregunta fija indicada"
                );
            }

            if (question.getStatus() != FixedQuestionStatus.ACTIVE) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden usar preguntas fijas activas"
                );
            }
        }

        return questionsById;
    }

    /**
     * Construye los bloques de una encuesta desde un DTO.
     */
    private void buildBlocks(
        Survey survey,
        List<SurveyBlockRequestDto> blockRequests,
        SurveyBuildContext context
    ) {
        if (blockRequests == null || blockRequests.isEmpty()) {
            return;
        }

        int position = 1;

        for (SurveyBlockRequestDto blockRequest : blockRequests) {
            SurveyBlock block = buildBlock(blockRequest, position, context);
            survey.addBlock(block);
            position++;
        }
    }

    /**
     * Construye un bloque o grupo de preguntas.
     */
    private SurveyBlock buildBlock(
        SurveyBlockRequestDto request,
        int position,
        SurveyBuildContext context
    ) {
        Block sourceBlock = null;

        if (request.sourceBlockId() != null) {
            sourceBlock = context.blocksById().get(request.sourceBlockId());
        }

        SurveyBlock block = SurveyBlock.builder()
            .sourceBlock(sourceBlock)
            .title(resolveBlockTitle(request.title(), sourceBlock))
            .position(position)
            .build();

        buildQuestions(block, request.questions(), context);

        return block;
    }

    /**
     * Resuelve el título visible de un bloque.
     */
    private String resolveBlockTitle(
        String title,
        Block sourceBlock
    ) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }

        if (sourceBlock != null) {
            return sourceBlock.getName();
        }

        return null;
    }

    /**
     * Construye las preguntas de un bloque de encuesta.
     */
    private void buildQuestions(
        SurveyBlock block,
        List<SurveyQuestionRequestDto> questionRequests,
        SurveyBuildContext context
    ) {
        if (questionRequests == null || questionRequests.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cada bloque debe contener al menos una pregunta"
            );
        }

        int position = 1;

        for (SurveyQuestionRequestDto questionRequest : questionRequests) {
            SurveyQuestion question = buildQuestion(block, questionRequest, position, context);
            block.addQuestion(question);
            position++;
        }
    }

    /**
     * Construye una pregunta según su origen.
     */
    private SurveyQuestion buildQuestion(
        SurveyBlock block,
        SurveyQuestionRequestDto request,
        int position,
        SurveyBuildContext context
    ) {
        if (request.source() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El origen de la pregunta es obligatorio"
            );
        }

        return switch (request.source()) {
            case FIXED -> buildFixedQuestion(block, request, position, context);
            case CUSTOM -> buildCustomQuestion(request, position);
            case TEMPLATE -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de plantilla solo se generan al crear una encuesta desde plantilla"
            );
        };
    }

    /**
     * Construye una pregunta copiando una pregunta fija activa.
     */
    private SurveyQuestion buildFixedQuestion(
        SurveyBlock block,
        SurveyQuestionRequestDto request,
        int position,
        SurveyBuildContext context
    ) {
        if (request.sourceFixedQuestionId() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta fija de origen es obligatoria"
            );
        }

        if (request.sourceTemplateQuestionId() != null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Una pregunta fija no debe indicar pregunta de plantilla"
            );
        }

        FixedQuestion fixedQuestion =
            context.fixedQuestionsById().get(request.sourceFixedQuestionId());

        validateFixedQuestionBlock(block, fixedQuestion);

        return copyFixedQuestion(
            fixedQuestion,
            SurveyQuestionSource.FIXED,
            null,
            fixedQuestion.isRequiredByDefault(),
            position
        );
    }

    /**
     * Construye una pregunta propia de encuesta.
     */
    private SurveyQuestion buildCustomQuestion(
        SurveyQuestionRequestDto request,
        int position
    ) {
        if (request.sourceFixedQuestionId() != null || request.sourceTemplateQuestionId() != null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas propias no deben indicar pregunta de origen"
            );
        }

        if (!StringUtils.hasText(request.text())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El enunciado de la pregunta es obligatorio"
            );
        }

        if (request.type() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El tipo de pregunta es obligatorio"
            );
        }

        List<OptionConfig> options = buildOptions(request.options());
        List<MatrixRowConfig> matrixRows = buildMatrixRows(request.matrixRows());

        validateQuestionConfig(
            request.type(),
            options,
            matrixRows,
            request.minValue(),
            request.maxValue(),
            request.leftLabel(),
            request.rightLabel()
        );

        return SurveyQuestion.builder()
            .source(SurveyQuestionSource.CUSTOM)
            .text(request.text().trim())
            .description(cleanOptional(request.description()))
            .type(request.type())
            .evaluationDirection(resolveEvaluationDirection(request.evaluationDirection()))
            .required(Boolean.TRUE.equals(request.required()))
            .minValue(request.minValue())
            .maxValue(request.maxValue())
            .scaleDisplayType(request.scaleDisplayType())
            .leftLabel(cleanOptional(request.leftLabel()))
            .rightLabel(cleanOptional(request.rightLabel()))
            .options(options)
            .matrixRows(matrixRows)
            .position(position)
            .build();
    }

    /**
     * Valida que una pregunta fija se incluya dentro del bloque adecuado.
     */
    private void validateFixedQuestionBlock(
        SurveyBlock block,
        FixedQuestion fixedQuestion
    ) {
        Block sourceBlock = block.getSourceBlock();
        Block fixedQuestionBlock = fixedQuestion.getBlock();

        if (sourceBlock == null && fixedQuestionBlock != null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta fija pertenece a un bloque y debe usarse dentro de ese bloque"
            );
        }

        if (sourceBlock != null && fixedQuestionBlock == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta fija no pertenece al bloque indicado"
            );
        }

        if (sourceBlock != null
            && fixedQuestionBlock != null
            && !sourceBlock.getId().equals(fixedQuestionBlock.getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La pregunta fija no pertenece al bloque indicado"
            );
        }
    }

    /**
     * Copia los bloques y preguntas de una plantilla a una encuesta.
     */
    private void copyTemplateBlocks(
        SurveyTemplate template,
        Survey survey
    ) {
        int blockPosition = 1;

        for (TemplateBlock templateBlock : template.getBlocks()) {
            validateTemplateBlockHasQuestions(templateBlock);

            Block sourceBlock = templateBlock.getBlock();

            SurveyBlock surveyBlock = SurveyBlock.builder()
                .sourceBlock(sourceBlock)
                .title(sourceBlock != null ? sourceBlock.getName() : null)
                .position(blockPosition)
                .build();

            int questionPosition = 1;

            for (TemplateQuestion templateQuestion : templateBlock.getQuestions()) {
                SurveyQuestion surveyQuestion = copyTemplateQuestion(
                    templateQuestion,
                    questionPosition
                );

                surveyBlock.addQuestion(surveyQuestion);
                questionPosition++;
            }

            survey.addBlock(surveyBlock);
            blockPosition++;
        }
    }

    /**
     * Valida que un bloque de plantilla tenga preguntas.
     */
    private void validateTemplateBlockHasQuestions(TemplateBlock templateBlock) {
        if (templateBlock.getQuestions() == null || templateBlock.getQuestions().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La plantilla contiene un grupo sin preguntas"
            );
        }
    }

    /**
     * Copia una pregunta de plantilla dentro de una encuesta.
     */
    private SurveyQuestion copyTemplateQuestion(
        TemplateQuestion templateQuestion,
        int position
    ) {
        if (templateQuestion.getSource() == TemplateQuestionSource.FIXED) {
            return copyTemplateFixedQuestion(templateQuestion, position);
        }

        return copyTemplateCustomQuestion(templateQuestion, position);
    }

    /**
     * Copia una pregunta fija usada en una plantilla.
     */
    private SurveyQuestion copyTemplateFixedQuestion(
        TemplateQuestion templateQuestion,
        int position
    ) {
        FixedQuestion fixedQuestion = templateQuestion.getFixedQuestion();

        if (fixedQuestion == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La plantilla contiene una pregunta fija inválida"
            );
        }

        if (fixedQuestion.getStatus() != FixedQuestionStatus.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La plantilla contiene una pregunta fija que ya no está activa"
            );
        }

        return copyFixedQuestion(
            fixedQuestion,
            SurveyQuestionSource.TEMPLATE,
            templateQuestion,
            templateQuestion.isRequired(),
            position
        );
    }

    /**
     * Copia una pregunta propia de plantilla.
     */
    private SurveyQuestion copyTemplateCustomQuestion(
        TemplateQuestion templateQuestion,
        int position
    ) {
        return SurveyQuestion.builder()
            .source(SurveyQuestionSource.TEMPLATE)
            .sourceTemplateQuestion(templateQuestion)
            .text(templateQuestion.getText())
            .description(templateQuestion.getDescription())
            .type(templateQuestion.getType())
            .evaluationDirection(templateQuestion.getEvaluationDirection())
            .required(templateQuestion.isRequired())
            .minValue(templateQuestion.getMinValue())
            .maxValue(templateQuestion.getMaxValue())
            .scaleDisplayType(templateQuestion.getScaleDisplayType())
            .leftLabel(templateQuestion.getLeftLabel())
            .rightLabel(templateQuestion.getRightLabel())
            .options(copyOptions(templateQuestion.getOptions()))
            .matrixRows(copyMatrixRows(templateQuestion.getMatrixRows()))
            .position(position)
            .build();
    }

    /**
     * Copia una pregunta fija a una encuesta.
     */
    private SurveyQuestion copyFixedQuestion(
        FixedQuestion fixedQuestion,
        SurveyQuestionSource source,
        TemplateQuestion sourceTemplateQuestion,
        boolean required,
        int position
    ) {
        return SurveyQuestion.builder()
            .source(source)
            .sourceFixedQuestion(fixedQuestion)
            .sourceTemplateQuestion(sourceTemplateQuestion)
            .text(fixedQuestion.getText())
            .description(fixedQuestion.getDescription())
            .type(fixedQuestion.getType())
            .evaluationDirection(fixedQuestion.getEvaluationDirection())
            .required(required)
            .minValue(fixedQuestion.getMinValue())
            .maxValue(fixedQuestion.getMaxValue())
            .scaleDisplayType(fixedQuestion.getScaleDisplayType())
            .leftLabel(fixedQuestion.getLeftLabel())
            .rightLabel(fixedQuestion.getRightLabel())
            .options(copyOptions(fixedQuestion.getOptions()))
            .matrixRows(copyMatrixRows(fixedQuestion.getMatrixRows()))
            .position(position)
            .build();
    }

    /**
     * Valida la configuración de una pregunta propia.
     */
    private void validateQuestionConfig(
        QuestionType type,
        List<OptionConfig> options,
        List<MatrixRowConfig> matrixRows,
        Integer minValue,
        Integer maxValue,
        String leftLabel,
        String rightLabel
    ) {
        validateScaleRange(minValue, maxValue);

        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE) {
            validateChoiceOptions(options);
        } else if (!options.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Este tipo de pregunta no admite opciones"
            );
        }

        if (type == QuestionType.MATRIX_SCALE) {
            validateMatrixRows(matrixRows);
        } else if (!matrixRows.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Este tipo de pregunta no admite filas de matriz"
            );
        }

        if (type == QuestionType.BIPOLAR_SCALE
            && (!StringUtils.hasText(leftLabel) || !StringUtils.hasText(rightLabel))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La escala bipolar requiere etiqueta izquierda y derecha"
            );
        }
    }

    /**
     * Valida que el rango numérico sea coherente.
     */
    private void validateScaleRange(
        Integer minValue,
        Integer maxValue
    ) {
        if (minValue != null && maxValue != null && minValue >= maxValue) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El valor mínimo debe ser menor que el valor máximo"
            );
        }
    }

    /**
     * Valida las opciones de una pregunta de selección.
     */
    private void validateChoiceOptions(List<OptionConfig> options) {
        if (options.size() < 2) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de selección deben tener al menos dos opciones"
            );
        }
    }

    /**
     * Valida las filas de una pregunta de matriz.
     */
    private void validateMatrixRows(List<MatrixRowConfig> matrixRows) {
        if (matrixRows.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las preguntas de matriz deben tener al menos una fila"
            );
        }
    }

    /**
     * Construye las opciones configurables de una pregunta.
     */
    private List<OptionConfig> buildOptions(List<OptionConfigRequestDto> optionRequests) {
        if (optionRequests == null || optionRequests.isEmpty()) {
            return new ArrayList<>();
        }

        List<OptionConfig> options = new ArrayList<>();
        Set<String> usedValues = new HashSet<>();

        for (OptionConfigRequestDto optionRequest : optionRequests) {
            if (!StringUtils.hasText(optionRequest.label())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La etiqueta de la opción es obligatoria"
                );
            }

            String label = optionRequest.label().trim();
            String storedValue = resolveStoredValue(optionRequest.storedValue(), label);

            if (!usedValues.add(storedValue)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La pregunta contiene opciones repetidas"
                );
            }

            options.add(OptionConfig.builder()
                .label(label)
                .storedValue(storedValue)
                .scoreValue(optionRequest.scoreValue())
                .nonResponseOption(Boolean.TRUE.equals(optionRequest.nonResponseOption()))
                .allowsFreeText(Boolean.TRUE.equals(optionRequest.allowsFreeText()))
                .build());
        }

        return options;
    }

    /**
     * Construye las filas de una pregunta de matriz.
     */
    private List<MatrixRowConfig> buildMatrixRows(List<MatrixRowRequestDto> rowRequests) {
        if (rowRequests == null || rowRequests.isEmpty()) {
            return new ArrayList<>();
        }

        List<MatrixRowConfig> rows = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();

        for (MatrixRowRequestDto rowRequest : rowRequests) {
            if (!StringUtils.hasText(rowRequest.label())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La etiqueta de la fila de matriz es obligatoria"
                );
            }

            String label = rowRequest.label().trim();
            String key = StringUtils.hasText(rowRequest.key())
                ? rowRequest.key().trim()
                : UUID.randomUUID().toString();

            if (!usedKeys.add(key)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La pregunta contiene filas de matriz repetidas"
                );
            }

            rows.add(MatrixRowConfig.builder()
                .key(key)
                .label(label)
                .build());
        }

        return rows;
    }

    /**
     * Copia una lista de opciones para evitar compartir referencias.
     */
    private List<OptionConfig> copyOptions(List<OptionConfig> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }

        return source.stream()
            .map(option -> OptionConfig.builder()
                .label(option.getLabel())
                .storedValue(option.getStoredValue())
                .scoreValue(option.getScoreValue())
                .nonResponseOption(option.isNonResponseOption())
                .allowsFreeText(option.isAllowsFreeText())
                .build())
            .toList();
    }

    /**
     * Copia una lista de filas de matriz para evitar compartir referencias.
     */
    private List<MatrixRowConfig> copyMatrixRows(List<MatrixRowConfig> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }

        return source.stream()
            .map(row -> MatrixRowConfig.builder()
                .key(row.getKey())
                .label(row.getLabel())
                .build())
            .toList();
    }

    /**
     * Genera el código público de acceso a la encuesta.
     *
     * La unicidad queda protegida por la restricción UNIQUE de base de datos.
     */
    private String generatePublicCode() {
        return UUID.randomUUID().toString();
    }

    /**
     * Resuelve el valor interno de una opción.
     */
    private String resolveStoredValue(
        String storedValue,
        String label
    ) {
        if (StringUtils.hasText(storedValue)) {
            return storedValue.trim();
        }

        return normalize(label);
    }

    /**
     * Normaliza un texto para usarlo como valor interno.
     */
    private String normalize(String value) {
        String normalized = Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD);

        return normalized
            .replaceAll("\\p{M}", "")
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_|_$", "");
    }

    /**
     * Devuelve una dirección de evaluación por defecto.
     */
    private EvaluationDirection resolveEvaluationDirection(EvaluationDirection direction) {
        return direction != null ? direction : EvaluationDirection.NEUTRAL;
    }

    /**
     * Construye un DTO resumido para listados privados y administrativos.
     */
    private SurveySummaryResponseDto toSummary(Survey survey) {
        return new SurveySummaryResponseDto(
            survey.getId(),
            survey.getPublicCode(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageUrl(),
            survey.getStatus(),
            survey.getOwner().getId(),
            survey.getOwner().getName(),
            toTagNames(survey),
            survey.getCreatedAt(),
            survey.getUpdatedAt(),
            survey.getPublishedAt()
        );
    }

    /**
     * Construye la respuesta privada de encuesta.
     */
    private SurveyResponseDto toResponse(Survey survey) {
        return new SurveyResponseDto(
            survey.getId(),
            survey.getPublicCode(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getImageUrl(),
            survey.getStatus(),
            survey.getOwner().getId(),
            survey.getOwner().getName(),
            toTagNames(survey),
            toBlockResponses(survey.getBlocks()),
            survey.getCreatedAt(),
            survey.getUpdatedAt(),
            survey.getPublishedAt()
        );
    }

    /**
     * Convierte las etiquetas de la encuesta en una lista de nombres ordenados.
     */
    private List<String> toTagNames(Survey survey) {
        return survey.getTags()
            .stream()
            .map(tag -> tag.getName())
            .sorted()
            .toList();
    }

    /**
     * Convierte bloques de entidad a DTO.
     */
    private List<SurveyBlockResponseDto> toBlockResponses(List<SurveyBlock> blocks) {
        if (blocks == null) {
            return List.of();
        }

        return blocks.stream()
            .sorted(Comparator.comparing(SurveyBlock::getPosition))
            .map(this::toBlockResponse)
            .toList();
    }

    /**
     * Convierte un bloque de entidad a DTO.
     */
    private SurveyBlockResponseDto toBlockResponse(SurveyBlock block) {
        return new SurveyBlockResponseDto(
            block.getId(),
            block.getSourceBlock() != null ? block.getSourceBlock().getId() : null,
            block.getTitle(),
            block.getPosition(),
            toQuestionResponses(block.getQuestions())
        );
    }

    /**
     * Convierte preguntas de entidad a DTO.
     */
    private List<SurveyQuestionResponseDto> toQuestionResponses(List<SurveyQuestion> questions) {
        if (questions == null) {
            return List.of();
        }

        return questions.stream()
            .sorted(Comparator.comparing(SurveyQuestion::getPosition))
            .map(this::toQuestionResponse)
            .toList();
    }

    /**
     * Convierte una pregunta de entidad a DTO.
     */
    private SurveyQuestionResponseDto toQuestionResponse(SurveyQuestion question) {
        return new SurveyQuestionResponseDto(
            question.getId(),
            question.getSource(),
            question.getSourceFixedQuestion() != null
                ? question.getSourceFixedQuestion().getId()
                : null,
            question.getSourceTemplateQuestion() != null
                ? question.getSourceTemplateQuestion().getId()
                : null,
            question.getText(),
            question.getDescription(),
            question.getType(),
            question.getEvaluationDirection(),
            question.isRequired(),
            question.getMinValue(),
            question.getMaxValue(),
            question.getScaleDisplayType(),
            question.getLeftLabel(),
            question.getRightLabel(),
            toOptionResponses(question.getOptions()),
            toMatrixRowResponses(question.getMatrixRows()),
            question.getPosition()
        );
    }

    /**
     * Convierte opciones de entidad a DTO.
     */
    private List<OptionConfigResponseDto> toOptionResponses(List<OptionConfig> options) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
            .map(option -> new OptionConfigResponseDto(
                option.getLabel(),
                option.getStoredValue(),
                option.getScoreValue(),
                option.isNonResponseOption(),
                option.isAllowsFreeText()
            ))
            .toList();
    }

    /**
     * Convierte filas de matriz de entidad a DTO.
     */
    private List<MatrixRowResponseDto> toMatrixRowResponses(List<MatrixRowConfig> rows) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
            .map(row -> new MatrixRowResponseDto(
                row.getKey(),
                row.getLabel()
            ))
            .toList();
    }

    /**
     * Limpia textos opcionales.
     */
    private String cleanOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    /**
     * Contexto interno con entidades ya cargadas en lote.
     */
    private record SurveyBuildContext(
        Map<Long, Block> blocksById,
        Map<Long, FixedQuestion> fixedQuestionsById
    ) {}
}