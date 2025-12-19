package com.medassist.common.exception;

/**
 * Exception thrown when authentication fails due to invalid credentials.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username/email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
