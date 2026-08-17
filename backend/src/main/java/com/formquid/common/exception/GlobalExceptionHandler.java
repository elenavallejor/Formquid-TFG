package com.formquid.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones de la API.
 *
 * Centraliza la transformación de excepciones en respuestas HTTP
 * homogéneas y evita que errores controlados de negocio terminen
 * devolviendo incorrectamente un código 500.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 401 Unauthorized.
     *
     * Gestiona errores producidos durante el proceso de autenticación,
     * como credenciales incorrectas o usuarios no autenticados.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
        AuthenticationException ex,
        HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            ex.getMessage(),
            request.getServletPath()
        );

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(error);
    }

    /**
     * 403 Forbidden.
     *
     * Gestiona los intentos de acceso a recursos para los que
     * el usuario autenticado no dispone de permisos suficientes.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            "No tienes permisos para realizar esta acción",
            request.getServletPath()
        );

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(error);
    }

    /**
     * Conserva el código HTTP indicado explícitamente por los servicios.
     *
     * Esta excepción se utiliza en el backend para representar errores
     * controlados como 400, 401, 403 o 404. Sin este handler acabarían
     * siendo capturados por Exception y convertidos incorrectamente en 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
        ResponseStatusException ex,
        HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(
            ex.getStatusCode().value()
        );

        ApiErrorResponse error = new ApiErrorResponse(
            status.value(),
            status.getReasonPhrase(),
            ex.getReason() != null
                ? ex.getReason()
                : status.getReasonPhrase(),
            request.getServletPath()
        );

        return ResponseEntity
            .status(status)
            .body(error);
    }

    /**
     * 400 Bad Request.
     *
     * Gestiona errores producidos por las anotaciones de validación
     * de los DTOs, por ejemplo @NotBlank, @NotNull o @Size.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FieldValidationErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();

        for (
            FieldError fieldError
                : ex.getBindingResult().getFieldErrors()
        ) {
            fieldErrors.put(
                fieldError.getField(),
                fieldError.getDefaultMessage()
            );
        }

        FieldValidationErrorResponse error =
            new FieldValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Error de validación en los campos enviados",
                request.getServletPath(),
                fieldErrors
            );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * 400 Bad Request.
     *
     * Gestiona reglas de negocio o argumentos inválidos detectados
     * explícitamente en entidades y servicios.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getServletPath()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * 400 Bad Request.
     *
     * Gestiona JSON mal formado, valores incompatibles con enums
     * o tipos que Jackson no puede convertir al DTO esperado.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "El cuerpo de la petición no tiene un formato válido",
            request.getServletPath()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * 400 Bad Request.
     *
     * Gestiona parámetros de URL o query parameters cuyo tipo
     * no coincide con el esperado por el controlador.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatchException(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "El parámetro '" + ex.getName()
                + "' contiene un valor no válido",
            request.getServletPath()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * 500 Internal Server Error.
     *
     * Este handler debe ejecutarse únicamente para errores inesperados.
     * La excepción completa se registra en el log del servidor para
     * facilitar su diagnóstico sin revelar detalles internos al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error(
            "Error interno no controlado en {} {}",
            request.getMethod(),
            request.getServletPath(),
            ex
        );

        ApiErrorResponse error = new ApiErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "Ha ocurrido un error interno e inesperado en el servidor",
            request.getServletPath()
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}