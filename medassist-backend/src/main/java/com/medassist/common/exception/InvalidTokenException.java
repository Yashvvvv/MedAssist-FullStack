package com.medassist.common.exception;

/**
 * Exception thrown when a JWT or verification token is invalid, expired, or malformed.
 */
public class InvalidTokenException extends RuntimeException {

    private final String tokenType;

    public InvalidTokenException(String message) {
        super(message);
        this.tokenType = "unknown";
    }

    public InvalidTokenException(String tokenType, String message) {
        super(message);
        this.tokenType = tokenType;
    }

    public String getTokenType() {
        return tokenType;
    }
}
