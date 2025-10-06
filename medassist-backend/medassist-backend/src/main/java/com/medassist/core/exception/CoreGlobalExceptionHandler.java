package com.medassist.core.exception;

import com.medassist.core.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@ControllerAdvice(basePackages = "com.medassist.core")
public class CoreGlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CoreGlobalExceptionHandler.class);

    /**
     * Handle validation errors for request body
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        String traceId = generateTraceId();

        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldError(
                    error.getField(),
                    error.getRejectedValue(),
                    error.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Invalid request data"
        );
        errorResponse.setDetails("One or more fields contain invalid values");
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setFieldErrors(fieldErrors);
        errorResponse.setTraceId(traceId);

        logger.warn("Validation error [{}]: {} field errors on {}",
            traceId, fieldErrors.size(), getRequestPath(request));

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle validation errors for request parameters
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        String traceId = generateTraceId();

        List<ApiErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ApiErrorResponse.FieldError(
                    violation.getPropertyPath().toString(),
                    violation.getInvalidValue(),
                    violation.getMessage()))
                .collect(Collectors.toList());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "CONSTRAINT_VIOLATION",
            "Request parameter validation failed"
        );
        errorResponse.setDetails(ex.getMessage());
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setFieldErrors(fieldErrors);
        errorResponse.setTraceId(traceId);

        logger.warn("Constraint violation [{}]: {} on {}",
            traceId, ex.getMessage(), getRequestPath(request));

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, WebRequest request) {

        String traceId = generateTraceId();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "MISSING_PARAMETER",
            "Required request parameter is missing"
        );
        errorResponse.setDetails(String.format("Parameter '%s' of type '%s' is required",
            ex.getParameterName(), ex.getParameterType()));
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(traceId);

        logger.warn("Missing parameter [{}]: {} on {}",
            traceId, ex.getParameterName(), getRequestPath(request));

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle type mismatch errors
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String traceId = generateTraceId();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "Unknown";

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "TYPE_MISMATCH",
            "Invalid parameter type"
        );
        errorResponse.setDetails(String.format("Parameter '%s' should be of type '%s'",
            ex.getName(), requiredType));
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(traceId);

        logger.warn("Type mismatch [{}]: {} on {}",
            traceId, ex.getMessage(), getRequestPath(request));

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle authentication errors
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        String traceId = generateTraceId();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "AUTHENTICATION_FAILED",
            "Authentication failed"
        );

        if (ex instanceof BadCredentialsException) {
            errorResponse.setDetails("Invalid username or password");
        } else {
            errorResponse.setDetails("Please provide valid authentication credentials");
        }

        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(traceId);

        logger.warn("Authentication failed [{}]: {} on {}",
            traceId, ex.getMessage(), getRequestPath(request));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle authorization errors
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        String traceId = generateTraceId();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "ACCESS_DENIED",
            "Access denied"
        );
        errorResponse.setDetails("You don't have permission to access this resource");
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(traceId);

        logger.warn("Access denied [{}]: {} on {}",
            traceId, ex.getMessage(), getRequestPath(request));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle file upload size exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, WebRequest request) {

        String traceId = generateTraceId();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "FILE_TOO_LARGE",
            "File size exceeds maximum limit"
        );
        errorResponse.setDetails("Maximum file size allowed is 10MB");
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(traceId);

        logger.warn("File too large [{}]: {} on {}",
            traceId, ex.getMessage(), getRequestPath(request));

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * Handle AI service errors
     */
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ApiErrorResponse> handleCompletionException(
            CompletionException ex, WebRequest request) {

        String traceId = generateTraceId();

        Throwable cause = ex.getCause();

        if (cause instanceof TimeoutException) {
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.REQUEST_TIMEOUT.value(),
                "AI_SERVICE_TIMEOUT",
                "AI service request timed out"
            );
            errorResponse.setDetails("The AI service took too long to respond. Please try again.");
            errorResponse.setPath(getRequestPath(request));
            errorResponse.setTraceId(traceId);

            logger.error("AI service timeout [{}]: {} on {}",
                traceId, ex.getMessage(), getRequestPath(request));

            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
        }

        return handleGenericException(ex, request);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        String traceId = generateTraceId();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_INPUT",
            "Invalid input provided"
        );
        errorResponse.setDetails(ex.getMessage());
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(traceId);

        logger.warn("Invalid input [{}]: {} on {}",
            traceId, ex.getMessage(), getRequestPath(request));

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        String traceId = generateTraceId();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred"
        );
        errorResponse.setDetails("Please contact support if the problem persists");
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setTraceId(traceId);

        logger.error("Unexpected error [{}]: {} on {}",
            traceId, ex.getMessage(), getRequestPath(request), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Generate unique trace ID for error tracking
     */
    private String generateTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);
        return traceId;
    }

    /**
     * Extract request path from WebRequest
     */
    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
