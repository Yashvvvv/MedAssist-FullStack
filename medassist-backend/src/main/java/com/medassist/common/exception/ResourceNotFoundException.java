package com.medassist.common.exception;

/**
 * Exception thrown when a requested resource (user, medicine, pharmacy, etc.) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String identifier;

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.identifier = null;
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public ResourceNotFoundException(String resourceType, String fieldName, String identifier) {
        super(String.format("%s not found with %s: %s", resourceType, fieldName, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(String.format("%s not found with id: %d", resourceType, id));
        this.resourceType = resourceType;
        this.identifier = String.valueOf(id);
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getIdentifier() {
        return identifier;
    }
}
