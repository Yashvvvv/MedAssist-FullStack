package com.medassist.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "com.medassist")
public class BackendGlobalExceptionHandler {

    // ==================== Custom Application Exceptions ====================

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User registration conflict: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), "USER_ALREADY_EXISTS");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND");
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        log.warn("Invalid token ({}): {}", ex.getTokenType(), ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_TOKEN");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.debug("Invalid credentials attempt");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_CREDENTIALS");
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessValidation(BusinessValidationException ex) {
        log.debug("Business validation failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrorCode());
    }

    // ==================== Validation Exceptions ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.debug("Validation errors found: {}", ex.getBindingResult().getFieldErrorCount());

        Map<String, String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
                    (existing, replacement) -> existing
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
            "message", "Validation failed",
            "code", "VALIDATION_ERROR",
            "details", validationErrors,
            "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.badRequest().body(response);
    }

    // ==================== Security Exceptions ====================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(@SuppressWarnings("unused") BadCredentialsException ex) {
        log.debug("Bad credentials authentication attempt");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username/email or password", "INVALID_CREDENTIALS");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabledException(DisabledException ex) {
        log.debug("Disabled account access attempt: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, 
            "Account not verified. Please check your email for a verification link.", 
            "ACCOUNT_NOT_VERIFIED");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.debug("Username not found: {}", ex.getMessage());

        if (ex.getMessage().contains("User not verified")) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, 
                "Email not verified. Please check your email for a verification link.",
                "EMAIL_NOT_VERIFIED");
        }
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", "INVALID_CREDENTIALS");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(@SuppressWarnings("unused") AuthenticationException ex) {
        log.debug("Generic authentication failure");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", "AUTHENTICATION_ERROR");
    }

    // ==================== Fallback Handlers ====================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.warn("Unhandled RuntimeException: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
            ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
            "RUNTIME_ERROR");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log the actual exception for debugging (never expose to client)
        log.error("Unexpected exception: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
            "An unexpected error occurred. Please try again later.",
            "INTERNAL_ERROR");
    }

    // ==================== Helper Methods ====================

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message, String code) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
            "message", message,
            "code", code,
            "timestamp", System.currentTimeMillis()
        ));
        return ResponseEntity.status(status).body(response);
    }
}
