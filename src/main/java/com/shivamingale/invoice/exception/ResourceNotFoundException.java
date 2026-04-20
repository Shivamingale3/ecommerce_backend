package com.shivamingale.invoice.exception;

public class ResourceNotFoundException extends NotFoundException {

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resource, Object id) {
        super("%s not found with id %s".formatted(resource, id), "RESOURCE_NOT_FOUND");
    }
}
