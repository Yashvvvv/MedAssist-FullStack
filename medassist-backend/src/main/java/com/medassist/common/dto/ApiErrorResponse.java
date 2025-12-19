package com.medassist.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Standard API error response")
public class ApiErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    @JsonProperty("status")
    private int status;

    @Schema(description = "Error code for client handling", example = "VALIDATION_ERROR")
    @JsonProperty("error_code")
    private String errorCode;

    @Schema(description = "Human-readable error message", example = "Invalid request data")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Detailed error description for debugging", example = "Field 'latitude' must be between -90 and 90")
    @JsonProperty("details")
    private String details;

    @Schema(description = "Request path that caused the error", example = "/api/v1/pharmacies/location/nearby")
    @JsonProperty("path")
    private String path;

    @Schema(description = "Timestamp when error occurred")
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Field-specific validation errors")
    @JsonProperty("field_errors")
    private List<FieldError> fieldErrors;

    @Schema(description = "Unique trace ID for error tracking")
    @JsonProperty("trace_id")
    private String traceId;

    public static class FieldError {
        @Schema(description = "Field name", example = "latitude")
        @JsonProperty("field")
        private String field;

        @Schema(description = "Rejected value")
        @JsonProperty("rejected_value")
        private Object rejectedValue;

        @Schema(description = "Error message", example = "must be between -90 and 90")
        @JsonProperty("message")
        private String message;

        public FieldError() {}

        public FieldError(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        // Getters and setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public Object getRejectedValue() { return rejectedValue; }
        public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public ApiErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiErrorResponse(int status, String errorCode, String message) {
        this();
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
    }

    // Getters and setters
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public List<FieldError> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(List<FieldError> fieldErrors) { this.fieldErrors = fieldErrors; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
