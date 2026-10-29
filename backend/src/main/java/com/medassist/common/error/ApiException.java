package com.medassist.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Base for every error we raise on purpose. Carries an HTTP status plus a stable,
 * machine-readable {@code code} that clients (the Android app) switch on, so a copy
 * change in the message never breaks a client.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final transient Map<String, Object> details;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ApiException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " not found");
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static ApiException unprocessable(String code, String message, Map<String, Object> details) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, code, message, details);
    }
}
