package com.medassist.common.exception;

/**
 * Exception thrown when a business rule validation fails.
 * Examples: email already verified, user is not a healthcare provider, etc.
 */
public class BusinessValidationException extends RuntimeException {

    private final String errorCode;

    public BusinessValidationException(String message) {
        super(message);
        this.errorCode = "BUSINESS_VALIDATION_ERROR";
    }

    public BusinessValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
