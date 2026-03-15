package com.project.backend.exception;

public class InvalidReturnStateException extends BusinessException {
    public InvalidReturnStateException(String message) {
        super(message, "INVALID_RETURN_STATE");
    }
}