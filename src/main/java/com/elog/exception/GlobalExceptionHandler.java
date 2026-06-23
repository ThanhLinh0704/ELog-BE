package com.elog.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * Global exception handler — converts ALL exceptions to the standard error
 * envelope:
 * { "success": false, "error": { "code": "...", "message": "...", "details":
 * [...] } }
 *
 * See docs/api-conventions.md for the full response format spec.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Business rule violations ──────────────────────────────────────────────

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());
        return buildError(ex.getHttpStatus(), ex.getErrorCode().getCode(), ex.getMessage(), null);
    }

    // ── Bean Validation (@Valid) ───────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> {
                    String fieldName = fieldError.getField();
                    String defaultMessage = fieldError.getDefaultMessage();
                    try {
                        ErrorCode errorCode = ErrorCode.valueOf(defaultMessage);
                        return fieldName + ": " + errorCode.getCode();
                    } catch (IllegalArgumentException | NullPointerException e) {
                        return fieldName + ": " + defaultMessage;
                    }
                })
                .toList();
        return buildError(HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED.getCode(),
                "Validation failed", details);
    }

    // ── Security ──────────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN,
                ErrorCode.ACCESS_DENIED.getCode(),
                "You do not have permission to perform this action", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return buildError(HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                ex.getMessage(), null);
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR.getCode(),
                "An unexpected error occurred", null);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String code, String message, List<String> details) {

        var error = details != null
                ? Map.of("code", code, "message", message, "details", details)
                : Map.of("code", code, "message", message);

        return ResponseEntity.status(status)
                .body(Map.of("success", false, "error", error));
    }
}
