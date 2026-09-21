package com.example.store.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(resourceType + " " + resourceId + " not found");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
}
