package com.medassist.common.exception;

/**
 * Exception thrown when attempting to register a user that already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {

    private final String field;
    private final String value;

    public UserAlreadyExistsException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }

    public UserAlreadyExistsException(String field, String value) {
        super(String.format("%s '%s' is already registered", field, value));
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
