package com.formquid.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Manejador global de excepciones de la API.
 *
 * Centraliza la respuesta ante errores de validación, permisos,
 * reglas de negocio y problemas de base de datos.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gestiona errores lanzados manualmente desde los servicios.
     *
     * @param exception excepción recibida.
     * @param request petición HTTP.
     * @return respuesta de error normalizada.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
        ResponseStatusException exception,
        HttpServletRequest request
    ) {
        HttpStatus status = resolveHttpStatus(exception.getStatusCode());
        String message = exception.getReason() != null
            ? exception.getReason()
            : "Error en la petición";

        return buildResponse(status, message, request);
    }

    /**
     * Gestiona errores de validación producidos por @Valid en DTOs.
     *
     * @param exception excepción de validación.
     * @param request petición HTTP.
     * @return respuesta con los campos inválidos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<FieldValidationErrorResponse> fieldErrors = exception
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldValidationErrorResponse(
                error.getField(),
                error.getDefaultMessage()
            ))
            .toList();

        ApiErrorResponse response = ApiErrorResponse.withFieldErrors(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Existen campos inválidos en la petición",
            request.getRequestURI(),
            fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gestiona errores de validación sobre parámetros.
     *
     * @param exception excepción de validación.
     * @param request petición HTTP.
     * @return respuesta con los campos o parámetros inválidos.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        List<FieldValidationErrorResponse> fieldErrors = exception
            .getConstraintViolations()
            .stream()
            .map(violation -> new FieldValidationErrorResponse(
                violation.getPropertyPath().toString(),
                violation.getMessage()
            ))
            .toList();

        ApiErrorResponse response = ApiErrorResponse.withFieldErrors(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Existen parámetros inválidos en la petición",
            request.getRequestURI(),
            fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gestiona JSON mal formado o cuerpos de petición no legibles.
     *
     * @param exception excepción recibida.
     * @param request petición HTTP.
     * @return respuesta de error.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "El cuerpo de la petición no tiene un formato válido",
            request
        );
    }

    /**
     * Gestiona errores de integridad de base de datos.
     *
     * Incluye duplicados, claves foráneas, campos obligatorios y otras
     * restricciones definidas en PostgreSQL.
     *
     * @param exception excepción de integridad.
     * @param request petición HTTP.
     * @return respuesta de error.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        String message = resolveDataIntegrityMessage(exception);

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            message,
            request
        );
    }

    /**
     * Gestiona errores de conexión con la base de datos.
     *
     * @param exception excepción de conexión.
     * @param request petición HTTP.
     * @return respuesta de error.
     */
    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<ApiErrorResponse> handleCannotCreateTransactionException(
        CannotCreateTransactionException exception,
        HttpServletRequest request
    ) {
        log.error("No se pudo abrir una transacción con la base de datos", exception);

        return buildResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "No se ha podido conectar con la base de datos",
            request
        );
    }

    /**
     * Gestiona otros errores de acceso a datos.
     *
     * @param exception excepción de acceso a datos.
     * @param request petición HTTP.
     * @return respuesta de error.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccessException(
        DataAccessException exception,
        HttpServletRequest request
    ) {
        log.error("Error de acceso a la base de datos", exception);

        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Se produjo un error al acceder a la base de datos",
            request
        );
    }

    /**
     * Gestiona accesos autenticados sin permisos suficientes.
     *
     * @param exception excepción de permisos.
     * @param request petición HTTP.
     * @return respuesta de error.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.FORBIDDEN,
            "No tienes permisos para realizar esta operación",
            request
        );
    }

    /**
     * Gestiona errores no controlados.
     *
     * @param exception excepción inesperada.
     * @param request petición HTTP.
     * @return respuesta de error genérica.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
        Exception exception,
        HttpServletRequest request
    ) {
        log.error("Error no controlado", exception);

        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Se produjo un error inesperado en el servidor",
            request
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
        HttpStatus status,
        String message,
        HttpServletRequest request
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus resolveHttpStatus(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());

        return status != null
            ? status
            : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException exception) {
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(exception);
        String detail = rootCause.getMessage() != null
            ? rootCause.getMessage().toLowerCase()
            : "";

        if (detail.contains("uk_users_email")) {
            return "Ya existe un usuario registrado con ese correo electrónico";
        }

        if (detail.contains("uk_tags_name")) {
            return "Ya existe una etiqueta con ese nombre";
        }

        if (detail.contains("foreign key")) {
            return "No se puede completar la operación porque el registro está relacionado con otros datos";
        }

        if (detail.contains("not-null") || detail.contains("null value")) {
            return "Faltan datos obligatorios para completar la operación";
        }

        return "No se ha podido guardar la información porque incumple una restricción de la base de datos";
    }
}