package com.eventbooking.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String code;

    public ResourceNotFoundException() {
        super();
        this.code = "RESOURCE_NOT_FOUND";
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.code = "RESOURCE_NOT_FOUND";
    }

    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }
}
