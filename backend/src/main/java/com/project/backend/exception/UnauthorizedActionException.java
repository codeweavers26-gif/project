package com.project.backend.exception;

public class UnauthorizedActionException extends BusinessException {
    public UnauthorizedActionException(String message) {
        super(message, "UNAUTHORIZED_ACTION");
    }
}