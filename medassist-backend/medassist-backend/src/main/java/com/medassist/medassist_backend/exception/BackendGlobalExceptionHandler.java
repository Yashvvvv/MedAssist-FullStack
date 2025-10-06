package com.medassist.medassist_backend.exception;

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

@RestControllerAdvice(basePackages = "com.medassist.medassist_backend")
public class BackendGlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Add logging to confirm this handler is being called
        System.out.println("BackendGlobalExceptionHandler: Handling MethodArgumentNotValidException");
        System.out.println("Validation errors found: " + ex.getBindingResult().getFieldErrorCount());

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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(@SuppressWarnings("unused") BadCredentialsException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
            "message", "Invalid username/email or password",
            "code", "INVALID_CREDENTIALS",
            "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabledException(DisabledException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
            "message", "Account not verified. Please check your email for a verification link.",
            "code", "ACCOUNT_NOT_VERIFIED",
            "timestamp", System.currentTimeMillis()
        ));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        if (ex.getMessage().contains("User not verified")) {
            response.put("error", Map.of(
                "message", "Email not verified. Please check your email for a verification link.",
                "code", "EMAIL_NOT_VERIFIED",
                "timestamp", System.currentTimeMillis()
            ));
        } else {
            response.put("error", Map.of(
                "message", "Invalid credentials",
                "code", "INVALID_CREDENTIALS",
                "timestamp", System.currentTimeMillis()
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(@SuppressWarnings("unused") AuthenticationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
            "message", "Authentication failed",
            "code", "AUTHENTICATION_ERROR",
            "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
            "message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
            "code", "RUNTIME_ERROR",
            "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log the actual exception for debugging
        System.err.println("Unexpected exception in BackendGlobalExceptionHandler: " + ex.getClass().getSimpleName());
        System.err.println("Exception message: " + ex.getMessage());
        ex.printStackTrace();

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
            "message", "An unexpected error occurred. Please try again later.",
            "code", "INTERNAL_ERROR",
            "exceptionType", ex.getClass().getSimpleName(), // Add for debugging
            "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
