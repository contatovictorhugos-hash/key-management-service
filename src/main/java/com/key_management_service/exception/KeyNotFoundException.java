package com.key_management_service.exception;

public class KeyNotFoundException extends RuntimeException {
    public KeyNotFoundException(String message) {
        super(message);
    }
}
